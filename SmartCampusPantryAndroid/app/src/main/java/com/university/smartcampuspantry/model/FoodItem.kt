package com.university.smartcampuspantry.model

data class FoodItem(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    var quantity: Int = 0,
    val imageUrl: String = "",
    val daysToExpiry: Int = 30
)
