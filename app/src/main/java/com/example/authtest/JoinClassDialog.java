package com.example.authtest;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
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

public class JoinClassDialog extends Dialog {

    private final Runnable onSuccess;
    private static final String TAG = "JoinClassDialog";
    private AppCompatButton joinButton;

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
                    (int)(context.getResources().getDisplayMetrics().widthPixels * 0.92),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        EditText codeInput = findViewById(R.id.classCodeInput);
        joinButton = findViewById(R.id.joinButton);
        AppCompatButton cancelButton = findViewById(R.id.cancelButton);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        cancelButton.setOnClickListener(v -> dismiss());

        joinButton.setOnClickListener(v -> {
            String classCode = codeInput.getText().toString().trim().toUpperCase();

            if (classCode.isEmpty()) {
                codeInput.setError("Required");
                return;
            }

            if (mAuth.getCurrentUser() == null) {
                Toast.makeText(context, "Not authenticated", Toast.LENGTH_SHORT).show();
                return;
            }

            String studentId = mAuth.getCurrentUser().getUid();
            joinButton.setEnabled(false);

            Log.d(TAG, "Searching for class code: " + classCode);

            // First, let's see what's in allClasses
            db.collection("allClasses")
                    .get()
                    .addOnSuccessListener(allSnapshot -> {
                        Log.d(TAG, "Total classes in allClasses: " + allSnapshot.size());
                        for (DocumentSnapshot doc : allSnapshot.getDocuments()) {
                            String code = doc.getString("classCode");
                            Log.d(TAG, "Found class code: '" + code + "'");
                        }
                    });

            // Search in global allClasses collection
            db.collection("allClasses")
                    .whereEqualTo("classCode", classCode)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        Log.d(TAG, "Query result: " + snapshot.size() + " documents");

                        if (snapshot.isEmpty()) {
                            Log.d(TAG, "Class not found with code: " + classCode);
                            codeInput.setError("Invalid code");
                            joinButton.setEnabled(true);
                            return;
                        }

                        DocumentSnapshot doc = snapshot.getDocuments().get(0);
                        ClassModel classModel = doc.toObject(ClassModel.class);
                        String classId = doc.getId();

                        if (classModel != null) {
                            classModel.setId(classId);
                            Log.d(TAG, "Class found: " + classModel.getClassName());
                            addEnrolledClass(db, studentId, classId, classModel, context);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error searching classes", e);
                        Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        joinButton.setEnabled(true);
                    });
        });
    }

    private void addEnrolledClass(FirebaseFirestore db, String studentId, String classId, ClassModel classModel, Context context) {
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
                    Log.d(TAG, "Successfully joined class!");
                    Toast.makeText(context, "Successfully joined class!", Toast.LENGTH_SHORT).show();
                    dismiss();
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error enrolling in class", e);
                    Toast.makeText(context, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    joinButton.setEnabled(true);
                });
    }
}