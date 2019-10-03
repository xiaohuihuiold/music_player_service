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

class MusicPlayerServicePlugin(private val registrar: Registrar, private val channel: MethodChannel) : MethodCallHandler, IMusicPlayerCallback.Stub() {

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
            iMusicPlayerController?.setPlayerCallback(this@MusicPlayerServicePlugin)
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
            "initNotification" -> {
                // 初始化通知栏
                initNotification(call, result)
            }
            "initMusicList" -> {
                // 设置播放列表
                initMusicList(call, result)
            }
            "play" -> {
                play(call, result)
            }
            "pause" -> {
                pause(call, result)
            }
            "previous" -> {
                previous(call, result)
            }
            "next" -> {
                next(call, result)
            }
            "stop" -> {
                stop(call, result)
            }
            "seek" -> {
                seek(call, result)
            }
            "getMediaPlayerId" -> {
                getMediaPlayerId(call, result)
            }
            "getDuration" -> {
                getDuration(call, result)
            }
            "getPosition" -> {
                getPosition(call, result)
            }
            "isPlaying" -> {
                isPlaying(call, result)
            }
            "getMusicListId" -> {
                getMusicListId(call, result)
            }
            "getMusicId" -> {
                getMusicId(call, result)
            }
            "getMusicMode" -> {
                getMusicMode(call, result)
            }
            "setMusicMode" -> {
                setMusicMode(call, result)
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

    /**
     * 初始化通知管理器
     */
    private fun initNotification(call: MethodCall, result: Result) {
        val title: String? = call.argument<String>("title")
        val text: String? = call.argument<String>("text")
        val sub: String? = call.argument<String>("sub")
        val largeIcon: String? = call.argument<String>("largeIcon")
        val smallIcon: String? = call.argument<String>("smallIcon")
        val play: String? = call.argument<String>("play")
        val pause: String? = call.argument<String>("pause")
        val previous: String? = call.argument<String>("previous")
        val next: String? = call.argument<String>("next")
        iMusicPlayerController?.initNotification(title, text, sub, largeIcon, smallIcon, play, pause, previous, next)
        result.success(null)
    }

    /**
     * 设置播放列表
     */
    private fun initMusicList(call: MethodCall, result: Result) {
        val id = call.argument<Int>("id")
        val list = call.argument<MutableList<Any?>>("list")
        iMusicPlayerController?.initMusicList(id ?: -1, list)
        result.success(null)
    }

    private fun play(call: MethodCall, result: Result) {
        val index = call.argument<Int>("index")
        iMusicPlayerController?.play(index ?: -1, true)
        result.success(null)
    }

    private fun pause(call: MethodCall, result: Result) {
        iMusicPlayerController?.pause()
        result.success(null)
    }

    private fun previous(call: MethodCall, result: Result) {
        iMusicPlayerController?.previous()
        result.success(null)
    }

    private fun next(call: MethodCall, result: Result) {
        iMusicPlayerController?.next()
        result.success(null)
    }

    private fun stop(call: MethodCall, result: Result) {
        iMusicPlayerController?.stop()
        result.success(null)
    }

    private fun seek(call: MethodCall, result: Result) {
        val time = call.argument<Int>("time")
        iMusicPlayerController?.seek(time ?: 0)
        result.success(null)
    }

    private fun getMediaPlayerId(call: MethodCall, result: Result) {
        result.success(iMusicPlayerController?.mediaPlayerId)
    }

    private fun getDuration(call: MethodCall, result: Result) {
        result.success(iMusicPlayerController?.duration)
    }

    private fun getPosition(call: MethodCall, result: Result) {
        result.success(iMusicPlayerController?.position)
    }

    private fun isPlaying(call: MethodCall, result: Result) {
        result.success(iMusicPlayerController?.isPlaying)
    }

    private fun getMusicListId(call: MethodCall, result: Result) {
        result.success(iMusicPlayerController?.musicListId)
    }

    private fun getMusicId(call: MethodCall, result: Result) {
        result.success(iMusicPlayerController?.musicId)
    }

    private fun getMusicMode(call: MethodCall, result: Result) {
        result.success(iMusicPlayerController?.musicMode)
    }

    private fun setMusicMode(call: MethodCall, result: Result) {
        val mode = call.argument<Int>("mode")
        iMusicPlayerController?.musicMode = mode ?: 1
        result.success(null)
    }

    override fun onPlay(musicListId: Int, musicId: Int) {
        channel.invokeMethod("onPlay", mapOf("musicListId" to musicListId, "musicId" to musicId))
    }

    override fun onPause(musicListId: Int, musicId: Int) {
        channel.invokeMethod("onPause", mapOf("musicListId" to musicListId, "musicId" to musicId))
    }

    override fun onPrevious(musicListId: Int, musicId: Int) {
        channel.invokeMethod("onPrevious", mapOf("musicListId" to musicListId, "musicId" to musicId))
    }

    override fun onNext(musicListId: Int, musicId: Int) {
        channel.invokeMethod("onNext", mapOf("musicListId" to musicListId, "musicId" to musicId))
    }

    override fun onStop(musicListId: Int, musicId: Int) {
        channel.invokeMethod("onStop", mapOf("musicListId" to musicListId, "musicId" to musicId))
    }

    override fun onCompleted(musicListId: Int, musicId: Int) {
        channel.invokeMethod("onCompleted", mapOf("musicListId" to musicListId, "musicId" to musicId))
    }

}
