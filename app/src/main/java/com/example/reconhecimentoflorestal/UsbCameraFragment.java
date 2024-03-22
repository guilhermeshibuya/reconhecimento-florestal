package com.example.reconhecimentoflorestal;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.herohan.uvcapp.CameraHelper;
import com.herohan.uvcapp.ICameraHelper;
import com.herohan.uvcapp.ImageCapture;
import com.serenegiant.usb.Size;
import com.serenegiant.utils.FileUtils;
import com.serenegiant.widget.AspectRatioSurfaceView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class UsbCameraFragment extends Fragment implements View.OnClickListener {
    private static final int DEFAULT_WIDTH = 640;
    private static final int DEFAULT_HEIGHT = 480;

    private ICameraHelper mCameraHelper;
    private AspectRatioSurfaceView mCameraViewMain;
    ActivityResultLauncher<CropImageContractOptions> cropImage = registerForActivityResult(
            new CropImageContract(),
            result -> {
                if (result.isSuccessful()) {
                    Bitmap cropped = BitmapFactory.decodeFile(result.getUriFilePath(requireContext().getApplicationContext(), true));
                    saveImage(cropped);

                    File tempFile = new File(requireContext().getCacheDir(), "temp.jpg");
                    if (tempFile.exists()) {
                        boolean deleted = tempFile.delete();
                        if (!deleted) {
                            Toast.makeText(requireContext(), "Erro ao excluir a imagem temporária", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    private void launchImageCropper(Uri uri) {
        CropImageOptions cropImageOptions = new CropImageOptions();
        cropImageOptions.imageSourceIncludeGallery = true;
        cropImageOptions.imageSourceIncludeCamera = true;

        cropImageOptions.autoZoomEnabled = true;

        cropImageOptions.toolbarColor = Color.rgb(90, 194,121);
        cropImageOptions.activityMenuTextColor = Color.rgb(0, 22,9);
        cropImageOptions.toolbarBackButtonColor = Color.rgb(0, 22,9);
        cropImageOptions.activityMenuIconColor = Color.rgb(0, 22,9);

        CropImageContractOptions cropImageContractOptions = new CropImageContractOptions(uri, cropImageOptions);

        cropImage.launch(cropImageContractOptions);
    }

    private void saveImage(Bitmap bitmap) {
        File file = FileUtils.getCaptureFile(
                requireContext(),
                Environment.DIRECTORY_DCIM,
                ".jpg");
        try {
            OutputStream outputStream = new FileOutputStream(file);

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");
            requireContext().getApplicationContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            MediaScannerConnection.scanFile(requireContext().getApplicationContext(), new String[]{ file.getAbsolutePath()}, null, null);

            Toast.makeText(
                    requireContext().getApplicationContext(),
                    "Foto salva",
                    Toast.LENGTH_SHORT).show();

            // NOVO CÓDIGO
            ModelUtilities modelUtilities = new ModelUtilities(getContext());

            Bitmap cropped = BitmapFactory.decodeFile(file.getAbsolutePath());

            float[][][][] inputArray = modelUtilities.preprocessImages(cropped);

            modelUtilities.runInference(inputArray);
        } catch (Exception e) {
            Toast.makeText(
                    requireContext().getApplicationContext(),
                    "Erro ao salvar a imagem recortada",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.usb_camera_fragment, container, false);
        initViews(view);

        return view;
    }

    private void initViews(View view) {
        mCameraViewMain = view.findViewById(R.id.svCameraViewMain);
        mCameraViewMain.setAspectRatio(DEFAULT_WIDTH, DEFAULT_HEIGHT);

        mCameraViewMain.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                if (mCameraHelper != null) {
                    mCameraHelper.addSurface(holder.getSurface(), false);
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) { }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                if (mCameraHelper != null) {
                    mCameraHelper.removeSurface(holder.getSurface());
                }
            }
        });

        ImageButton btnCaptureImage = view.findViewById(R.id.btnCaptureImage);
        btnCaptureImage.setOnClickListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        initCameraHelper();
    }

    @Override
    public void onStop() {
        super.onStop();
        clearCameraHelper();
    }

    public void initCameraHelper() {
        if (mCameraHelper == null) {
            mCameraHelper = new CameraHelper();
            mCameraHelper.setStateCallback(mStateListener);
        }
    }

    public void clearCameraHelper() {
        if (mCameraHelper != null) {
            mCameraHelper.release();
            mCameraHelper = null;
        }
    }

    private void selectDevice(final UsbDevice device) {
        Log.d("USB DEVICE", "DEVICE: " + device.getDeviceName());
        mCameraHelper.selectDevice(device);
    }

    private final ICameraHelper.StateCallback mStateListener = new ICameraHelper.StateCallback() {
        @Override
        public void onAttach(UsbDevice device) {
            selectDevice(device);
        }

        @Override
        public void onDeviceOpen(UsbDevice device, boolean isFirstOpen) {
            mCameraHelper.openCamera();
        }

        @Override
        public void onCameraOpen(UsbDevice device) {
            mCameraHelper.startPreview();

            Size size = mCameraHelper.getPreviewSize();
            if (size != null) {
                int width = size.width;
                int height = size.height;

                mCameraViewMain.setAspectRatio(width, height);
            }

            mCameraHelper.addSurface(mCameraViewMain.getHolder().getSurface(), false);
        }

        @Override
        public void onCameraClose(UsbDevice device) {
            if (mCameraHelper != null) {
                mCameraHelper.removeSurface(mCameraViewMain.getHolder().getSurface());
            }
        }

        @Override
        public void onDeviceClose(UsbDevice device) { }

        @Override
        public void onDetach(UsbDevice device) { }

        @Override
        public void onCancel(UsbDevice device) { }
    };

    protected void takePhoto() {
        if (mCameraHelper != null) {
            ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(
                    new File(requireContext().getCacheDir(), "temp.jpg")
            ).build();

            mCameraHelper.takePicture(outputFileOptions, new ImageCapture.OnImageCaptureCallback() {

                @Override
                public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                    launchImageCropper(outputFileResults.getSavedUri());
                }

                @Override
                public void onError(int imageCaptureError, @NonNull String message, @Nullable Throwable cause) {
                    Toast.makeText(
                            requireContext(),
                            message,
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onClick(View v) {
        //if (v.getId() == R.id.btnCaptureImage) {
        if (v.getId() == R.id.btnTakePhoto) {
            if (mCameraHelper != null) {
                File file = FileUtils.getCaptureFile(
                        requireContext(),
                        Environment.DIRECTORY_DCIM,
                        ".jpg");

                ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions.Builder(file).build();

                mCameraHelper.takePicture(options, new ImageCapture.OnImageCaptureCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        launchImageCropper(outputFileResults.getSavedUri());

                        Toast.makeText(
                                requireContext(),
                                "Foto salva",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(int imageCaptureError, @NonNull String message, @Nullable Throwable cause) {
                        Toast.makeText(
                                requireContext(),
                                message,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }
}
