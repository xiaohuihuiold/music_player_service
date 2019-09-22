package com.xhhold.flutter.plugin.musicplayerservice

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar

class MusicPlayerServicePlugin(private val registrar: Registrar, private val channel: MethodChannel) : MethodCallHandler {
    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "com.xhhold.flutter.plugin.musicplayerservice")
            channel.setMethodCallHandler(MusicPlayerServicePlugin(registrar, channel))
        }
    }

    /**
     * 是否绑定服务
     */
    private var isBind: Boolean = false

    /**
     * 音乐播放器控制器
     */
    private var iMusicPlayerController: IMusicPlayerController? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            // 解除绑定
            isBind = false
            iMusicPlayerController = null
            channel.invokeMethod("onServiceDisconnected", null)
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            // 绑定成功
            isBind = true
            iMusicPlayerController = service as IMusicPlayerController
            channel.invokeMethod("onServiceConnected", null)
        }
    }

    init {
        // 销毁时自动解除绑定
        registrar.addViewDestroyListener {
            if (isBind) {
                // 解除绑定
                registrar.activity().unbindService(serviceConnection)
                isBind = false
                channel.invokeMethod("onServiceDisconnected", null)
            }
            true
        }
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "startService" -> {
                // 启动并绑定服务
                startAndBindService(result)
            }
            "stopService" -> {
                // 解绑并停止服务
                unbindAndStopService(result)
            }
            "isServiceRunning" -> {
                // 判断服务是否运行
                result.success(isServiceRunning() && isBind)
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    /***
     * 启动并绑定服务
     */
    private fun startAndBindService(result: Result) {
        registrar.activity().apply {
            if (!isServiceRunning()) {
                // 服务没有运行则先启动服务
                startService(Intent(registrar.activity(), PlayerService::class.java))
            }
            if (!isBind) {
                // 绑定播放器服务
                bindService(Intent(registrar.activity(), PlayerService::class.java), serviceConnection, Context.BIND_IMPORTANT or Context.BIND_WAIVE_PRIORITY)
            }
        }
        result.success(null)
    }

    /**
     * 解绑并停止服务
     */
    private fun unbindAndStopService(result: Result) {
        registrar.activity().apply {
            if (isBind) {
                // 解除绑定
                unbindService(serviceConnection)
                isBind = false
                channel.invokeMethod("onServiceDisconnected", null)
            }
            if (isServiceRunning()) {
                // 停止播放器服务
                stopService(Intent(registrar.activity(), PlayerService::class.java))
            }
        }
        result.success(null)
    }

    /**
     * 判断服务器是否运行中
     */
    private fun isServiceRunning(): Boolean {
        val activityManager = registrar.activity().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val services = activityManager.getRunningServices(Int.MAX_VALUE)
        for (service in services) {
            if (PlayerService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }
}
