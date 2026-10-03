package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.remote.ErrandPlaceItem
import com.example.ui.theme.CalmMint
import com.example.ui.theme.DopamineAmber
import com.example.ui.theme.VioletPrimary
import com.example.ui.viewmodel.AiOperationState
import com.example.ui.viewmodel.MainViewModel

@Composable
fun ErrandsMapScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val errandsState by viewModel.errandsState.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("داروخانه شبانه‌روزی") }
    var locationContext by remember { mutableStateOf("تهران") }

    val presetErrands = listOf(
        "داروخانه شبانه‌روزی 💊",
        "کافه دنج و خلوت برای کار ☕",
        "خرید نان و سوپرمارکت 🛒",
        "شعبه بانک یا عابربانک 🏦",
        "کتاب‌فروشی و لوازم‌تحریر 📚",
        "تعویض روغن یا کارواش 🚗"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
    ) {
        // Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(CalmMint.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        tint = CalmMint,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "دستیار کارهای بیرون و مکان‌ها (Maps)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "غلبه بر استرس بیرون رفتن (Errand Paralysis) با نقشه و هوش مصنوعی",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Info card about ADHD Errand Paralysis
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = DopamineAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "حالت انتظار و ترس از کارهای بیرون (Waiting Mode) سراغ خیلی از افراد ADHD میاد. هوش مصنوعی مکان‌ها رو پیدا می‌کنه و راهنمای ضد حواس‌پرتی بهت میده تا کار در کمترین زمان تموم بشه!",
                        fontSize = 11.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Quick Preset Chips
        item {
            Text(
                text = "کارهای متداول بیرون از خانه:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(presetErrands) { preset ->
                    val cleanPreset = preset.replace(Regex("[^\\u0600-\\u06FF\\s]"), "").trim()
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                searchQuery = cleanPreset
                                viewModel.searchErrandsWithMaps(cleanPreset, locationContext)
                            }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = preset,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Inputs
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("چه کاری یا مکانی مد نظرته؟") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("errand_query_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = locationContext,
                onValueChange = { locationContext = it },
                label = { Text("شهر یا منطقه تقریبی شما") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("errand_location_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { viewModel.searchErrandsWithMaps(searchQuery, locationContext) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("search_errands_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CalmMint,
                    contentColor = Color.Black
                ),
                enabled = searchQuery.isNotBlank() && errandsState !is AiOperationState.Loading
            ) {
                Icon(imageVector = Icons.Default.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "جستجوی مکان‌ها با نقشه هوشمند 📍",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Feedback / Result list
        when (val state = errandsState) {
            is AiOperationState.Loading -> {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = CalmMint,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "هوش مصنوعی در حال جستجوی اطلاعات نقشه و زمان‌بندی...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is AiOperationState.Error -> {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.message,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            is AiOperationState.Success -> {
                val places = state.data
                item {
                    Text(
                        text = "مکان‌های پیدا شده (${places.size} مکان):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(places) { place ->
                    ErrandPlaceCard(
                        place = place,
                        onOpenMap = {
                            val mapUri = Uri.parse("geo:0,0?q=${Uri.encode("${place.name} ${place.addressOrLocation}")}")
                            val intent = Intent(Intent.ACTION_VIEW, mapUri)
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode("${place.name} ${place.addressOrLocation}")}")
                                context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                            }
                        },
                        onAddAsTask = {
                            viewModel.addErrandAsTask(place)
                        }
                    )
                }
            }

            else -> Unit
        }
    }
}

@Composable
private fun ErrandPlaceCard(
    place: ErrandPlaceItem,
    onOpenMap: () -> Unit,
    onAddAsTask: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, CalmMint.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = place.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(CalmMint.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${place.estimatedTimeMinutes} دقیقه زمان کل",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = CalmMint
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = place.addressOrLocation,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = DopamineAmber,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = place.openStatus,
                    fontSize = 11.sp,
                    color = DopamineAmber
                )
            }

            // ADHD Anti-Distraction Tip
            if (place.adhdAntiDistractionTip.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(VioletPrimary.copy(alpha = 0.12f))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "🛡️ راهنمای ضد حواس‌پرتی: ${place.adhdAntiDistractionTip}",
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onOpenMap,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "مسیریابی با نقشه", fontSize = 11.sp)
                }

                Button(
                    onClick = onAddAsTask,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = DopamineAmber, contentColor = Color.Black)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "افزودن به تسک‌ها", fontSize = 11.sp)
                }
            }
        }
    }
}
