import 'dart:async';

import 'package:flutter/services.dart';

/// 服务状态
enum ServiceStatus {
  /// 已连接
  CONNECTED,

  /// 已断开连接
  DISCONNECTED,
}

typedef OnServiceStatusCallback = void Function(ServiceStatus status);

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
    String title,
    String text,
    String sub,
    String largeIcon,
    String smallIcon,
    String play,
    String pause,
    String previous,
    String next,
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
  Future<Null> initMusicList(List<MusicData> list) async {
    return await _channel.invokeMethod('initMusicList', {
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
}
