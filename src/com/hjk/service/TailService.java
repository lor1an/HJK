package com.hjk.service;

import java.io.IOException;

import org.atmosphere.cpr.AtmosphereResource;

import com.hjk.tail.Tailer;

public interface TailService {

    void getLogEntries(AtmosphereResource event) throws IOException;

    void initTail(AtmosphereResource event) throws IOException;
    
    void destroy();
    
    Tailer getTailer();

}
