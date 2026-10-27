package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class LeRobotPolicyBenchmark(
    val policy: String,
    val status: String,
    val device: String,
    @SerialName("inference_median_ms")
    val inferenceMedianMs: Double? = null,
    val error: String? = null
)

enum class ProfilerFilter(val label: String, val tag: String) {
    ALL("All", "profiler_filter_all"),
    SUCCESS_ONLY("Success Only", "profiler_filter_success"),
    FAILED_ONLY("Failed Only", "profiler_filter_failed")
}

object LeRobotProfilerData {
    const val RAW_JSON = """
[
  {"policy": "act", "status": "success", "device": "NVIDIA L4", "inference_median_ms": 6.63},
  {"policy": "diffusion", "status": "success", "device": "NVIDIA L4", "inference_median_ms": 1251.23},
  {"policy": "dm05", "status": "failed", "device": "NVIDIA L4", "inference_median_ms": 693.78, "error": "CUDA Out of Memory"},
  {"policy": "eo1", "status": "failed", "device": "NVIDIA L4", "inference_median_ms": 384.13, "error": "CUDA Out of Memory"},
  {"policy": "evo1", "status": "success", "device": "NVIDIA L4", "inference_median_ms": 396.24},
  {"policy": "gaussian_actor", "status": "success", "device": "NVIDIA L4", "inference_median_ms": 1.96},
  {"policy": "groot", "status": "failed", "device": "NVIDIA L4", "error": "GatedRepoError 403"},
  {"policy": "pi0", "status": "failed", "device": "NVIDIA L4", "inference_median_ms": 478.14, "error": "CUDA Out of Memory"},
  {"policy": "pi05", "status": "failed", "device": "NVIDIA L4", "inference_median_ms": 578.06, "error": "CUDA Out of Memory"},
  {"policy": "smolvla", "status": "success", "device": "NVIDIA L4", "inference_median_ms": 263.32},
  {"policy": "xvla", "status": "success", "device": "NVIDIA L4", "inference_median_ms": 339.91}
]
"""

    private val jsonParser = Json { ignoreUnknownKeys = true }

    fun parseAndSortBenchmarks(): List<LeRobotPolicyBenchmark> {
        val parsed = jsonParser.decodeFromString<List<LeRobotPolicyBenchmark>>(RAW_JSON)
        // Sort by Inference Speed (ascending) by default so fastest models are at the top
        return parsed.sortedBy { it.inferenceMedianMs ?: Double.MAX_VALUE }
    }
}

// Tailwind-aligned Dark Palette (bg-gray-900 / text-gray-100 / green-500 / red-500)
private val Gray900 = Color(0xFF111827)
private val Gray800 = Color(0xFF1F2937)
private val Gray700 = Color(0xFF374151)
private val Gray400 = Color(0xFF9CA3AF)
private val Gray100 = Color(0xFFF3F4F6)
private val StatusGreen = Color(0xFF22C55E)
private val StatusGreenBg = Color(0xFF052E16)
private val StatusRed = Color(0xFFEF4444)
private val StatusRedBg = Color(0xFF450A0A)
private val AmberGold = Color(0xFFF59E0B)

