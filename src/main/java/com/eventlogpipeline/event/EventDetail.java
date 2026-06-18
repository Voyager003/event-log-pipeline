package com.eventlogpipeline.event;

import java.math.BigDecimal;

public sealed interface EventDetail permits EventDetail.Lecture, EventDetail.VideoError {

    record Lecture(String courseId, String lectureId, String lectureTitle, int playbackPositionSeconds,
                   int watchDurationSeconds, BigDecimal completionRate) implements EventDetail {
    }

    record VideoError(String courseId, String lectureId, String errorType, String errorCode, String errorMessage)
            implements EventDetail {
    }
}
