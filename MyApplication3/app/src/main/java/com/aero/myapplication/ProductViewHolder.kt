package com.aero.myapplication

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun bind(product: Product) {
        val nameTextView = itemView.findViewById<TextView>(R.id.nameTextView)
        val quantityTextView = itemView.findViewById<TextView>(R.id.quantityTextView)
        val priceTextView = itemView.findViewById<TextView>(R.id.priceTextView)
        val productImageView = itemView.findViewById<ImageView>(R.id.productImageView)

        nameTextView.text = product.name
        quantityTextView.text = "Cantidad: ${product.quantity}"
        priceTextView.text = "Precio: ${product.price}"

        Glide.with(itemView.context)
            .load(product.imageUrl)
            .placeholder(R.drawable.imgcarga)
            .error(R.drawable.imgerror)
            .into(productImageView)
    }
}
