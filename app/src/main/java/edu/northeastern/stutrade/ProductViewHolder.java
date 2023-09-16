package edu.northeastern.stutrade;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

public class ProductViewHolder extends RecyclerView.ViewHolder {
    public TextView itemName;
    public TextView itemPrice;
    public ImageView itemImage;

    public ProductViewHolder(View itemView) {
        super(itemView);
        itemName = itemView.findViewById(R.id.text_item_name);
        itemPrice = itemView.findViewById(R.id.text_item_price);
        itemImage = itemView.findViewById(R.id.prod_image);
    }
}
