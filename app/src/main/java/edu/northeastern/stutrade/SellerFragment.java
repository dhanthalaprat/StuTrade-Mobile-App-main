package edu.northeastern.stutrade;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import edu.northeastern.stutrade.Models.Product;
import edu.northeastern.stutrade.Models.ProductViewModel;
import android.Manifest;


public class SellerFragment extends Fragment {

    private Button galleryButton, uploadButton;
    private static final int REQUEST_CAMERA_PERMISSION = 102; // Define your own request code
    private TextView emptyTextView;

    private static final int REQUEST_IMAGE_PICK = 100;
    private static final int REQUEST_IMAGE_CAPTURE = 101;
    String userName, userId;
    private List<Uri> selectedImageUris = new ArrayList<>();
    private DatabaseReference databaseReference;

    private StorageReference imageStorageRef;
    ProgressDialog progressDialog;
    private Button selectImageButton;
    private LinearLayout imageContainer;

    private Uri currentImageUri;
    private EditText productNameEditText, productDescriptionEditText, productPriceEditText;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_seller, container, false);

        ProductViewModel productViewModel = new ViewModelProvider(requireActivity()).get(ProductViewModel.class);
        productViewModel.setCurrentFragment("seller_fragment");

        galleryButton = view.findViewById(R.id.selectImagebtn);
        uploadButton = view.findViewById(R.id.uploadimagebtn);
        databaseReference = FirebaseDatabase.getInstance().getReference("products");
        selectImageButton = view.findViewById(R.id.selectImagebtn);
        imageContainer = view.findViewById(R.id.imageContainer);
        productNameEditText = view.findViewById(R.id.productNameEditText);
        productDescriptionEditText = view.findViewById(R.id.productDescriptionEditText);
        productPriceEditText = view.findViewById(R.id.productPriceEditText);
        galleryButton.setOnClickListener(v -> selectImages());
        uploadButton.setOnClickListener(v -> uploadProducts());

        selectImageButton.setOnClickListener(v -> selectImages());
        uploadButton.setOnClickListener(v -> uploadProducts());
        return view;
    }

