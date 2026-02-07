package com.example.authtest;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class SeatPlan extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private AppCompatButton backButton;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private CardView[] seatCards = new CardView[40];
    private String currentClassId = null;
    private final HashMap<String, List<String>> classSeatCache = new HashMap<>();
    private ListenerRegistration classListener;
    private List<StudentAttendanceModel> sortedStudentList = new ArrayList<>();
    private TextView occupiedText;
    private TextView vacantText;
    private TextView falseText;
    private ListenerRegistration attendanceRecordsListener;

    private String classStartTime;
    private static final int totalSeats = 40;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seat_plan);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navigation_view);

        ImageView hamburgerIcon = findViewById(R.id.hamburger_icon);
        hamburgerIcon.setOnClickListener(view -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });

        setupNavigationDrawer();
        setupBackPressHandler();
        loadUserInfoInDrawer();
        checkUserTypeAndConfigureMenu();

        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(view -> {
            finish();
        });

        initializeSeatCards();

        currentClassId = getIntent().getStringExtra("CLASS_ID");

        if (currentClassId == null || currentClassId.isEmpty()) {
            Toast.makeText(this, "Error: Class ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadSeatPlanForClass(currentClassId);

        occupiedText = findViewById(R.id.occupiedText);
        vacantText = findViewById(R.id.vacantText);
        falseText = findViewById(R.id.falseText);
    }
    private void setupAttendanceRecordsListener() {
        if (currentClassId == null || currentClassId.isEmpty()) {
            return;
        }

        if (attendanceRecordsListener != null) {
            attendanceRecordsListener.remove();
        }

        attendanceRecordsListener = db.collection("allClasses")
                .document(currentClassId)
                .collection("attendanceRecords")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        return;
                    }

                    if (snapshots != null) {
                        boolean needsUpdate = false;

                        for (StudentAttendanceModel student : sortedStudentList) {
                            if (student.getStudentId() != null) {
                                for (var doc : snapshots.getDocuments()) {
                                    if (doc.getId().equals(student.getStudentId())) {
                                        Boolean falseMarked = doc.getBoolean("falseMarked");
                                        Long timestamp = doc.getLong("timestamp");
                                        Boolean marked = doc.getBoolean("marked");

                                        String oldStatus = student.getAttendanceStatus();
                                        String newStatus;

                                        if (falseMarked != null && falseMarked) {
                                            newStatus = "False";
                                        } else if (marked != null && marked && timestamp != null) {
                                            newStatus = getAttendanceStatus(timestamp);
                                        } else {
                                            newStatus = "Not Marked";
                                        }

                                        if (!oldStatus.equals(newStatus)) {
                                            student.setAttendanceStatus(newStatus);
                                            student.setMarked(marked != null && marked);
                                            student.setTimestamp(timestamp);
                                            needsUpdate = true;
                                        }
                                        break;
                                    }
                                }
                            }
                        }

                        if (needsUpdate) {
                            updateSeatColors(sortedStudentList.size());
                        }
                    }
                });
    }

    private void initializeSeatCards() {
        for (int i = 0; i < 40; i++) {
            int resId = getResources().getIdentifier("seatPlanCard" + (i + 1), "id", getPackageName());
            seatCards[i] = findViewById(resId);

            final int seatNumber = i + 1;
            seatCards[i].setOnClickListener(view -> {
                onSeatClicked(seatNumber);
            });
        }
    }



    private void onSeatClicked(int seatNumber) {
        if (seatNumber <= sortedStudentList.size()) {
            StudentAttendanceModel student = sortedStudentList.get(seatNumber - 1);

            StudentSeatDialog dialog = new StudentSeatDialog(
                    this,
                    student,
                    currentClassId,
                    new StudentSeatDialog.OnStatusChangedListener() {
                        @Override
                        public void onStatusChanged() {
                            // Refresh the seat colors and counters when status changes
                            refreshSeatCounters();
                            // Optionally reload student data to ensure sync
                            loadSeatPlanForClass(currentClassId);
                        }
                    }
            );
            dialog.show();
        } else {
            Toast.makeText(this, "Seat " + seatNumber + " is empty", Toast.LENGTH_SHORT).show();
        }
    }
    private void refreshSeatCounters() {
        int occupiedCount = 0;
        int falseCount = 0;

        for (StudentAttendanceModel student : sortedStudentList) {
            String status = student.getAttendanceStatus();
            if ("False".equalsIgnoreCase(status)) {
                falseCount++;
            } else if (!"Not Marked".equalsIgnoreCase(status)) {
                occupiedCount++;
            }
        }

        updateSeatCounters(occupiedCount, falseCount);
    }



    private void loadSeatPlanForClass(String classId) {
        clearSeatColors();

        if (classListener != null) {
            classListener.remove();
        }

        // Changed from users/{teacherId}/classes to allClasses - works for both teachers and students
        classListener = db.collection("allClasses")
                .document(classId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error loading seat plan", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        classStartTime = documentSnapshot.getString("startTime");

                        List<String> students =
                                (List<String>) documentSnapshot.get("allowedStudentEmails");

                        if (students == null) students = new ArrayList<>();

                        fetchAndSortStudents(students);
                    } else {
                        updateSeatColors(0);
                    }
                });
    }

    private void fetchAndSortStudents(List<String> allowedEmails) {
        if (allowedEmails == null || allowedEmails.isEmpty()) {
            updateSeatColors(0);
            return;
        }

        List<StudentAttendanceModel> studentList = new ArrayList<>();
        final int[] loadedCount = {0};
        final int totalEmails = allowedEmails.size();

        for (String email : allowedEmails) {
            String cleanEmail = email.toLowerCase().trim();

            db.collection("users")
                    .whereEqualTo("schoolEmail", cleanEmail)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            var userDoc = querySnapshot.getDocuments().get(0);
                            String firstName = userDoc.getString("firstName");
                            String lastName = userDoc.getString("lastName");
                            String studentId = userDoc.getId();

                            StudentAttendanceModel student = new StudentAttendanceModel(
                                    studentId,
                                    cleanEmail,
                                    firstName != null ? firstName : "Unknown",
                                    lastName != null ? lastName : "User"
                            );
                            studentList.add(student);
                            checkStudentAttendance(student);
                        } else {
                            db.collection("users")
                                    .whereEqualTo("email", cleanEmail)
                                    .limit(1)
                                    .get()
                                    .addOnSuccessListener(altQuerySnapshot -> {
                                        if (!altQuerySnapshot.isEmpty()) {
                                            var userDoc = altQuerySnapshot.getDocuments().get(0);
                                            String firstName = userDoc.getString("firstName");
                                            String lastName = userDoc.getString("lastName");
                                            String studentId = userDoc.getId();

                                            StudentAttendanceModel student = new StudentAttendanceModel(
                                                    studentId,
                                                    cleanEmail,
                                                    firstName != null ? firstName : "Unknown",
                                                    lastName != null ? lastName : "User"
                                            );
                                            studentList.add(student);
                                            checkStudentAttendance(student);
                                        }

                                        loadedCount[0]++;
                                        if (loadedCount[0] == totalEmails) {
                                            sortAndUpdateSeats(studentList);
                                        }
                                    });
                            return;
                        }

                        loadedCount[0]++;
                        if (loadedCount[0] == totalEmails) {
                            sortAndUpdateSeats(studentList);
                        }
                    })
                    .addOnFailureListener(e -> {
                        loadedCount[0]++;
                        if (loadedCount[0] == totalEmails) {
                            sortAndUpdateSeats(studentList);
                        }
                    });
        }
    }

    private void checkUserTypeAndConfigureMenu() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        db.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String userType = documentSnapshot.getString("userType");

                        if (userType != null && !"Student".equalsIgnoreCase(userType.trim())) {
                            navigationView.getMenu().findItem(R.id.menu_streak).setVisible(false);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    navigationView.getMenu().findItem(R.id.menu_streak).setVisible(false);
                });
    }

    // Replace checkStudentAttendance method in SeatPlan.java

    private void checkStudentAttendance(StudentAttendanceModel student) {
        if (currentClassId == null || student.getStudentId() == null) {
            student.setAttendanceStatus("Not Marked");
            return;
        }

        db.collection("allClasses")
                .document(currentClassId)
                .collection("attendanceRecords")
                .document(student.getStudentId())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Boolean marked = doc.getBoolean("marked");
                        Long timestamp = doc.getLong("timestamp");
                        Boolean falseMarked = doc.getBoolean("falseMarked");

                        // Check if marked as False first
                        if (falseMarked != null && falseMarked) {
                            student.setMarked(true);
                            student.setTimestamp(timestamp);
                            student.setAttendanceStatus("False");
                        } else if (marked != null && marked && timestamp != null) {
                            student.setMarked(true);
                            student.setTimestamp(timestamp);
                            String status = getAttendanceStatus(timestamp);
                            student.setAttendanceStatus(status);
                        } else {
                            student.setAttendanceStatus("Not Marked");
                        }
                    } else {
                        student.setAttendanceStatus("Not Marked");
                    }
                })
                .addOnFailureListener(e -> {
                    student.setAttendanceStatus("Not Marked");
                });
    }

    private String getAttendanceStatus(long markedTimestamp) {
        if (classStartTime == null || classStartTime.isEmpty()) {
            return "Present";
        }

        try {
            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
            Date startTime = timeFormat.parse(classStartTime);
            Date markedTime = new Date(markedTimestamp);

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String today = dateFormat.format(markedTime);
            String startTimeString = today + " " + classStartTime;

            SimpleDateFormat fullFormat = new SimpleDateFormat("yyyy-MM-dd h:mm a", Locale.getDefault());
            Date classStartDateTime = fullFormat.parse(startTimeString);

            if (classStartDateTime == null) return "Present";

            long differenceMs = markedTimestamp - classStartDateTime.getTime();
            long differenceMinutes = differenceMs / (60 * 1000);

            if (differenceMinutes <= 15) {
                return "Present";
            } else if (differenceMinutes <= 30) {
                return "Late";
            } else {
                return "Absent";
            }

        } catch (ParseException e) {
            return "Present";
        }
    }


    private void sortAndUpdateSeats(List<StudentAttendanceModel> studentList) {
        Collections.sort(studentList, (student1, student2) -> {
            String lastName1 = student1.getLastName() != null ? student1.getLastName().toLowerCase() : "";
            String lastName2 = student2.getLastName() != null ? student2.getLastName().toLowerCase() : "";

            int lastNameComparison = lastName1.compareToIgnoreCase(lastName2);

            if (lastNameComparison == 0) {
                String firstName1 = student1.getFirstName() != null ? student1.getFirstName().toLowerCase() : "";
                String firstName2 = student2.getFirstName() != null ? student2.getFirstName().toLowerCase() : "";
                return firstName1.compareToIgnoreCase(firstName2);
            }

            return lastNameComparison;
        });

        sortedStudentList = new ArrayList<>(studentList);

        updateSeatColors(studentList.size());
        setupAttendanceRecordsListener();

    }

    private void clearSeatColors() {
        int emptyColor = ContextCompat.getColor(this, R.color.empty_seat);
        for (CardView seat : seatCards) {
            seat.setCardBackgroundColor(emptyColor);
        }
    }

    private void updateSeatColors(int studentCount) {
        int emptyColor = ContextCompat.getColor(this, R.color.empty_seat);
        int occupiedColor = ContextCompat.getColor(this, R.color.occupied_seat); // Green
        int falseColor = ContextCompat.getColor(this, R.color.false_seat); // Orange

        int occupiedCount = 0;
        int falseCount = 0;

        for (int i = 0; i < seatCards.length; i++) {
            if (i < studentCount) {
                StudentAttendanceModel student = sortedStudentList.get(i);
                String status = student.getAttendanceStatus();

                if ("False".equalsIgnoreCase(status)) {
                    seatCards[i].setCardBackgroundColor(falseColor);
                    falseCount++;
                } else {
                    seatCards[i].setCardBackgroundColor(occupiedColor);
                    occupiedCount++;
                }
            } else {
                seatCards[i].setCardBackgroundColor(emptyColor);
            }
        }

        // Update counters dynamically
        updateSeatCounters(occupiedCount, falseCount);
    }

    private void updateSeatCounters(int occupiedCount, int falseCount) {
        int totalOccupied = occupiedCount + falseCount;
        int vacantCount = totalSeats - totalOccupied;

        occupiedText.setText(occupiedCount + " Occupied");
        vacantText.setText(vacantCount + " Vacant");
        falseText.setText(falseCount + " False");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (classListener != null) classListener.remove();
        if (attendanceRecordsListener != null) attendanceRecordsListener.remove();
    }


    private void setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.menu_home) {
                navigateToUserHome();
                return true;
            } else if (itemId == R.id.menu_profile) {
                startActivity(new Intent(SeatPlan.this, ProfilePage.class));
                return true;
            } else if (itemId == R.id.menu_streak) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(SeatPlan.this, AttendanceStreak.class));
                return true;
            } else if (itemId == R.id.menu_leaderboards) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(SeatPlan.this, Leaderboards.class));
                return true;
            } else if (itemId == R.id.menu_attendance_history) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(SeatPlan.this, AttendanceHistoryActivity.class));
                return true;
            } else if (itemId == R.id.menu_archive) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(SeatPlan.this, ArchiveActivity.class));
                return true;
            } else if (itemId == R.id.menu_settings) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(SeatPlan.this, SettingsActivity.class));
                return true;
            } else if (itemId == R.id.menu_logout) {
                LogoutConfirmationDialog confirmDialog = new LogoutConfirmationDialog(this, this::logout,
                        () -> {
                            drawerLayout.closeDrawer(GravityCompat.START);
                        }
                );
                confirmDialog.show();
                return true;
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return false;
        });
    }

    private void navigateToUserHome() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    if (documentSnapshot.exists()) {
                        String userType = documentSnapshot.getString("userType");

                        if (userType != null) {
                            if ("Student".equalsIgnoreCase(userType.trim())) {
                                startActivity(new Intent(SeatPlan.this, StudentHome.class));
                            } else if ("Teacher".equalsIgnoreCase(userType.trim())) {
                                startActivity(new Intent(SeatPlan.this, TeacherHome.class));
                            } else {
                                Toast.makeText(SeatPlan.this, "Unknown user type: " + userType, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(SeatPlan.this, "User type not found in document", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(SeatPlan.this, "User document not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    Toast.makeText(SeatPlan.this, "Error loading user info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(SeatPlan.this, SignIn.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                            drawerLayout.closeDrawer(GravityCompat.START);
                        } else {
                            setEnabled(false);
                            getOnBackPressedDispatcher().onBackPressed();
                        }
                    }
                });
    }

    private void loadUserInfoInDrawer() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            View headerView = navigationView.getHeaderView(0);
            TextView userNameTextView = headerView.findViewById(R.id.drawer_user_name);
            TextView userEmailTextView = headerView.findViewById(R.id.drawer_user_email);

            db.collection("users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String userType = documentSnapshot.getString("userType");
                            if ("Student".equalsIgnoreCase(userType)) {
                                navigationView.getMenu().findItem(R.id.menu_streak).setVisible(true);
                            }

                            String firstName = documentSnapshot.getString("firstName");
                            String lastName = documentSnapshot.getString("lastName");
                            String fullName = "";

                            if (firstName != null && !firstName.isEmpty()) {
                                fullName = firstName;
                            }

                            if (lastName != null && !lastName.isEmpty()) {
                                fullName += (fullName.isEmpty() ? "" : " ") + lastName;
                            }

                            userNameTextView.setText(fullName.isEmpty() ? "User" : fullName);
                        } else {
                            userNameTextView.setText("User");
                        }
                    })
                    .addOnFailureListener(e -> {
                        userNameTextView.setText("User");
                    });

            userEmailTextView.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "");
        }
    }
}