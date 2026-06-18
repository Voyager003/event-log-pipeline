SELECT
    date_trunc('hour', e.occurred_at) AS event_hour,
    COUNT(*) AS playback_heartbeat_count
FROM event_logs e
WHERE e.event_type = 'lecture_playback_heartbeat'
GROUP BY event_hour
ORDER BY event_hour;
