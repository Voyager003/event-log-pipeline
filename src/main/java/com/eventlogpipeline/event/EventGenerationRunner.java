package com.eventlogpipeline.event;

import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class EventGenerationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(EventGenerationRunner.class);

    private final EventGenerator eventGenerator;
    private final EventGeneratorProperties properties;

    public EventGenerationRunner(EventGenerator eventGenerator, EventGeneratorProperties properties) {
        this.eventGenerator = eventGenerator;
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        var events = eventGenerator.generate(properties.count(), properties.seed());
        Map<EventType, Long> countsByType = events.stream()
                .collect(Collectors.groupingBy(GeneratedEvent::eventType, Collectors.counting()));

        log.info("Generated {} Liveklass events with seed {}", events.size(), properties.seed());
        countsByType.forEach((eventType, count) -> log.info("{}={}", eventType, count));
    }
}
