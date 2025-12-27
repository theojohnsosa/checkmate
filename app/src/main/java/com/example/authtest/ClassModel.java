package com.example.authtest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

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
    private long createdAt;

    // Empty constructor for Firestore
    public ClassModel() {
        this.students = 0;
    }

    // Constructor with parameters (auto-generate classCode if null or empty)
    public ClassModel(String className, String classCode, String subjectCode,
                      String classDays, String startTime, String endTime,
                      String room, String teacher) {

        this.className = className;
        this.classCode = (classCode == null || classCode.isEmpty()) ? generateClassCode() : classCode;
        this.subjectCode = subjectCode;
        this.classDays = classDays;
        this.startTime = startTime;
        this.endTime = endTime;
        this.room = room;
        this.teacher = teacher;
        this.students = 0;
        this.createdAt = System.currentTimeMillis();
    }

    // Generate a 6-character alphanumeric class code
    private String generateClassCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 6; i++) {
            int idx = random.nextInt(chars.length());
            code.append(chars.charAt(idx));
        }
        return code.toString();
    }

    // Getters
    public String getId() { return id; }
    public String getClassName() { return className; }
    public String getClassCode() { return classCode; }
    public String getSubjectCode() { return subjectCode; }
    public String getClassDays() { return classDays; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getRoom() { return room; }
    public String getTeacher() { return teacher; }
    public int getStudents() { return students; }
    public long getCreatedAt() { return createdAt; }

    // Get days as a List (splits "Wed/Sat" into ["Wed", "Sat"])
    public List<String> getDays() {
        if (classDays != null && !classDays.isEmpty()) {
            return Arrays.asList(classDays.split("/"));
        }
        return new ArrayList<>();
    }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setClassName(String className) { this.className = className; }

    // Auto-generate classCode if null/empty
    public void setClassCode(String classCode) {
        if (classCode == null || classCode.isEmpty()) {
            this.classCode = generateClassCode();
        } else {
            this.classCode = classCode;
        }
    }

    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }
    public void setClassDays(String classDays) { this.classDays = classDays; }

    // Set days from a List (converts ["Wed", "Sat"] to "Wed/Sat")
    public void setDays(List<String> days) {
        if (days != null && !days.isEmpty()) {
            this.classDays = String.join("/", days);
        } else {
            this.classDays = "";
        }
    }

    public void setStartTime(String startTime) { this.startTime = startTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public void setRoom(String room) { this.room = room; }
    public void setTeacher(String teacher) { this.teacher = teacher; }
    public void setStudents(int students) { this.students = students; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}