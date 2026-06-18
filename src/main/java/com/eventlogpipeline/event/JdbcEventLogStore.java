package com.eventlogpipeline.event;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Profile("db")
@Repository
public class JdbcEventLogStore implements EventLogStore {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcEventLogStore(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public int saveAll(List<GeneratedEvent> events) {
        int saved = 0;

        for (GeneratedEvent event : events) {
            saveEnvelope(event);
            saveDetail(event);
            saved++;
        }

        return saved;
    }

    private void saveEnvelope(GeneratedEvent event) {
        jdbcTemplate.update("""
                INSERT INTO event_logs (
                    event_id,
                    event_type,
                    source,
                    schema_version,
                    occurred_at,
                    user_id,
                    anonymous_id,
                    session_id,
                    device_type
                ) VALUES (
                    :eventId,
                    :eventType,
                    :source,
                    :schemaVersion,
                    :occurredAt,
                    :userId,
                    :anonymousId,
                    :sessionId,
                    :deviceType
                )
                """, Map.of(
                "eventId", event.eventId(),
                "eventType", event.eventType().eventName(),
                "source", event.source().name(),
                "schemaVersion", event.schemaVersion(),
                "occurredAt", Timestamp.from(event.occurredAt()),
                "userId", event.userId(),
                "anonymousId", event.anonymousId(),
                "sessionId", event.sessionId(),
                "deviceType", event.deviceType()
        ));
    }

    private void saveDetail(GeneratedEvent event) {
        switch (event.detail()) {
            case EventDetail.Lecture lecture -> saveLectureDetail(event.eventId(), lecture);
            case EventDetail.VideoError videoError -> saveVideoErrorDetail(event.eventId(), videoError);
        }
    }

    private void saveLectureDetail(String eventId, EventDetail.Lecture detail) {
        jdbcTemplate.update("""
                INSERT INTO lecture_event_details (
                    event_id,
                    course_id,
                    lecture_id,
                    lecture_title,
                    playback_position_seconds,
                    watch_duration_seconds,
                    completion_rate
                ) VALUES (
                    :eventId,
                    :courseId,
                    :lectureId,
                    :lectureTitle,
                    :playbackPositionSeconds,
                    :watchDurationSeconds,
                    :completionRate
                )
                """, Map.of(
                "eventId", eventId,
                "courseId", detail.courseId(),
                "lectureId", detail.lectureId(),
                "lectureTitle", detail.lectureTitle(),
                "playbackPositionSeconds", detail.playbackPositionSeconds(),
                "watchDurationSeconds", detail.watchDurationSeconds(),
                "completionRate", detail.completionRate()
        ));
    }

    private void saveVideoErrorDetail(String eventId, EventDetail.VideoError detail) {
        jdbcTemplate.update("""
                INSERT INTO video_error_event_details (
                    event_id,
                    course_id,
                    lecture_id,
                    error_type,
                    error_code,
                    error_message
                ) VALUES (
                    :eventId,
                    :courseId,
                    :lectureId,
                    :errorType,
                    :errorCode,
                    :errorMessage
                )
                """, Map.of(
                "eventId", eventId,
                "courseId", detail.courseId(),
                "lectureId", detail.lectureId(),
                "errorType", detail.errorType(),
                "errorCode", detail.errorCode(),
                "errorMessage", detail.errorMessage()
        ));
    }
}
