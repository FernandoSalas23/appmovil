package com.aero.myapplication


data class Product(
    val id: String,
    val name: String,
    val quantity: String,
    val price: String,
    val category: String,
    val imageUrl: String? = null
)
