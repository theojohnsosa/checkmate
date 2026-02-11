package com.example.authtest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// Data model for class information
public class ClassModel implements Serializable {

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

    /*
        Default constructor initializes collection fields and boolean flags
        Parameterized constructor sets all class properties
        Initializes allowedStudentEmails as empty ArrayList
     */
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

    public List<String> getAllowedStudentEmails() {
        return allowedStudentEmails != null ? allowedStudentEmails : new ArrayList<>();
    }

    public boolean isAttendanceActive() {
        return isAttendanceActive;
    }

    public boolean isArchived() {
        return isArchived;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    // setStudents(): updates student count when students join/leave
    public void setStudents(int students) {
        this.students = students;
    }

    // setAllowedStudentEmails(): updates list of allowed student emails
    public void setAllowedStudentEmails(List<String> allowedStudentEmails) {
        this.allowedStudentEmails = allowedStudentEmails;
    }

    // setAttendanceActive(): toggles attendance session state
    public void setAttendanceActive(boolean attendanceActive) {
        isAttendanceActive = attendanceActive;
    }

    // setArchived(): marks class as archived
    public void setArchived(boolean archived) {
        isArchived = archived;
    }

    // setCreatedAt(): records class creation timestamp
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}