// based on: http://note19.com/2007/05/27/javascript-guid-generator/
Util = {
	S4: function() { return (((1+Math.random())*0x10000)|0).toString(16).substring(1); },
	S12 : function(){ return Util.S4()+Util.S4()+Util.S4();}
};

function UploadProgressReporter(form,options){
	this.form  = form;
	this.panel = $("#" + options.panel_id);
	this.target = $("#" + options.target_id);
	this.message_url = "{URL}/message".replace("{URL}",options.upload_url);
	
	this.template_progress  = "<progress value=\"{VALUE}\" max=\"100\"/> Status {VALUE} %";
	this.template_completed = "Status: 100%. Upload completed. click <a href=\"{LINK}\">here</a> to download"
}

UploadProgressReporter.prototype = {
	show_start : function(){
		this.form.hide();
		
		this.panel.html(this.template_progress.replace(/{VALUE}/g,0));
	},
	show_progress: function(data){
		
		this.panel.html(this.template_progress.replace(/{VALUE}/g,data.percentage));	
	},
	show_completed: function(data){
		
		this.panel.html(this.template_completed.replace(/{LINK}/g,data.link));
		this.target.attr('action',this.message_url);
		$('input[type=submit]',this.target).removeAttr('disabled');
	},
	show_error: function(){
		this.panel.html('Ops... some error happens with this request, please try again');
	}
}

function UploadProgressObserver(form,options){
	this.form          = form;
	this.options       = options;
	this.last_progress = -1.0;
	this.upr           = new UploadProgressReporter(form,options);
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
		if(data.status == "error"){
			this.on_error()
		} if(data.status === "complete" || data.percentage === 100.0){
			this.on_complete(data);
		} else if (data.percentage > this.last_progress){
			this.on_progress(data);
		} 
	},
	on_error : function (){
		this._stopAjaxLoop(this.interval_id);
		this.upr.show_error();
	},
	on_ajax_error : function(jqXHR, textStatus, errorThrown){
		this._stopAjaxLoop(this.interval_id);
		//console.log("ops..." , textStatus, errorThrown);
	},	
	load : function(){
		var self = this;
		total=0;
        $.ajax({
            cache: false,
            dataType: 'json',
            url: self._generateStatusUrl(),
			success: function(data, textStatus){self.on_ajax_success(data, textStatus)},
			error: function(jqXHR, textStatus, errorThrown){self.on_ajax_error(jqXHR, textStatus, errorThrown)}
        });
	},
	_generateStatusUrl : function(){
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
		var iframe;
		try {
			// to avoid IE problems...
		    iframe = document.createElement('<iframe name="' + this.iframe_id + '">');
		} catch (ex) {
		    iframe = document.createElement('iframe');
		    iframe.name= this.iframe_id;
		}	
		$(iframe)
			.attr({ id : this.iframe_id})
			.width(0).height(0).css('border','none').hide()
			.appendTo(this.form.parent());
			
		return this;
	},
	_getFileName : function(){
		// fix becouse IE does not understand the old code
		return $(this.file).val().split(/[/\\]/).reverse()[0];
	},
	_id : function(){
		return Util.S12();	
	},
	_generateUploadUrl : function(){	
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