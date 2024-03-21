package com.example.reconhecimentoflorestal;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class ImageProcessing {
    public static float[] preprocessImage(Bitmap bitmap) {
        // Decodificar a imagem byte[] para Bitmap
//        Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);

        // Redimensionar imagem
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true);

        float[] inputArray = convertBitmapToArray(resizedBitmap);

        return inputArray;
    }

    private static float[] convertBitmapToArray(Bitmap bitmap)  {
        float[] inputArray = new float[3 * 224 * 224];

        int idx = 0;
        for (int y = 0; y < 244; y++) {
            for (int x = 0; x < 244; x++) {
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

}
