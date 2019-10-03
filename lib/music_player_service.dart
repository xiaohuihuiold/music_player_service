import 'dart:async';

import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';

/// 服务状态
enum ServiceStatus {
  /// 已连接
  CONNECTED,

  /// 已断开连接
  DISCONNECTED,
}

typedef OnServiceStatusCallback = void Function(ServiceStatus status);

/// 播放顺序枚举
enum PlayMode {
  INV, // 顺序
  SEQ, // 逆序
  RAN, // 随机
  LOOP, // 单曲循环
  ONCE // 播放一次
}

/// player回调
abstract class MediaPlayerCallback {
  void onPlay(int musicListId, int musicId);

  void onPause(int musicListId, int musicId);

  void onPrevious(int musicListId, int musicId);

  void onNext(int musicListId, int musicId);

  void onStop(int musicListId, int musicId);

  void onCompleted(int musicListId, int musicId);
}

/// 音乐数据类
class MusicData {
  final int id;

  final String title;

  final String artist;

  final String albumPath;

  final String path;

  MusicData({this.id, this.title, this.artist, this.albumPath, this.path});
}

class MusicPlayerService {
  static const MethodChannel _channel =
      const MethodChannel('com.xhhold.flutter.plugin.musicplayerservice');

  static MusicPlayerService _instance;

  static MusicPlayerService get instance => _getInstance();

  factory MusicPlayerService() => _getInstance();

  static MusicPlayerService _getInstance() {
    if (_instance == null) {
      _instance = MusicPlayerService._internal();
    }
    return _instance;
  }

  /// 服务状态回调列表
  List<OnServiceStatusCallback> _onServiceStatusCallbacks = List();

  /// 播放器回调
  List<MediaPlayerCallback> _onMediaPlayerCallbacks = List();

  MusicPlayerService._internal() {
    _channel.setMethodCallHandler(onMethodCall);
  }

  Future onMethodCall(MethodCall call) async {
    switch (call.method) {
      case 'onServiceConnected':
        // 服务绑定成功
        _onServiceStatusCallbacks?.forEach((callback) {
          if (callback != null) {
            callback(ServiceStatus.CONNECTED);
          }
        });
        break;
      case 'onServiceDisconnected':
        // 服务解绑成功
        _onServiceStatusCallbacks?.forEach((callback) {
          if (callback != null) {
            callback(ServiceStatus.DISCONNECTED);
          }
        });
        break;
      case 'onPlay':
        _onMediaPlayerCallbacks?.forEach((callback) {
          callback?.onPlay(
              call.arguments["musicListId"], call.arguments["musicId"]);
        });
        break;
      case 'onPause':
        _onMediaPlayerCallbacks?.forEach((callback) {
          callback?.onPause(
              call.arguments["musicListId"], call.arguments["musicId"]);
        });
        break;
      case 'onPrevious':
        _onMediaPlayerCallbacks?.forEach((callback) {
          callback?.onPrevious(
              call.arguments["musicListId"], call.arguments["musicId"]);
        });
        break;
      case 'onNext':
        _onMediaPlayerCallbacks?.forEach((callback) {
          callback?.onNext(
              call.arguments["musicListId"], call.arguments["musicId"]);
        });
        break;
      case 'onStop':
        _onMediaPlayerCallbacks?.forEach((callback) {
          callback?.onStop(
              call.arguments["musicListId"], call.arguments["musicId"]);
        });
        break;
      case 'onCompleted':
        _onMediaPlayerCallbacks?.forEach((callback) {
          callback?.onCompleted(
              call.arguments["musicListId"], call.arguments["musicId"]);
        });
        break;
    }
    return call;
  }

  /// 启动服务
  Future<Null> startService() async {
    await _channel.invokeMethod('startService');
  }

  /// 停止服务
  Future<Null> stopService() async {
    await _channel.invokeMethod('stopService');
  }

  /// 停止服务
  Future<bool> isServiceRunning() async {
    return await _channel.invokeMethod<bool>('isServiceRunning');
  }

