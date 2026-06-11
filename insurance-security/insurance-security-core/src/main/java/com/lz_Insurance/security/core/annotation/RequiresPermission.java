package com.lz_Insurance.security.core.annotation;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@PreAuthorize("hasRole(@environment.getProperty('insurance.role.' + #permission))")
public @interface RequiresPermission {
    String value();
}
