SELECT
    date_trunc('hour', e.occurred_at) AS event_hour,
    COUNT(*) FILTER (WHERE e.event_type IN ('lecture_started', 'lecture_played', 'lecture_completed'))
        AS lecture_event_count,
    COUNT(*) FILTER (WHERE e.event_type = 'video_error_occurred') AS error_event_count,
    ROUND(
        COUNT(*) FILTER (WHERE e.event_type = 'video_error_occurred')::numeric
            / NULLIF(COUNT(*), 0) * 100,
        2
    ) AS error_rate
FROM event_logs e
GROUP BY event_hour
ORDER BY event_hour;
