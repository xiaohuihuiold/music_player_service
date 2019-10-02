package com.xhhold.flutter.plugin.musicplayerservice

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.IBinder
import java.io.File
import java.io.IOException

class PlayerService : Service() {

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
        // 创建广播接收器
        mediaReceiver = MediaBroadcastReceiver()
        val intentFilter = IntentFilter()
        intentFilter.addAction(MediaNotificationManager.ACTION_PLAY)
        intentFilter.addAction(MediaNotificationManager.ACTION_PAUSE)
        intentFilter.addAction(MediaNotificationManager.ACTION_PREVIOUS)
        intentFilter.addAction(MediaNotificationManager.ACTION_NEXT)
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

    inner class MusicControllerBinder : IMusicPlayerController.Stub() {
        override fun play(index: Int) {
            if ((musicList?.size ?: 0) == 0) {
                return
            }
            notificationManager?.play()
            if (index != -1) {
                currentIndex = index
                if (currentIndex < 0 || (currentIndex >= (musicList?.size ?: 0))) {
                    return
                }
                val map: MutableMap<*, *> = musicList!![currentIndex] as MutableMap<*, *>
                notificationManager?.setTitle(map["title"] as String)
                notificationManager?.setText(map["artist"] as String)
                notificationManager?.setLargeIcon(loadBitmap(map["albumPath"] as String))
            }
            if (currentIndex < 0 || (currentIndex >= (musicList?.size ?: 0))) {
                return
            }
            playerManager.play((musicList!![currentIndex] as MutableMap<*, *>)["path"] as String)
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