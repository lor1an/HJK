package com.hjk.controller;

import org.atmosphere.cpr.AtmosphereResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.hjk.service.TailService;
import javax.servlet.http.HttpSession;

import java.io.IOException;

@Controller
public class AtmosphereController  {

    @Autowired
    TailService tailService;
    
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public ModelAndView getView() {
	    if(tailService.getTailer() != null){
	        tailService.destroy();
	    }
		return new ModelAndView("index");
	}

	@RequestMapping(value = "/logviewer", method = RequestMethod.GET)
	@ResponseBody
	public void onRequest(AtmosphereResource event, HttpSession session)
			throws IOException {
	    tailService.getLogEntries(event);
	}

	@RequestMapping(value = "/logviewer", method = RequestMethod.POST)
	@ResponseBody
	public void onPost(AtmosphereResource event) throws IOException {
	    tailService.initTail(event);
	}
}