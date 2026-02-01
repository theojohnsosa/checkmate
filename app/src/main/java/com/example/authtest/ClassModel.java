package com.example.authtest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ClassModel implements Serializable {

    private String id;
    private String className;
    private String classCode;
    private String subjectCode;
    private String classDays;
    private String startTime;
    private String endTime;
    private String room;
    private String teacher;
    private int students;
    private List<String> allowedStudentEmails;
    private boolean isAttendanceActive = false;
    private String teacherId;
    private boolean isArchived = false;
    private long createdAt = 0;

    public ClassModel() {
        this.allowedStudentEmails = new ArrayList<>();
        this.isAttendanceActive = false;
        this.isArchived = false;
        this.createdAt = 0;
    }

    public ClassModel(
            String className,
            String classCode,
            String subjectCode,
            String classDays,
            String startTime,
            String endTime,
            String room,
            String teacher,
            int students,
            String teacherId
    ) {
        this.className = className;
        this.classCode = classCode;
        this.subjectCode = subjectCode;
        this.classDays = classDays;
        this.startTime = startTime;
        this.endTime = endTime;
        this.room = room;
        this.teacher = teacher;
        this.students = students;
        this.teacherId = teacherId;
        this.allowedStudentEmails = new ArrayList<>();
        this.isAttendanceActive = false;
        this.isArchived = false;
        this.createdAt = 0;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClassName() {
        return className;
    }

    public String getClassCode() {
        return classCode;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public String getClassDays() {
        return classDays;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getRoom() {
        return room;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public int getStudents() {
        return students;
    }

    public void setStudents(int students) {
        this.students = students;
    }

    public List<String> getAllowedStudentEmails() {
        return allowedStudentEmails != null ? allowedStudentEmails : new ArrayList<>();
    }

    public void setAllowedStudentEmails(List<String> allowedStudentEmails) {
        this.allowedStudentEmails = allowedStudentEmails;
    }

    public boolean isAttendanceActive() {
        return isAttendanceActive;
    }

    public void setAttendanceActive(boolean attendanceActive) {
        isAttendanceActive = attendanceActive;
    }

    public boolean isArchived() {
        return isArchived;
    }

    public void setArchived(boolean archived) {
        isArchived = archived;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}