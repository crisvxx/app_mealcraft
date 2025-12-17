package com.example.comida.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.comida.data.firestore.Review
import com.example.comida.viewmodel.RecipeViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun RecipeDetailScreen(
    recipeId: String,
    onBack: () -> Unit
) {
    val recipeViewModel: RecipeViewModel = viewModel()
    val allRecipes by recipeViewModel.recipes.collectAsState()
    val reviews by recipeViewModel.currentReviews.collectAsState()

    // Buscamos la receta
    val recipe = allRecipes.find { it.id == recipeId }
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme

    // Auth para likes
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(recipeId) {
        recipeViewModel.fetchReviews(recipeId)
    }

    // Estados para el formulario de nuevo comentario
    var userRating by remember { mutableIntStateOf(0) }
    var userComment by remember { mutableStateOf("") }

    if (recipe == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Receta no encontrada", color = Color.Gray)
        }
        return
    }

    val isFavorite = recipe.likes.contains(currentUserId)

    // ⭐ FUNCIÓN DE COMPARTIR ARREGLADA (Ahora imprime texto normal)
    fun shareRecipe() {
        val shareText = """
            🍽️ ¡Mira esta receta en MealCraft!
            
            *${recipe.titulo}*
            
            📝 Ingredientes:
            ${recipe.ingredientes}
            
            🍳 Instrucciones:
            ${recipe.instrucciones}
            
            Descubre más en mi App.
        """.trimIndent()

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Compartir receta vía...")
        context.startActivity(shareIntent)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
    ) {
        // --- HEADER CON IMAGEN ---
        Box(modifier = Modifier.height(300.dp).fillMaxWidth()) {
            AsyncImage(
                model = recipe.imagenUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Botón Atrás
            IconButton(
                onClick = onBack,
                modifier = Modifier.padding(16.dp).align(Alignment.TopStart)
                    .clip(CircleShape).background(Color.White.copy(alpha = 0.8f))
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = colors.primary)
            }

            // Botón Compartir
            IconButton(
                onClick = { shareRecipe() },
                modifier = Modifier.padding(16.dp).align(Alignment.TopEnd)
                    .clip(CircleShape).background(Color.White.copy(alpha = 0.8f))
            ) {
                Icon(Icons.Default.Share, contentDescription = "Compartir", tint = colors.primary)
            }

            // Botón Favorito (Nuevo, en la imagen)
            IconButton(
                onClick = { recipeViewModel.toggleFavorite(recipe) },
                modifier = Modifier.padding(16.dp).align(Alignment.BottomEnd)
                    .clip(CircleShape).background(Color.White)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorite) Color.Red else Color.Gray
                )
            }
        }

        // --- CONTENIDO ---
        Column(
            modifier = Modifier
                .offset(y = (-20).dp)
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(colors.background)
                .padding(24.dp)
        ) {
            Text(recipe.titulo, style = MaterialTheme.typography.headlineMedium, color = colors.primary, fontWeight = FontWeight.Bold)

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Surface(color = colors.surface, shape = RoundedCornerShape(8.dp)) {
                    Text(recipe.categoria, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = colors.secondary, style = MaterialTheme.typography.labelMedium)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Icon(Icons.Default.Timer, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                // ⭐ TIEMPO REAL
                Text(recipe.tiempo.ifBlank { "30 min" }, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ⭐ TARJETA BLANCA PARA DETALLES (Lo que pediste)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Ingredientes
                    Text("Ingredientes", style = MaterialTheme.typography.titleMedium, color = colors.secondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    // Ahora es texto normal, no lista
                    Text(recipe.ingredientes, lineHeight = 24.sp, color = Color.DarkGray)

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Instrucciones
                    Text("Instrucciones", style = MaterialTheme.typography.titleMedium, color = colors.secondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(recipe.instrucciones, lineHeight = 24.sp, color = Color.DarkGray)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // SECCIÓN DE OPINIONES
            Text("Tu Opinión", style = MaterialTheme.typography.titleLarge, color = colors.secondary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Califica esta receta:", fontSize = 14.sp, color = Color.Gray)
                    InteractiveRatingBar(
                        currentRating = userRating,
                        onRatingChanged = { userRating = it },
                        starColor = Color(0xFFFFC107)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = userComment,
                        onValueChange = { userComment = it },
                        placeholder = { Text("Escribe un comentario...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (userRating > 0 && userComment.isNotBlank()) {
                                recipeViewModel.submitReview(recipe.id, userComment, userRating)
                                Toast.makeText(context, "¡Gracias por tu opinión!", Toast.LENGTH_SHORT).show()
                                userRating = 0
                                userComment = ""
                            } else {
                                Toast.makeText(context, "Elige una calificación y escribe algo", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Enviar")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // LISTA DE RESEÑAS
            Text("Reseñas (${reviews.size})", style = MaterialTheme.typography.titleLarge, color = colors.secondary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            if (reviews.isEmpty()) {
                Text("Sé el primero en opinar sobre este platillo.", color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            } else {
                reviews.forEach { review ->
                    ReviewItem(review)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// --- TUS COMPONENTES SIGUEN IGUAL ---

@Composable
fun InteractiveRatingBar(
    currentRating: Int,
    onRatingChanged: (Int) -> Unit,
    maxStars: Int = 5,
    starColor: Color = Color.Yellow
) {
    Row {
        for (i in 1..maxStars) {
            Icon(
                imageVector = if (i <= currentRating) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = null,
                tint = if (i <= currentRating) starColor else Color.Gray,
                modifier = Modifier.size(32.dp).clickable { onRatingChanged(i) }
            )
        }
    }
}

@Composable
fun ReviewItem(review: Review) {
    val colors = MaterialTheme.colorScheme
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(review.userName, fontWeight = FontWeight.Bold, color = colors.secondary)
                Spacer(modifier = Modifier.weight(1f))
                repeat(review.rating) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(dateFormat.format(review.timestamp), fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Text(review.comment, color = Color.DarkGray)
        }
    }
}