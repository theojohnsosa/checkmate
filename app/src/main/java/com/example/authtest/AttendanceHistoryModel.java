package com.example.authtest;

public class AttendanceHistoryModel {

    private String classId;
    private String className;
    private String classCode;
    private String subjectCode;
    private String startTime;
    private String endTime;
    private long dateAdded;

    public AttendanceHistoryModel(
            String classId,
            String className,
            String classCode,
            String subjectCode,
            String startTime,
            String endTime,
            long dateAdded) {
        this.classId = classId;
        this.className = className;
        this.classCode = classCode;
        this.subjectCode = subjectCode;
        this.startTime = startTime;
        this.endTime = endTime;
        this.dateAdded = dateAdded;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getClassCode() {
        return classCode;
    }

    public void setClassCode(String classCode) {
        this.classCode = classCode;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
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

    public long getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(long dateAdded) {
        this.dateAdded = dateAdded;
    }
}