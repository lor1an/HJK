$(document).ready(
        function() {
          var connectedEndpoint;
          var callbackAdded = false;
          var detectedTransport = null;
          var lines = 0, notice = $("#info"), buffer = $('#tail');
          var pause = false;
          var pause_buffer = '';
          var filename;

          function subscribe() {
            function callback(response) {
              $.atmosphere.log('info', ["response.state: " + response.state]);
              $.atmosphere.log('info', ["response.transport: "
                      + response.transport]);

              detectedTransport = response.transport;
              if (response.transport != 'polling'
                      && response.state != 'connected'
                      && response.state != 'closed') {
                $.atmosphere.log('info', ["response.responseBody: "
                        + response.responseBody]);
                if (response.status == 200) {
                  var data = jQuery.parseJSON(response.responseBody);
                  if (data.filename) {
                    filename = data.filename;
                    notice.html('watching ' + filename);
                  } else if (data.logs) {
                    var selector = $("#selector select");
                    $.each(data.logs, function() {
                      var log = new Option(this, this);
                      if ($.browser.msie)
                        selector[0].add(log);
                      else
                        selector[0].add(log, null);
                    });
                    selector.bind('change', function(e) {
                      var log = selector[0];
                      if (log.selectedIndex == 0) {
                        $("#info,#tail").empty();
                        return;
                      }
                      connectedEndpoint.push(document.location.toString()
                              + 'viewer', null, $.atmosphere.request = {
                        data: 'log=' + log.options[log.selectedIndex].value
                      });
                    });
                  } else if (data.tail) {
                    if (pause) {
                      pause_buffer += data.tail;
                    } else {
                      addCR(data.tail, buffer);
                      buffer.scrollTop(lines * 100)
                      lines = lines + data.tail.length;
                    }

                  }
                }
              }
            }

            var location = document.location.toString() + '/viewer';
            location = location.replace('log', "/" + 'log');
            $.atmosphere.subscribe(location, !callbackAdded ? callback : null,
                    $.atmosphere.request = {
                      transport: "websocket",
                    });
            connectedEndpoint = $.atmosphere.response;
            callbackAdded = true;
          }

          function addCR(str, buffer) {
            if (str.substr(str.length - 2) != '\n' && str.length > 5) {
              str = str.replace(/(?:\r\n|\r|\n)/g, '<br />');
              str = str.replace(/(?:\\r\\n|\\r|\\n)/g, '<br />');
              buffer.append(str);
            }
          }
          function connect() {
            subscribe();
          }

          $('.start_stop').click(function() {
            var $this = $(this);
            $this.toggleClass('flag');
            if ($this.hasClass('flag')) {
              addCR(pause_buffer, buffer);
              buffer.scrollTop(lines * 100)
              lines = lines + pause_buffer.length;
              pause = false;
              pause_buffer = '';
              $this.text('Pause');
            } else {
              pause = true;
              $this.text('Resume');
            }
          });
          
          $('.prev').click(function() {
            var location = document.location.toString() + '/prev';
            $.get(location);
          });
          
          $('.download').click(function() {
            var location = document.location.toString() + '/download';
            var iframe = document.getElementById("downloadFrame");
            iframe.src = location;
          });
          
          $('.gist').click(function() {
            var gistFileName = filename + '_gist.log';
            var text = buffer.html().replace(/<br>/g, '\n');
            text = text.replace(/<br>/g, '\n');
            var toGist = {
                    "description": "the description for this gist",
                    "public": true,
                    "files": {
                      'console.log': {
                        "content": text
                      }
                    }
                  }
            $.post('https://api.github.com/gists', JSON.stringify(toGist), function(data) {
              window.prompt("",data.html_url);
            });
          });
          
          $('.gistSel').click(function() {
            var gistFileName = filename + '_gist.log';
            var text = window.getSelection().toString();
            if(text.length == 0){
              alert('Nothing selected');
            }
            var toGist = {
                    "description": "the description for this gist",
                    "public": true,
                    "files": {
                      'console.log': {
                        "content": text
                      }
                    }
                  }
            $.post('https://api.github.com/gists', JSON.stringify(toGist), function(data) {
              window.prompt("",data.html_url);
            });
          });
          
     

          connect();
        });

$(function() {
  $("#refresh").click(function() {
    $("#tail").empty();
  });
})

$(window).unload( function () { 
  var location = document.location.toString() + '/close';
  jQuery.ajax({url:location, async:false})
} );

