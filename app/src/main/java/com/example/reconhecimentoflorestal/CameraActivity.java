package com.example.reconhecimentoflorestal;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class CameraActivity extends AppCompatActivity implements View.OnClickListener {
    private boolean isCameraActive = true;
    private UsbCameraFragment usbCameraFragment;
    private BackCameraFragment backCameraFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        usbCameraFragment = new UsbCameraFragment();
        backCameraFragment = new BackCameraFragment();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frameLayout,backCameraFragment)
                    .commit();
        }

        ImageButton btnSwitchCamera = findViewById(R.id.btnSwitchCamera);
        btnSwitchCamera.setOnClickListener(this);
        ImageButton btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnTakePhoto.setOnClickListener(this);
        ImageButton btnEnableTorch = findViewById(R.id.btnEnableTorch);
        btnEnableTorch.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnSwitchCamera) {
            toggleCameraView();
        } else if (v.getId() == R.id.btnTakePhoto) {
            if (isCameraActive) {
                backCameraFragment.takePhoto();
            } else {
                usbCameraFragment.takePhoto();
            }
        } else if (v.getId() == R.id.btnEnableTorch) {
            if (isCameraActive) {
                backCameraFragment.enableTorch();
            }
        }
    }

    private void toggleCameraView() {
        if (isCameraActive) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frameLayout, usbCameraFragment)
                    .commit();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frameLayout, backCameraFragment)
                    .commit();
        }

        isCameraActive = !isCameraActive;
    }
}