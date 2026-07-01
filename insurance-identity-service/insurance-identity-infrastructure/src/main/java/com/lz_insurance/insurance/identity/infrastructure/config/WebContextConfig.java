package com.lz_insurance.insurance.identity.infrastructure.config;

import com.lz_insurance.web.config.WebMvcConfig;
import com.lz_insurance.web.filter.RequestContextFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

/**
 * Activates the shared insurance-web request-context plumbing inside the identity app.
 *
 * <p>insurance-web sits OUTSIDE this app's {@code com.lz_insurance.insurance} component-scan root and
 * ships no Boot autoconfiguration, so its beans do not self-register (same reason
 * {@code GlobalControllerAdvice} needs an explicit {@code @Import}). Here we:
 * <ul>
 *   <li>{@code @Import} {@link WebMvcConfig} so its {@code RequestContextArgumentResolver} (and CORS /
 *       formatters) take effect — this is what lets controllers receive {@code RequestContext} as a
 *       plain parameter;</li>
 *   <li>register {@link RequestContextFilter} first in the chain via a {@link FilterRegistrationBean}
 *       so the context is built before any controller runs.</li>
 * </ul>
 * Kept in a dedicated config (rather than another {@code @Import} on the application class) to keep
 * the app class's import list minimal.
 */
@Configuration
@Import(WebMvcConfig.class)
public class WebContextConfig {

    // NB: method (bean) name must NOT be "requestContextFilter" — Spring Boot's WebMvcAutoConfiguration
    // already registers a bean by that name (its own framework RequestContextFilter), and bean-override
    // is disabled by default.
    @Bean
    FilterRegistrationBean<RequestContextFilter> requestContextFilterRegistration() {
        FilterRegistrationBean<RequestContextFilter> registration =
                new FilterRegistrationBean<>(new RequestContextFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
