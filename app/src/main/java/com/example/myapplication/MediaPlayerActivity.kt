package com.example.myapplication

import android.app.Activity
import android.media.MediaPlayer
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import java.lang.reflect.Field

class MediaPlayerActivity : Activity() {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var seekBar: SeekBar
    private lateinit var volumeBar: SeekBar
    private lateinit var trackName: TextView
    private lateinit var trackList: ListView
    private val handler = Handler(Looper.getMainLooper())

    private var tracks: List<Int> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_player)

        seekBar = findViewById(R.id.seekBar)
        trackName = findViewById(R.id.trackName)
        volumeBar = findViewById(R.id.volumeBar)
        trackList = findViewById(R.id.trackList)
        val playBtn: Button = findViewById(R.id.playBtn)
        val pauseBtn: Button = findViewById(R.id.pauseBtn)

        loadTracks()

        playBtn.setOnClickListener {
            if (::mediaPlayer.isInitialized && !mediaPlayer.isPlaying) {
                mediaPlayer.start()
                updateSeekBar()
            }
        }

        pauseBtn.setOnClickListener {
            if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
                mediaPlayer.pause()
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (::mediaPlayer.isInitialized && fromUser) {
                    mediaPlayer.seekTo(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        volumeBar.max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        volumeBar.progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        volumeBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun loadTracks() {
        tracks = listOf()
        val trackNames = mutableListOf<String>()
        val trackIds = mutableListOf<Int>()

        try {
            val rClass = R.raw::class.java
            val fields: Array<Field> = rClass.fields

            for (field in fields) {
                val resourceId = field.getInt(null)
                val resourceName = field.name
                trackNames.add(resourceName)
                trackIds.add(resourceId)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка при загрузке файлов", Toast.LENGTH_SHORT).show()
        }

        tracks = trackIds

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, trackNames)
        trackList.adapter = adapter

        trackList.setOnItemClickListener { _, _, position, _ ->
            playTrack(tracks[position], trackNames[position])
        }
    }

    private fun playTrack(resourceId: Int, fileName: String) {
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }

        mediaPlayer = MediaPlayer.create(this, resourceId)
        trackName.text = fileName
        seekBar.max = mediaPlayer.duration
        mediaPlayer.start()
        updateSeekBar()
    }

    private fun updateSeekBar() {
        handler.post(object : Runnable {
            override fun run() {
                if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
                    seekBar.progress = mediaPlayer.currentPosition
                    handler.postDelayed(this, 500)
                }
            }
        })
    }

    override fun onPause() {
        super.onPause()
        if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
        handler.removeCallbacksAndMessages(null)
    }
}
