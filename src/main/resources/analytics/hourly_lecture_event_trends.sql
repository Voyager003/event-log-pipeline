SELECT
    date_trunc('hour', e.occurred_at) AS event_hour,
    e.event_type,
    COUNT(*) AS event_count
FROM event_logs e
WHERE e.event_type IN ('lecture_started', 'lecture_played', 'lecture_completed')
GROUP BY event_hour, e.event_type
ORDER BY event_hour, e.event_type;
