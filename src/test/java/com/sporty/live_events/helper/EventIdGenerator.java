package com.sporty.live_events.helper;

import java.util.Random;

public class EventIdGenerator {
    private static final Random random = new Random();
    private static final long MIN_ID = 1000;
    private static final long MAX_ID = 9999;

    public static long generateValidEventId() {
        return random.nextLong(MAX_ID - MIN_ID + 1) + MIN_ID;
    }

    public static long generateInvalidEventId() {
        return random.nextLong(MIN_ID);
    }
}