package com.xhhold.flutter.plugin.musicplayerservice

import android.media.MediaPlayer

class MediaPlayerManager {

    private var mediaPlayer: MediaPlayer? = null
    private var path: String? = null
    private var isPrepared = false

    /**
     * 播放音乐
     */
    fun play(path: String) {
        if (path == this.path && mediaPlayer?.isPlaying == false && isPrepared) {
            mediaPlayer?.start()
            return
        }
        this.path = path
        stop()
        mediaPlayer?.setDataSource(path)
        mediaPlayer?.prepareAsync()
        mediaPlayer?.setOnPreparedListener {
            isPrepared = true
            mediaPlayer?.start()
        }
    }

    /**
     * 暂停音乐
     */
    fun pause() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
        }
    }

    /**
     * 停止音乐
     */
    fun stop() {
        isPrepared = false
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        mediaPlayer = MediaPlayer()

    }

}