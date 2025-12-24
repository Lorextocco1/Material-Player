package com.example.pixelcatalog

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import wseemann.media.ffmpegnative.FFmpegMediaMetadataRetriever
import java.io.File
import java.util.concurrent.TimeUnit

// Modello Dati Video Completo
data class VideoFile(
    val id: Long,
    val title: String,
    val path: String,
    val uri: Uri,
    val duration: String,
    val size: String,
    // Metadati Tecnici
    val resolution: String = "Unknown",
    val codec: String = "Unknown",
    val container: String = "Unknown", // mp4, mkv, webm
    val thumbnailUri: Any? = null // Può essere Bitmap o Uri
)

// Modello Playlist
data class Playlist(
    val id: Long,
    val title: String,
    val path: String, // Cartella di origine
    val videoCount: Int,
    val videos: List<VideoFile>
)

object MetadataEngine {

    // Funzione principale: Scansiona una cartella o tutto il dispositivo
    fun scanDeviceVideos(context: Context): List<VideoFile> {
        val videos = mutableListOf<VideoFile>()
        
        // 1. Usiamo MediaStore per trovare i percorsi dei file velocemente
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA, // Path assoluto
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION
        )

        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null, null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

            while (cursor.moveToNext()) {
                val path = cursor.getString(pathCol)
                val file = File(path)
                
                // Filtro base: il file deve esistere
                if (file.exists()) {
                    // ESTRATTORI METADATI ROBUSTI
                    // Qui chiamiamo FFmpeg solo se necessario o per dettagli extra
                    // Nota: Chiamare FFmpeg su 1000 video rallenta. 
                    // In un'app reale si fa in background o lazy-loading.
                    // Per ora facciamo un parsing veloce del nome/estensione e lazy load del codec
                    
                    val ext = file.extension.uppercase()
                    
                    videos.add(VideoFile(
                        id = cursor.getLong(idCol),
                        title = cursor.getString(nameCol),
                        path = path,
                        uri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cursor.getString(idCol)),
                        duration = formatMillis(cursor.getLong(durCol)),
                        size = formatSize(cursor.getLong(sizeCol)),
                        container = ext, // MP4, MKV
                        codec = "Auto", // Sarà caricato poi o dedotto
                        resolution = "HD" // Placeholder, FFmpeg lo aggiornerà nel dettaglio
                    ))
                }
            }
        }
        return videos
    }

    // Questa funzione estrae i DETTAGLI REALI usando FFmpeg
    // Da chiamare quando si apre il dettaglio o in background
    fun enrichVideoMetadata(path: String): Map<String, String> {
        val mmr = FFmpegMediaMetadataRetriever()
        return try {
            mmr.setDataSource(path)
            
            // Estrazione sicura
            val codec = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_VIDEO_CODEC) ?: "UNK"
            val width = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val height = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            
            val res = if (width != null && height != null) {
                val w = width.toInt()
                val h = height.toInt()
                when {
                    w >= 3800 -> "4K"
                    w >= 2500 -> "2K"
                    w >= 1900 -> "1080p"
                    w >= 1200 -> "720p"
                    else -> "${w}p"
                }
            } else "SD"

            mapOf(
                "codec" to codec.uppercase(),
                "resolution" to res
            )
        } catch (e: Exception) {
            Log.e("MetadataEngine", "Errore FFmpeg su $path", e)
            mapOf("codec" to "N/A", "resolution" to "N/A")
        } finally {
            mmr.release()
        }
    }

    // Generatore Miniature FFmpeg (Per Coil)
    // Questo permette di vedere la miniatura anche se è un MKV AV1
    fun getThumbnail(path: String): Bitmap? {
        val mmr = FFmpegMediaMetadataRetriever()
        return try {
            mmr.setDataSource(path)
            // Prende il frame al secondo 1 (spesso il secondo 0 è nero)
            mmr.getFrameAtTime(1000000, FFmpegMediaMetadataRetriever.OPTION_CLOSEST)
        } catch (e: Exception) {
            null
        } finally {
            mmr.release()
        }
    }

    private fun formatMillis(millis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun formatSize(bytes: Long): String {
        val mb = bytes / (1024 * 1024)
        return if (mb > 1000) {
            String.format("%.1f GB", mb / 1024.0)
        } else {
            "$mb MB"
        }
    }
}
