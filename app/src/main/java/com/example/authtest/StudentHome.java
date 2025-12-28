package com.example.authtest;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.authtest.databinding.ActivityStudentHomeBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class StudentHome extends AppCompatActivity {

    private ActivityStudentHomeBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private ClassAdapter classAdapter;
    private List<ClassModel> classList = new ArrayList<>();
    private static final String TAG = "StudentHome";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityStudentHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setupRecyclerView();
        loadClasses();

        binding.joinClassButton.setOnClickListener(v -> {
            JoinClassDialog dialog = new JoinClassDialog(this, this::loadClasses);
            dialog.show();
        });

        binding.ctaButton.setOnClickListener(v -> {
            JoinClassDialog dialog = new JoinClassDialog(this, this::loadClasses);
            dialog.show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadClasses();
    }

    private void setupRecyclerView() {
        classAdapter = new ClassAdapter(classModel -> {
            // Open class information when clicked
            Intent intent = new Intent(StudentHome.this, ClassInformation.class);
            intent.putExtra("CLASS_MODEL", classModel);
            startActivity(intent);
        });

        binding.classesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.classesRecyclerView.setAdapter(classAdapter);

        classAdapter.setClasses(classList);
    }

    private void loadClasses() {
        String studentId = mAuth.getCurrentUser().getUid();
        Log.d(TAG, "Loading enrolled classes for student: " + studentId);

        // Load from enrolledClasses subcollection
        db.collection("users")
                .document(studentId)
                .collection("enrolledClasses")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d(TAG, "Found " + querySnapshot.size() + " enrolled classes");

                    classList.clear();

                    // Get full class details from allClasses collection
                    for (var doc : querySnapshot) {
                        String classId = doc.getString("classId");

                        if (classId != null) {
                            db.collection("allClasses")
                                    .document(classId)
                                    .get()
                                    .addOnSuccessListener(classDoc -> {
                                        if (classDoc.exists()) {
                                            ClassModel model = classDoc.toObject(ClassModel.class);
                                            if (model != null) {
                                                model.setId(classDoc.getId());
                                                classList.add(model);
                                                classAdapter.notifyDataSetChanged();
                                                updateUI();
                                            }
                                        }
                                    });
                        }
                    }

                    // Update UI immediately in case there are no classes
                    if (querySnapshot.isEmpty()) {
                        updateUI();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading classes", e);
                    updateUI();
                });
    }

    private void updateUI() {
        boolean isEmpty = classList.isEmpty();
        binding.emptyStateLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.classesRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }
}