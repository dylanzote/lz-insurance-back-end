package com.lz_insurance.insurance.identity.api.controller;

import com.lz_insurance.core.context.RequestContext;
import com.lz_insurance.insurance.identity.api.dto.request.BootstrapRequest;
import com.lz_insurance.insurance.identity.api.dto.response.BootstrapResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "bootstrap API", description = "endpoints to handle initial data in the system")
@RestController
@RequestMapping("/identity")
public interface BootstrapApi {

    @Operation(summary = "The bootstrap endpoint is the one-time break-glass that creates the very first TENANT_ADMIN, before anyone can log in when a new tenant is deployed")
    @PostMapping("/bootstrap")
    BootstrapResponse bootstrap(@RequestHeader(value = "X-Bootstrap-Secret", required = false) String secret, @Valid @RequestBody BootstrapRequest request, RequestContext context);
}
