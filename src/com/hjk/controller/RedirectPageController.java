package com.hjk.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class RedirectPageController {


    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ModelAndView getPublicIP() throws  IOException {
        URL whatismyip = new URL("http://checkip.amazonaws.com");
        BufferedReader in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
        String ip = in.readLine();
        ModelAndView model = new ModelAndView();
        String url = "http://" + ip + ":9001/tail/log";
        model.addObject("message", url);
        model.setViewName("redirect");
        return model;

    }
    
}
