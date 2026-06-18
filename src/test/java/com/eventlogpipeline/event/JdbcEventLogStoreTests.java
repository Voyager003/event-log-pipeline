package com.eventlogpipeline.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class JdbcEventLogStoreTests {

    private final NamedParameterJdbcTemplate jdbcTemplate = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
    private final JdbcEventLogStore store = new JdbcEventLogStore(jdbcTemplate);

    @Test
    void savesEnvelopeAndTypeSpecificDetails() {
        when(jdbcTemplate.update(org.mockito.ArgumentMatchers.anyString(), anyMap())).thenReturn(1);
        var events = new EventGenerator().generate(1_000, new Random(20260615L));

        int savedCount = store.saveAll(events);

        assertThat(savedCount).isEqualTo(1_000);
        verify(jdbcTemplate, times(1_000)).update(contains("INSERT INTO event_logs"), anyMap());
        verify(jdbcTemplate, times(countDetails(events, EventDetail.Lecture.class)))
                .update(contains("INSERT INTO lecture_event_details"), anyMap());
        verify(jdbcTemplate, times(countDetails(events, EventDetail.VideoError.class)))
                .update(contains("INSERT INTO video_error_event_details"), anyMap());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void storesEnvelopeFieldsAsSeparatedColumns() {
        when(jdbcTemplate.update(org.mockito.ArgumentMatchers.anyString(), anyMap())).thenReturn(1);
        var event = new EventGenerator().generate(1, new Random(20260615L)).getFirst();

        store.saveAll(List.of(event));

        ArgumentCaptor<java.util.Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(java.util.Map.class);
        verify(jdbcTemplate).update(contains("INSERT INTO event_logs"), paramsCaptor.capture());

        assertThat(paramsCaptor.getValue())
                .containsEntry("eventId", event.eventId())
                .containsEntry("eventType", event.eventType().eventName())
                .containsEntry("source", event.source().name())
                .containsEntry("schemaVersion", event.schemaVersion())
                .containsEntry("userId", event.userId())
                .containsEntry("sessionId", event.sessionId())
                .containsEntry("deviceType", event.deviceType());
    }

    private int countDetails(List<GeneratedEvent> events, Class<? extends EventDetail> detailType) {
        return (int) events.stream()
                .map(GeneratedEvent::detail)
                .filter(detailType::isInstance)
                .count();
    }
}
