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
    private SAMModelRunner sam;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.camera_fragment , container, false);

        try {
            sam = new SAMModelRunner(getContext());
        } catch (IOException | OrtException e) {
            Log.e("ERRO SAM", e.toString());
        }


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
        } else if (v.getId() == R.id.btnConfirmBoundingBox) {
            confirmBoundingBox();
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

    private void confirmBoundingBox() {
        // Aqui você pode obter as coordenadas da bounding box e enviá-las para o modelo
        BoundingBoxImageView boundingBoxImageView = backCameraFragment.getBoundingBoxImageView();
        RectF boundingBox = boundingBoxImageView.getBoundingBox();
        Log.d("BBOX", boundingBox.toString());
        String imagePath = backCameraFragment.getSavedImagePath();

        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imagePath, options);

            int origWidth = options.outWidth;
            int origHeight = options.outHeight;

            float[][][] inputTensor = SAMImagePreprocessor.preprocessImage(imagePath);
            float[][][][] embeddings = sam.generateEmbeddings(inputTensor);
            Log.d("TENSOR", "Tensor preprocessado: " + Arrays.deepToString(inputTensor));
            float[] boundingBoxArray = {
                boundingBox.left * (origWidth / (float) boundingBoxImageView.getWidth()),
                boundingBox.top * (origHeight / (float) boundingBoxImageView.getHeight()),
                boundingBox.right * (origWidth / (float) boundingBoxImageView.getWidth()),
                boundingBox.bottom * (origHeight / (float) boundingBoxImageView.getHeight())
            };
            Log.d("BBOX ARRAY", Arrays.toString(boundingBoxArray));

            int[] inputLabels = {2, 3};

//            int origWidth = boundingBoxImageView.getWidth();
//            int origHeight = boundingBoxImageView.getHeight();
            int resizedWidth = 1024;
            int resizedHeight = 1024;

            float[][][] masks = sam.runDecoder(embeddings, boundingBoxArray, inputLabels, origWidth, origHeight, resizedWidth, resizedHeight);

            saveMaskAsImage(masks);
        } catch(IOException | OrtException e) {
            Log.e("ERRO SAM", e.toString());
        }

//        Toast.makeText(getContext(), "Bounding box confirmada: " + boundingBox.toString(), Toast.LENGTH_SHORT).show();

        // Esconder o bounding box view e o botão OK
        boundingBoxImageView.setVisibility(View.GONE);
        btnConfirmBoundingBox.setVisibility(View.GONE);
    }

    private void saveMaskAsImage(float[][][] masks) {
        // Converter a máscara float[][][] para um bitmap
        Bitmap maskBitmap = masksToBitmap(masks);

        // Salvar o bitmap como arquivo de imagem
        File maskFile = saveBitmap(maskBitmap);

        // Adicionar a imagem à galeria
        addToGallery(maskFile);
    }

    private Bitmap masksToBitmap(float[][][] masks) {
        // Lógica para converter a máscara float[][][] para um Bitmap
        int height = masks[0].length;
        int width = masks[0][0].length;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float maskValue = masks[0][y][x];
                int pixelColor = maskValue > 0 ? Color.WHITE : Color.BLACK;
                bitmap.setPixel(x, y, pixelColor);
            }
        }

        return bitmap;
    }

    private File saveBitmap(Bitmap bitmap) {
        // Salvar o bitmap como arquivo
//        File file = new File(requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "mask_image.jpg");
        File file = FileUtils.getCaptureFile(
                requireContext(),
                Environment.DIRECTORY_DCIM,
                ".jpg");
        try {
            OutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
            outputStream.close();
//            Toast.makeText(getContext(), "Máscara salva em: " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            Log.d("MASCARA", file.getAbsolutePath());
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Erro ao salvar máscara", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void addToGallery(File file) {
        // Adicionar a imagem à galeria
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(file);
        mediaScanIntent.setData(contentUri);
        requireContext().sendBroadcast(mediaScanIntent);
    }
}
