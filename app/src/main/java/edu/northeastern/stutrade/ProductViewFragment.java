package edu.northeastern.stutrade;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import edu.northeastern.stutrade.Models.Product;
import edu.northeastern.stutrade.Models.ProductViewModel;

public class ProductViewFragment extends Fragment {
    private Product selectedProduct;
    private StorageReference storageReference;
    ProductViewModel productViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        View rootView = inflater.inflate(R.layout.fragment_product_view, container, false);
        FirebaseStorage storage = FirebaseStorage.getInstance();
        Bundle args = getArguments();
        if (args != null && args.containsKey("selected_product")) {
            selectedProduct = (Product) args.getSerializable("selected_product");
        }
        if (selectedProduct != null) {
            storageReference = storage.getReference("images/" + selectedProduct.getSellerId() + "/" + selectedProduct.getProductName());
            ImageView productImageView = rootView.findViewById(R.id.productImageView);
            TextView productDescriptionTextView = rootView.findViewById(R.id.productDescriptionTextView);
            TextView datePostedTextView = rootView.findViewById(R.id.datePostedTextView);
            TextView sellerNameTextView = rootView.findViewById(R.id.sellerNameTextView);
            TextView productPriceTextView = rootView.findViewById(R.id.productPriceTextView);
            TextView productNameTextView = rootView.findViewById(R.id.productNameTextViews);
            Button chatButton = rootView.findViewById(R.id.chatButton);

            productNameTextView.setText(selectedProduct.getProductName());
            productDescriptionTextView.setText("About the product: " + selectedProduct.getProductDescription());
            datePostedTextView.setText("Posted on: " + getDateOnly(selectedProduct.getDatePosted()));
            sellerNameTextView.setText("Sold by: " + selectedProduct.getSellerName());
            productPriceTextView.setText("$" + String.valueOf(selectedProduct.getProductPrice()));
            Picasso.get().load(selectedProduct.getImageUrl()).into(productImageView);
            chatButton.setOnClickListener(view -> openChatFragment());

        }

        ImageView productImageView = rootView.findViewById(R.id.productImageView);
        productImageView.setOnClickListener(v -> showImagePopup());
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        productViewModel = new ViewModelProvider(requireActivity()).get(ProductViewModel.class);
        productViewModel.setIsProductSelected(true);
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == android.view.KeyEvent.ACTION_UP && keyCode == android.view.KeyEvent.KEYCODE_BACK) {
                requireActivity().getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, new BuyFragment()).commit();
                return true;
            }
            return false;
        });
    }


    private void showImagePopup() {
        Dialog imagePopup = new Dialog(requireContext());
        imagePopup.setContentView(R.layout.layout_popup_image);
        RecyclerView imageRecyclerView = imagePopup.findViewById(R.id.imageRecyclerView);

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        imageRecyclerView.setLayoutManager(layoutManager);

        ImageAdapter imageAdapter = new ImageAdapter(new ArrayList<>());
        imageRecyclerView.setAdapter(imageAdapter);

        storageReference.listAll()
                .addOnSuccessListener(listResult -> {
                    List<StorageReference> items = listResult.getItems();
                    List<String> imageUrls = new ArrayList<>();

                    for (StorageReference item : items) {
                        item.getDownloadUrl().addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();
                            if (!imageUrl.isEmpty()) {
                                imageUrls.add(imageUrl);
                            }

                            imageAdapter.setImageUrls(imageUrls);
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Unable to load images", Toast.LENGTH_SHORT).show();
                });

        imagePopup.show();
    }


    private void openChatFragment() {
        // Create a new instance of ChatFragment with the required arguments
        UserSessionManager sessionManager = new UserSessionManager(getContext());
        ChatFragment chatFragment = ChatFragment.newInstance(sessionManager.getUsername(), sessionManager.getEmail(), selectedProduct.getSellerId());
        BottomNavigationView bottomNavigationView = getActivity().findViewById(R.id.bottom_navigation_view);
        MenuItem chatMenuItem = bottomNavigationView.getMenu().findItem(R.id.navigation_chat);
        chatMenuItem.setChecked(true);
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_layout, chatFragment)
                .addToBackStack(null)
                .commit();
    }

    private LocalDate getDateOnly(String datePosted) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);
        try {
            return LocalDate.parse(datePosted, formatter);
        } catch (DateTimeParseException e) {
            return LocalDate.now();
        }
    }

}