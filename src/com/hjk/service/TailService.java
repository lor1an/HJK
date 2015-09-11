package com.hjk.service;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.atmosphere.cpr.AtmosphereResource;

public interface TailService {

    void getLogEntries(AtmosphereResource resource, String uid) throws IOException;

    void initTail(AtmosphereResource resource, String uid) throws IOException;

    void closeTailForUrl(AtmosphereResource resource, String uid);

    String getPreviousLines(int count, String uid) throws IOException;

    void downloadLog(HttpServletRequest request, HttpServletResponse response, String uid) throws IOException;

}
