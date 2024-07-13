package com.example.reconhecimentoflorestal;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.RectF;
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

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.serenegiant.utils.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import ai.onnxruntime.OrtException;

public class BoundingBoxFragment extends Fragment {
    private Uri imageUri;
    private BoundingBoxImageView boundingBoxImageView;
    private SAMModelRunner sam;
    private Bitmap bitmap;

    public static BoundingBoxFragment newInstance(Uri imageUri) {
        BoundingBoxFragment fragment = new BoundingBoxFragment();
        Bundle args = new Bundle();
        args.putParcelable("imageUri", imageUri);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            imageUri = getArguments().getParcelable("imageUri");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bounding_box_fragment, container, false);
        boundingBoxImageView = view.findViewById(R.id.bboxImageViewFragment);

        try {
            sam = new SAMModelRunner(getContext());
        } catch (IOException | OrtException e) {
            Log.e("ERRO  CARREGAR SAM", e.toString());
        }

        if (imageUri != null) {
            try {
//                bitmap = BitmapFactory.decodeStream(requireContext().getContentResolver().openInputStream(imageUri));
                String path = getImagePath(imageUri);
                bitmap = ImageUtils.getCorrectlyOrientedBitmap(path);
                boundingBoxImageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.e("BoundingBoxFragment", "imageUri is null");
        }

        Button btnConfirmBoundingBox = view.findViewById(R.id.btnConfirmBbox);
        btnConfirmBoundingBox.setOnClickListener(v -> {
            confirmBoundingBox();
        });

        return view;
    }

    private String getImagePath(Uri uri) {
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { MediaStore.Images.Media.DATA };
            Cursor cursor = requireContext().getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null) {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                String path = cursor.getString(column_index);
                cursor.close();
                return path;
            }
        }
        return null;
    }

    private void confirmBoundingBox() {
        if (bitmap == null) {
            Log.e("BoundingBoxFragment", "Bitmap is null");
            return;
        }

        RectF boundingBox = boundingBoxImageView.getBoundingBox();
        Log.d("BBOX", boundingBox.toString());

        String imagePath = getImagePath(imageUri);

        if (imagePath == null) {;
            Log.e("ERRO CONFIRM BOUNDING BOX", "Caminho é nulo");
            return;
        }

        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imagePath, options);

            int origWidth = options.outWidth;
            int origHeight = options.outHeight;

            Log.d("Orig width e height", origWidth + " x " + origHeight);

            float[][][][] inputTensor = SAMImagePreprocessor.preprocessImage(imagePath);
            float[][][][] embeddings = sam.generateEmbeddings(inputTensor);
            Log.d("TENSOR", "Tensor preprocessado: " + Arrays.deepToString(inputTensor));

//            float[] boundingBoxArray = {
//                    boundingBox.left * (origWidth / (float) boundingBoxImageView.getWidth()),
//                    boundingBox.top * (origHeight / (float) boundingBoxImageView.getHeight()),
//                    boundingBox.right * (origWidth / (float) boundingBoxImageView.getWidth()),
//                    boundingBox.bottom * (origHeight / (float) boundingBoxImageView.getHeight())
//            };

            float widthScale = (float) origWidth / boundingBoxImageView.getWidth();
            float heightScale = (float) origHeight / boundingBoxImageView.getHeight();

            float[] boundingBoxArray = {
                    Math.min(boundingBox.left * widthScale, origWidth - 1),
                    Math.min(boundingBox.top * heightScale, origHeight - 1),
                    Math.min(boundingBox.right * widthScale, origWidth - 1),
                    Math.min(boundingBox.bottom * heightScale, origHeight - 1)
            };

            Log.d("BBOX ARRAY", Arrays.toString(boundingBoxArray));

            int[] inputLabels = {2, 3};

            int resizedWidth = 1024;
            int resizedHeight = 1024;

            float[][][] masks = sam.runDecoder(embeddings, boundingBoxArray, inputLabels, origWidth, origHeight, resizedWidth, resizedHeight);

            saveMaskAsImage(masks);
        } catch(IOException | OrtException e) {
            Log.e("ERRO SAM", e.toString());
        }
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
