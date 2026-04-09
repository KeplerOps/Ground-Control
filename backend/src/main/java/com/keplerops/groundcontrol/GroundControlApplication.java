package com.keplerops.groundcontrol;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GroundControlApplication {

    public static void main(String[] args) {
        SpringApplication.run(GroundControlApplication.class, args);
    }
}
