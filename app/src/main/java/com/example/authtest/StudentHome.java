package com.example.authtest;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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
    private boolean isLoadingClasses = false;

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
        if (!isLoadingClasses) {
            loadClasses();
        }
    }

    private void setupRecyclerView() {
        classAdapter = new ClassAdapter(classModel -> {
            Intent intent = new Intent(StudentHome.this, ClassInformation.class);
            intent.putExtra("CLASS_MODEL", classModel);
            startActivity(intent);
        });

        binding.classesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.classesRecyclerView.setAdapter(classAdapter);

        classAdapter.setClasses(classList);
    }

    private void loadClasses() {
        if (isLoadingClasses) {
            Log.d(TAG, "Already loading classes, skipping duplicate call");
            return;
        }

        isLoadingClasses = true;
        String studentId = mAuth.getCurrentUser().getUid();
        Log.d(TAG, "Loading enrolled classes for student: " + studentId);

        classList.clear();
        classAdapter.notifyDataSetChanged();

        db.collection("users")
                .document(studentId)
                .collection("enrolledClasses")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d(TAG, "Found " + querySnapshot.size() + " enrolled classes");

                    if (querySnapshot.isEmpty()) {
                        isLoadingClasses = false;
                        updateUI();
                        return;
                    }

                    final int totalClasses = querySnapshot.size();
                    final int[] loadedClasses = {0};

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
                                            }
                                        }

                                        loadedClasses[0]++;
                                        if (loadedClasses[0] == totalClasses) {
                                            classAdapter.notifyDataSetChanged();
                                            updateUI();
                                            isLoadingClasses = false;
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error loading class details", e);
                                        loadedClasses[0]++;
                                        if (loadedClasses[0] == totalClasses) {
                                            classAdapter.notifyDataSetChanged();
                                            updateUI();
                                            isLoadingClasses = false;
                                        }
                                    });
                        } else {
                            loadedClasses[0]++;
                            if (loadedClasses[0] == totalClasses) {
                                classAdapter.notifyDataSetChanged();
                                updateUI();
                                isLoadingClasses = false;
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading classes", e);
                    isLoadingClasses = false;
                    updateUI();
                });
    }

    private void updateUI() {
        boolean isEmpty = classList.isEmpty();
        binding.emptyStateLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.classesRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        Log.d(TAG, "UI Updated - Total classes displayed: " + classList.size());
    }
}