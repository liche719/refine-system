package com.achobeta.refine.contracts.event;

public final class EventTopics {
    public static final String EXCHANGE = "refine.domain.events";
    public static final String DEAD_LETTER_EXCHANGE = "refine.domain.events.dlx";
    public static final String USER_LOGGED_IN = "identity.user.logged-in.v1";
    public static final String LEARNING_ACTIVITY_RECORDED = "learning.activity.recorded.v1";

    private EventTopics() {
    }
}
