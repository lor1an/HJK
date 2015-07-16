  $(document).ready(function() {
        var connectedEndpoint;
        var callbackAdded = false;
        var detectedTransport = null;
        var lines = 0, notice = $("#info"), buffer = $('#tail');

        function subscribe() {
            // jquery.atmosphere.response
            function callback(response) {
                // Websocket events.
                $.atmosphere.log('info', ["response.state: " + response.state]);
                $.atmosphere.log('info', ["response.transport: " + response.transport]);

                detectedTransport = response.transport;
                if (response.transport != 'polling' && response.state != 'connected' && response.state != 'closed') {
                    $.atmosphere.log('info', ["response.responseBody: " + response.responseBody]);
                    if (response.status == 200) {
                    	if(response.responseBody.slice(-1) != '}'){
                    		response.responseBody = response.responseBody.slice(0, -1);
                    	}
                        var data = jQuery.parseJSON(response.responseBody);
                        if (data.filename) {
                            notice.html('watching ' + data.filename);
                        } else if (data.logs) {
                            var selector = $("#selector select");
                            $.each(data.logs, function() {
                                var log = new Option(this, this);
                                if ($.browser.msie) selector[0].add(log); else selector[0].add(log, null);
                            });
                            selector.bind('change', function(e) {
                                var log = selector[0];
                                if (log.selectedIndex == 0) {
                                    $("#info,#tail").empty();
                                    return;
                                }
                                //socket.send({log:log.options[log.selectedIndex].value});
                                connectedEndpoint.push(document.location.toString() + 'logviewer' ,null,
                                    $.atmosphere.request = {data: 'log=' +log.options[log.selectedIndex].value});
                            });
                        } else if (data.tail) {
                        	if( typeof data.tail === 'string' ) {
                        	    addCR(data.tail,buffer);
                        	}else{
                        		for (var i = 0; i < data.tail.length; i++) {
                        			addCR(data.tail[i],buffer)
                        		}
                        	}
                            buffer.scrollTop(lines * 100)
                            lines = lines + data.tail.length;
                        } else {
                        }

                    }
                }
            }

            var location = document.location.toString() + 'logviewer';

            $.atmosphere.subscribe(location, !callbackAdded ? callback : null, $.atmosphere
                    .request = { transport: 'websocket' });
            connectedEndpoint = $.atmosphere.response;
            callbackAdded = true;
        }
		
    	function addCR (str, buffer){
    		if(str.substr(str.length - 2) != '\n' && str.length > 5){
    			//console.log(str);
    			str = str.replace(/(?:\r\n|\r|\n)/g, '<br />');
            	buffer.append(str.concat('<br/>'));//.join('<br/>')
            }
    	}
        function connect() {
            subscribe();
        }

        connect();
    });
    
	  $(function(){
		  $("#refresh").click(function() {
			  $("#tail").empty();
		});
	  })