//    private void selectImages() {
//        Intent intent = new Intent();
//        intent.setType("image/*");
//        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // Allow selecting multiple images
//        intent.setAction(Intent.ACTION_GET_CONTENT);
//        startActivityForResult(intent, 100);
//    }

    private void selectImages() {

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            showImageSourceDialog();
        }
    }

    private void showImageSourceDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Select Image Source");
        // Permission is already granted, proceed with camera operations
        builder.setItems(new CharSequence[]{"Gallery", "Camera"}, (dialog, which) -> {
            if (which == 0) {
                selectImagesFromGallery();
            } else {
                captureImageFromCamera();
            }
        });
        builder.show();
    }
    private void selectImagesFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    @SuppressLint("QueryPermissionsNeeded")
    private void captureImageFromCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        currentImageUri = requireActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentImageUri);
        PackageManager packageManager = requireActivity().getPackageManager();
        startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with camera operations
                showImageSourceDialog();
            } else {
                // Permission denied, show a message or handle accordingly
                Toast.makeText(getContext(),"Camera access denied. Please enable permissions",Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        imageContainer.setVisibility(View.VISIBLE);
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == getActivity().RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    selectedImageUris.add(imageUri);

                    // Create a new ImageView for each selected image and add it to the container
                    displaySelectedImage(imageUri);
                }
            } else if (data.getData() != null) {
                Uri imageUri = data.getData();
                selectedImageUris.add(imageUri);

                // Create a new ImageView for the selected image and add it to the container
                displaySelectedImage(imageUri);
            }
        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == getActivity().RESULT_OK) {
            //Bitmap capturedImage = (Bitmap) data.getExtras().get("data");
            Bitmap capturedImage = getBitmapFromUri(currentImageUri);
            if(capturedImage!=null) {
                Uri imageUri = getImageUri(getContext(), capturedImage);
            // Create a new ImageView for the captured image and add it to the container
            displaySelectedImage(imageUri);
            selectedImageUris.add(imageUri);}
            else{
                Toast.makeText(getContext(),"Failed to process captured Image", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private Bitmap getBitmapFromUri(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);

            // Decode the image while considering orientation
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = false;
            options.inSampleSize = 1;  // You might want to adjust this value for memory efficiency
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);

            // Rotate the bitmap based on its orientation
            int orientation = getOrientation(uri);
            if (orientation != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(orientation);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }

            return bitmap;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private int getOrientation(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            ExifInterface exif = new ExifInterface(inputStream);

            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int rotationDegrees = 0;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotationDegrees = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotationDegrees = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotationDegrees = 270;
                    break;
            }

            return rotationDegrees;
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }



    private void displaySelectedImage(Uri imageUri) {
        // Load and resize the image
        Bitmap thumbnail = getResizedBitmap(imageUri, 350, 350);

        // Create a new ImageView for the selected image
        ImageView imageView = new ImageView(getContext());
        imageView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        imageView.setImageBitmap(thumbnail);

        // Add the ImageView to the imageContainer LinearLayout
        imageContainer.addView(imageView);
    }

    private Bitmap getResizedBitmap(Uri imageUri, int newWidth, int newHeight) {
        try {
            InputStream inputStream = getContext().getContentResolver().openInputStream(imageUri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);

            inputStream.close();

            inputStream = getContext().getContentResolver().openInputStream(imageUri);

            int originalWidth = options.outWidth;
            int originalHeight = options.outHeight;

            int scaleFactor = Math.min(originalWidth / newWidth, originalHeight / newHeight);

            options.inJustDecodeBounds = false;
            options.inSampleSize = scaleFactor;

            Bitmap resizedBitmap = BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            return resizedBitmap;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }



    private Uri getImageUri(Context context, Bitmap imageBitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), imageBitmap, "ImageTitle", null);
        return Uri.parse(path);
    }

    private void uploadProducts() {
        String productName = productNameEditText.getText().toString().trim();
        String productDescription = productDescriptionEditText.getText().toString().trim();
        String productPrice = productPriceEditText.getText().toString().trim();
        if (productName.isEmpty() || productDescription.isEmpty() || productPrice.isEmpty() || selectedImageUris.size() == 0) {
            Toast.makeText(getContext(), "All fields should be filled", Toast.LENGTH_SHORT).show();
            return;
        }
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setTitle("Uploading Files....");
        progressDialog.show();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.CANADA);
        Date now = new Date();
        UserSessionManager sessionManager = new UserSessionManager(getContext());
        String email = sessionManager.getEmail();
        userName = sessionManager.getUsername();
        userId = email.substring(0, email.indexOf("@"));
        for (int i = 0; i < selectedImageUris.size(); i++) {
            String fileName = formatter.format(now) + "_" + i;
            imageStorageRef = FirebaseStorage.getInstance().getReference("images/" + userId + "/" + productName + "/" + fileName);
            StorageReference imageRef = FirebaseStorage.getInstance().getReference("images/" + userId + "/" + productName + "/");
            final int finalI = i;
            imageStorageRef.putFile(selectedImageUris.get(i))
                    .addOnSuccessListener(taskSnapshot -> {
                        if (finalI == selectedImageUris.size() - 1) {
                            imageRef.listAll()
                                    .addOnSuccessListener(listResult -> {
                                        List<StorageReference> items = listResult.getItems();
                                        if (!items.isEmpty()) {
                                            StorageReference firstItem = items.get(0);
                                            firstItem.getDownloadUrl().addOnSuccessListener(uri -> {
                                                String imageUrl = uri.toString();
                                                saveProductToDatabase(productName, productDescription, productPrice, imageUrl);
                                            });
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(getContext(), "Unable to load images", Toast.LENGTH_SHORT).show();
                                    });
                            Toast.makeText(getContext(), "All Images Uploaded", Toast.LENGTH_SHORT).show();
                            if (progressDialog.isShowing())
                                progressDialog.dismiss();
                        }
                    }).addOnFailureListener(e -> {
                        if (progressDialog.isShowing())
                            progressDialog.dismiss();
                        Toast.makeText(getContext(), "Failed to Upload Image " + (finalI + 1), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void saveProductToDatabase(String name, String description, String price, String imageUrl) {
        Product product = new Product(name, description, price, imageUrl, userName, userId, String.valueOf(new Date()));
        String productId = databaseReference.push().getKey(); // Generate a unique ID
        databaseReference.child(productId).setValue(product)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Product uploaded successfully", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                    // Clear the fields
                    imageContainer.removeAllViews();
                    selectedImageUris.clear();
                    productNameEditText.setText("");
                    productDescriptionEditText.setText("");
                    productPriceEditText.setText("");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to upload product", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                });
    }


}