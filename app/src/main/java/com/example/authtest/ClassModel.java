package com.example.authtest;

import java.io.Serializable;

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

    public ClassModel() {
        // Required empty constructor for Firestore
    }

    public ClassModel(String className, String classCode, String subjectCode,
                      String classDays, String startTime, String endTime,
                      String room, String teacher, int students) {
        this.className = className;
        this.classCode = classCode;
        this.subjectCode = subjectCode;
        this.classDays = classDays;
        this.startTime = startTime;
        this.endTime = endTime;
        this.room = room;
        this.teacher = teacher;
        this.students = students;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getClassName() { return className; }
    public String getClassCode() { return classCode; }
    public String getSubjectCode() { return subjectCode; }
    public String getClassDays() { return classDays; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getRoom() { return room; }
    public String getTeacher() { return teacher; }
    public int getStudents() { return students; }

    public void setStudents(int students) { this.students = students; }
}