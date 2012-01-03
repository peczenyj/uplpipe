Util = { // based on: http://note19.com/2007/05/27/javascript-guid-generator/
	S4  : function() { return (((1+Math.random())*0x10000)|0).toString(16).substring(1); },
	S12 : function(){ return Util.S4()+Util.S4()+Util.S4();}
};
function UplPipe(file,options){
	this.file      = file;
	this.form      = file.parent();
	this.options   = options;
	this.errors    = 0;
	this.max_errors= 5;
	this.last_progress = -1.0;
	this.templates = {
		resource : "{BASEURL}/upload/{ID}-{FILENAME}",
		status   : "{RESOURCE}/status",
		download : "{RESOURCE}/download",
		message  : "{RESOURCE}/message"
	};
}

UplPipe.prototype = {
	setupDOMObjects : function(){
		this.panel        = $('.panel');
		this.progress_div = $('#progress');
		this.progress_bar = $('progress');
		this.error        = $('#error');
		this.downloadLink = $('a#download-link');
		this.complete     = $('#complete');
		this.form2        = $('form#the-form');
		this.form2submit  = $('form#the-form input[type=submit]');
		this.percentage   = $('span#percentage');
		this.emitter      = $('#emitter');		
		this.iframe       = $('#iframe-hidden');
	},
	init: function(){
		this.setupDOMObjects();	
		var self = this;
		//this.iframe.load(function(){ self.iframeLoad();});
		this.file.change(function(){ self.prepareFormAndSubmit(); });
	},
	iframeLoad: function(){
		console.log(this.iframe.contents().find('html').html());
	},
	prepareFormAndSubmit: function(){
		this._generateUrls();
				
		this.form.attr('action', this.upload_url);	
		var self = this;
		this.form.submit(function(){ self.startAjaxPooling()});
		
		this.form.submit();
	},		
	_id : function(){ return Util.S12(); },
	_getFileName : function(){ return $(this.file).val().split(/[/\\]/).reverse()[0]; },
	_generateUrls : function(){	
		this.upload_url = this.templates.resource
			.replace('{ID}',this._id())		
			.replace('{BASEURL}',this.options.baseurl)
			.replace('{FILENAME}',this._getFileName());
			
		this.upload_status_url = this.templates.status.replace('{RESOURCE}',this.upload_url);
		this.upload_download_url = this.templates.download.replace('{RESOURCE}',this.upload_url);
		this.upload_message_url = this.templates.message.replace('{RESOURCE}',this.upload_url);
	},
	startAjaxPooling : function(){
		this.emitter.hide();
		this.panel.hide();
		this.progress_div.show();
		var self = this;
		this.interval_id = this._setInterval(function(){ self.ajaxPooling(); },this.options.interval);		
	},
	stopAjaxPooling: function(){
		this._clearInterval(this.interval_id);
	},
	ajaxPooling: function(){ 
		var self = this;
        $.ajax({
            cache: false,
            dataType: 'json',
            url: this.upload_status_url,
			success: function(d,t){ self.onAjaxSuccess(d,t); },
			error: function(j,t,e){ self.onAjaxError(j,t,e); }
        });
	},
	onAjaxSuccess: function(data, textStatus){
		if(data.status === "error"){
			this.emitErrorAndStopPooling();
		} else if (data.status === "completed"){
			this.emitComplete();
		} else if (data.percentage > this.last_progress){
			this.emitProgress(data.percentage);
		}
	},
	onAjaxError: function(jqXHR, textStatus, errorThrown){
		if(this.errors++ > this.max_errors){
			this.emitErrorAndStopPooling();
		}
	},
	emitErrorAndStopPooling : function(){
		this.stopAjaxPooling();
		this.panel.hide();	this.error.show();
	},
	emitComplete : function(){
		this.stopAjaxPooling();
		this.panel.hide();
		this.downloadLink.attr('href',this.upload_download_url);
		this.complete.show();
		this.form2.attr('action', this.upload_message_url);
		this.form2submit.removeAttr('disabled');
	},
	emitProgress: function(value){
		this.last_progress = value;	
		this.progress_bar.attr('value',value);
		this.percentage.html(value);
	},
	_setInterval : function(code, interval){
		return setInterval(code, interval); 
	},
	_clearInterval : function(id){
		clearInterval(id);
	}
};