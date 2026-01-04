package com.example.authtest;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.authtest.databinding.ActivityTeacherHomeBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class TeacherHome extends AppCompatActivity implements ClassAdapter.OnClassClickListener {

    private ActivityTeacherHomeBinding binding;
    private ClassAdapter classAdapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private static final String TAG = "TeacherHome";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTeacherHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        binding.classesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        classAdapter = new ClassAdapter(this);
        binding.classesRecyclerView.setAdapter(classAdapter);

        binding.createClassButton.setOnClickListener(v -> openCreateClass());
        binding.ctaButton.setOnClickListener(v -> openCreateClass());

        loadClasses();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadClasses();
    }

    private void openCreateClass() {
        Intent intent = new Intent(TeacherHome.this, CreateClass.class);
        startActivity(intent);
    }

    private void loadClasses() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            showEmptyState();
            return;
        }

        String userId = currentUser.getUid();
        Log.d(TAG, "Loading classes for user: " + userId);

        db.collection("users")
                .document(userId)
                .collection("classes")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Classes loaded: " + queryDocumentSnapshots.size());

                    List<ClassModel> classes = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        ClassModel classModel = document.toObject(ClassModel.class);
                        classModel.setId(document.getId());
                        classes.add(classModel);
                        Log.d(TAG, "Loaded class: " + classModel.getClassName());
                    }

                    if (classes.isEmpty()) {
                        showEmptyState();
                    } else {
                        showClasses(classes);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading classes", e);
                    Toast.makeText(TeacherHome.this, "Error loading classes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showEmptyState();
                });
    }

    private void showEmptyState() {
        binding.emptyStateLayout.setVisibility(View.VISIBLE);
        binding.classesRecyclerView.setVisibility(View.GONE);
    }

    private void showClasses(List<ClassModel> classes) {
        binding.emptyStateLayout.setVisibility(View.GONE);
        binding.classesRecyclerView.setVisibility(View.VISIBLE);
        classAdapter.setClasses(classes);
    }

    @Override
    public void onClassClick(ClassModel classModel) {
        Intent intent = new Intent(TeacherHome.this, ClassInformation.class);
        intent.putExtra("CLASS_MODEL", classModel);
        startActivity(intent);
    }
}