package com.hjk.tail;

import java.io.IOException;

import org.atmosphere.cpr.AtmosphereResource;

public interface TailHandler {
	
	void handle(final String line, final String uid, final AtmosphereResource resource) throws IOException;
	
}