@Composable
fun LeRobotProfilerScreen(
    modifier: Modifier = Modifier
) {
    val allBenchmarks = remember { LeRobotProfilerData.parseAndSortBenchmarks() }
    var activeFilter by remember { mutableStateOf(ProfilerFilter.ALL) }

    val totalCount = allBenchmarks.size
    val passedCount = remember(allBenchmarks) { allBenchmarks.count { it.status.equals("success", ignoreCase = true) } }
    val failedCount = remember(allBenchmarks) { allBenchmarks.count { it.status.equals("failed", ignoreCase = true) } }

    val filteredBenchmarks = remember(allBenchmarks, activeFilter) {
        when (activeFilter) {
            ProfilerFilter.ALL -> allBenchmarks
            ProfilerFilter.SUCCESS_ONLY -> allBenchmarks.filter { it.status.equals("success", ignoreCase = true) }
            ProfilerFilter.FAILED_ONLY -> allBenchmarks.filter { it.status.equals("failed", ignoreCase = true) }
        }
    }

    val maxSpeedMs = remember(allBenchmarks) {
        allBenchmarks.mapNotNull { it.inferenceMedianMs }.maxOrNull() ?: 1251.23
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Gray900)
            .testTag("lerobot_profiler_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Banner
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "LeRobot Profiler",
                            tint = AmberGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "LEROBOT PROFILER • VAULT 1293",
                                style = MaterialTheme.typography.titleMedium,
                                color = Gray100,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Hardware Benchmark • NVIDIA L4 • Sorted by Fastest (ms)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Gray400
                            )
                        }
                    }
                    Surface(
                        color = Gray800,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Gray700)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = AmberGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "NVIDIA L4",
                                style = MaterialTheme.typography.labelSmall,
                                color = Gray100
                            )
                        }
                    }
                }
            }

            // Requirement 1: Summary Card at the top ("Total Policies: 11", "Passed: 6", "Failed: 5")
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Gray800),
                    border = BorderStroke(1.dp, Gray700),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profiler_summary_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SummaryStatBadge(
                            title = "Total Policies: $totalCount",
                            value = "$totalCount",
                            subtitle = "Total Policies",
                            accentColor = Gray100,
                            tag = "summary_total_policies"
                        )
                        SummaryStatBadge(
                            title = "Passed: $passedCount",
                            value = "$passedCount",
                            subtitle = "Passed",
                            accentColor = StatusGreen,
                            tag = "summary_passed_policies"
                        )
                        SummaryStatBadge(
                            title = "Failed: $failedCount",
                            value = "$failedCount",
                            subtitle = "Failed",
                            accentColor = StatusRed,
                            tag = "summary_failed_policies"
                        )
                    }
                }
            }

            // Requirement 2: Filter buttons ("All", "Success Only", "Failed Only")
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter Policies",
                        tint = Gray400,
                        modifier = Modifier.size(18.dp)
                    )
                    ProfilerFilter.entries.forEach { filter ->
                        val isSelected = activeFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { activeFilter = filter },
                            label = {
                                Text(
                                    text = filter.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Gray800,
                                labelColor = Gray100,
                                selectedContainerColor = when (filter) {
                                    ProfilerFilter.ALL -> AmberGold
                                    ProfilerFilter.SUCCESS_ONLY -> StatusGreen
                                    ProfilerFilter.FAILED_ONLY -> StatusRed
                                },
                                selectedLabelColor = Gray900
                            ),
                            modifier = Modifier.testTag(filter.tag)
                        )
                    }
                }
            }

            // Table Header ("Policy Name", "Status", "Device", "Inference Speed (ms)")
            item {
                Card(
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Gray800),
                    border = BorderStroke(1.dp, Gray700),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Policy Name",
                            style = MaterialTheme.typography.labelMedium,
                            color = AmberGold,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1.35f)
                        )
                        Text(
                            text = "Status",
                            style = MaterialTheme.typography.labelMedium,
                            color = AmberGold,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(0.95f)
                        )
                        Text(
                            text = "Device",
                            style = MaterialTheme.typography.labelMedium,
                            color = AmberGold,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(0.95f)
                        )
                        Text(
                            text = "Inference Speed (ms)",
                            style = MaterialTheme.typography.labelMedium,
                            color = AmberGold,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1.15f)
                        )
                    }
                }
            }

            // Table Rows (Sorted ascending by Inference Speed)
            items(filteredBenchmarks, key = { it.policy }) { item ->
                PolicyBenchmarkRow(
                    benchmark = item,
                    maxSpeedMs = maxSpeedMs
                )
            }
        }
    }
}

@Composable
private fun SummaryStatBadge(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    tag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.testTag(tag)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = accentColor,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = Gray100,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = Gray400
        )
    }
}

@Composable
private fun PolicyBenchmarkRow(
    benchmark: LeRobotPolicyBenchmark,
    maxSpeedMs: Double
) {
    val isSuccess = benchmark.status.equals("success", ignoreCase = true)
    val statusColor = if (isSuccess) StatusGreen else StatusRed
    val statusBgColor = if (isSuccess) StatusGreenBg else StatusRedBg

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Gray800),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSuccess) Gray700 else StatusRed.copy(alpha = 0.45f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("profiler_row_${benchmark.policy}")
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Column 1: Policy Name (+ red error text underneath if failed)
                Column(modifier = Modifier.weight(1.35f)) {
                    Text(
                        text = benchmark.policy,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Gray100,
                        fontWeight = FontWeight.Bold
                    )
                    if (!isSuccess && !benchmark.error.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = benchmark.error,
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusRed,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag("profiler_error_${benchmark.policy}")
                        )
                    }
                }

                // Column 2: Status Badge (Green for Success, Red for Failed)
                Box(
                    modifier = Modifier.weight(0.95f),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = statusBgColor,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.7f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = benchmark.status,
                                tint = statusColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = benchmark.status.uppercase(Locale.US),
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Column 3: Device
                Text(
                    text = benchmark.device,
                    style = MaterialTheme.typography.labelMedium,
                    color = Gray100,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(0.95f)
                )

                // Column 4: Inference Speed (ms)
                Column(
                    modifier = Modifier.weight(1.15f),
                    horizontalAlignment = Alignment.End
                ) {
                    val speedText = benchmark.inferenceMedianMs?.let {
                        String.format(Locale.US, "%.2f ms", it)
                    } ?: "N/A"
                    Text(
                        text = speedText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (benchmark.inferenceMedianMs != null && benchmark.inferenceMedianMs < 50.0) {
                            StatusGreen
                        } else {
                            Gray100
                        },
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End
                    )
                }
            }

            // Lightweight inline latency bar (no external chart library)
            if (benchmark.inferenceMedianMs != null && maxSpeedMs > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                val fraction = (benchmark.inferenceMedianMs / maxSpeedMs).toFloat().coerceIn(0.02f, 1.0f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Gray900, RoundedCornerShape(2.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(4.dp)
                            .background(statusColor.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    }
}
