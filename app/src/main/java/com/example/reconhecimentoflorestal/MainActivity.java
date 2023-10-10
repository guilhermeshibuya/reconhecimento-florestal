package com.example.reconhecimentoflorestal;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.google.android.material.button.MaterialButton;
import com.hjq.permissions.XXPermissions;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initListeners();
    }

    private void initListeners() {
        MaterialButton btnOpenCamera = findViewById(R.id.btnOpenCamera);
        btnOpenCamera.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        List<String> needPermissions = new ArrayList<>();
        needPermissions.add(android.Manifest.permission.CAMERA);
        needPermissions.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE);

        XXPermissions.with(this)
                .permission(needPermissions)
                .request((permissions, all) -> {
                    if (!all) {
                        return;
                    }

                    if (v.getId() == R.id.btnOpenCamera) {
                        startActivity(new Intent(this, CameraActivity.class));
                    }
                });
    }
}