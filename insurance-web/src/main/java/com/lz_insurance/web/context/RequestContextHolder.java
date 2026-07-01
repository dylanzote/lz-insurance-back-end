package com.lz_insurance.web.context;

import com.lz_insurance.core.context.RequestContext;

/**
 * Thread-local transport for {@link RequestContext} between {@code RequestContextFilter} and the
 * {@link RequestContextArgumentResolver} — nothing more. The filter {@link #set}s it at the start of
 * a request and {@link #clear}s it in a finally block; the resolver {@link #get}s it once, centrally,
 * to inject {@code RequestContext} into controller methods.
 *
 * <p>Neither controllers nor the domain read this holder directly: the resolver is the ONLY caller of
 * {@link #get()}, and context crosses into use cases as an explicit parameter — business logic stays
 * decoupled from the servlet thread model.
 */
public final class RequestContextHolder {

    private static final ThreadLocal<RequestContext> CONTEXT = new ThreadLocal<>();

    private RequestContextHolder() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void set(RequestContext context) {
        CONTEXT.set(context);
    }

    /**
     * @return the context for the current request
     * @throws IllegalStateException if called outside a request the filter has initialized
     */
    public static RequestContext get() {
        RequestContext context = CONTEXT.get();
        if (context == null) {
            throw new IllegalStateException(
                    "RequestContext not set on this thread — is RequestContextFilter registered?");
        }
        return context;
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
