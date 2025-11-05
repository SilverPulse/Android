package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

class MediaPlayerActivity : Activity() {

    var songUris = ArrayList<Uri>()
    var songNames = ArrayList<String>()
    var pos = 0
    lateinit var player: MediaPlayer
    lateinit var listView: ListView
    lateinit var seekBar: SeekBar
    lateinit var playBtn: ImageButton
    lateinit var pauseBtn: ImageButton
    lateinit var nextBtn: ImageButton
    lateinit var prevBtn: ImageButton
    lateinit var volumeBar: SeekBar
    lateinit var label: TextView
    var handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_player)
        listView = findViewById(R.id.songList)
        seekBar = findViewById(R.id.seekBar)
        playBtn = findViewById(R.id.playBtn)
        pauseBtn = findViewById(R.id.pauseBtn)
        nextBtn = findViewById(R.id.nextBtn)
        prevBtn = findViewById(R.id.prevBtn)
        volumeBar = findViewById(R.id.volumeBar)
        label = findViewById(R.id.songName)

        requestAllAudioPermissions()

        listView.setOnItemClickListener { _, _, position, _ ->
            play(position)
        }

        playBtn.setOnClickListener {
            if (::player.isInitialized && !player.isPlaying) player.start()
        }
        pauseBtn.setOnClickListener {
            if (::player.isInitialized && player.isPlaying) player.pause()
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

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (::player.isInitialized && fromUser) player.seekTo(progress)
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

        val musicDir = File(Environment.getExternalStorageDirectory(), "Music")

        if (musicDir.exists() && musicDir.isDirectory) {
            musicDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".mp3", true)) {
                    songNames.add(file.nameWithoutExtension)
                    songUris.add(Uri.fromFile(file))
                }
            }
        }

        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, songNames)
    }

    fun play(index: Int) {
        try {
            if (::player.isInitialized) player.release()
        } catch (_: Exception) {}
        player = MediaPlayer.create(this, songUris[index])
        player.start()
        label.text = songNames[index]
        seekBar.max = player.duration
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (::player.isInitialized && player.isPlaying) {
                    seekBar.progress = player.currentPosition
                    handler.postDelayed(this, 400)
                }
            }
        }, 400)
        pos = index
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
            if (::player.isInitialized && player.isPlaying) player.pause()
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        try { if (::player.isInitialized) player.release() } catch (_: Exception) {}
    }
}
