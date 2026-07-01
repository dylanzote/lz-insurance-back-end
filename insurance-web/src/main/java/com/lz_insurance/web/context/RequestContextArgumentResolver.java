package com.lz_insurance.web.context;

import com.lz_insurance.core.context.RequestContext;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Injects the per-request {@link RequestContext} into any controller method that declares it as a
 * parameter — the same mechanism Spring uses for {@code @RequestBody} / {@code @PathVariable}.
 * Controllers therefore never import or call {@link RequestContextHolder}; the holder is touched here,
 * once, centrally. Registered in {@code WebMvcConfig.addArgumentResolvers}.
 */
public class RequestContextArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return RequestContext.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        return RequestContextHolder.get();
    }
}
