package com.eventlogpipeline.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class AnalyticsQueryTests {

    private static final List<String> ANALYTICS_QUERIES = List.of(
            "analytics/hourly_lecture_event_trends.sql",
            "analytics/hourly_active_sessions.sql",
            "analytics/hourly_video_error_rate.sql"
    );

    @Test
    void providesAnalyticsQueriesForStepThree() {
        assertThat(ANALYTICS_QUERIES)
                .allSatisfy(path -> assertThat(new ClassPathResource(path).exists()).isTrue());
    }

    @Test
    void queryFilesUseStoredColumnsInsteadOfJsonPayloads() throws IOException {
        for (String path : ANALYTICS_QUERIES) {
            String sql = readSql(path);

            assertThat(sql).contains("SELECT");
            assertThat(sql).doesNotContainIgnoringCase("json");
            assertThat(sql).doesNotContainIgnoringCase("payload");
        }
    }

    @Test
    void hourlyLectureTrendQueryAggregatesLectureEventsByHour() throws IOException {
        String sql = readSql("analytics/hourly_lecture_event_trends.sql");

        assertThat(sql)
                .contains("date_trunc('hour'")
                .contains("lecture_started")
                .contains("lecture_played")
                .contains("lecture_completed")
                .contains("event_count");
    }

    @Test
    void hourlyActiveSessionQueryCountsDistinctSessionsAndUsers() throws IOException {
        String sql = readSql("analytics/hourly_active_sessions.sql");

        assertThat(sql)
                .contains("COUNT(DISTINCT e.session_id)")
                .contains("active_session_count")
                .contains("active_user_count");
    }

    @Test
    void hourlyVideoErrorRateQueryCalculatesErrorRate() throws IOException {
        String sql = readSql("analytics/hourly_video_error_rate.sql");

        assertThat(sql)
                .contains("video_error_occurred")
                .contains("error_event_count")
                .contains("error_rate");
    }

    private String readSql(String path) throws IOException {
        return new String(new ClassPathResource(path).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
