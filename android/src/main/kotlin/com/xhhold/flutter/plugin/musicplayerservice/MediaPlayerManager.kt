package com.xhhold.flutter.plugin.musicplayerservice

import android.media.MediaPlayer

class MediaPlayerManager(private val playerService: PlayerService){
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
        mediaPlayer?.setOnCompletionListener(playerService)
        mediaPlayer?.setOnErrorListener(playerService)
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

    /**
     * 设置进度
     */
    fun seek(time: Int) {
        mediaPlayer?.seekTo(time)
    }

    /**
     * 获取音频id
     */
    fun getId(): Int {
        return mediaPlayer?.audioSessionId ?: 0
    }

    /**
     * 获取总长度
     */
    fun getDuration(): Int {
        return mediaPlayer?.duration ?: 0
    }

    /**
     * 获取当前位置
     */
    fun getPosition(): Int {
        return mediaPlayer?.currentPosition ?: 0
    }

    /**
     * 获取是否正在播放
     */
    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying ?: false
    }

}