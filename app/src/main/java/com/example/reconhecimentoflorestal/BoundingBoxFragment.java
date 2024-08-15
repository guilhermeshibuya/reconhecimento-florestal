package com.example.reconhecimentoflorestal;

import android.content.Context;
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
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
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
                String path = getImagePath(imageUri);
                bitmap = ImageUtils.getCorrectlyOrientedBitmap(path);

                adjustImageViewDimensions(boundingBoxImageView, bitmap);

                boundingBoxImageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.e("BoundingBoxFragment", "imageUri is null");
        }

        Button btnConfirmBoundingBox = view.findViewById(R.id.btnConfirmBbox);
        btnConfirmBoundingBox.setOnClickListener(v -> {
            confirmBoundingBox(bitmap);
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

    private void confirmBoundingBox(Bitmap bitmap) {
        if (bitmap == null) {
            Log.e("BoundingBoxFragment", "Bitmap is null");
            return;
        }

        RectF boundingBox = boundingBoxImageView.getBoundingBox();
        Log.d("BBOX", boundingBox.toString());

        try {
            int origWidth = bitmap.getWidth();
            int origHeight = bitmap.getHeight();

            SAMPreprocessingResult results = SAMImagePreprocessor.preprocessImage(getContext().getApplicationContext(), bitmap);
            float[][][][] inputTensor = results.getTensor();

            float[][][][] embeddings = sam.generateEmbeddings(inputTensor);
            Log.d("TENSOR", "Tensor preprocessado: " + Arrays.deepToString(inputTensor));

            Log.d("BBOX Image View", "Size: " + boundingBoxImageView.getWidth() + ", " + boundingBoxImageView.getHeight());
            float[] boundingBoxArray = {
                    boundingBox.left * (origWidth / (float) boundingBoxImageView.getWidth()),
                    boundingBox.top * (origHeight / (float) boundingBoxImageView.getHeight()),
                    boundingBox.right * (origWidth / (float) boundingBoxImageView.getWidth()),
                    boundingBox.bottom * (origHeight / (float) boundingBoxImageView.getHeight())
            };
            Log.d("BBOX ARRAY", Arrays.toString(boundingBoxArray));

            int[] inputLabels = {2, 3};

            int resizedWidth = results.getResizedWidth();
            int resizedHeight = results.getResizedHeight();

            Log.d("resizedWidth", ""+ resizedWidth);

            float[][][] mask = sam.runDecoder(embeddings, boundingBoxArray, inputLabels, origWidth, origHeight, resizedWidth, resizedHeight);

            saveMaskAsImage(mask);

//            int[][] intMask = new int[mask[0].length][mask[0][0].length];
//            for (int i = 0; i < mask[0].length; i++) {
//                for (int j = 0; j < mask[0][0].length; j++) {
//                    intMask[i][j] = mask[0][i][j] > 0.5 ? 1 : 0;
//                }
//            }

//            Bitmap segmentedBitmap = generateSegmentedArea(bitmap, intMask);
//
//            File segmentedImg = saveBitmap(segmentedBitmap);
//            addToGallery(segmentedImg);
        } catch(IOException | OrtException e) {
            Log.e("ERRO SAM", e.toString());
        }
    }

    private Bitmap generateSegmentedArea(Bitmap bitmap, int[][] mask) {
        int[] largestSquare = ImageUtils.findLargestSquare(mask);
        int x = largestSquare[0];
        int y = largestSquare[1];
        int size = largestSquare[2];

        Bitmap segmentedBitmap = Bitmap.createBitmap(bitmap, x, y, size, size);

        return segmentedBitmap;
    }

    private void adjustImageViewDimensions(ImageView imageView, Bitmap bitmap) {
        // Obtenha a largura da tela
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;

        // Calcule a proporção do Bitmap
        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();
        float aspectRatio = (float) bitmapHeight / bitmapWidth;

        // Defina a largura do ImageView como a largura da tela
        // e a altura do ImageView proporcionalmente à altura do Bitmap
        ViewGroup.LayoutParams layoutParams = imageView.getLayoutParams();
        layoutParams.width = screenWidth;
        layoutParams.height = (int) (screenWidth * aspectRatio);
        imageView.setLayoutParams(layoutParams);
    }

    private void saveMaskAsImage(float[][][] masks) {
        Bitmap maskBitmap = masksToBitmap(masks);

        File maskFile = saveBitmap(maskBitmap);

        addToGallery(maskFile);
    }

    private Bitmap masksToBitmap(float[][][] masks) {
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
        File file = FileUtils.getCaptureFile(
                requireContext(),
                Environment.DIRECTORY_DCIM,
                ".png");
        try {
            OutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.close();
            Log.d("MASCARA", file.getAbsolutePath());
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Erro ao salvar máscara", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void addToGallery(File file) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(file);
        mediaScanIntent.setData(contentUri);
        requireContext().sendBroadcast(mediaScanIntent);
    }
}
