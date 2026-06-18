SELECT
    date_trunc('hour', e.occurred_at) AS event_hour,
    COUNT(*) FILTER (WHERE e.event_type = 'lecture_playback_heartbeat') AS playback_heartbeat_count,
    COUNT(*) FILTER (WHERE e.event_type = 'video_error_occurred') AS error_event_count,
    ROUND(
        COUNT(*) FILTER (WHERE e.event_type = 'video_error_occurred')::numeric
            / NULLIF(COUNT(*) FILTER (WHERE e.event_type = 'lecture_playback_heartbeat'), 0) * 100,
        2
    ) AS error_rate
FROM event_logs e
WHERE e.event_type IN ('lecture_playback_heartbeat', 'video_error_occurred')
GROUP BY event_hour
ORDER BY event_hour;
