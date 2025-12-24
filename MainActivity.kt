package com.example.pixelcatalog

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.fetch.Fetcher
import coil.decode.DataSource
import coil.request.Options
import coil.ImageLoader
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.example.pixelcatalog.ui.theme.PixelCatalogTheme // Assicurati che il tema esista
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Usa colori dinamici (Material You)
            MaterialTheme(colorScheme = dynamicDarkColorScheme(LocalContext.current)) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF121212)) {
                    AppContent()
                }
            }
        }
    }
}

// --- COIL EXTENSION PER FFMPEG ---
// Questo serve per dire a Coil: "Se è un video, usa FFmpeg per la miniatura"
class FFmpegFetcher(
    private val path: String
) : Fetcher {
    override suspend fun fetch(): coil.fetch.FetchResult? {
        // Estrazione miniatura robusta su thread IO
        val bitmap = withContext(Dispatchers.IO) {
            MetadataEngine.getThumbnail(path)
        }
        return bitmap?.let {
            coil.fetch.DrawableResult(
                drawable = android.graphics.drawable.BitmapDrawable(it),
                isSampled = true,
                dataSource = DataSource.DISK
            )
        }
    }
    class Factory : Fetcher.Factory<String> {
        override fun create(data: String, options: Options, imageLoader: ImageLoader): Fetcher? {
            // Attiva solo se sembra un percorso file video
            if (data.startsWith("/") && (data.endsWith(".mkv", true) || data.endsWith(".mp4", true) || data.endsWith(".webm", true))) {
                return FFmpegFetcher(data)
            }
            return null
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AppContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Permessi
    val permissionState = rememberPermissionState(
        if (Build.VERSION.SDK_INT >= 33) android.Manifest.permission.READ_MEDIA_VIDEO 
        else android.Manifest.permission.READ_EXTERNAL_STORAGE
    )

    var activeTab by remember { mutableStateOf("videos") }
    var videos by remember { mutableStateOf<List<VideoFile>>(emptyList()) }
    
    // Setup Coil Loader Custom
    val imageLoader = ImageLoader.Builder(context)
        .components { add(FFmpegFetcher.Factory()) } // Aggiungiamo il nostro estrattore
        .build()

    // Importazione File Singolo
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { 
            // Qui gestiresti l'importazione del singolo file
            // Per ora lo apriamo direttamente
            launchExternalPlayer(context, it)
        }
    }

    // Importazione Cartella (Playlist)
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        // Qui scansioneresti la cartella per creare una playlist
    }

    LaunchedEffect(Unit) {
        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        } else {
            // Caricamento iniziale
            withContext(Dispatchers.IO) {
                videos = MetadataEngine.scanDeviceVideos(context)
            }
        }
    }

    if (!permissionState.status.isGranted) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = { permissionState.launchPermissionRequest() }) {
                Text("Permetti accesso ai video")
            }
        }
        return
    }

    Scaffold(
        containerColor = Color(0xFF121212),
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(Color(0xFF121212))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (activeTab == "videos") "Libreria" else "Playlist",
                    fontSize = 22.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Normal
                )

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // TASTO IMPORTA DINAMICO
                    FilledTonalButton(
                        onClick = { 
                            if (activeTab == "videos") filePicker.launch(arrayOf("video/*"))
                            else folderPicker.launch(null)
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF2A2A2A)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            if (activeTab == "videos") Icons.Outlined.FileDownload else Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (activeTab == "videos") "Importa Video" else "Importa Playlist",
                            color = Color(0xFFE0E0E0),
                            fontSize = 12.sp
                        )
                    }
                    
                    IconButton(onClick = { /* Cerca */ }) {
                        Icon(Icons.Default.Search, null, tint = Color.White)
                    }
                }
            }
        },
        bottomBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(Color(0xFF121212))
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NavBarItem("Video", Icons.Filled.Videocam, activeTab == "videos") { activeTab = "videos" }
                NavBarItem("Playlist", Icons.Filled.VideoLibrary, activeTab == "playlists") { activeTab = "playlists" }
            }
        }
    ) { padding ->
        
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(videos) { video ->
                // Carichiamo i metadati avanzati al volo (Codec/Res)
                // Usiamo "produceState" per farlo in background senza bloccare lo scroll
                val metadata by produceState(initialValue = mapOf("codec" to "...", "resolution" to "...")) {
                    value = withContext(Dispatchers.IO) {
                        MetadataEngine.enrichVideoMetadata(video.path)
                    }
                }

                VideoRow(
                    video = video.copy(codec = metadata["codec"] ?: "", resolution = metadata["resolution"] ?: ""),
                    imageLoader = imageLoader,
                    onClick = { launchExternalPlayer(context, video.uri) }
                )
            }
        }
    }
}

@Composable
fun VideoRow(video: VideoFile, imageLoader: ImageLoader, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // MINIATURA (Gestita da Coil + FFmpeg)
        Box(
            modifier = Modifier
                .width(130.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E1E1E))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(video.path) // Passiamo il path stringa per attivare il nostro Fetcher
                    .crossfade(true)
                    .build(),
                imageLoader = imageLoader, // Usiamo il loader custom
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Durata
            Surface(
                color = Color.Black.copy(alpha = 0.8f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)
            ) {
                Text(
                    text = video.duration,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        // METADATI
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = video.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )
            
            Spacer(Modifier.height(6.dp))
            
            // Chips Tecnici
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (video.resolution.contains("4K") || video.resolution.contains("2K")) {
                    TechBadge(video.resolution, Color(0xFFD0BCFF), Color(0xFF121212))
                }
                TechBadge(video.codec, Color(0xFF2A2A2A), Color(0xFFC4C7C5))
                TechBadge(video.container, Color(0xFF2A2A2A), Color(0xFFC4C7C5))
            }
            
            Spacer(Modifier.height(4.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.FolderOpen, null, tint = Color(0xFF8E918F), modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = video.path.substringBeforeLast("/"), // Mostra solo cartella
                    color = Color(0xFF8E918F),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun TechBadge(text: String, bg: Color, fg: Color) {
    Surface(
        color = bg,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.height(18.dp),
        border = if (bg == Color(0xFF2A2A2A)) null else null
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
            Text(text = text, color = fg, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun NavBarItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, active: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .height(32.dp)
                .width(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (active) Color(0xFF4F378B) else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = if (active) Color(0xFFEADDFF) else Color(0xFF8E918F))
        }
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (active) Color(0xFFEADDFF) else Color(0xFF8E918F),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

fun launchExternalPlayer(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "video/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Apri con..."))
}
