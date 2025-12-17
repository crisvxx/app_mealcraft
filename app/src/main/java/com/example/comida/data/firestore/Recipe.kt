package com.example.comida.data.firestore

data class Recipe(
    val id: String = "",
    val userId: String = "",
    val titulo: String = "",
    val ingredientes: String = "", // String normal
    val instrucciones: String = "",
    val categoria: String = "",
    val imagenUrl: String = "",
    val tiempo: String = "30 min",
    val likes: List<String> = emptyList(),
    val reviews: List<Review> = emptyList() // Automáticamente leerá la Review del otro archivo
)