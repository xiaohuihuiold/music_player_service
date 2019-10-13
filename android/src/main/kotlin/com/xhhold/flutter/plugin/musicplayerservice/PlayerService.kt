package com.xhhold.flutter.plugin.musicplayerservice

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.*
import android.media.session.MediaSession
import android.os.Build
import android.os.IBinder
import android.service.media.MediaBrowserService
import android.view.KeyEvent
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

/**
 * 播放模式
 */
enum class PlayMode {
    INV,// 顺序
    SEQ,// 逆序
    RAN,// 随机
    LOOP,// 单曲循环
    ONCE// 播放一次
}

class PlayerService : Service(), AudioManager.OnAudioFocusChangeListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    /**
     * 控制器binder
     */
    private val iBinder = MusicControllerBinder()

    /**
     * 播放器回调
     */
    private var callback: IMusicPlayerCallback? = null

    /**
     * 通知管理器
     */
    private var notificationManager: MediaNotificationManager? = null

    /**
     * MediaSession
     */
    private var mediaSession: MediaSession? = null
    /**
     * 媒体播放管理器
     */
    private var playerManager: MediaPlayerManager = MediaPlayerManager(this)

    /**
     * audio manager
     */
    private var audioManager: AudioManager? = null
    /**
     * 音频焦点
     */
    private var audioFocusRequest: AudioFocusRequest? = null

    /**
     * 通知广播
     */
    private var mediaReceiver: MediaBroadcastReceiver? = null

    /**
     * 播放列表
     */
    private var musicList: MutableList<Any?>? = ArrayList()

    /**
     * 播放列表id
     */
    private var musicListId = -1

    /**
     * 播放id
     */
    private var musicId = -1

    /**
     * 当前播放的下标
     */
    private var currentIndex: Int = 0

    /**
     * 播放模式
     */
    private var playMode: PlayMode = PlayMode.INV

    override fun onCreate() {
        super.onCreate()
        // 创建通知管理器
        notificationManager = MediaNotificationManager(this)
        mediaSession = MediaSession(this, "PlayerService")
        // 创建audio manager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes: AudioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            // 创建音频焦点
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAcceptsDelayedFocusGain(true)
                    .setAudioAttributes(audioAttributes)
                    .setOnAudioFocusChangeListener(this)
                    .build()
        }
        // 创建广播接收器
        mediaReceiver = MediaBroadcastReceiver()
        val intentFilter = IntentFilter()
        intentFilter.addAction(MediaNotificationManager.ACTION_PLAY)
        intentFilter.addAction(MediaNotificationManager.ACTION_PAUSE)
        intentFilter.addAction(MediaNotificationManager.ACTION_PREVIOUS)
        intentFilter.addAction(MediaNotificationManager.ACTION_NEXT)
        intentFilter.addAction(Intent.ACTION_MEDIA_BUTTON)
        registerReceiver(mediaReceiver, intentFilter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? = iBinder

    override fun onUnbind(intent: Intent?): Boolean = true

    override fun onDestroy() {
        playerManager.stop()
        unregisterReceiver(mediaReceiver)
        super.onDestroy()
    }

    /**
     * 音频焦点改变
     */
    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // 丢失焦点
                iBinder.pause()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (audioFocusRequest != null) {
                        audioManager?.abandonAudioFocusRequest(audioFocusRequest!!)
                    }
                } else {
                    audioManager?.abandonAudioFocus(this)
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                iBinder.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                iBinder.play(-1, false)
            }
        }
    }

    /**
     * 播放完成
     */
    override fun onCompletion(mp: MediaPlayer?) {
        when (playMode) {
            PlayMode.INV -> {
                // 顺序
                iBinder.next()
            }
            PlayMode.SEQ -> {
                // 逆序
                iBinder.previous()
            }
            PlayMode.RAN -> {
                // 随机
                iBinder.play(Random().nextInt(musicList?.size ?: 0), false)
            }
            PlayMode.LOOP -> {
                // 单曲循环
                iBinder.play(-1, false)
            }
            PlayMode.ONCE -> {
                // 播放一次
                callback?.onCompleted(musicListId, musicId)
            }
        }
    }

    /**
     * 播放错误
     */
    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        // TODO: 添加播放错误回调
        return true
    }

    inner class MusicControllerBinder : IMusicPlayerController.Stub() {

        /**
         * 播放
         */
        override fun play(index: Int, program: Boolean) {
            if ((musicList?.size ?: 0) == 0) {
                return
            }
            if (index != -1) {
                // 播放被暂停的音乐
                currentIndex = index

            }
            if (currentIndex < 0 || (currentIndex >= (musicList?.size ?: 0))) {
                return
            }

            // 根据下标获取要播放的音乐信息
            val map: MutableMap<*, *> = musicList!![currentIndex] as MutableMap<*, *>
            this@PlayerService.musicId = map["id"] as Int
            // 更新通知栏信息
            notificationManager?.setTitle(map["title"] as String)
            notificationManager?.setText(map["artist"] as String)
            notificationManager?.setSub("")
            notificationManager?.setLargeIcon(loadBitmap(map["albumPath"] as String))
            notificationManager?.play()
            mediaSession?.setMetadata(MediaMetadata.Builder()
                    .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, loadBitmap(map["albumPath"] as String))
                    .putBitmap(MediaMetadata.METADATA_KEY_ART, loadBitmap(map["albumPath"] as String))
                    .putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, loadBitmap(map["albumPath"] as String))
                    .build())

            playerManager.play((musicList!![currentIndex] as MutableMap<*, *>)["path"] as String)
            if (!program) {
                callback?.onPlay(this@PlayerService.musicListId, this@PlayerService.musicId)
            }
            // 获得音频焦点
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (audioFocusRequest != null) {
                    audioManager?.requestAudioFocus(audioFocusRequest!!)
                }
            } else {
                audioManager?.requestAudioFocus(this@PlayerService, AudioManager.STREAM_MUSIC, AudioManager
                        .AUDIOFOCUS_GAIN)
            }
        }

        /**
         * 暂停
         */
        override fun pause() {
            if ((musicList?.size ?: 0) == 0) {
                return
            }
            notificationManager?.pause()
            playerManager.pause()
            callback?.onPause(this@PlayerService.musicListId, this@PlayerService.musicId)
        }

        /**
         * 上一曲
         */
        override fun previous() {
            if ((musicList?.size ?: 0) == 0) {
                return
            }
            currentIndex--
            if (currentIndex < 0) {
                currentIndex = musicList?.size ?: 0 - 1
            }
            play(currentIndex, true)
            callback?.onPrevious(this@PlayerService.musicListId, this@PlayerService.musicId)
        }

        /**
         * 下一曲
         */
        override fun next() {
            if ((musicList?.size ?: 0) == 0) {
                return
            }
            currentIndex++
            if (currentIndex >= (musicList?.size ?: 0)) {
                currentIndex = 0
            }
            play(currentIndex, true)
            callback?.onNext(this@PlayerService.musicListId, this@PlayerService.musicId)
        }

        /**
         * 停止
         */
        override fun stop() {
            callback?.onStop(this@PlayerService.musicListId, this@PlayerService.musicId)
            playerManager.stop()
        }

        /**
         * 设置进度
         */
        override fun seek(time: Int) {
            playerManager.seek(time)
        }

        /**
         * 获取音频id
         */
        override fun getMediaPlayerId(): Int {
            return playerManager.getId()
        }

        /**
         * 获取总长度
         */
        override fun getDuration(): Int {
            return playerManager.getDuration()
        }

        /**
         * 获取当前位置
         */
        override fun getPosition(): Int {
            return playerManager.getPosition()
        }

        /**
         * 获取是否正在播放
         */
        override fun isPlaying(): Boolean {
            return playerManager.isPlaying()
        }

        /**
         * 获取音乐列表id
         */
        override fun getMusicListId(): Int {
            return this@PlayerService.musicListId
        }

        /**
         * 获取音乐id
         */
        override fun getMusicId(): Int {
            return this@PlayerService.musicId
        }

        /**
         * 获取播放模式
         */
        override fun getMusicMode(): Int {
            when (playMode) {
                PlayMode.INV -> {
                    return 1
                }
                PlayMode.SEQ -> {
                    return 2
                }
                PlayMode.RAN -> {
                    return 3
                }
                PlayMode.LOOP -> {
                    return 4
                }
                PlayMode.ONCE -> {
                    return 5
                }
            }
        }

        /**
         * 设置播放模式
         */
        override fun setMusicMode(mode: Int) {
            when (mode) {
                1 -> {
                    playMode = PlayMode.INV
                }
                2 -> {
                    playMode = PlayMode.SEQ
                }
                3 -> {
                    playMode = PlayMode.RAN
                }
                4 -> {
                    playMode = PlayMode.LOOP
                }
                5 -> {
                    playMode = PlayMode.ONCE
                }
            }
        }

        /**
         * 添加播放器回调
         */
        override fun setPlayerCallback(callback: IMusicPlayerCallback) {
            this@PlayerService.callback = callback
        }

        /**
         * 设置音乐列表
         */
        override fun initMusicList(id: Int, list: MutableList<Any?>?) {
            musicList = list ?: ArrayList()
            this@PlayerService.musicListId = id
            currentIndex = 0
        }

        /**
         * 初始化通知信息
         */
        override fun initNotification(title: String?, text: String?, sub: String?, largeIcon: String?, smallIcon: String?, play: String?, pause: String?, previous: String?, next: String?) {
            notificationManager?.init(title, text, sub, loadBitmap(largeIcon), loadBitmap(smallIcon), loadBitmap(play), loadBitmap(pause), loadBitmap(previous), loadBitmap(next))
        }
    }

    /**
     * 广播控制
     */
    inner class MediaBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_MEDIA_BUTTON -> {
                    // 耳机按键监听(暂定)
                    val event: KeyEvent? = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT) as KeyEvent
                    when (event?.keyCode) {
                        KeyEvent.KEYCODE_MEDIA_PLAY -> {
                            if (playerManager.isPlaying()) {
                                iBinder.pause()
                            } else {
                                iBinder.play(-1, false)
                            }
                        }
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                            iBinder.previous()
                        }
                        KeyEvent.KEYCODE_MEDIA_NEXT -> {
                            iBinder.next()
                        }
                    }
                }
                MediaNotificationManager.ACTION_PLAY -> {
                    iBinder.play(-1, false)
                }
                MediaNotificationManager.ACTION_PAUSE -> {
                    iBinder.pause()
                }
                MediaNotificationManager.ACTION_PREVIOUS -> {
                    iBinder.previous()
                }
                MediaNotificationManager.ACTION_NEXT -> {
                    iBinder.next()
                }
            }
        }

    }

    /**
     * 加载图片
     */
    private fun loadBitmap(path: String?): Bitmap? {
        val file: File = File(path)
        try {
            return if (file.exists()) {
                BitmapFactory.decodeFile(path)
            } else {
                BitmapFactory.decodeStream(assets.open("flutter_assets/$path"))
            }
        } catch (e: IOException) {

        }
        return null
    }


}