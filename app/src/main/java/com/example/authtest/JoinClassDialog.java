package com.example.authtest;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.widget.AppCompatButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.List;

// Dialog allowing students to join classes by entering class code
public class JoinClassDialog extends Dialog {

    private final Runnable onSuccess;
    private AppCompatButton joinButton;

    /*
         Creates dialog with transparent background
         Sets up EditText for code input and Join/Cancel buttons
     */
    public JoinClassDialog(Context context, Runnable onSuccess) {
        super(context);
        this.onSuccess = onSuccess;

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_join_class);
        setCancelable(true);

        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(
                    (int) (context.getResources().getDisplayMetrics().widthPixels * 0.92),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        EditText codeInput = findViewById(R.id.classCodeInput);
        joinButton = findViewById(R.id.joinButton);
        AppCompatButton cancelButton = findViewById(R.id.cancelButton);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        cancelButton.setOnClickListener(view -> {
            dismiss();
        });

        /*
             Gets entered class code and converts to uppercase
             Validates code is not empty
             Gets current user's email for authorization check
             Queries allClasses collection for matching classCode
         */
        joinButton.setOnClickListener(view -> {
            String classCode = codeInput.getText().toString().trim().toUpperCase();

            if (classCode.isEmpty()) {
                codeInput.setError("Required");
                return;
            }

            if (mAuth.getCurrentUser() == null) {
                Toast.makeText(context, "Not authenticated", Toast.LENGTH_SHORT).show();
                return;
            }

            String rawEmail = mAuth.getCurrentUser().getEmail();
            String studentId = mAuth.getCurrentUser().getUid();

            if (rawEmail == null) {
                Toast.makeText(context, "Error: Could not retrieve your email", Toast.LENGTH_SHORT).show();
                return;
            }

            final String studentEmail = rawEmail.toLowerCase().trim();

            joinButton.setEnabled(false);

            db.collection("allClasses")
                    .whereEqualTo("classCode", classCode)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(snapshot -> {

                        if (snapshot.isEmpty()) {
                            codeInput.setError("Invalid code");
                            joinButton.setEnabled(true);
                            return;
                        }

                        DocumentSnapshot doc = snapshot.getDocuments().get(0);
                        ClassModel classModel = doc.toObject(ClassModel.class);

                        if (classModel == null) {
                            Toast.makeText(context, "Invalid class data", Toast.LENGTH_SHORT).show();
                            joinButton.setEnabled(true);
                            return;
                        }

                        String classId = doc.getId();

                        List<String> allowedEmails = classModel.getAllowedStudentEmails();

                        if (allowedEmails == null || allowedEmails.isEmpty()) {
                            Toast.makeText(context, "No students have been added to this class yet. Contact your teacher.", Toast.LENGTH_LONG).show();
                            joinButton.setEnabled(true);
                            return;
                        }

                        boolean isAllowed = false;
                        for (String allowedEmail : allowedEmails) {
                            if (allowedEmail != null && allowedEmail.equalsIgnoreCase(studentEmail)) {
                                isAllowed = true;
                                break;
                            }
                        }

                        if (!isAllowed) {
                            Toast.makeText(context, "You are not authorized to join this class. Contact your teacher.", Toast.LENGTH_LONG).show();
                            joinButton.setEnabled(true);
                            return;
                        }

                        checkIfAlreadyEnrolled(db, studentId, classId, classModel, context);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        joinButton.setEnabled(true);
                    });
        });
    }

    /*
         Queries student's enrolledClasses subcollection for this classId
         Shows error if student already enrolled
         Otherwise calls addEnrolledClass()
     */
    private void checkIfAlreadyEnrolled(
            FirebaseFirestore db,
            String studentId,
            String classId,
            ClassModel classModel,
            Context context
    ) {
        db.collection("users")
                .document(studentId)
                .collection("enrolledClasses")
                .document(classId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Toast.makeText(
                                context,
                                "You have already joined this class.",
                                Toast.LENGTH_SHORT
                        ).show();
                        joinButton.setEnabled(true);
                    } else {
                        addEnrolledClass(db, studentId, classId, classModel, context);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error checking enrollment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    joinButton.setEnabled(true);
                });
    }

    /*
         Creates enrollment data with classId, code, name, enrolledAt timestamp
         Saves to student's enrolledClasses subcollection
         Shows success message and calls onSuccess runnable
         Parent activity can then refresh student's class list
     */
    private void addEnrolledClass(
            FirebaseFirestore db,
            String studentId,
            String classId,
            ClassModel classModel,
            Context context
    ) {
        HashMap<String, Object> enrollmentData = new HashMap<>();
        enrollmentData.put("classId", classId);
        enrollmentData.put("classCode", classModel.getClassCode());
        enrollmentData.put("className", classModel.getClassName());
        enrollmentData.put("enrolledAt", System.currentTimeMillis());

        db.collection("users")
                .document(studentId)
                .collection("enrolledClasses")
                .document(classId)
                .set(enrollmentData)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(context, "Successfully joined class!", Toast.LENGTH_SHORT).show();
                    dismiss();
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    joinButton.setEnabled(true);
                });
    }
}