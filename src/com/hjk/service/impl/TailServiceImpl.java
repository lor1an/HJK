package com.hjk.service.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringEscapeUtils;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.cpr.MetaBroadcaster;
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

    //@Value("#{environment.HYBRIS_HOME}\\log\\tomcat\\")
    private String directory;
    private static final  Integer MAX_CHUNK_LENTH = 1500;
    private static final long DELAY = 100;
    private static List<String> watchableLogs = new ArrayList<String>();
    private static volatile Map<String, SrceTailer> uidTailer = new HashMap<String, SrceTailer>();

    @Override
    public void getLogEntries(AtmosphereResource resource, String uid) throws IOException {
        directory = System.getProperty("catalina.base").replace("bin\\platform\\tomcat", "log\\tomcat\\");
        reInit();
        if(uidTailer.get(uid) != null){
            uidTailer.get(uid).stopTail();
        }
        HttpServletResponse res = resource.getResponse();
        res.setContentType("text/html");
        res.addHeader("Cache-Control", "private");
        res.addHeader("Pragma", "no-cache");
        resource.suspend();
        BroadcasterFactory broadcasterFactory = resource.getAtmosphereConfig().getBroadcasterFactory();
        Broadcaster broadcaster = broadcasterFactory.lookup(uid, true);
        broadcaster.addAtmosphereResource(resource);
        getMetaBroadcaster(resource).broadcastTo(uid, asJsonArray("logs", watchableLogs));
    }

    @Override
    public void initTail(final AtmosphereResource resource, String uid) throws IOException {
        HttpServletRequest req = resource.getRequest();
        final String postPayload = req.getReader().readLine();
        if (postPayload != null && postPayload.startsWith("log=")) {
            SrceTailer tailer = uidTailer.get(uid);
            if(tailer != null){
                tailer.stopTail();
            }
            tailer = SrceTailer.createTailer(directory + postPayload.split("=")[1], directory, this, uid, resource);
            tailer.startTailerThread();
            uidTailer.put(uid, tailer);
            tailer.startTail();
        }
        getMetaBroadcaster(resource).broadcastTo(uid, asJson("filename", postPayload.split("=")[1]));
    }

    public void reInit() {
        watchableLogs.clear();
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
    public void handle(final String line, String uid, final AtmosphereResource resource) throws IOException {
        int lineLen = line.length();
        LOG.info("line's length " + lineLen);
        if (lineLen > 0) {
            if (lineLen > MAX_CHUNK_LENTH) {
                for (final String token : Splitter.fixedLength(MAX_CHUNK_LENTH).split(line)) {
                    getMetaBroadcaster(resource).broadcastTo(uid, asJson("tail", token));
                    try {
                        Thread.sleep(DELAY);
                    } catch (InterruptedException e) {
                        LOG.error("Exception during splitting line", e);
                    }
                }
            } else {
                getMetaBroadcaster(resource).broadcastTo(uid, asJson("tail", StringEscapeUtils.escapeJson(line)));
            }
        }
    }

    protected String asJson(final String key, final String value) {
        return "{\"" + key + "\":\"" + StringEscapeUtils.escapeJson(value) + "\"}";
    }

    protected String asJsonArray(final String key, final List<String> list) {
        return ("{\"" + key + "\":" + JSONValue.toJSONString(list) + "}");
    }

    private MetaBroadcaster getMetaBroadcaster(AtmosphereResource resource) {
        return resource.getAtmosphereConfig().metaBroadcaster();
    }

    @Override
    public void closeTailForUrl(AtmosphereResource resource, String uid) {
        if(uidTailer.get(uid) != null){
            uidTailer.get(uid).stopTail();
            uidTailer.put(uid, null);
        }
    }
}
