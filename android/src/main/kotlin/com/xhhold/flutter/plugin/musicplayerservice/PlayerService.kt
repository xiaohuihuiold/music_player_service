package com.xhhold.flutter.plugin.musicplayerservice

import android.app.Service
import android.content.Intent
import android.os.IBinder

class PlayerService : Service() {

    /**
     * 控制器binder
     */
    private val iBinder = MusicControllerBinder()

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? = iBinder

    override fun onUnbind(intent: Intent?): Boolean = true

    override fun onDestroy() {
        super.onDestroy()
    }

    inner class MusicControllerBinder : IMusicPlayerController.Stub() {
        override fun basicTypes(anInt: Int, aLong: Long, aBoolean: Boolean, aFloat: Float, aDouble: Double, aString: String?) {

        }
    }
}