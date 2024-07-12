package com.example.reconhecimentoflorestal;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.google.common.util.concurrent.ListenableFuture;
import com.serenegiant.utils.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class BackCameraFragment extends Fragment {
    private MainActivity mainActivity;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture imageCapture;
    private Camera camera;
    private ProcessCameraProvider cameraProvider;
    private boolean isTorchOn = false;
    private PreviewView previewView;
    private SharedViewModel viewModel;
    //
    private BoundingBoxImageView boundingBoxImageView;
    private String savedImagePath;

    public String getSavedImagePath() {
        return savedImagePath;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        mainActivity = (MainActivity) requireActivity();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.back_camera_fragment, container, false);

        previewView = view.findViewById(R.id.previewView);
        //
        boundingBoxImageView = view.findViewById(R.id.boundingBoxImageView);

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
//                        launchImageCropper(outputFileResults.getSavedUri());
//                        displayCapturedImage(outputFileResults.getSavedUri());
                        saveImage(outputFileResults.getSavedUri());
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

    private void saveImage(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri);
            Bitmap rotated = correctImageOrientation(bitmap, uri);
            File file = FileUtils.getCaptureFile(
                    requireContext(),
                    Environment.DIRECTORY_DCIM,
                    ".jpg");

            OutputStream outputStream = new FileOutputStream(file);
            rotated.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
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

            savedImagePath = file.getAbsolutePath();

            displayCapturedImage(file.getAbsolutePath());

            File tempFile = new File(requireContext().getCacheDir(), "temp.jpg");
            if (tempFile.exists()) {
                boolean deleted = tempFile.delete();
                if (!deleted) {
                    Toast.makeText(requireContext(), "Erro ao excluir a imagem temporária", Toast.LENGTH_SHORT).show();
                }
            }
//            viewModel.setImage(bitmap);

//            switchToResultsFragment();
        } catch (IOException e) {
            Toast.makeText(
                    requireContext().getApplicationContext(),
                    "Erro ao salvar a imagem",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void displayCapturedImage(String imagePath) {
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        if (bitmap != null) {
            Log.d("BackCameraFragment", "Imagem carregada com sucesso");
            boundingBoxImageView.setImageBitmap(bitmap);
            boundingBoxImageView.setVisibility(View.VISIBLE);

            Button btnConfirmBoundingBox = getActivity().findViewById(R.id.btnConfirmBoundingBox);
            btnConfirmBoundingBox.setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(requireContext(), "Erro ao carregar a imagem", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap correctImageOrientation(Bitmap bitmap, Uri uri) throws IOException {
        ExifInterface exif = new ExifInterface(requireContext().getContentResolver().openInputStream(uri));
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            default:
                return bitmap;
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private void switchToResultsFragment() {
        mainActivity.switchToResultsFragment();
    }

    public BoundingBoxImageView getBoundingBoxImageView() {
        return this.boundingBoxImageView;
    }
}