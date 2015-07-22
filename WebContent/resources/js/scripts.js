$(document).ready(
        function() {
          var connectedEndpoint;
          var callbackAdded = false;
          var detectedTransport = null;
          var lines = 0, notice = $("#info"), buffer = $('#tail');
          var pause = false;
          var pause_buffer = '';

          function subscribe() {
            // jquery.atmosphere.response
            function callback(response) {
              // Websocket events.
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
                  if (response.responseBody.slice(-1) != '}') {
                    response.responseBody = response.responseBody.slice(0, -1);
                  }
                  var data = jQuery.parseJSON(response.responseBody);
                  if (data.filename) {
                    notice.html('watching ' + data.filename);
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
                      // socket.send({log:log.options[log.selectedIndex].value});
                      connectedEndpoint.push(document.location.toString()
                              + 'logviewer', null, $.atmosphere.request = {
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

            var location = document.location.toString() + '/logviewer';

            $.atmosphere.subscribe(location, !callbackAdded ? callback : null,
                    $.atmosphere.request = {
                      transport: 'websocket'
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

          connect();
        });

$(function() {
  $("#refresh").click(function() {
    $("#tail").empty();
  });
})
