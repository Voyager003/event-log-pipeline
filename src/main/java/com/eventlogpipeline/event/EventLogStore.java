package com.eventlogpipeline.event;

import java.util.List;

public interface EventLogStore {

    int saveAll(List<GeneratedEvent> events);
}
