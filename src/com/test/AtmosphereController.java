package com.test;

import org.apache.commons.lang3.StringEscapeUtils;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.Broadcaster;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.google.common.base.Splitter;
import com.test.SrceTailer;
import com.test.TailHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
public class AtmosphereController implements TailHandler {

	private static final Logger LOG = LoggerFactory.getLogger(AtmosphereController.class);
	private String directory = "D:\\hybrisSuite\\hybris\\log\\tomcat\\";
	private static SrceTailer tailer;
	private final static Integer MAX_CHUNK_LENTH = 1500;
	private static final long DELAY = 100;
	private Broadcaster BROADCASTER;
	private static List<String> watchableLogs = new ArrayList<String>();

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public ModelAndView getView() {
		return new ModelAndView("index");
	}

	@RequestMapping(value = "/logviewer", method = RequestMethod.GET)
	@ResponseBody
	public void onRequestLog(AtmosphereResource event, HttpSession session)
			throws IOException {

		if (watchableLogs.size() == 0) {
			reInit();
		}
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

	@RequestMapping(value = "/logviewer", method = RequestMethod.POST)
	@ResponseBody
	public void onPostLog(AtmosphereResource event) throws IOException {
		HttpServletRequest req = event.getRequest();
		HttpServletResponse res = event.getResponse();
		final String postPayload = req.getReader().readLine();
		if (postPayload != null && postPayload.startsWith("log=")) {
			tailer = SrceTailer.createTailer(directory
					+ postPayload.split("=")[1], directory, this);
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
			LOG.info("either logsDir doesn't exist or is not a folder");
		}
	}

	protected String asJson(final String key, final String value) {
		return "{\"" + key + "\":\"" + StringEscapeUtils.escapeJson(value)
				+ "\"}";
	}

	protected String asJsonArray(final String key, final List<String> list) {

		return ("{\"" + key + "\":" + JSONValue.toJSONString(list) + "}");
	}

	@Override
	public void handle(String line) {
		int lineLen = line.length();
		LOG.info("line's length " + lineLen);
		if (lineLen > 0) {
			if (lineLen > MAX_CHUNK_LENTH) {
				for (final String token : Splitter.fixedLength(MAX_CHUNK_LENTH)
						.split(line)) {
					BROADCASTER.broadcast(asJson("tail", token));
					try {
						Thread.sleep(DELAY);
					} catch (InterruptedException e) {
						LOG.error("Exception during splitting line", e);
					}
				}
			} else {
				BROADCASTER.broadcast(asJson("tail",
						StringEscapeUtils.escapeJson(line)));
			}
		}
		
	}
	
	public void destroy() {
		tailer.stop();
	}

}