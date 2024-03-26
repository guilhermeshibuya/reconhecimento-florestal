package com.example.reconhecimentoflorestal;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class CameraFragment extends Fragment implements View.OnClickListener {
    private boolean isBackCameraActive = true;
    private UsbCameraFragment usbCameraFragment;
    private BackCameraFragment backCameraFragment;

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
