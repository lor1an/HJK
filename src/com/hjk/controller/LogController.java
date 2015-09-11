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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.IOException;

@Controller
@RequestMapping(value = "/log")
public class LogController {

    public static final int UNIQUE_ID_LENGHTH = 6;

    @Autowired
    TailService tailService;

    @RequestMapping(method = RequestMethod.GET)
    public ModelAndView getView() {
        String uniqueID = RandomStringUtils.randomNumeric(UNIQUE_ID_LENGHTH);
        return new ModelAndView("redirect:/log/" + uniqueID);
    }
    
    @RequestMapping(value = "/{id}/prev", method = RequestMethod.GET)
    public String showPreviousLines(@PathVariable("id") String id) throws IOException {
        return tailService.getPreviousLines(5, "/" + id);
    }
    
    @RequestMapping(value = "/{id}/download", method = RequestMethod.GET)
    public void doDownload(HttpServletRequest request, HttpServletResponse response, @PathVariable("id") String id) throws IOException {
       tailService.downloadLog(request, response, "/" + id);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ModelAndView getViewForID(@PathVariable("id") String id, final AtmosphereResource resource) {
        return new ModelAndView("index");
    }

    @RequestMapping(value = "/{id}/viewer", method = RequestMethod.GET)
    @ResponseBody
    public void onRequest(@PathVariable("id") String id, final AtmosphereResource resource, final HttpSession session)
            throws IOException {
        tailService.getLogEntries(resource, "/" + id);
    }

    @RequestMapping(value = "/{id}/viewer", method = RequestMethod.POST)
    @ResponseBody
    public void onPost(@PathVariable("id") String id, final AtmosphereResource resource) throws IOException {
        tailService.initTail(resource, "/" + id);
    }

    @RequestMapping(value = "/{id}/close", method = RequestMethod.GET)
    public void close(@PathVariable("id") String id, final AtmosphereResource resource) {
        tailService.closeTailForUrl(resource, "/" + id);
    }
}