package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.random.Random

class MediaPlayerActivity : Activity() {

    var songUris = ArrayList<Uri>()
    var songNames = ArrayList<String>()
    var originalUris = ArrayList<Uri>()
    var originalNames = ArrayList<String>()
    var pos = 0
    var isSorted = false
    lateinit var player: MediaPlayer
    lateinit var listView: ListView
    lateinit var seekBar: SeekBar
    lateinit var playPauseBtn: ImageButton
    lateinit var nextBtn: ImageButton
    lateinit var prevBtn: ImageButton
    lateinit var volumeBar: SeekBar
    lateinit var label: TextView
    lateinit var timeElapsed: TextView
    lateinit var timeRemaining: TextView
    lateinit var sortBtn: ImageButton
    lateinit var shuffleBtn: ImageButton
    var handler = Handler(Looper.getMainLooper())
    var progressRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_player)
        listView = findViewById(R.id.songList)
        seekBar = findViewById(R.id.seekBar)
        playPauseBtn = findViewById(R.id.playPauseBtn)
        nextBtn = findViewById(R.id.nextBtn)
        prevBtn = findViewById(R.id.prevBtn)
        volumeBar = findViewById(R.id.volumeBar)
        label = findViewById(R.id.songName)
        timeElapsed = findViewById(R.id.timeElapsed)
        timeRemaining = findViewById(R.id.timeRemaining)
        sortBtn = findViewById(R.id.sortBtn)
        shuffleBtn = findViewById(R.id.shuffleBtn)

        requestAllAudioPermissions()

        listView.setOnItemClickListener { parent, view, position, id -> play(position)
        }

        playPauseBtn.setOnClickListener {
            if (!::player.isInitialized) {
                if (songUris.isNotEmpty()) play(pos)
                return@setOnClickListener
            }
            if (player.isPlaying) {
                player.pause()
                playPauseBtn.setImageResource(android.R.drawable.ic_media_play)
            } else {
                player.start()
                playPauseBtn.setImageResource(android.R.drawable.ic_media_pause)
            }
        }

        nextBtn.setOnClickListener {
            if (songUris.size > 0) {
                pos = (pos + 1) % songUris.size
                play(pos)
            }
        }
        prevBtn.setOnClickListener {
            if (songUris.size > 0) {
                pos = if (pos - 1 < 0) songUris.size - 1 else pos - 1
                play(pos)
            }
        }

        sortBtn.setOnClickListener {
            if (!isSorted) {
                bubbleSortByName()
                isSorted = true
                label.text = "Отсортировано A->Z"
            } else {
                songNames = ArrayList(originalNames)
                songUris = ArrayList(originalUris)
                listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, songNames)
                playPauseBtn.setImageResource(android.R.drawable.ic_media_play)
                label.text = "Мои треки"
                isSorted = false
                pos = 0
            }
        }

        shuffleBtn.setOnClickListener {
            shuffleSongs()
            listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, songNames)
            pos = 0
            playPauseBtn.setImageResource(android.R.drawable.ic_media_play)
            label.text = "Перемешано"
            isSorted = false
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (::player.isInitialized && fromUser) {
                    player.seekTo(progress)
                    updateTimeLabels(progress, player.duration)
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        volumeBar.max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        volumeBar.progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        volumeBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    fun requestAllAudioPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_AUDIO), 123)
            } else {
                getSongs()
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 123)
            } else {
                getSongs()
            }
        }
    }

    fun getSongs() {
        songNames.clear()
        songUris.clear()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            android.provider.MediaStore.Audio.Media.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL)
        else
            android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            android.provider.MediaStore.Audio.Media._ID,
            android.provider.MediaStore.Audio.Media.DISPLAY_NAME,
            android.provider.MediaStore.Audio.Media.MIME_TYPE
        )

        val selection = "${android.provider.MediaStore.Audio.Media.MIME_TYPE}=?"
        val selectionArgs = arrayOf("audio/mpeg")

        val sortOrder: String? = null

        contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DISPLAY_NAME)
            val typeCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.MIME_TYPE)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val nameFull = cursor.getString(nameCol) ?: continue
                val type = cursor.getString(typeCol) ?: ""
                if (type == "audio/mpeg" && nameFull.lowercase().endsWith(".mp3")) {
                    val uri = android.content.ContentUris.withAppendedId(collection, id)
                    val name = if (nameFull.endsWith(".mp3", true)) nameFull.substring(0, nameFull.length - 4) else nameFull
                    songNames.add(name)
                    songUris.add(uri)
                }
            }
        }

        originalNames = ArrayList(songNames)
        originalUris = ArrayList(songUris)
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, songNames)
        label.text = "Мои треки"
        pos = 0
        isSorted = false
    }

    fun play(index: Int) {
        try {
            if (::player.isInitialized) {
                player.stop()
                player.release()
            }
        } catch (_: Exception) {}
        player = MediaPlayer.create(this, songUris[index])
        player.setOnCompletionListener {
            if (songUris.size > 0) {
                pos = (pos + 1) % songUris.size
                play(pos)
            }
        }
        player.start()
        label.text = songNames[index]
        seekBar.max = player.duration
        playPauseBtn.setImageResource(android.R.drawable.ic_media_pause)
        startProgressUpdates()
        pos = index
        updateTimeLabels(0, player.duration)
    }

    fun startProgressUpdates() {
        progressRunnable?.let { handler.removeCallbacks(it) }
        progressRunnable = object : Runnable {
            override fun run() {
                if (::player.isInitialized) {
                    val cur = try { player.currentPosition } catch (e: Exception) { 0 }
                    val dur = try { player.duration } catch (e: Exception) { 0 }
                    seekBar.progress = cur
                    updateTimeLabels(cur, dur)
                    if (player.isPlaying) handler.postDelayed(this, 500)
                }
            }
        }
        handler.post(progressRunnable!!)
    }

    fun updateTimeLabels(currentMs: Int, durationMs: Int) {
        val remain = if (durationMs > currentMs) durationMs - currentMs else 0
        timeElapsed.text = toMMSS(currentMs)
        timeRemaining.text = "-" + toMMSS(remain)
    }

    fun toMMSS(ms: Int): String {
        val totalSec = ms / 1000
        val m = totalSec / 60
        val s = totalSec % 60
        val mm = if (m < 10) "0$m" else "$m"
        val ss = if (s < 10) "0$s" else "$s"
        return "$mm:$ss"
    }

    fun bubbleSortByName() {
        val n = songNames.size
        if (n <= 1) return
        for (i in 0 until n - 1) {
            var swapped = false
            for (j in 0 until n - i - 1) {
                if (songNames[j] > songNames[j + 1]) {
                    val nameTmp = songNames[j]
                    songNames[j] = songNames[j + 1]
                    songNames[j + 1] = nameTmp
                    val uriTmp = songUris[j]
                    songUris[j] = songUris[j + 1]
                    songUris[j + 1] = uriTmp
                    swapped = true
                }
            }
            if (!swapped) break
        }
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, songNames)
        playPauseBtn.setImageResource(android.R.drawable.ic_media_play)
        pos = 0
    }

    fun shuffleSongs() {
        if (songNames.size <= 1) return
        val indices = ArrayList<Int>()
        for (i in 0 until songNames.size) indices.add(i)
        var i = indices.size - 1
        while (i > 0) {
            val j = Random.nextInt(i + 1)
            val t = indices[i]
            indices[i] = indices[j]
            indices[j] = t
            i--
        }
        val newNames = ArrayList<String>(songNames.size)
        val newUris = ArrayList<Uri>(songUris.size)
        for (k in 0 until indices.size) {
            val idx = indices[k]
            newNames.add(songNames[idx])
            newUris.add(songUris[idx])
        }
        songNames = newNames
        songUris = newUris
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 123 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getSongs()
        } else {
            Toast.makeText(this, "Нет доступа к памяти/аудио", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            if (::player.isInitialized && player.isPlaying) {
                player.pause()
                playPauseBtn.setImageResource(android.R.drawable.ic_media_play)
            }
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            progressRunnable?.let { handler.removeCallbacks(it) }
            if (::player.isInitialized) player.release()
        } catch (_: Exception) {}
    }
}
