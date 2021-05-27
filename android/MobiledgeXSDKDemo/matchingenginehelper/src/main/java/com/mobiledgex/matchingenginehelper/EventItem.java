package com.mobiledgex.matchingenginehelper;

import android.text.format.DateFormat;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * An Activity item.
 */
public class EventItem {
    public final long timestamp;
    public final String timestampText;
    public final String id;
    public final String content;
    public enum EventType {
        DEBUG,
        INFO,
        WARN,
        ERROR
    }
    public EventType eventType;

    public EventItem(EventType type, String content) {
        eventType = type;
        timestamp = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        cal.setTimeInMillis(timestamp);
        // TODO: Update to use device's configured date and time format.
//        this.timestampText = DateFormat.format("MM-dd-yyyy HH:mm:ss", cal).toString();
        timestampText = DateFormat.format("HH:mm:ss", cal).toString();
        id = ""+timestamp;
        this.content = content;
    }

    @Override
    public String toString() {
        return timestamp + " " + content;
    }
}
