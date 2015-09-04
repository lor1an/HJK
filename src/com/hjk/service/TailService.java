package com.hjk.service;

import java.io.IOException;

import org.atmosphere.cpr.AtmosphereResource;

public interface TailService {

    public void getLogEntries(AtmosphereResource resource, String uid) throws IOException;

    void initTail(AtmosphereResource resource, String uid) throws IOException;

    void closeTailForUrl(AtmosphereResource resource, String uid);
}
