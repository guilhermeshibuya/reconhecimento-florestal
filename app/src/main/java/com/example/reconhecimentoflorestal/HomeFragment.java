package com.example.reconhecimentoflorestal;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.example.reconhecimentoflorestal.databinding.ActivityMainBinding;
import com.google.android.material.button.MaterialButton;
import com.hjq.permissions.XXPermissions;
import com.serenegiant.utils.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;


public class HomeFragment extends Fragment implements View.OnClickListener{
    private ActivityMainBinding binding;

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.home_fragment, container, false);

        initListeners(view);

        return view;
    }

    ActivityResultLauncher<CropImageContractOptions> cropImage = registerForActivityResult(
            new CropImageContract(),
            result -> {
                if (result.isSuccessful()) {
                    Bitmap cropped = BitmapFactory.decodeFile(result.getUriFilePath(requireContext(), true));
                    saveImage(cropped);
                }
            });

    ActivityResultLauncher<Intent> getImage = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK) {
            Intent data = result.getData();
            if (data != null && data.getData() != null) {
                Uri imageUri = data.getData();
                launchImageCropper(imageUri);
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

    private void getImageFile() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        getImage.launch(intent);
    }

    private void saveImage(Bitmap bitmap) {
        File file = FileUtils.getCaptureFile(
                requireContext().getApplicationContext(),
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
            requireContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            MediaScannerConnection.scanFile(requireContext(), new String[]{ file.getAbsolutePath()}, null, null);

            Toast.makeText(
                    requireContext(),
                    "Foto salva",
                    Toast.LENGTH_SHORT).show();

            // NOVO CÓDIGO
            ModelUtilities modelUtilities = new ModelUtilities(requireContext());

            Bitmap cropped = BitmapFactory.decodeFile(file.getAbsolutePath());

            float[][][][] inputArray = modelUtilities.preprocessImages(cropped);

            modelUtilities.runInference(inputArray);
        } catch (Exception e) {
            Toast.makeText(
                    requireContext(),
                    "Erro ao salvar a imagem recortada",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void initListeners(View view) {
        MaterialButton btnOpenCamera = view.findViewById(R.id.btnOpenCamera);
        btnOpenCamera.setOnClickListener(this);

        MaterialButton btnChooseImage = view.findViewById(R.id.btnChoosePicture);
        btnChooseImage.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        List<String> needPermissions = new ArrayList<>();
        needPermissions.add(android.Manifest.permission.CAMERA);
        needPermissions.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE);

        XXPermissions.with(this)
                .permission(needPermissions)
                .request((permissions, all) -> {
                    if (!all) {
                        return;
                    }

                    if (v.getId() == R.id.btnOpenCamera) {
                        startActivity(new Intent(requireContext(), CameraActivity.class));
                    } else if (v.getId() == R.id.btnChoosePicture) {
                        getImageFile();
                    }
                });
    }
}