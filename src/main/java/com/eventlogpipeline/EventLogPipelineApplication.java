package com.eventlogpipeline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class EventLogPipelineApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventLogPipelineApplication.class, args);
    }

}
