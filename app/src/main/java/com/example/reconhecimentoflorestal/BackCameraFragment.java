package com.example.reconhecimentoflorestal;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.google.common.util.concurrent.ListenableFuture;
import com.serenegiant.utils.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class BackCameraFragment extends Fragment {
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture imageCapture;
    private Camera camera;
    private ProcessCameraProvider cameraProvider;
    private boolean isTorchOn = false;
    PreviewView previewView;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.back_camera_fragment, container, false);

        previewView = view.findViewById(R.id.previewView);

        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                startCamera(cameraProvider);
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, getExecutor());

        return view;
    }

    private Executor getExecutor() {
        return ContextCompat.getMainExecutor(requireContext());
    }

    private void startCamera(ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageCapture);
    }

    protected void enableTorch() {
        if (camera.getCameraInfo().hasFlashUnit()) {
            if (!isTorchOn) {
                camera.getCameraControl().enableTorch(true);
                isTorchOn = true;
            } else {
                camera.getCameraControl().enableTorch(false);
                isTorchOn = false;
            }
        }
    }

    protected void takePhoto() {
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(
                new File(requireContext().getCacheDir(), "temp.jpg")
        ).build();

        imageCapture.takePicture(
                outputFileOptions,
                getExecutor(),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        launchImageCropper(outputFileResults.getSavedUri());
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Toast.makeText(
                                requireContext(),
                                "Erro ao salvar a foto: " + exception.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

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

            Bitmap cropped = BitmapFactory.decodeFile(file.getAbsolutePath());

            SharedViewModel sharedViewModel = new ViewModelProvider(this).get(SharedViewModel.class);
            sharedViewModel.setImage(cropped);

            // NOVO CÓDIGO
//            ModelUtilities modelUtilities = new ModelUtilities(getContext());

//            Bitmap cropped = BitmapFactory.decodeFile(file.getAbsolutePath());

//            float[][][][] inputArray = modelUtilities.preprocessImages(cropped);

//            modelUtilities.runInference(inputArray);
        } catch (Exception e) {
            Toast.makeText(
                    requireContext().getApplicationContext(),
                    "Erro ao salvar a imagem recortada",
                    Toast.LENGTH_SHORT).show();
        }
    }
}