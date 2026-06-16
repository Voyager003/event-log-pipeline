package com.eventlogpipeline.event;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "event-generator")
public record EventGeneratorProperties(int count, long seed) {
}
