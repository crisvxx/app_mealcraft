package com.example.comida.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.comida.data.firestore.Recipe
import com.example.comida.viewmodel.RecipeViewModel
import com.google.firebase.auth.FirebaseAuth

val categories = listOf("Todas", "Desayuno", "Almuerzo", "Cena", "Postre")

@Composable
fun HomeScreen(
    onRecipeClick: (String) -> Unit
) {
    val recipeViewModel: RecipeViewModel = viewModel()
    val allRecipes by recipeViewModel.recipes.collectAsState()
    val colors = MaterialTheme.colorScheme

    var searchText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Todas") }
    var showInfoDialog by remember { mutableStateOf(false) }

    val filteredRecipes = allRecipes.filter { recipe ->
        val matchesCategory = if (selectedCategory == "Todas") true else recipe.categoria == selectedCategory
        val matchesSearch = if (searchText.isBlank()) true else recipe.titulo.contains(searchText, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    // ⭐ DIÁLOGO DE INFORMACIÓN CORREGIDO (CON SCROLL)
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Sobre MealCraft", fontWeight = FontWeight.Bold, color = colors.primary) },
            text = {
                // Usamos Column con verticalScroll para que el texto largo no se corte
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "MealCraft es tu compañero culinario ideal.\n\n" +
                                "Versión: 1.0\n" +
                                "Desarrollado con ❤️ usando Kotlin y Jetpack Compose.\n\n" +
                                "Funciones:\n" +
                                "• Guarda tus recetas favoritas.\n" +
                                "• Comparte tus creaciones.\n" +
                                "• Descubre nuevos sabores según su categoría.\n\n" +
                                "¡Gracias por cocinar con nosotros!",
                        color = Color.Gray
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showInfoDialog = false }) { Text("Entendido") } },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }


    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        item(span = { GridItemSpan(2) }) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recetas", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, color = colors.primary))
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = colors.primary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("Buscar...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = colors.primary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.surface, unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.White, unfocusedContainerColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text("Categoría", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = colors.secondary))
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { category ->
                        CategoryChip(category, category == selectedCategory) { selectedCategory = category }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("Popular", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = colors.secondary))
            }
        }

        items(filteredRecipes) { recipe ->
            RecipeCard(
                recipe = recipe,
                onClick = { onRecipeClick(recipe.id) },
                onFavoriteClick = { recipeViewModel.toggleFavorite(recipe) }
            )
        }

        // Espacio para que el botón flotante no tape la última receta
        item(span = { GridItemSpan(2) }) { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun CategoryChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(color = if (isSelected) colors.primary else colors.surface, shape = RoundedCornerShape(18.dp), modifier = Modifier.clickable { onClick() }) {
        Text(text, color = if (isSelected) Color.White else colors.secondary, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
    }
}

@Composable
fun RecipeCard(recipe: Recipe, onClick: () -> Unit, onFavoriteClick: () -> Unit) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val isFavorite = recipe.likes.contains(currentUserId)
    val colors = MaterialTheme.colorScheme

    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), modifier = Modifier.fillMaxWidth().aspectRatio(0.8f).clickable { onClick() }) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column {
                AsyncImage(model = recipe.imagenUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().weight(1f).background(Color.LightGray))
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(recipe.titulo, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = colors.secondary), maxLines = 1)
                    Text(recipe.categoria, style = MaterialTheme.typography.labelSmall.copy(color = colors.primary))
                    Spacer(modifier = Modifier.height(4.dp))

                    // ⭐ TIEMPO REAL CORREGIDO
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        // Usamos el tiempo de la receta. Si está vacío, muestra "30 min" por defecto
                        Text(
                            text = recipe.tiempo.ifBlank { "30 min" },
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                        )
                    }
                }
            }
            IconButton(onClick = onFavoriteClick, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(32.dp).background(Color.White.copy(alpha = 0.7f), CircleShape)) {
                Icon(imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null, tint = if (isFavorite) Color.Red else Color.Gray, modifier = Modifier.size(20.dp))
            }
        }
    }
}