  /// 初始化通知
  Future<Null> initNotification({
    @required String title,
    @required String text,
    @required String sub,
    @required String largeIcon,
    @required String smallIcon,
    @required String play,
    @required String pause,
    @required String previous,
    @required String next,
  }) async {
    return await _channel.invokeMethod('initNotification', {
      'title': title,
      'text': text,
      'sub': sub,
      'largeIcon': largeIcon,
      'smallIcon': smallIcon,
      'play': play,
      'pause': pause,
      'previous': previous,
      'next': next,
    });
  }

  /// 设置播放列表
  Future<Null> initMusicList({
    @required int id,
    @required List<MusicData> list,
  }) async {
    return await _channel.invokeMethod('initMusicList', {
      'id': id,
      'list': List<Map>.generate(list.length, (index) {
        return {
          'id': list[index].id,
          'title': list[index].title,
          'artist': list[index].artist,
          'albumPath': list[index].albumPath,
          'path': list[index].path,
        };
      }),
    });
  }

  /// 播放
  Future<Null> play({int index}) async {
    return await _channel.invokeMethod('play', {'index': index});
  }

  /// 暂停
  Future<Null> pause() async {
    return await _channel.invokeMethod('pause');
  }

  /// 上一曲
  Future<Null> previous() async {
    return await _channel.invokeMethod('previous');
  }

  /// 下一曲
  Future<Null> next() async {
    return await _channel.invokeMethod('next');
  }

  /// 停止
  Future<Null> stop() async {
    return await _channel.invokeMethod('stop');
  }

  /// 跳转
  Future<Null> seek({int time}) async {
    return await _channel.invokeMethod('seek', {'time': time});
  }

  Future<int> getMediaPlayerId() async {
    return await _channel.invokeMethod('getMediaPlayerId');
  }

  Future<int> getDuration() async {
    return await _channel.invokeMethod('getDuration');
  }

  Future<int> getPosition() async {
    return await _channel.invokeMethod('getPosition');
  }

  Future<bool> isPlaying() async {
    return await _channel.invokeMethod('isPlaying');
  }

  Future<int> getMusicListId() async {
    return await _channel.invokeMethod('getMusicListId');
  }

  Future<int> getMusicId() async {
    return await _channel.invokeMethod('getMusicId');
  }

  Future<PlayMode> getMusicMode() async {
    int mode = await _channel.invokeMethod('getMusicMode');
    switch (mode) {
      case 1:
        return PlayMode.INV;
      case 2:
        return PlayMode.SEQ;
      case 3:
        return PlayMode.RAN;
      case 4:
        return PlayMode.LOOP;
      case 5:
        return PlayMode.ONCE;
    }
    return null;
  }

  Future<Null> setMusicMode(PlayMode mode) async {
    int m = 1;
    switch (mode) {
      case PlayMode.INV:
        m = 1;
        break;
      case PlayMode.SEQ:
        m = 2;
        break;
      case PlayMode.RAN:
        m = 3;
        break;
      case PlayMode.LOOP:
        m = 4;
        break;
      case PlayMode.ONCE:
        m = 5;
        break;
    }
    return _channel.invokeMethod('setMusicMode', {'mode': m});
  }

  /// 添加服务状态回调
  void addServiceStatusCallback(OnServiceStatusCallback callback) {
    if (_onServiceStatusCallbacks.contains(callback)) {
      return;
    }
    _onServiceStatusCallbacks.add(callback);
  }

  /// 移除服务状态回调
  void removeServiceStatusCallback(OnServiceStatusCallback callback) {
    _onServiceStatusCallbacks.remove(callback);
  }

  /// 添加media回调
  void addMediaPlayerCallback(MediaPlayerCallback callback) {
    if (_onMediaPlayerCallbacks.contains(callback)) {
      return;
    }
    _onMediaPlayerCallbacks.add(callback);
  }

  /// 移除media回调
  void removeMediaPlayerCallback(MediaPlayerCallback callback) {
    _onMediaPlayerCallbacks.remove(callback);
  }
}
