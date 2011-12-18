Util = {
	// based on: http://note19.com/2007/05/27/javascript-guid-generator/
	S4: function() { return (((1+Math.random())*0x10000)|0).toString(16).substring(1); },
	S12 : function(){ return Util.S4()+Util.S4()+Util.S4();}
};

function UploadProgressReporter(options){
	this.form  = options.form;
	this.panel = $("#" + options.panel_id);
	this.template_progress  = "Status {VALUE} %";
	this.template_completed = "Status: 100%. <a href=\"{LINK}\">Uploaded to here.</a>"
}

UploadProgressReporter.prototype = {
	show_start : function(){
		this.form.hide();
		
		this.panel.html(template_progress.replace("{VALUE}",0));
	},
	show_progress: function(data){
		
		this.panel.html(template_progress.replace("{VALUE}",data.percentage));	
	},
	show_completed: function(data){
		
		this.panel.html(template_completed.replace("{LINK}",data.link));
	}	
}

function UploadProgressObserver(form,options){
	this.form          = form;
	this.options       = options;
	this.last_progress = -1.0;
	this.upr           = new UploadProgressReporter(options);
}

UploadProgressObserver.prototype = {
	init : function(){
		this.upr.show_start();
		this.create_interval(this.options.interval);
	},
	create_interval : function(interval){
		var self = this;
			
		this.interval_id = setInterval(function(){ self.load(); } ,interval);
	},
	on_progress : function(data){
		this.last_progress = data.percentage;
		this.upr.show_progress(data);		
	},
	_stopAjaxLoop : function(id){
		clearInterval(id);
	},
	on_complete : function(data){
		this._stopAjaxLoop(this.interval_id);
		this.upr.show_completed(data);		
	},		
	on_ajax_success: function(data) {
		if(data.status === "complete" || data.percentage === 100.0){
			this.on_complete(data);
		} else if (data.percentage > this.last_progress){
			this.on_progress(data);
		} 
	},
	on_error : function(jqXHR, textStatus, errorThrown){
		this._stopAjaxLoop(this.interval_id);
		console.log("ops..." , textStatus, errorThrown);
	},	
	load : function(){
		var self = this;
		total=0;
        $.ajax({
            cache: false,
            dataType: 'json',
            url: self._generateStatusUrl(),
			success: function(data, textStatus){self.on_ajax_success(data, textStatus)},
			error: function(jqXHR, textStatus, errorThrown){self.on_error(jqXHR, textStatus, errorThrown)},
        });
	},
	_generateStatusUrl : function(){
		// to upload:
		// POST /upload/xxxx-someFile.zip
		// to watch the status upload:
		// GET /upload/xxxx-someFile.zip/status OR
		// GET /upload/xxxx-someFile.zip/status?timestamp  -- to avoid cache et all.
		// returns 
		//    { percentage : y, status: text, link : www}
		// where:
		//      y is the percentage progress
		// status is some status message (progress|complete|error)
		// link   is the url to download content
		// otherwise: 404
		
		var template = "{URL}/status";
		return template.replace("{URL}",this.options.upload_url);
	}	
};

function UplPipe(file,options){
	this.file      = file;
	this.iframe_id = "uplpipe-temp";
	this.options   = options;
}

UplPipe.prototype = {
	init: function(){	
		var self = this;
		this.file.change( function(){
			self.on_file_change();
		});
		return this;
	},
	on_file_change: function(){
		this
		.generateForm()
		.generateIFrame()
		.prepareForm()
		.submitForm();		
	},
	generateForm: function(){
		this.form = $('<form>');
		this.form.appendTo(this.file.parent());
		this.form.append( this.file );
		return this;	
	},
	generateIFrame: function(){
		$("<iframe>")
			.attr({ id : this.iframe_id, name : this.iframe_id})
			.width(0).height(0).css('border','none').hide()
			.appendTo(this.form);
		return this;
	},
	_getFileName : function(){
		return this.file[0].files[0].fileName;
	},
	_id : function(){
		return Util.S12();	
	},
	_generateUploadUrl : function(){	
		// upload url for someFile.zip
		// POST /baseurl/xxxxxxxxxxx-someFile.zip
		// where xxxx is a 12digit hexadecimal number
		// to avoid filename colision
		
		var template = "{BASEURL}/{ID}-{FILENAME}";
		this.options.upload_url = template
			.replace('{BASEURL}',this.options.baseurl)
			.replace('{ID}',this._id())
			.replace('{FILENAME}',this._getFileName());
		
		return this.options.upload_url;
	},
	
	prepareForm: function(){
		this.form.attr({
			action: this._generateUploadUrl(),
			target: this.iframe_id,
	 		method: "post",
	 		enctype: "multipart/form-data",
	 		encoding: "multipart/form-data"
		});	
		
		var self = this;
		
		this.form.submit(function(){ 
			return self.on_form_submit(); 
		});
		
		return this;		
	},
	create_upload_progress_observer : function(){
		return new UploadProgressObserver(this.form,this.options);	
	},
	
	on_form_submit: function(){
		
		this.create_upload_progress_observer().init();
		
		return true;
	},
	
	submitForm: function(){
		this.form.submit();
	}
}; 