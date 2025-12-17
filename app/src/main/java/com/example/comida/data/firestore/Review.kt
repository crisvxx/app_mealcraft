
package com.example.comida.data.firestore

data class Review(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val comment: String = "",
    val rating: Int = 0,
    val timestamp: Long = System.currentTimeMillis() // Esto es lo importante
)