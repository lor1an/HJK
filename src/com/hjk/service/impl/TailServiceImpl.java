package com.hjk.service.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringEscapeUtils;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.Broadcaster;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.base.Splitter;
import com.hjk.service.TailService;
import com.hjk.tail.TailHandler;
import com.hjk.tail.impl.SrceTailer;

@Service
public class TailServiceImpl implements TailService, TailHandler {

    private static final Logger LOG = LoggerFactory.getLogger(TailServiceImpl.class);
    
    @Value("#{environment.HYBRIS_HOME}\\log\\tomcat\\")
    private String directory;
    private static SrceTailer tailer;
    private final static Integer MAX_CHUNK_LENTH = 1500;
    private static final long DELAY = 100;
    private Broadcaster BROADCASTER;
    private volatile boolean run = true;
    private static List<String> watchableLogs = new ArrayList<String>();

    @Override
    public void getLogEntries(final AtmosphereResource event) throws IOException {
        reInit();
        stopTail();
        HttpServletResponse res = event.getResponse();
        res.setContentType("text/html");
        res.addHeader("Cache-Control", "private");
        res.addHeader("Pragma", "no-cache");

        event.suspend();
        if (BROADCASTER == null)
            BROADCASTER = event.getBroadcaster();

        if (watchableLogs.size() != 0) {
            BROADCASTER.broadcast(asJsonArray("logs", watchableLogs));
        }
        res.getWriter().flush();
    }

    @Override
    public void initTail(final AtmosphereResource event) throws IOException {
        HttpServletRequest req = event.getRequest();
        HttpServletResponse res = event.getResponse();
        final String postPayload = req.getReader().readLine();
        if (postPayload != null && postPayload.startsWith("log=")) {
            stopTail();
            tailer = SrceTailer.createTailer(directory + postPayload.split("=")[1], directory, this);
            startTail();
        }
        BROADCASTER.broadcast(asJson("filename", postPayload.split("=")[1]));
        res.getWriter().flush();
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
            LOG.info("Not a directory");
        }
    }

    @Override
    public void handle(final String line) {
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
    
    public SrceTailer getTailer(){
        return tailer;
    }

    @Override
    public boolean isRun() {
        return this.run;
    }
    
    private void startTail(){
        LOG.info("Starting tailer...");
        this.run = true;        
    }

    private void stopTail() {
        LOG.info("Stopping tailer...");
        this.run = false;        
    }
    
}
