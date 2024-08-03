package com.example.reconhecimentoflorestal;

import android.graphics.Matrix;
import android.media.ExifInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.IOException;

public class ImageUtils {
    public static Bitmap getCorrectlyOrientedBitmap(String imagePath) throws IOException {
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        ExifInterface exif = new ExifInterface(imagePath);
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        int rotate = 0;
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                rotate = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                rotate = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                rotate = 270;
                break;
        }

        if (rotate != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotate);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }

        return bitmap;
    }

    public static int[] findLargestSquare(int[][] mask) {
        int rows = mask.length;
        int cols = mask[0].length;
        int[] dp = new int[cols + 1];
        int maxSize = 0;
        int maxI = 0, maxJ = 0;

        for (int i = 0; i < rows; i++) {
            int prev = 0;
            for (int j = 1; j <= cols; j++) {
                int temp = dp[j];
                if (mask[i][j - 1] == 1) {
                    dp[j] = Math.min(prev, Math.min(dp[j], dp[j - 1])) + 1;
                    if (dp[j] > maxSize) {
                        maxSize = dp[j];
                        maxI = i;
                        maxJ = j - 1;
                    }
                } else {
                    dp[j] = 0;
                }
                prev = temp;
            }
        }

        int topLeftX = maxJ - maxSize + 1;
        int topLeftY = maxI - maxSize + 1;

        return new int[]{topLeftX, topLeftY, maxSize};
    }
}
