package com.example.reconhecimentoflorestal;

import android.graphics.Bitmap;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SharedViewModel extends ViewModel {
    private final MutableLiveData<Bitmap> image = new MutableLiveData<>();

    public void setImage(Bitmap bitmap) {
        image.setValue(bitmap);
    }

    public LiveData<Bitmap> getImage() {
        return image;
    }
}
