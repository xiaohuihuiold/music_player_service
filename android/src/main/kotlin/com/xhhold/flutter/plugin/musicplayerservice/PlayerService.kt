package com.xhhold.flutter.plugin.musicplayerservice

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.KeyEvent
import java.io.File
import java.io.IOException

class PlayerService : Service(), AudioManager.OnAudioFocusChangeListener {

    /**
     * 控制器binder
     */
    private val iBinder = MusicControllerBinder()

    /**
     * 通知管理器
     */
    private var notificationManager: MediaNotificationManager? = null

    /**
     * 媒体播放管理器
     */
    private var playerManager: MediaPlayerManager = MediaPlayerManager()

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
     * 当前播放的下标
     */
    private var currentIndex: Int = 0

    override fun onCreate() {
        super.onCreate()
        // 创建通知管理器
        notificationManager = MediaNotificationManager(this)
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
        super.onDestroy()
        unregisterReceiver(mediaReceiver)
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


    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
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
                iBinder.play(-1)
            }
        }
    }

    inner class MusicControllerBinder : IMusicPlayerController.Stub() {
        override fun play(index: Int) {
            if ((musicList?.size ?: 0) == 0) {
                return
            }
            notificationManager?.play()
            if (index != -1) {
                currentIndex = index

            }
            if (currentIndex < 0 || (currentIndex >= (musicList?.size ?: 0))) {
                return
            }
            val map: MutableMap<*, *> = musicList!![currentIndex] as MutableMap<*, *>
            notificationManager?.setTitle(map["title"] as String)
            notificationManager?.setText(map["artist"] as String)
            notificationManager?.setSub("")
            notificationManager?.setLargeIcon(loadBitmap(map["albumPath"] as String))
            playerManager.play((musicList!![currentIndex] as MutableMap<*, *>)["path"] as String)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (audioFocusRequest != null) {
                    audioManager?.requestAudioFocus(audioFocusRequest!!)
                }
            } else {
                audioManager?.requestAudioFocus(this@PlayerService, AudioManager.STREAM_MUSIC, AudioManager
                        .AUDIOFOCUS_GAIN)
            }
        }

        override fun pause() {
            if ((musicList?.size ?: 0) == 0) {
                return
            }
            notificationManager?.pause()
            playerManager.pause()
        }

        override fun previous() {
            if ((musicList?.size ?: 0) == 0) {
                return
            }
            currentIndex--
            if (currentIndex < 0) {
                currentIndex = musicList?.size ?: 0 - 1
            }
            play(currentIndex)
        }

        override fun next() {
            if ((musicList?.size ?: 0) == 0) {
                return
            }
            currentIndex++
            if (currentIndex >= (musicList?.size ?: 0)) {
                currentIndex = 0
            }
            play(currentIndex)
        }

        override fun initMusicList(list: MutableList<Any?>?) {
            musicList = list ?: ArrayList()
        }

        override fun initNotification(title: String?, text: String?, sub: String?, largeIcon: String?, smallIcon: String?, play: String?, pause: String?, previous: String?, next: String?) {
            notificationManager?.init(title, text, sub, loadBitmap(largeIcon), loadBitmap(smallIcon), loadBitmap(play), loadBitmap(pause), loadBitmap(previous), loadBitmap(next))
        }

        override fun basicTypes(anInt: Int, aLong: Long, aBoolean: Boolean, aFloat: Float, aDouble: Double, aString: String?) {

        }
    }

    inner class MediaBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_MEDIA_BUTTON -> {
                    val event: KeyEvent? = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT) as KeyEvent
                    when (event?.keyCode) {
                        KeyEvent.KEYCODE_MEDIA_PLAY -> {
                            if (playerManager.isPlaying()) {
                                iBinder.pause()
                            } else {
                                iBinder.play(-1)
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
                    iBinder.play(-1)
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
}