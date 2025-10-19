package dev.jdtech.jellyfin

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.presentation.theme.JellyCastTheme
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.settings.R as SettingsR

data class PlayerApp(
    val packageName: String,
    val name: String,
    val icon: android.graphics.drawable.Drawable
)

class PlayerPickerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d("PlayerPickerActivity", "onCreate called")
        
        // Get currently selected player
        val prefsName = this.packageName + "_preferences"
        val sharedPreferences = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val currentPlayer = sharedPreferences.getString("pref_player_external_app", null)
        
        setContent {
            JellyCastTheme {
                PlayerPickerScreen(
                    currentPlayerPackage = currentPlayer,
                    onPlayerSelected = { packageName ->
                        Log.d("PlayerPickerActivity", "Player selected: $packageName")
                        // Save the selected player to app preferences
                        sharedPreferences.edit().putString("pref_player_external_app", packageName).apply()
                        Log.d("PlayerPickerActivity", "Saved to preferences: $prefsName")
                        
                        setResult(Activity.RESULT_OK)
                        finish()
                    },
                    onCancel = {
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerPickerScreen(
    currentPlayerPackage: String?,
    onPlayerSelected: (String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val players = remember { getAvailablePlayers(context) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(SettingsR.string.select_external_player)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_x),
                            contentDescription = "Close"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (players.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "No se encontraron reproductores externos",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Instala una aplicación como VLC, MX Player o similar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(players) { player ->
                    PlayerItem(
                        player = player,
                        isSelected = player.packageName == currentPlayerPackage,
                        onClick = { onPlayerSelected(player.packageName) }
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerItem(
    player: PlayerApp,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App icon would go here - for now just show text
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = player.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_check),
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_arrow_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun getAvailablePlayers(context: Context): List<PlayerApp> {
    val packageManager = context.packageManager
    val players = mutableListOf<PlayerApp>()
    
    Log.d("PlayerPicker", "Starting player detection...")
    
    // Known popular video players
    val knownPlayers = listOf(
        "org.videolan.vlc",              // VLC
        "com.mxtech.videoplayer.ad",     // MX Player
        "com.mxtech.videoplayer.pro",    // MX Player Pro
        "com.google.android.videos",     // Google Play Movies
        "com.google.android.youtube",     // YouTube
        "org.xbmc.kodi",                 // Kodi
        "com.bsplayer.bspandroid.free",  // BSPlayer
        "com.kmplayer",                  // KMPlayer
        "net.gtvbox.videoplayer",        // Video Player All Format
        "pl.solidexplorer2",             // Solid Explorer
    )
    
    // Method 1: Query with http video URL
    val httpIntent = Intent(Intent.ACTION_VIEW)
    httpIntent.setDataAndType(Uri.parse("http://example.com/video.mp4"), "video/*")
    val httpResolveInfos = packageManager.queryIntentActivities(httpIntent, PackageManager.MATCH_DEFAULT_ONLY)
    Log.d("PlayerPicker", "HTTP query found ${httpResolveInfos.size} apps")
    
    // Method 2: Query with file video URL
    val fileIntent = Intent(Intent.ACTION_VIEW)
    fileIntent.setDataAndType(Uri.parse("file:///sdcard/video.mp4"), "video/*")
    val fileResolveInfos = packageManager.queryIntentActivities(fileIntent, PackageManager.MATCH_DEFAULT_ONLY)
    Log.d("PlayerPicker", "File query found ${fileResolveInfos.size} apps")
    
    // Method 3: Query with content video URL
    val contentIntent = Intent(Intent.ACTION_VIEW)
    contentIntent.setDataAndType(Uri.parse("content://media/external/video/media/1"), "video/*")
    val contentResolveInfos = packageManager.queryIntentActivities(contentIntent, PackageManager.MATCH_DEFAULT_ONLY)
    Log.d("PlayerPicker", "Content query found ${contentResolveInfos.size} apps")
    
    // Combine all results
    val allResolveInfos = (httpResolveInfos + fileResolveInfos + contentResolveInfos).distinctBy { 
        it.activityInfo.packageName 
    }
    
    Log.d("PlayerPicker", "Total unique apps after combining: ${allResolveInfos.size}")
    
    allResolveInfos.forEach { resolveInfo ->
        val activityInfo = resolveInfo.activityInfo
        val packageName = activityInfo.packageName
        
        Log.d("PlayerPicker", "Found package: $packageName")
        
        // Filter out this app and system package picker
        if (packageName == context.packageName || packageName == "android") {
            Log.d("PlayerPicker", "Filtering out: $packageName")
            return@forEach
        }
        
        val appName = resolveInfo.loadLabel(packageManager).toString()
        val appIcon = resolveInfo.loadIcon(packageManager)
        
        if (!players.any { it.packageName == packageName }) {
            Log.d("PlayerPicker", "Adding player: $appName ($packageName)")
            players.add(PlayerApp(packageName, appName, appIcon))
        }
    }
    
    Log.d("PlayerPicker", "Checking ${knownPlayers.size} known players...")
    
    // Add known players that are installed but might not have been detected
    knownPlayers.forEach { packageName ->
        try {
            if (!players.any { it.packageName == packageName }) {
                // Try to get package info to verify it's installed
                val packageInfo = packageManager.getPackageInfo(packageName, 0)
                val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                val appName = packageManager.getApplicationLabel(appInfo).toString()
                val appIcon = packageManager.getApplicationIcon(appInfo)
                Log.d("PlayerPicker", "Found known player: $appName ($packageName)")
                players.add(PlayerApp(packageName, appName, appIcon))
            } else {
                Log.d("PlayerPicker", "Known player already in list: $packageName")
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d("PlayerPicker", "Known player not installed: $packageName - ${e.message}")
        } catch (e: Exception) {
            Log.e("PlayerPicker", "Error checking known player $packageName", e)
        }
    }
    
    Log.d("PlayerPicker", "Final player count: ${players.size}")
    
    return players.sortedBy { it.name }
}
