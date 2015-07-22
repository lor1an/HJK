package com.hjk.controller;

import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.Meteor;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.http.HttpServletRequest;

public class AtmosphereResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(final MethodParameter methodParameter) {
        return AtmosphereResource.class.isAssignableFrom(methodParameter.getParameterType());
    }

    @Override
    public Object resolveArgument(final MethodParameter methodParameter,
            final ModelAndViewContainer modelAndViewContainer, final NativeWebRequest nativeWebRequest,
            final WebDataBinderFactory webDataBinderFactory) throws Exception {
        Meteor m = Meteor.build(nativeWebRequest.getNativeRequest(HttpServletRequest.class));
        return m.getAtmosphereResource();
    }
}