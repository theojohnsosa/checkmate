package com.example.authtest;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// Data model for attendance session (time range when attendance was recorded)
public class RecentSession implements Serializable {

    private String sessionId;
    private String classId;
    private long sessionStartTime;
    private long sessionEndTime;
    private String startTime;
    private String endTime;
    private String date;

    /*
         Takes sessionId, classId, sessionStartTime, sessionEndTime as parameters
         Calls formatTimes() to convert timestamps to display strings
     */
    public RecentSession(String sessionId, String classId, long sessionStartTime, long sessionEndTime) {
        this.sessionId = sessionId;
        this.classId = classId;
        this.sessionStartTime = sessionStartTime;
        this.sessionEndTime = sessionEndTime;
        formatTimes();
    }

    /*
         Uses SimpleDateFormat to convert long timestamps to date/time strings
         Date format: "MM/dd/yyyy"
         Time format: "h:mm a" (12-hour with AM/PM)
         Stores formatted strings for UI display
     */
    private void formatTimes() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

        Date startDate = new Date(sessionStartTime);
        Date endDate = new Date(sessionEndTime);

        this.date = dateFormat.format(startDate);
        this.startTime = timeFormat.format(startDate);
        this.endTime = timeFormat.format(endDate);
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public long getSessionStartTime() {
        return sessionStartTime;
    }

    public void setSessionStartTime(long sessionStartTime) {
        this.sessionStartTime = sessionStartTime;
        formatTimes();
    }

    public long getSessionEndTime() {
        return sessionEndTime;
    }

    public void setSessionEndTime(long sessionEndTime) {
        this.sessionEndTime = sessionEndTime;
        formatTimes();
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    /*
         Returns concatenated string: "startTime - endTime"
         Example: "9:00 AM - 9:47 AM"
     */
    public String getSessionTimeRange() {
        return startTime + " - " + endTime;
    }
}