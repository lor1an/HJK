package com.hjk.controller;

import org.apache.commons.lang3.RandomStringUtils;
import org.atmosphere.cpr.AtmosphereResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.hjk.service.TailService;

import javax.servlet.http.HttpSession;

import java.io.IOException;
import java.net.UnknownHostException;

@Controller
public class AtmosphereController {

    public static final int UNIQUE_ID_LENGHTH = 6;

    @Autowired
    TailService tailService;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ModelAndView getView() {
        String uniqueID = RandomStringUtils.randomNumeric(UNIQUE_ID_LENGHTH);
        return new ModelAndView("redirect:/" + uniqueID);
    }

    @RequestMapping(value = "/link", method = RequestMethod.GET)
    public ModelAndView getLink() throws UnknownHostException {
        String ip = java.net.InetAddress.getLocalHost().getHostAddress();
        ModelAndView model = new ModelAndView();
        String url = "http://" + ip + ":9001/tail";
        model.addObject("message", url);
        model.setViewName("hello");
        return model;

    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ModelAndView getViewForID(@PathVariable("id") String id, final AtmosphereResource resource) {
        return new ModelAndView("index");
    }

    @RequestMapping(value = "/{id}/logviewer", method = RequestMethod.GET)
    @ResponseBody
    public void onRequest(@PathVariable("id") String id, final AtmosphereResource resource, final HttpSession session)
            throws IOException {
        tailService.getLogEntries(resource, "/" + id);
    }

    @RequestMapping(value = "/{id}/logviewer", method = RequestMethod.POST)
    @ResponseBody
    public void onPost(@PathVariable("id") String id, final AtmosphereResource resource) throws IOException {
        tailService.initTail(resource, "/" + id);
    }

    @RequestMapping(value = "/{id}/close", method = RequestMethod.GET)
    public void close(@PathVariable("id") String id, final AtmosphereResource resource) {
        tailService.closeTailForUrl(resource, "/" + id);
    }
}