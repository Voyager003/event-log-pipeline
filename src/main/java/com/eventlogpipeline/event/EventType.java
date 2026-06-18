package com.eventlogpipeline.event;

public enum EventType {
    LECTURE_STARTED("lecture_started"),
    LECTURE_PLAYED("lecture_played"),
    LECTURE_COMPLETED("lecture_completed"),
    VIDEO_ERROR_OCCURRED("video_error_occurred");

    private final String eventName;

    EventType(String eventName) {
        this.eventName = eventName;
    }

    public String eventName() {
        return eventName;
    }
}
