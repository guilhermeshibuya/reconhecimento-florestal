package com.example.reconhecimentoflorestal;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.CameraProfile;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraProvider;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraState;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;

import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.google.common.util.concurrent.ListenableFuture;
import com.serenegiant.utils.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

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
        File file = FileUtils.getCaptureFile(
                requireContext(),
                Environment.DIRECTORY_DCIM,
                ".jpg");

        imageCapture.takePicture(
                new ImageCapture.OutputFileOptions.Builder(file).build(),
                getExecutor(),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Toast.makeText(
                                requireContext(),
                                "Foto salva",
                                Toast.LENGTH_SHORT).show();

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
            Bitmap cropped = BitmapFactory.decodeFile(file.getAbsolutePath());
            float[][][][] inputArray = preprocessImages(cropped);

            runInference(inputArray);
        } catch (Exception e) {
            Toast.makeText(
                    requireContext().getApplicationContext(),
                    "Erro ao salvar a imagem recortada",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private float[] resizeImage(Bitmap bitmap) {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true);
        float[] inputArray = convertBitmapToArray(resizedBitmap);

        return inputArray;
    }

    private static float[] convertBitmapToArray(Bitmap bitmap)  {
        float[] inputArray = new float[3 * 224 * 224];

        int idx = 0;
        for (int y = 0; y < 224; y++) {
            for (int x = 0; x < 224; x++) {
                int pixel = bitmap.getPixel(x, y);

                float red = (float) ((pixel >> 16) & 0xFF);
                float green = (float) ((pixel >> 8) & 0xFF);
                float blue = (float) (pixel & 0xFF);

                inputArray[idx++] = red / 255.0f;
                inputArray[idx++] = green / 255.0f;
                inputArray[idx++] = blue / 255.0f;
            }
        }
        return inputArray;
    }

    private float[][][][] preprocessImages(Bitmap bitmap) {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true);
        float[][][][] inputArray = new float[1][3][224][224]; // Batch size = 1

        // Converte a imagem em um array de float
        for (int y = 0; y < 224; y++) {
            for (int x = 0; x < 224; x++) {
                int pixel = resizedBitmap.getPixel(x, y);
                inputArray[0][0][y][x] = Color.red(pixel) / 255.0f;
                inputArray[0][1][y][x] = Color.green(pixel) / 255.0f;
                inputArray[0][2][y][x] = Color.blue(pixel) / 255.0f;
            }
        }
        return inputArray;
    }

    private void runInference(float[][][][] inputArray) {
        try {
            OrtEnvironment env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();

            // Obtém o identificador do recurso raw do modelo ONNX
            int resourceId = getResources().getIdentifier("model", "raw",requireContext().getPackageName());

            InputStream inputStream = getResources().openRawResource(resourceId);

            File modelFile = new File(requireContext().getCacheDir(), "model.onnx");
            FileOutputStream outputStream = new FileOutputStream(modelFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();

            OrtSession session = env.createSession(modelFile.getAbsolutePath(), options);

            OnnxTensor inputTensor = OnnxTensor.createTensor(env, inputArray);
            OrtSession.Result output = session.run(Collections.singletonMap("images", inputTensor));

            float[][] outputValues = (float[][]) output.get(0).getValue();

            for (float[] row: outputValues) {
                Log.d("RESULTADO DO MODELO", Arrays.toString(row));
            }

            inputTensor.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}