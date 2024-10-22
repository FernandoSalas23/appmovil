package com.aero.myapplication

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView

class ProductAdapter(private val listener: OnItemClickListener) : RecyclerView.Adapter<ProductViewHolder>() {

    private var allProducts = mutableListOf<Product>()
    private var filteredProducts = mutableListOf<Product>()

    fun setProducts(products: List<Product>) {
        allProducts = products.toMutableList()
        filteredProducts = products.toMutableList()
        notifyDataSetChanged()
    }

    fun filter(query: String, maxPrice: Double) {
        filteredProducts = allProducts.filter { product ->
            product.name.contains(query, ignoreCase = true) &&
                    (product.price.toDoubleOrNull() ?: 0.0) <= maxPrice
        }.toMutableList()
        notifyDataSetChanged()
    }

    fun sort(sortOption: Int) {
        when (sortOption) {
            0 -> filteredProducts.sortBy { it.price.toDoubleOrNull() ?: 0.0 } // Más baratos primero
            1 -> filteredProducts.sortByDescending { it.price.toDoubleOrNull() ?: 0.0 } // Más caros primero
            2 -> filteredProducts.sortBy { it.name } // A-Z
            3 -> filteredProducts.sortByDescending { it.name } // Z-A
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.product_item, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = filteredProducts[position]
        holder.bind(product)

        holder.itemView.setOnClickListener {
            listener.onEditClick(product)
        }

        holder.itemView.findViewById<ImageButton>(R.id.btnDelete).setOnClickListener {
            listener.onDeleteClick(product)
        }
    }

    override fun getItemCount(): Int = filteredProducts.size
}