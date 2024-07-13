package com.example.reconhecimentoflorestal;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.serenegiant.utils.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import ai.onnxruntime.OrtException;

public class CameraFragment extends Fragment implements View.OnClickListener {
    private boolean isBackCameraActive = true;
    private UsbCameraFragment usbCameraFragment;
    private BackCameraFragment backCameraFragment;
    private Button btnConfirmBoundingBox;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.camera_fragment , container, false);

        usbCameraFragment = new UsbCameraFragment();
        backCameraFragment = new BackCameraFragment();

        getChildFragmentManager().beginTransaction()
                .replace(R.id.cameraFrameLayout, backCameraFragment)
                .commit();

        ImageButton btnSwitchCamera = view.findViewById(R.id.btnSwitchCamera);
        btnSwitchCamera.setOnClickListener(this);

        ImageButton btnTakePhoto = view.findViewById(R.id.btnTakePhoto);
        btnTakePhoto.setOnClickListener(this);

        ImageButton btnEnableTorch = view.findViewById(R.id.btnEnableTorch);
        btnEnableTorch.setOnClickListener(this);

        btnConfirmBoundingBox = view.findViewById(R.id.btnConfirmBoundingBox);
        btnConfirmBoundingBox.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnSwitchCamera) {
            toggleCameraView();
        } else if (v.getId() == R.id.btnTakePhoto) {
            if (isBackCameraActive) {
                backCameraFragment.takePhoto();
            } else {
                usbCameraFragment.takePhoto();
            }
        } else if (v.getId() == R.id.btnEnableTorch) {
            if (isBackCameraActive) {
                backCameraFragment.enableTorch();
            }
        }
    }

    private void toggleCameraView() {
        if (isBackCameraActive) {
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.cameraFrameLayout, usbCameraFragment)
                    .commit();
        } else {
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.cameraFrameLayout, backCameraFragment)
                    .commit();
        }

        isBackCameraActive = !isBackCameraActive;
    }
}
