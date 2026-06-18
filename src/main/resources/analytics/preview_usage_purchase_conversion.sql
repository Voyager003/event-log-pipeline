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
),
session_groups AS (
    SELECT
        v.user_id,
        v.session_id,
        v.course_id,
        CASE
            WHEN pc.session_id IS NOT NULL THEN 'completed'
            WHEN ps.session_id IS NOT NULL THEN 'started_only'
            ELSE 'not_started'
        END AS preview_usage_group,
        pco.session_id IS NOT NULL AS purchased
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
)
SELECT
    preview_usage_group,
    COUNT(*) AS session_count,
    COUNT(*) FILTER (WHERE purchased) AS purchase_count,
    ROUND(COUNT(*) FILTER (WHERE purchased)::numeric / NULLIF(COUNT(*), 0) * 100, 2)
        AS purchase_conversion_rate
FROM session_groups
GROUP BY preview_usage_group
ORDER BY CASE preview_usage_group
    WHEN 'not_started' THEN 1
    WHEN 'started_only' THEN 2
    WHEN 'completed' THEN 3
END;
