import 'dart:async';

import 'package:flutter/services.dart';

class MusicPlayerService {
  static const MethodChannel _channel =
      const MethodChannel('music_player_service');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
}
