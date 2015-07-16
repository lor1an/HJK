package com.test;

import org.apache.commons.lang3.StringEscapeUtils;
import org.atmosphere.cpr.AtmosphereHandler;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.Broadcaster;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// "D:\\hybrisSuite\\hybris\\log\\tomcat\\"
public class LogHandler implements AtmosphereHandler<HttpServletRequest, HttpServletResponse>, TailHandler {

    private static final Logger LOG = LoggerFactory.getLogger(LogHandler.class);
    private String directory = "D:\\hybrisSuite\\hybris\\log\\tomcat\\";
    private static SrceTailer tailer;
    private final static Integer MAX_CHUNK_LENTH = 1500;
    private static final long DELAY = 100;
    private Broadcaster BROADCASTER;
    private static List<String> watchableLogs = new ArrayList<String>();

    public LogHandler() {
        final File logsDir = new File(directory);
        if (logsDir.exists() && logsDir.isDirectory()) {
            File[] logs = logsDir.listFiles();
            for (File file : logs) {
                if (file.getName().endsWith(".log")) {
                    watchableLogs.add(file.getName());
                }
            }
        } else {
            LOG.info("either logsDir doesn't exist or is not a folder");
        }
    }

    public void reInit() {
        final File logsDir = new File(directory);
        if (logsDir.exists() && logsDir.isDirectory()) {
            File[] logs = logsDir.listFiles();
            for (File file : logs) {
                if (file.getName().endsWith(".log")) {
                    watchableLogs.add(file.getName());
                }
            }
        } else {
            LOG.info("either logsDir doesn't exist or is not a folder");
        }
    }

    @Override
    public void onRequest(final AtmosphereResource<HttpServletRequest, HttpServletResponse> event) throws IOException {

        if (watchableLogs.size() == 0) {
            reInit();
        }
        HttpServletRequest req = event.getRequest();
        HttpServletResponse res = event.getResponse();
        res.setContentType("text/html");
        res.addHeader("Cache-Control", "private");
        res.addHeader("Pragma", "no-cache");

        if (req.getMethod().equalsIgnoreCase("GET")) {
            event.suspend();
            if (BROADCASTER == null)
                BROADCASTER = event.getBroadcaster();

            if (watchableLogs.size() != 0) {
                BROADCASTER.broadcast(asJsonArray("logs", watchableLogs));
            }

            res.getWriter().flush();
        } else { // POST
            final String postPayload = req.getReader().readLine();
            if (postPayload != null && postPayload.startsWith("log=")) {
                tailer = SrceTailer.createTailer(directory + postPayload.split("=")[1], directory, this);
            }
            BROADCASTER.broadcast(asJson("filename", postPayload.split("=")[1]));
            res.getWriter().flush();
        }
    }

    @Override
    public void onStateChange(final AtmosphereResourceEvent<HttpServletRequest, HttpServletResponse> event)
            throws IOException {

        HttpServletResponse res = event.getResource().getResponse();
        if (event.isResuming()) {
            res.getWriter().write("Atmosphere closed<br/>");
            res.getWriter().write("</body></html>");
        } else {
            res.getWriter().write(event.getMessage().toString());
        }
        res.getWriter().flush();
    }

    public void destroy() {
        tailer.stop();
    }

    @Override
    public synchronized void handle(String line) {
        int lineLen = line.length();
        LOG.info("line's length " + lineLen);
        if (lineLen > 0) {
            if (lineLen > MAX_CHUNK_LENTH) {
                for (final String token : Splitter.fixedLength(MAX_CHUNK_LENTH).split(line)) {
                    BROADCASTER.broadcast(asJson("tail", token));
                    try {
                        Thread.sleep(DELAY);
                    } catch (InterruptedException e) {
                        LOG.error("Exception during splitting line", e);
                    }
                }
            } else {
                BROADCASTER.broadcast(asJson("tail", StringEscapeUtils.escapeJson(line)));
            }
        }
    }

    protected String asJson(final String key, final String value) {
        return "{\"" + key + "\":\"" + StringEscapeUtils.escapeJson(value) + "\"}";
    }

    protected String asJsonArray(final String key, final List<String> list) {

        return ("{\"" + key + "\":" + JSONValue.toJSONString(list) + "}");
    }

}
