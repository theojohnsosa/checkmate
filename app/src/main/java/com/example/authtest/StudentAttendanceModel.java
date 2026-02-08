package com.example.authtest;

public class StudentAttendanceModel {

    private String studentId;
    private String email;
    private String firstName;
    private String lastName;
    private String attendanceStatus;
    private String originalStatus;
    private Long timestamp;
    private boolean marked;
    private boolean falseMarked;
    private String classId;

    public StudentAttendanceModel() {

    }

    public StudentAttendanceModel(String studentId, String email, String firstName, String lastName) {
        this.studentId = studentId;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.attendanceStatus = "Not Marked";
        this.originalStatus = null;
        this.marked = false;
        this.falseMarked = false;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getAttendanceStatus() {
        return attendanceStatus;
    }

    public void setAttendanceStatus(String attendanceStatus) {
        this.attendanceStatus = attendanceStatus;
    }

    public String getOriginalStatus() {
        return originalStatus;
    }

    public void setOriginalStatus(String originalStatus) {
        this.originalStatus = originalStatus;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isMarked() {
        return marked;
    }

    public void setMarked(boolean marked) {
        this.marked = marked;
    }

    public boolean isFalseMarked() {
        return falseMarked;
    }

    public void setFalseMarked(boolean falseMarked) {
        this.falseMarked = falseMarked;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }
}