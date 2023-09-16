package edu.northeastern.stutrade;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.List;

import edu.northeastern.stutrade.Models.Product;

public class ProductAdapter extends RecyclerView.Adapter<ProductViewHolder> {
    private List<Product> productList;

    public ProductAdapter(List<Product> productList) {
        this.productList = productList;
    }

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    private OnProductClickListener productClickListener;

    public void setOnProductClickListener(OnProductClickListener listener) {
        this.productClickListener = listener;
    }
    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
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

    public void setProductList(List<Product> productList) {
        this.productList = productList;
        notifyDataSetChanged();
    }

    public List<Product> getProductList() {
       return productList;
    }
}
