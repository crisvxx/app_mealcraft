package com.example.comida.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.comida.data.firestore.Recipe
import com.example.comida.data.firestore.RecipeRepository
import com.example.comida.data.firestore.Review
import com.example.comida.data.storage.FirebaseStorageManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

class RecipeViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes: StateFlow<List<Recipe>> = _recipes.asStateFlow()

    private val _currentReviews = MutableStateFlow<List<Review>>(emptyList())
    val currentReviews: StateFlow<List<Review>> = _currentReviews.asStateFlow()

    init {
        fetchRecipes()
    }

    // 1. Obtener todas las recetas (Escucha cambios en tiempo real)
    fun fetchRecipes() {
        RecipeRepository.getRecipes { listaRecetas ->
            _recipes.value = listaRecetas
        }
    }

    // 2. Obtener una receta específica por ID (Para la pantalla de detalles)
    fun getRecipeById(recipeId: String): Flow<Recipe?> {
        return _recipes.map { list -> list.find { it.id == recipeId } }
    }

    // 3. Obtener reseñas de una receta
    fun fetchReviews(recipeId: String) {
        RecipeRepository.getReviewsForRecipe(recipeId) { reviews ->
            _currentReviews.value = reviews
        }
    }

    // 4. Subir una reseña
    fun submitReview(recipeId: String, commentText: String, rating: Int) {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        val review = Review(
            userId = user.uid,
            userName = user.displayName ?: "Usuario",
            comment = commentText,
            rating = rating
        )

        viewModelScope.launch {
            RecipeRepository.addReview(recipeId, review,
                onSuccess = { fetchReviews(recipeId) }, // Recargar reseñas al subir
                onError = { e -> e.printStackTrace() }
            )
        }
    }

    // ⭐ 5. AGREGAR RECETA (ACTUALIZADO CON TIEMPO E INGREDIENTES STRING)
    fun addRecipe(
        userId: String,
        titulo: String,
        ingredientes: String, // Ahora es String para respetar los saltos de línea
        instrucciones: String,
        categoria: String,
        imageUri: Uri,
        tiempo: String, // Nuevo campo de Tiempo
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // A) Subir imagen al Storage
                val imageUrl = FirebaseStorageManager.uploadRecipeImage(imageUri)

                if (imageUrl.isEmpty()) {
                    onResult(false)
                    return@launch
                }

                // B) Crear objeto Receta
                val newRecipe = Recipe(
                    id = UUID.randomUUID().toString(), // Generamos ID único
                    userId = userId,
                    titulo = titulo,
                    ingredientes = ingredientes,
                    instrucciones = instrucciones,
                    categoria = categoria,
                    imagenUrl = imageUrl,
                    tiempo = tiempo, // Guardamos el tiempo
                    likes = emptyList()
                )

                // C) Guardar en Firestore
                firestore.collection("recipes")
                    .document(newRecipe.id)
                    .set(newRecipe)
                    .addOnSuccessListener {
                        onResult(true)
                        fetchRecipes() // Refrescar lista localmente
                    }
                    .addOnFailureListener {
                        onResult(false)
                    }

            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            }
        }
    }

    // 6. Borrar receta
    fun deleteRecipe(recipeId: String) {
        viewModelScope.launch {
            firestore.collection("recipes").document(recipeId)
                .delete()
                .addOnSuccessListener { fetchRecipes() }
                .addOnFailureListener { e -> e.printStackTrace() }
        }
    }

    // 7. Dar Like / Quitar Like
    fun toggleFavorite(recipe: Recipe) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val isLiked = recipe.likes.contains(currentUserId)

        val newLikes = if (isLiked) {
            recipe.likes - currentUserId // Quitar ID
        } else {
            recipe.likes + currentUserId // Agregar ID
        }

        // Actualización optimista (para que se vea rápido en pantalla)
        val updatedList = _recipes.value.map {
            if (it.id == recipe.id) it.copy(likes = newLikes) else it
        }
        _recipes.value = updatedList

        // Actualización real en base de datos
        firestore.collection("recipes").document(recipe.id)
            .update("likes", newLikes)
            .addOnFailureListener {
                fetchRecipes() // Si falla, recargamos la verdad de la BD
            }
    }
}