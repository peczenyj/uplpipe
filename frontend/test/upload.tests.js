function main(){
	module("UplPipe");
	
	test("UplPipe Constructor should be initialize file with attributes", function(){
        expect(6);

		var form=$('<form><input type="file" name="f" id="f"/></form>');
        var file = form.find("#f");
        
		var options = {
			baseurl : "/basepath",
			interval : 2000,
		};
		
		var uplpipe = new UplPipe(file,options);
		
        equals(uplpipe.file , file);
        equals(uplpipe.options, options);
        equals(uplpipe.errors, 0);  
		equals(uplpipe.max_errors,5);
		equals(uplpipe.last_progress, -1.0);
		equals(uplpipe.form.html(),form.html());
    });
	
	test("method init should bind change event with prepareFormAndSubmit method ", function(){
		expect(2);
		
		var form=$('<form><input type="file" name="f" id="f"/></form>');
        var file = form.find("#f");
        
		var options = {
			baseurl : "/basepath",
			interval : 2000,
		};
		
		var uplpipe = new UplPipe(file,options);
		
		var prepare_called = false;
		uplpipe.prepareFormAndSubmit = function() {prepare_called = true; };
		var setup_called = false;
		uplpipe.setupDOMObjects = function(){ setup_called = true; };
		
		uplpipe.init();
		ok(setup_called, "setupDomObjects called");
		file.change();
		ok(prepare_called, "prepareFormAndSubmit called");
	});
	
	test("method setupDomObjects should attach some elements in object", function(){
		expect(9);
		
		var html = '<div>'
		+ '<div id="progress" class="panel">'
		+ '<p><progress value="0" max="100"/> Status: <span id="percentage">0</span>%</p>'
		+ '</div>'
		+ '<div id="complete" class="panel">'
		+ '<p>Status: 100%. Upload completed. click <a id="download-link" href="#">here</a> to download</p>'
		+ '</div>'
		+ '<div id="error" class="panel">'
		+ '<p>Ops... some error happens with this request, please try again</p>'
		+ '</div>'
		+ '</div>'
		+ '<div id="emitter">'
		+ '<form id="emitter-form" method="post" action="#" target="iframe-hidden" enctype="multipart/form-data" encoding="multipart/form-data" accept-charset="UTF-8">'
		+ '<input type="file" name="f" id="f"/>'
		+ '</form>'
		+ '</div>'
		+ '<div>'
		+ '<form id="the-form" action="#" method="post" accept-charset="utf-8">'
		+ '<textarea name="message" rows="5" cols="60"></textarea></p>'
		+ '<input type="submit" value="Continue &rarr;" disabled="true">'
		+ '</form>'
		+ '</div>' ;
			
		var form=$(html);
		$('body').append(form);
        var file = form.find("#f");
        
		var options = {
			baseurl : "/basepath",
			interval : 2000,
		};
		
		var uplpipe = new UplPipe(file,options);
		uplpipe.setupDOMObjects();
		
		ok(uplpipe.panel.html()        == $('.panel').html());
		ok(uplpipe.progress_div.html()     == $('#progress').html());
		ok(uplpipe.progress_bar.html()     == $('progress').html());
		
		ok(uplpipe.error.html()        == $('#error').html());
		ok(uplpipe.downloadLink.html() == $('a#download-link').html());
		ok(uplpipe.complete.html()     == $('#complete').html());
		ok(uplpipe.form2.html()        == $('form#the-form').html());
		ok(uplpipe.form2submit.html()  == $('form#the-form input[type=submit]').html());
		ok(uplpipe.percentage.html()   == $('span#percentage').html());
	});
	
	test("method prepareFormAndSubmit should call _generateUrls, change action in form and bind onSubmit event", function(){
		expect(3);
		
		var form=$('<form><input type="file" name="f" id="f"/></form>');
        var file = form.find("#f");
        
		var options = {
			baseurl : "/basepath",
			interval : 2000,
		};
		
		var uplpipe = new UplPipe(file,options);
		
		var gen_urls_calls = false;
		uplpipe._generateUrls = function() { gen_urls_calls = true };
		var onsubmit_calls = false; 
		uplpipe.startAjaxPooling = function() { onsubmit_calls = true; };
		uplpipe.upload_url = "#";
		
		uplpipe.prepareFormAndSubmit();
		
		form.submit();
		
		ok(gen_urls_calls, "_generateUrls called");
		ok(onsubmit_calls, "onSubmit called");
		equals(form.attr('action'),uplpipe.upload_url,"action should be '#'");
	});
	
	test("method _generateUrls should create urls",function(){
		expect(4);
		
		var form=$('<form><input type="file" name="f" id="f"/></form>');
        var file = form.find("#f");
        
		var options = {
			baseurl : "/basepath",
			interval : 2000,
		};
		
		var uplpipe = new UplPipe(file,options);
		uplpipe._getFileName = function(){ return "a.txt"; }
		uplpipe._id = function(){ return "ABCDABCD1234";}
		
		uplpipe._generateUrls();
		
		equals(uplpipe.upload_url,"/basepath/upload/ABCDABCD1234-a.txt");
		equals(uplpipe.upload_status_url,"/basepath/upload/ABCDABCD1234-a.txt/status");
		equals(uplpipe.upload_download_url,"/basepath/upload/ABCDABCD1234-a.txt/download");
		equals(uplpipe.upload_message_url,"/basepath/upload/ABCDABCD1234-a.txt/message");
	});

	test("method _getFileName should return the filename", function(){
		expect(1);
		var file =  $('<input value="/path/to/a.txt"/>');
		var uplpipe = new UplPipe(file,{});
		equals('a.txt',uplpipe._getFileName(),"it is not possible test by setting some value using javascript (security)");
	});

	test("method _id should return a different 12bit hex number", function(){
		expect(3);	
		var form=$('<form><input type="file" name="f" id="f"/></form>');
        var file = form.find("#f");
		
		var uplpipe = new UplPipe(file,{});
		
		var id1 = uplpipe._id();
		var id2 = uplpipe._id();
		
		ok(/[0-9a-fA-F]{12}/.test(id1), "id = " + id1 + ", is a 12 char hex");
		ok(/[0-9a-fA-F]{12}/.test(id2), "id = " + id2 + ", is a 12 char hex");

		notEqual(id1,id2,"maybe it can be false, but rare!");
	});

	test("method startAjaxPooling should show progress and start a interval!", function(){
		expect(4);
		
		var form=$('<form><input type="file" name="f" id="f"/></form>');
        var file = form.find("#f");
        
		var options = {
			baseurl : "/basepath",
			interval : 2000,
		};
		
		var uplpipe = new UplPipe(file,options);
		
		uplpipe.ajaxPooling = function(){ ok(true); };
		
		var code;
		var interval;
		uplpipe._setInterval = function(_code,_interval){code = _code; interval = _interval};
		
		uplpipe.panel=$('<a></a>');
		uplpipe.emitter=$('<a></a>');
		uplpipe.progress_div=$('<a></a>').hide();
		
		uplpipe.startAjaxPooling();
		
		code();
		equals(interval,options.interval);
		equals(uplpipe.panel.attr('style'),"display: none; ");
		equals(uplpipe.progress_div.attr('style'),"");
	});

	asyncTest("method ajaxPooling starts ajax and should call onAjaxError if fails", function(){
        expect(1);
		
        var json_response = {
            percentage: 50,
        };
		var url_status = "/basepath/upload/aabbccdd1254-test.pdf/status";
        $.mockjax({ // mock ajax request
            url: url_status,
            dataType: "json",
            response: function(){
                 start(); // for asyncTest
            },
			status: 500,
            responseTime: 0,
            responseText: json_response
        });

        var form=$('<form><input type="file" name="f" id="f"/></form>');
        var file = form.find("#f");
        
		var options = {
			baseurl : "/basepath",
			interval : 2000,
		};
		
		var uplpipe = new UplPipe(file,options);
		uplpipe.upload_status_url = url_status;
		
        uplpipe.onAjaxError = function(j,data,t){
            ok(true);
        }
        uplpipe.ajaxPooling();
    });
	
	asyncTest("method ajaxPooling starts ajax and should call onAjaxSuccess if success", function(){
        expect(1);
		
        var json_response = {
            percentage: 50,
        };
		var url_status = "/basepath/upload/aabbccdd1234-test.pdf/status";
        $.mockjax({ // mock ajax request
            url: url_status,
            dataType: "json",
            response: function(){
                 start(); // for asyncTest
            },
            responseTime: 0,
            responseText: json_response
        });

        var form=$('<form><input type="file" name="f" id="f"/></form>');
        var file = form.find("#f");
        
		var options = {
			baseurl : "/basepath",
			interval : 2000,
		};
		
		var uplpipe = new UplPipe(file,options);
		uplpipe.upload_status_url = url_status;
		
        uplpipe.onAjaxSuccess = function(data,t){
            equals(json_response.percentage, data.percentage);
        }
        uplpipe.ajaxPooling();
    });

	
}

$(document).ready(main);