SELECT
    date_trunc('hour', e.occurred_at) AS event_hour,
    COUNT(DISTINCT e.session_id) AS active_session_count,
    COUNT(DISTINCT e.user_id) AS active_user_count
FROM event_logs e
WHERE e.event_type IN ('lecture_started', 'lecture_played', 'lecture_completed')
GROUP BY event_hour
ORDER BY event_hour;
