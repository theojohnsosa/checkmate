package com.example.authtest;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RecentSession implements Serializable {
    private String sessionId;
    private String classId;
    private long sessionStartTime;
    private long sessionEndTime;
    private String startTime;
    private String endTime;
    private String date;

    public RecentSession() {

    }

    public RecentSession(String sessionId, String classId, long sessionStartTime, long sessionEndTime) {
        this.sessionId = sessionId;
        this.classId = classId;
        this.sessionStartTime = sessionStartTime;
        this.sessionEndTime = sessionEndTime;
        formatTimes();
    }

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

    public String getSessionTimeRange() {
        return startTime + " - " + endTime;
    }
}