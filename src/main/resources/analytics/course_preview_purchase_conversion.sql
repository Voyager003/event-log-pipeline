WITH viewed_sessions AS (
    SELECT DISTINCT
        e.user_id,
        e.session_id,
        v.course_id
    FROM event_logs e
    JOIN view_event_details v ON v.event_id = e.event_id
    WHERE e.event_type = 'course_detail_viewed'
),
preview_starts AS (
    SELECT DISTINCT
        e.user_id,
        e.session_id,
        p.course_id
    FROM event_logs e
    JOIN preview_event_details p ON p.event_id = e.event_id
    WHERE e.event_type = 'preview_started'
),
preview_completions AS (
    SELECT DISTINCT
        e.user_id,
        e.session_id,
        p.course_id
    FROM event_logs e
    JOIN preview_event_details p ON p.event_id = e.event_id
    WHERE e.event_type = 'preview_completed'
),
purchase_completions AS (
    SELECT DISTINCT
        e.user_id,
        e.session_id,
        r.course_id
    FROM event_logs e
    JOIN request_event_details r ON r.event_id = e.event_id
    WHERE e.event_type = 'purchase_completed'
)
SELECT
    v.course_id,
    COUNT(*) AS viewed_sessions,
    COUNT(ps.session_id) AS preview_started_sessions,
    COUNT(pc.session_id) AS preview_completed_sessions,
    COUNT(pco.session_id) AS purchase_count,
    ROUND(COUNT(pc.session_id)::numeric / NULLIF(COUNT(ps.session_id), 0) * 100, 2)
        AS preview_completion_rate,
    ROUND(COUNT(pco.session_id)::numeric / NULLIF(COUNT(*), 0) * 100, 2)
        AS purchase_conversion_rate
FROM viewed_sessions v
LEFT JOIN preview_starts ps
    ON ps.user_id = v.user_id
    AND ps.session_id = v.session_id
    AND ps.course_id = v.course_id
LEFT JOIN preview_completions pc
    ON pc.user_id = v.user_id
    AND pc.session_id = v.session_id
    AND pc.course_id = v.course_id
LEFT JOIN purchase_completions pco
    ON pco.user_id = v.user_id
    AND pco.session_id = v.session_id
    AND pco.course_id = v.course_id
GROUP BY v.course_id
ORDER BY purchase_conversion_rate DESC, preview_completion_rate DESC;
