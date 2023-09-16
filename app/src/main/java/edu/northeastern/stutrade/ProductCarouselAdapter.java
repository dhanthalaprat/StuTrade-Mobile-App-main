package edu.northeastern.stutrade;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.List;

import edu.northeastern.stutrade.Models.Product;

public class ProductCarouselAdapter extends RecyclerView.Adapter<ProductViewHolder> {
    private List<Product> productList;
    private ProductAdapter.OnProductClickListener productClickListener;
    public interface OnProductClickListener {
        void onProductClick(Product product);
    }
    public ProductCarouselAdapter(List<Product> productList) {
        this.productList = productList;
    }

    public void setProductList(List<Product> productList) {
        this.productList = productList;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_carousel, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        // Bind your product data to the view holder's UI elements here
        Product product = productList.get(position);
        holder.itemName.setText(product.getProductName());
        holder.itemPrice.setText("$" + product.getProductPrice());
        Picasso.get().load(product.getImageUrl()).into(holder.itemImage);
        //holder.datePosted.setText("Posted on: " + product.getDatePosted());

        holder.itemView.setOnClickListener(view -> {
            if (productClickListener != null) {
                productClickListener.onProductClick(product);
            }
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public void setOnProductClickListener(ProductAdapter.OnProductClickListener listener) {
        this.productClickListener = listener;
    }
    }



