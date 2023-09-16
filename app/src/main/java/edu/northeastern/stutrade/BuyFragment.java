package edu.northeastern.stutrade;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import edu.northeastern.stutrade.Models.Product;
import edu.northeastern.stutrade.Models.ProductViewModel;

public class BuyFragment extends Fragment implements ProductAdapter.OnProductClickListener{
    private RecyclerView productsRecyclerView;
    private ProgressBar loader;
    private ProductAdapter productAdapter;
    private ProductViewModel productViewModel;
    private int currentPage = 0;
    private final long DELAY_MS = 3000; // Delay in milliseconds before auto-scrolling
    private final long PERIOD_MS = 5000; // Interval in milliseconds between auto-scrolls
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Timer timer;
    private final List<Product> originalProductList = new ArrayList<>();
    private String[] sortingOptions = {
            "Price Increasing",
            "Price Decreasing",
            "Date Ascending",
            "Date Descending"
    };

    private String selectedSortingOption="";
    private ViewPager2 productViewPager;
    private ProductCarouselAdapter productCarouselAdapter = new ProductCarouselAdapter(new ArrayList<>());

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_buy, container, false);
        productsRecyclerView = rootView.findViewById(R.id.productRecyclerView);
        loader = rootView.findViewById(R.id.loader);
        SearchView searchView = rootView.findViewById(R.id.searchView);
        productViewPager = rootView.findViewById(R.id.productViewPager);

        if (savedInstanceState != null && savedInstanceState.containsKey("product_list")) {
            List<Product> savedProductList = (ArrayList<Product>) savedInstanceState.getSerializable("product_list");
            if (savedProductList != null) {
                productAdapter.setProductList(savedProductList);
            }
            String sortValue = savedInstanceState.getString("sorting_option");
            if( !sortValue.equals("")){
                setDefaultSortingOption(sortValue,rootView);
            }
        }else{
            productsRecyclerView();
            //productsRecyclerView1();
            sortDropdown(rootView);
        }

        productViewModel = new ViewModelProvider(requireActivity()).get(ProductViewModel.class);
        productViewModel.setCurrentFragment("buy_fragment");
        productViewModel.getIsProductSelected().observe(getViewLifecycleOwner(), isProductSelected -> {
            if (isProductSelected) {
                Product product = productViewModel.getSelectedProduct().getValue();
                onProductClick(product);
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterProducts(newText);
                return true;
            }
        });
        startAutoScroll();

        return rootView;
    }

    private void startAutoScroll() {
        // Initialize the Timer and TimerTask
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.post(() -> {
                    if (currentPage == 5) {
                        currentPage = 0;
                    } else {
                        currentPage++;
                    }
                    productViewPager.setCurrentItem(currentPage);
                });
            }
        }, DELAY_MS, PERIOD_MS);
    }



    private void filterProducts(String query) {
        if (query.isEmpty()) {
            productAdapter.setProductList(originalProductList); // Reset the list
        } else {
            List<Product> productList = new ArrayList<>();
            for (Product product : originalProductList) {
                if (product.getProductName().toLowerCase().contains(query.toLowerCase())) {
                    productList.add(product);
                }
            }
            productAdapter.setProductList(productList);
        }
        productAdapter.notifyDataSetChanged();
    }

    private void sortDropdown(View rootView){
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.dropdown_item, sortingOptions);
        AutoCompleteTextView sortDropdown = rootView.findViewById(R.id.sortDropdown);
        sortDropdown.setAdapter(adapter);

        // Handle the selected sorting option
        sortDropdown.setOnItemClickListener((parent, view, position, id) -> {
            String selectedOption = sortingOptions[position];
            handleSorting(selectedOption);
        });
    }

    private void productsRecyclerView(){
        loader.setVisibility(View.VISIBLE);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference productsRef = database.getReference("products");

        productsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Product> productList = new ArrayList<>();
                for (DataSnapshot productSnapshot : dataSnapshot.getChildren()) {
                    Product product = productSnapshot.getValue(Product.class);

                    productList.add(product);
                    originalProductList.add(product);
                }

                // Set up the RecyclerView with the fetched data
                productsRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 2));
                productAdapter = new ProductAdapter(productList);
                productAdapter.setOnProductClickListener(BuyFragment.this);
                productsRecyclerView.setAdapter(productAdapter);
                List<Product> carouselProductList =productList;
                Collections.sort(carouselProductList, (product1, product2) -> {
                    Date date1 = product1.getDatePostedAsDate();
                    Date date2 = product2.getDatePostedAsDate();
                    return date1 != null && date2 != null ? date2.compareTo(date1) : 0;
                });
                carouselProductList = carouselProductList.stream().limit(5).collect(Collectors.toList());
                productCarouselAdapter = new ProductCarouselAdapter(productList);
                productCarouselAdapter.setProductList(carouselProductList);
                productViewPager.setAdapter(productCarouselAdapter);
                productCarouselAdapter.setOnProductClickListener(BuyFragment.this);

                loader.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                loader.setVisibility(View.GONE);
            }
        });
    }

    private void handleSorting(String selectedOption) {
        List<Product> sortedList = new ArrayList<>(productAdapter.getProductList());
        selectedSortingOption = selectedOption;
        switch (selectedOption) {
            case "Price Increasing":
                Collections.sort(sortedList, Comparator.comparing(Product::getPriceAsDouble));
                break;
            case "Price Decreasing":
                Collections.sort(sortedList, (product1, product2) -> product2.getPriceAsDouble().compareTo(product1.getPriceAsDouble()));
                break;
            case "Date Ascending":
                Collections.sort(sortedList, (product1, product2) -> {
                    Date date1 = product1.getDatePostedAsDate();
                    Date date2 = product2.getDatePostedAsDate();
                    return date1 != null && date2 != null ? date1.compareTo(date2) : 0;
                });
                break;
            case "Date Descending":
            default:
                Collections.sort(sortedList, (product1, product2) -> {
                    Date date1 = product1.getDatePostedAsDate();
                    Date date2 = product2.getDatePostedAsDate();
                    return date1 != null && date2 != null ? date2.compareTo(date1) : 0;
                });
                break;
        }

        productAdapter.setProductList(sortedList);
        productAdapter.notifyDataSetChanged();
    }

    @Override
    public void onProductClick(Product product) {
        // Create a new ProductViewFragment and pass the selected product details
        productViewModel.setSelectedProduct(product);
        ProductViewFragment productViewFragment = new ProductViewFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable("selected_product", product);
        productViewFragment.setArguments(bundle);

        // Replace the current fragment with the new ProductViewFragment
        FragmentTransaction fragmentTransaction = requireActivity().getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, productViewFragment);
        fragmentTransaction.commit();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("product_list", (ArrayList<Product>) productAdapter.getProductList());
        outState.putSerializable("sorting_option", selectedSortingOption);
    }

    private void setDefaultSortingOption(String defaultOption, View rootView) {
        AutoCompleteTextView sortDropdown = rootView.findViewById(R.id.sortDropdown);
        int position = Arrays.asList(sortingOptions).indexOf(defaultOption);
        if (position >= 0) {
            sortDropdown.setText(sortingOptions[position], false);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        productViewModel.setIsProductSelected(false);
    }
}
