package com.example.data

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import java.io.File

/**
 * Hardware MediaRecorder / MediaPlayer helper for real voice memos stored locally
 * and base64-encoded for cross-device Firestore synchronization.
 */
class AudioMemoController(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var activeRecordingFile: File? = null
    private var recordingStartMillis: Long = 0L

    private fun memoDir(): File = File(context.filesDir, "voice_memos").apply {
        if (!exists()) mkdirs()
    }

    fun memoFileForId(memoId: String): File = File(memoDir(), "memo_${memoId.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.m4a")

    fun startRecording(memoId: String): Boolean {
        stopPlayback()
        releaseRecorder()
        val outputFile = memoFileForId(memoId)
        return try {
            val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            rec.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(16000)
                setAudioEncodingBitRate(32000)
                setMaxDuration(30_000) // Cap at 30s so encoded base64 stays well under Firestore 1MB doc limit
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            recorder = rec
            activeRecordingFile = outputFile
            recordingStartMillis = System.currentTimeMillis()
            true
        } catch (_: Exception) {
            releaseRecorder()
            false
        }
    }

    /**
     * Stops recording and returns Pair(durationSeconds, base64AudioString).
     */
    fun stopRecording(): Pair<Long, String> {
        val elapsedSec = ((System.currentTimeMillis() - recordingStartMillis) / 1000L).coerceIn(1L, 30L)
        return try {
            recorder?.stop()
            releaseRecorder()
            val file = activeRecordingFile
            if (file != null && file.exists() && file.length() > 0L) {
                val bytes = file.readBytes()
                val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
                elapsedSec to encoded
            } else {
                elapsedSec to ""
            }
        } catch (_: Exception) {
            releaseRecorder()
            elapsedSec to ""
        }
    }

    fun playMemo(
        memo: SyncedVoiceMemo,
        onCompletion: () -> Unit,
        onError: () -> Unit
    ): Boolean {
        stopPlayback()
        val targetFile = memoFileForId(memo.id)
        if (!targetFile.exists() && memo.audioBase64.isNotBlank()) {
            try {
                val decoded = Base64.decode(memo.audioBase64, Base64.DEFAULT)
                targetFile.writeBytes(decoded)
            } catch (_: Exception) {
                onError()
                return false
            }
        }
        if (!targetFile.exists() || targetFile.length() == 0L) {
            onError()
            return false
        }
        return try {
            val mp = MediaPlayer().apply {
                setDataSource(targetFile.absolutePath)
                setOnCompletionListener {
                    stopPlayback()
                    onCompletion()
                }
                setOnErrorListener { _, _, _ ->
                    stopPlayback()
                    onError()
                    true
                }
                prepare()
                start()
            }
            player = mp
            true
        } catch (_: Exception) {
            stopPlayback()
            onError()
            false
        }
    }

    fun currentPositionSec(): Int {
        return try {
            ((player?.currentPosition ?: 0) / 1000).coerceAtLeast(0)
        } catch (_: Exception) {
            0
        }
    }

    fun isPlaying(): Boolean = try {
        player?.isPlaying == true
    } catch (_: Exception) {
        false
    }

    fun stopPlayback() {
        try {
            player?.stop()
        } catch (_: Exception) {
        }
        try {
            player?.release()
        } catch (_: Exception) {
        }
        player = null
    }

    fun releaseRecorder() {
        try {
            recorder?.reset()
        } catch (_: Exception) {
        }
        try {
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null
    }

    fun releaseAll() {
        stopPlayback()
        releaseRecorder()
    }
}
