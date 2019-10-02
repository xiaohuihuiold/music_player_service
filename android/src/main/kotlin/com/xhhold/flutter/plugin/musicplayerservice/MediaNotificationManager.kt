package com.xhhold.flutter.plugin.musicplayerservice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.media.session.MediaSession
import android.os.Build

class MediaNotificationManager(private val service: PlayerService) {
    companion object {
        private const val TAG = "MediaNotificationManager"
        private const val CHANNEL_ID = "PlayerServiceChannel"
        private const val CHANNEL_NAME = "PlayerService"

        const val ACTION_PLAY = "com.xhhold.flutter.plugin.musicplayerservice.Action.NOTIFY.PLAY"
        const val ACTION_PAUSE = "com.xhhold.flutter.plugin.musicplayerservice.Action.NOTIFY.PAUSE"
        const val ACTION_PREVIOUS = "com.xhhold.flutter.plugin.musicplayerservice.Action.NOTIFY.PREVIOUS"
        const val ACTION_NEXT = "com.xhhold.flutter.plugin.musicplayerservice.Action.NOTIFY.NEXT"
    }

    /**
     * 通知管理器
     */
    private var manager: NotificationManager? = null
    private var channel: NotificationChannel? = null

    /**
     * 通知样式
     */
    private var builder: Notification.Builder? = null
    private var style: Notification.MediaStyle? = null
    private var session: MediaSession? = null

    /**
     * 通知按钮
     */
    private var actionPlay: Notification.Action? = null
    private var actionPause: Notification.Action? = null
    private var actionPrevious: Notification.Action? = null
    private var actionNext: Notification.Action? = null

    init {
        manager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 媒体样式的通知栏
        style = Notification.MediaStyle()
        session = MediaSession(service, TAG)
        session?.isActive = true
        style?.setMediaSession(session?.sessionToken)
        style?.setShowActionsInCompactView(0, 1, 2)

        // 根据安卓版本创建通知
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 运行设备安卓版本>=O时
            // 创建channel
            channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_NONE)
            manager?.createNotificationChannel(channel!!)
            // 需要CHANNEL_ID构建通知
            builder = Notification.Builder(service, CHANNEL_ID)
            builder?.setColorized(true)
        } else {
            // 低于AndroidO
            builder = Notification.Builder(service)
        }

        // 设置媒体样式
        builder?.style = style
        builder?.setVisibility(Notification.VISIBILITY_PUBLIC)
    }

    fun init(title: String?, text: String?, sub: String?, largeIcon: Bitmap?, smallIcon: Bitmap?, play: Bitmap?, pause: Bitmap?, previous: Bitmap?, next: Bitmap?) {
        builder?.setLargeIcon(largeIcon)
        if (smallIcon != null) {
            builder?.setSmallIcon(Icon.createWithBitmap(smallIcon))
        }
        builder?.setContentTitle(title)
        builder?.setContentText(text)
        builder?.setSubText(sub)
        if (play != null) {
            actionPlay = Notification.Action.Builder(Icon.createWithBitmap(play), "play", PendingIntent.getBroadcast(service, 0, Intent(ACTION_PLAY), PendingIntent.FLAG_UPDATE_CURRENT)).build()
        }
        if (pause != null) {
            actionPause = Notification.Action.Builder(Icon.createWithBitmap(pause), "pause", PendingIntent.getBroadcast(service, 0, Intent(ACTION_PAUSE), PendingIntent.FLAG_UPDATE_CURRENT)).build()
        }
        if (previous != null) {
            actionPrevious = Notification.Action.Builder(Icon.createWithBitmap(previous), "previous", PendingIntent.getBroadcast(service, 0, Intent(ACTION_PREVIOUS), PendingIntent.FLAG_UPDATE_CURRENT)).build()
        }
        if (next != null) {
            actionNext = Notification.Action.Builder(Icon.createWithBitmap(next), "next", PendingIntent.getBroadcast(service, 0, Intent(ACTION_NEXT), PendingIntent.FLAG_UPDATE_CURRENT)).build()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder?.setActions(actionPrevious, actionPlay, actionNext)
        }
        updateNotification()
    }

    fun play() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder?.setActions(actionPrevious, actionPause, actionNext)
            updateNotification()
        }
    }

    fun pause() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder?.setActions(actionPrevious, actionPlay, actionNext)
            updateNotification()
        }
    }

    fun setLargeIcon(bitmap: Bitmap?) {
        builder?.setLargeIcon(bitmap)
        updateNotification()
    }

    fun setSmallIcon(bitmap: Bitmap?) {
        if (bitmap != null) {
            builder?.setSmallIcon(Icon.createWithBitmap(bitmap))
        }
        updateNotification()
    }

    fun setTitle(title: String?) {
        builder?.setContentTitle(title)
        updateNotification()
    }

    fun setText(text: String?) {
        builder?.setContentText(text)
        updateNotification()
    }

    fun setSub(sub: String?) {
        builder?.setSubText(sub)
        updateNotification()
    }

    private fun updateNotification() {
        service.startForeground(1, builder?.build())
    }
}