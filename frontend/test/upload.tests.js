function main(){
	module("UplPipe");
	
	test("UplPipe Constructor should be initialize file, option and iframe_id attributes", function(){
        expect(3);
        var file = $('<input type="file"/>');
        
		var options = {
			baseurl : "/scalaExamples/upload",
			target_id : "the-form",
			interval : 2000,
			panel_id : "panel"
		};
		
		var uplpipe = new UplPipe(file,options);
		
        equals(uplpipe.file , file);
        equals(uplpipe.options, options);
        equals(uplpipe.iframe_id, "uplpipe-temp");  
    });
	
	test("method generateForm should create a form tag outside to input file", function(){
		expect(1);
		
		var file = $('<input type="file"/>');
        
		var options = {
			baseurl : "/scalaExamples/upload",
			target_id : "the-form",
			interval : 2000,
			panel_id : "panel"
		};
		
		var uplpipe = new UplPipe(file,options).generateForm();
		
		equals($('<form><input type="file"/></form>').html(), uplpipe.form.html() );
	});
	
	test("method generateIFrame should create an iframe with id, name and 0 size", function(){
		expect(4);
		
		var file = $('<input type="file"/>');
        
		var options = {
			baseurl : "/scalaExamples/upload",
			target_id : "the-form",
			interval : 2000,
			panel_id : "panel"
		};
		
		var uplpipe = new UplPipe(file,options)
			.generateForm()
			.generateIFrame();
		
		var form = $(uplpipe.form);
		equals($('iframe',form).attr('id'),uplpipe.iframe_id);
		equals($('iframe',form).attr('name'),uplpipe.iframe_id);
		equals($('iframe',form).width(),0);
		equals($('iframe',form).height(),0);
	});
	
	test("method prepareForm should set action, method, enctype and target attributes",function(){
		expect(5);
		
		var file = $('<input type="file"/>');
        
		var options = {
			baseurl : "/scalaExamples/upload",
			target_id : "the-form",
			interval : 2000,
			panel_id : "panel"
		};
		
		var uplpipe = new UplPipe(file,options)
			.generateForm()
			.generateIFrame();
		
		// mock _generateUploadUrl
		uplpipe._generateUploadUrl = function() { return "/a/b/c/d"; };	
		uplpipe.prepareForm();
			
		var form = $(uplpipe.form);	

		equals(form.attr('action'),uplpipe._generateUploadUrl());
		equals(form.attr('target'), uplpipe.iframe_id);
		equals(form.attr('method'),'post');
		equals(form.attr('enctype'),'multipart/form-data');
		equals(form.attr('encoding'),'multipart/form-data');	
	});

	test("method on_file_change calls 4 methods in cascade",function(){
		expect(4);
		
		var file = $('<input type="file"/>');
        
		var options = {
			baseurl : "/scalaExamples/upload",
			target_id : "the-form",
			interval : 2000,
			panel_id : "panel"
		};
		
		var uplpipe = new UplPipe(file,options);
		
		var call_gf = false, call_gi = false, call_pf = false, call_sf = false;
		
		// lots of mocks...
		uplpipe.generateForm   = function() { call_gf = true; return this;};
		uplpipe.generateIFrame = function() { call_gi = true; return this;};
		uplpipe.prepareForm    = function() { call_pf = true; return this;};
		uplpipe.submitForm     = function() { call_sf = true };
		
		uplpipe.on_file_change();
		
		ok(call_gf,"generateForm called");
		ok(call_gi,"generateIFrame called");
		ok(call_pf,"prepareForm called");
		ok(call_sf,"submitForm called");
	});
	
	test("method init should attach on_file_change in change event of file object and return this", function(){
		expect(2);
		var file = $('<input type="file"/>');
        
		var options = {
			baseurl : "/scalaExamples/upload",
			target_id : "the-form",
			interval : 2000,
			panel_id : "panel"
		};
		
		var uplpipe = new UplPipe(file,options);
		var call_ofc = false;
		
		uplpipe.on_file_change = function(){
			call_ofc = true;
		};
		
		var z = uplpipe.init();
		
		ok(z == uplpipe);
		
		file.change();
		
		ok(call_ofc,"on_file_change called");
	});
	
	test("method _generateUploadUrl should create an url using random numbers",function(){
		expect(1);
		var file = $('<input type="file"/>');
        
		var options = {
			baseurl : "/scalaExamples/upload",
			target_id : "the-form",
			interval : 2000,
			panel_id : "panel"
		};
		
		var uplpipe = new UplPipe(file,options);
		
		uplpipe._id = function(){ return "fecobebecafe"; };
		uplpipe._getFileName = function(){ return "test.pdf";};
		var url = uplpipe._generateUploadUrl();
		
		equals(url,"/scalaExamples/upload/fecobebecafe-test.pdf");
	});
	
	test("method _getFileName should return the filename", function(){
		expect(1);
		var file =  [ { files : [ {fileName : "/dev/null"} ] } ] ;
		var uplpipe = new UplPipe(file,{});
		equals('/dev/null',uplpipe._getFileName(),"it is not possible test by setting some value using javascript (security)");
	});
	
	test("method _id should return a different 12bit hex number", function(){
		expect(3);	
		var uplpipe = new UplPipe(null,{});
		
		var id1 = uplpipe._id();
		var id2 = uplpipe._id();
		
		ok(/[0-9a-fA-F]{12}/.test(id1), "id = " + id1 + ", is a 12 char hex");
		ok(/[0-9a-fA-F]{12}/.test(id2), "id = " + id2 + ", is a 12 char hex");

		notEqual(id1,id2,"maybe it can be false, but rare!");
	});
	
	test("method submitForm should submit the form", function(){
		expect(1);
		var file = $('<input type="file"/>');
        
		var options = {
			baseurl : "/scalaExamples/upload",
			target_id : "the-form",
			interval : 2000,
			panel_id : "panel"
		};
		
		var uplpipe = new UplPipe(file,options);

		var submit_ok = false;
		uplpipe.form = { submit : function(){ submit_ok = true; } };

		uplpipe.submitForm();
		
		ok(submit_ok);
	});
	
	test("method prepareForm should attach on_form_submit in submit event", function(){
		expect(1);
		var file = $('<input type="file"/>');
        
		var options = {
			baseurl : "/scalaExamples/upload",
			target_id : "the-form",
			interval : 2000,
			panel_id : "panel"
		};
		
		var uplpipe = new UplPipe(file,options);
		uplpipe.form = $('<form></form>');
		
		var call_on_form_submit = false;
		
		uplpipe.on_form_submit = function(){ 
			call_on_form_submit = true; 
			return false;
		};
		uplpipe._generateUploadUrl = function(){ return "http://google.com/x/"; }
		
		uplpipe.prepareForm();
			
		uplpipe.form.submit();
		
		ok(call_on_form_submit);
	});
	
	test("method on_form_submit should call create_upload_progress_observer and init this", function(){
		expect(2);
		var file = $('<input type="file"/>');
        
		var options = {
			baseurl : "/scalaExamples/upload",
			target_id : "the-form",
			interval : 2000,
			panel_id : "panel"
		};
		
		var uplpipe = new UplPipe(file,options);
		
		var call_cupo = false;
		var call_upo_init = false;
		
		uplpipe.create_upload_progress_observer = function(){
			call_cupo = true;
			return { 
				// returns a upload progress observer mock...
				init : function(){ 
					call_upo_init = true;
				} 
			};
		};
		
		uplpipe.on_form_submit();
		ok(call_cupo);
		ok(call_upo_init);
	});
	
	test("method create_upload_progress_observer should create an UploadProgressObserver reference", function(){
		expect(4);
		var file = $('<input type="file"/>');
        
		var options = {
			baseurl : "/scalaExamples/upload",
			target_id : "the-form",
			interval : 2000,
			panel_id : "panel"
		};
		
		var uplpipe = new UplPipe(file,options);
		uplpipe.form = $('<form></form>');

		var obj = uplpipe.create_upload_progress_observer();
		
		equals(obj.form,uplpipe.form);
		equals(obj.options,options);
		equals(obj.last_progress,-1.0);
		equals(typeof(obj.init),"function");
	});
	
	module('UploadProgressObserver');
	
	test("constructor should init form, options and last_progress attribute", function(){
		expect(4);
		
		var form = $('<form></form>');
		var options = {
			baseurl : "/scalaExamples/upload",
			target_id : "the-form",
			interval : 2000,
			timeout: 2000,
			panel_id : "panel",
			upload_url : "/scalaExamples/upload/aabbccdd1234-test.pdf"
		};
		
		var upo = new UploadProgressObserver(form,options);
		equals(upo.form , form);
		equals(upo.options, options);
		equals(upo.last_progress,-1.0);
		equals(typeof(upo.upr), "object");
	});
	
	test("method _generateStatusUrl should concat /status at the end of url", function(){
		expect(1);
		
		var form = $('<form></form>');
		var options = {
			baseurl : "/scalaExamples/upload",
			target_id : "the-form",
			interval : 2000,
			panel_id : "panel",
			upload_url : "/scalaExamples/upload/aabbccdd1234-test.pdf"
		};
		
		var upo = new UploadProgressObserver(form,options);
		var url = upo._generateStatusUrl();
		equals("/scalaExamples/upload/aabbccdd1234-test.pdf/status",url);
	});
	
	asyncTest("method load starts ajax and should call on_ajax_success if success", function(){
        expect(1);
        var json_response = {
            percentage: 50,
        };

        $.mockjax({ // mock ajax request
            url: "/scalaExamples/upload/aabbccdd1234-test.pdf/status",
            dataType: "json",
            response: function(){
                start(); // for asyncTest
            },
            responseTime: 0,
            responseText: json_response
        });

        var form = $('<form></form>');
		var options = {
			baseurl : "/scalaExamples/upload",
			target_id : "the-form",
			interval : 2000,
			panel_id : "panel",
			upload_url : "/scalaExamples/upload/aabbccdd1234-test.pdf"
		};
		
		var upo = new UploadProgressObserver(form,options);
        upo.on_ajax_success = function(data){
            equals(json_response.percentage, data.percentage);
        }

        upo.load();
    });
	
	asyncTest("method load starts ajax and should call on_error if error", function(){
        expect(1);

        $.mockjax({ // mock ajax request
            url: "/scalaExamples/upload/aabbccdd1235-test.pdf/status",
            response: function(){
                start(); // for asyncTest
            },
            status: 500,
            responseTime: 0,
			responseText: "Internal Server Error"
        });

        var form = $('<form></form>');
		var options = {
			baseurl : "/scalaExamples/upload",
			target_id : "the-form",
			interval : 2000,
			panel_id : "panel",
			upload_url : "/scalaExamples/upload/aabbccdd1235-test.pdf"
		};
		
		var upo = new UploadProgressObserver(form,options);
		
        upo.on_error = function(j,a,b){
            equals(a, "error");
        }

        upo.load();
    });	

	test("method on_ajax_success should call on on_progress if percentage > last_progress", function(){
		expect(1);
		
		var form = $('<form></form>');
		var options = {
			baseurl : "/scalaExamples/upload",
			target_id : "the-form",
			interval : 2000,
			panel_id : "panel",
			upload_url : "/scalaExamples/upload/aabbccdd1234-test.pdf"
		};
		
		var data = { percentage : 50 };
		var upo = new UploadProgressObserver(form,options);		
		
		upo.on_progress = function(_data){
			equals(_data,data);
		};
		
		upo.on_ajax_success(data);
	});
	
	test("method on_ajax_success should not call on on_progress if percentage <= last_progress", function(){
		expect(1);
		
		var form = $('<form></form>');
		var options = {
			baseurl : "/scalaExamples/upload",
			target_id : "the-form",
			interval : 2000,
			panel_id : "panel",
			upload_url : "/scalaExamples/upload/aabbccdd1234-test.pdf"
		};
		
		var data = { percentage : 50 };
		var upo = new UploadProgressObserver(form,options);		
		upo.last_progress = 51;
		
		var on_progress_call = false;
		upo.on_progress = function(_data){
			on_progress_call = true;
		};
		
		upo.on_ajax_success(data);
		
		ok(!on_progress_call, "should not call on_progress");
	});
	
	test("method on_ajax_success should call on_complete if percentage is 100%", function(){
		expect(1);
		
		var form = $('<form></form>');
		var options = {
			baseurl : "/scalaExamples/upload",
			target_id : "the-form",
			interval : 2000,
			panel_id : "panel",
			upload_url : "/scalaExamples/upload/aabbccdd1234-test.pdf"
		};
		
		var data = { percentage : 100 };
		var upo = new UploadProgressObserver(form,options);		
		
		upo.on_complete = function(_data){
			equals(_data,data);
		};
		
		upo.on_ajax_success(data);
	});	
	
	test("method on_ajax_success should call on_complete if status is complete", function(){
		expect(1);
		
		var form = $('<form></form>');
		var options = {
			baseurl : "/scalaExamples/upload",
			target_id : "the-form",
			interval : 2000,
			panel_id : "panel",
			upload_url : "/scalaExamples/upload/aabbccdd1234-test.pdf"
		};
		
		var data = { status : "complete" };
		var upo = new UploadProgressObserver(form,options);		
		
		upo.on_complete = function(_data){
			equals(_data,data);
		};
		
		upo.on_ajax_success(data);
	});		
		
	test("method on_complete should call _stopAjaxLoop and upr.show_completed", function(){
		expect(2);
		var form = $('<form></form>');
		var options = {
			baseurl : "/scalaExamples/upload",
			target_id : "the-form",
			interval : 2000,
			panel_id : "panel",
			upload_url : "/scalaExamples/upload/aabbccdd1234-test.pdf"
		};
		
		var data = { status : "complete" };
		var upo = new UploadProgressObserver(form,options);		
		
		var call_stopAjaxLoop = false;
		upo._stopAjaxLoop = function(){ call_stopAjaxLoop = true;}
		
		var call_upr_show_completed = false;
		upo.upr.show_completed = function(){ call_upr_show_completed = true; }
		
		upo.on_complete(data);
		
		ok(call_stopAjaxLoop);
		ok(call_upr_show_completed);
	});
	
	test("method on_progress should update last_progress and call upr.show_progress", function(){
		expect(3);
		var form = $('<form></form>');
		var options = {
			baseurl : "/scalaExamples/upload",
			target_id : "the-form",
			interval : 2000,
			panel_id : "panel",
			upload_url : "/scalaExamples/upload/aabbccdd1234-test.pdf"
		};
		
		var data = { percentage : 50 };
		var upo = new UploadProgressObserver(form,options);		
				
		var call_show_progress = false;
		upo.upr.show_progress = function(){ call_show_progress = true; }
		
		equal(upo.last_progress,-1.0);
		
		upo.on_progress(data);
		
		equal(upo.last_progress,data.percentage);
		ok(call_show_progress);		
		
	});
	test("method on_erro should call _stopAjaxLoop and report error", function(){
		expect(1);
		var form = $('<form></form>');
		var options = {
			baseurl : "/scalaExamples/upload",
			target_id : "the-form",
			interval : 2000,
			panel_id : "panel",
			upload_url : "/scalaExamples/upload/aabbccdd1234-test.pdf"
		};
		
		var data = { percentage : 50 };
		var upo = new UploadProgressObserver(form,options);		
		
		var call_stopAjaxLoop = false;
		upo._stopAjaxLoop = function(){ call_stopAjaxLoop = true;}
		
		upo.on_error(data);
		
		ok(call_stopAjaxLoop);
	});
	
	test("function init should call create_interval and upr.show_start",function(){
		expect(2);
		var form = $('<form></form>');
		var options = {
			baseurl : "/scalaExamples/upload",
			target_id : "the-form",
			interval : 2000,
			panel_id : "panel",
			upload_url : "/scalaExamples/upload/aabbccdd1234-test.pdf"
		};
		
		var upo = new UploadProgressObserver(form,options);	
		
		var call_create_interval = false;
		upo.create_interval = function(){ call_create_interval = true;};
		
		var call_upr_show_start = false;
		upo.upr.show_start = function(){call_upr_show_start = true;};
		
		upo.init();
		
		ok(call_create_interval);
		ok(call_upr_show_start);	
	});
	
	test("function _stopAjaxLoop should clean interval",function(){
		expect(1);
	});
	
	test('function create_interval should create interval', function(){
		expect(1);
	})

	module('Util');

	test("Util.S4 should return some random hex value", function(){
		expect(3);	
		
		var id1 = Util.S4();
		var id2 = Util.S4();
		
		ok(/[0-9a-fA-F]{4}/.test(id1), "id = " + id1 + ", is a 4 char hex");
		ok(/[0-9a-fA-F]{4}/.test(id2), "id = " + id2 + ", is a 4 char hex");

		notEqual(id1,id2,"maybe it can be false, but rare!");		
	});	
	
	test("Util.S12 should return some random hex value", function(){
		expect(3);	
		
		var id1 = Util.S12();
		var id2 = Util.S12();
		
		ok(/[0-9a-fA-F]{12}/.test(id1), "id = " + id1 + ", is a 12 char hex");
		ok(/[0-9a-fA-F]{12}/.test(id2), "id = " + id2 + ", is a 12 char hex");

		notEqual(id1,id2,"maybe it can be false, but rare!");		
	});
}

$(document).ready(main);