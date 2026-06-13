package com.lz_insurance.insurance.identity.infrastructure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.lz_insurance.insurance"})
public class InsuranceIdentityInfrastructureApplication {

    public static void main(String[] args) {
        SpringApplication.run(InsuranceIdentityInfrastructureApplication.class, args);
    }

}
