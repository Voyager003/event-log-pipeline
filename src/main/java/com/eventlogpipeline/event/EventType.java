package com.eventlogpipeline.event;

public enum EventType {
    COURSE_DETAIL_VIEWED("course_detail_viewed"),
    PREVIEW_STARTED("preview_started"),
    PREVIEW_COMPLETED("preview_completed"),
    CHECKOUT_OPENED("checkout_opened"),
    PURCHASE_SUBMITTED("purchase_submitted"),
    PURCHASE_COMPLETED("purchase_completed");

    private final String eventName;

    EventType(String eventName) {
        this.eventName = eventName;
    }

    public String eventName() {
        return eventName;
    }
}
