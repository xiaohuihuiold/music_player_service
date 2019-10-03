import 'dart:async';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:music_scanner/music_scanner.dart';
import 'package:music_player_service/music_player_service.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: HomePage(),
    );
  }
}

class HomePage extends StatefulWidget {
  @override
  _HomePageState createState() => _HomePageState();
}

class _HomePageState extends State<HomePage>
    with SingleTickerProviderStateMixin
    implements MediaPlayerCallback {
  List<MusicInfo> _musicList;
  MusicInfo _currentMusic;
  bool _isPlaying = false;
  double _progress = 0.0;

  Timer _timer;

  @override
  void initState() {
    super.initState();
    _timer = Timer.periodic(Duration(milliseconds: 200), (_) async {
      int duration = await MusicPlayerService().getDuration();
      int position = await MusicPlayerService().getPosition();
      if (duration == 0) {
        setState(() {
          _progress = 0;
        });
        return;
      }
      setState(() {
        _progress = position / duration;
      });
    });
    MusicPlayerService().isPlaying().then((value) {
      setState(() {
        _isPlaying = value ?? false;
      });
    });
    MusicPlayerService().getMusicId().then((value) {
      setState(() {
        _currentMusic = _musicList?.firstWhere((music) => music.id == value);
      });
    });
    MusicPlayerService().addMediaPlayerCallback(this);
    MusicPlayerService().addServiceStatusCallback((status) {
      print('服务状态:$status');
      MusicPlayerService().initNotification(
        title: 'Example',
        text: 'music',
        sub: '测试',
        largeIcon: 'images/ic_launcher.png',
        smallIcon: 'images/ic_launcher.png',
        play: 'images/ic_play_arrow_white_24dp.png',
        pause: 'images/ic_pause_white_24dp.png',
        previous: 'images/ic_skip_previous_white_24dp.png',
        next: 'images/ic_skip_next_white_24dp.png',
      );
      MusicPlayerService().initMusicList(
        id: 0,
        list: List<MusicData>.generate(
          _musicList?.length ?? 0,
          (index) {
            return MusicData(
              id: _musicList[index].id,
              title: _musicList[index].title,
              artist: _musicList[index].artist,
              albumPath: _musicList[index].albumPath,
              path: _musicList[index].path,
            );
          },
        ),
      );
    });
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      MusicScanner.refreshAlbumImagesCache();
      List<MusicInfo> musicList = await MusicScanner.getAllMusic();
      if (!mounted) return;
      setState(() {
        _musicList = musicList;
      });
    });
  }

  Widget _buildMusicList() {
    return ListView.builder(
      itemCount: _musicList?.length ?? 0,
      itemBuilder: (context, index) {
        MusicInfo musicInfo = _musicList[index];
        return ListTile(
          selected: _currentMusic == musicInfo,
          onTap: () {
            MusicPlayerService().play(index: index);
          },
          leading: AspectRatio(
            aspectRatio: 1.0,
            child: ClipRRect(
              borderRadius: BorderRadius.circular(80.0),
              child: Stack(
                fit: StackFit.expand,
                children: <Widget>[
                  Image.file(
                    File(musicInfo.albumPath),
                    fit: BoxFit.cover,
                  ),
                  Container(
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(80.0),
                      border: Border.all(
                          color: Colors.grey.withOpacity(0.6), width: 2.0),
                    ),
                  ),
                ],
              ),
            ),
          ),
          title: Text(musicInfo.title),
          subtitle: Text(musicInfo.album),
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Music Player Service'),
      ),
      body: _buildMusicList(),
      bottomNavigationBar: Container(
        height: kToolbarHeight,
        decoration: BoxDecoration(
          color: Colors.white,
          boxShadow: [BoxShadow(color: Colors.grey, blurRadius: 4.0)],
        ),
        child: Column(
          children: <Widget>[
            Container(
              height: 2.0,
              width: double.infinity,
              child: LinearProgressIndicator(
                value: _progress ?? 0.0,
              ),
            ),
            Expanded(
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                crossAxisAlignment: CrossAxisAlignment.center,
                children: <Widget>[
                  Padding(
                    padding: EdgeInsets.all(4.0),
                    child: AspectRatio(
                      aspectRatio: 1.0,
                      child: ClipRRect(
                        borderRadius: BorderRadius.circular(80.0),
                        child: Stack(
                          fit: StackFit.expand,
                          children: <Widget>[
                            Image.file(
                              File(_currentMusic?.albumPath ?? ''),
                              fit: BoxFit.cover,
                            ),
                            Container(
                              decoration: BoxDecoration(
                                borderRadius: BorderRadius.circular(80.0),
                                border: Border.all(
                                    color: Colors.grey.withOpacity(0.6),
                                    width: 2.0),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                  SizedBox(width: 8.0),
                  Expanded(
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      mainAxisAlignment: MainAxisAlignment.center,
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: <Widget>[
                        Text(
                          _currentMusic?.title ?? '<Unknown>',
                          overflow: TextOverflow.clip,
                          maxLines: 1,
                          style: TextStyle(
                            fontSize: 18.0,
                            color: Colors.black.withOpacity(0.7),
                          ),
                        ),
                        Text(
                          _currentMusic?.artist ?? '<Unknown>',
                          overflow: TextOverflow.clip,
                          maxLines: 1,
                          style: TextStyle(
                            fontSize: 14.0,
                            color: Colors.grey,
                          ),
                        ),
                      ],
                    ),
                  ),
                  Row(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.center,
                    children: <Widget>[
                      InkWell(
                        child: Icon(
                          Icons.skip_previous,
                          size: 32.0,
                          color: Colors.black.withOpacity(0.7),
                        ),
                        onTap: () {
                          MusicPlayerService().previous();
                        },
                      ),
                      SizedBox(width: 12.0),
                      InkWell(
                        child: Icon(
                          _isPlaying
                              ? Icons.pause_circle_outline
                              : Icons.play_circle_outline,
                          size: 38.0,
                          color: Colors.black.withOpacity(0.7),
                        ),
                        onTap: () {
                          if (_isPlaying) {
                            MusicPlayerService().pause();
                          } else {
                            MusicPlayerService().play();
                          }
                        },
                      ),
                      SizedBox(width: 12.0),
                      InkWell(
                        child: Icon(
                          Icons.skip_next,
                          size: 32.0,
                          color: Colors.black.withOpacity(0.7),
                        ),
                        onTap: () {
                          MusicPlayerService().next();
                        },
                      ),
                    ],
                  ),
                  SizedBox(width: 8.0),
                ],
              ),
            ),
          ],
        ),
      ),
      floatingActionButton: Column(
        mainAxisSize: MainAxisSize.min,
        children: <Widget>[
          FloatingActionButton(
            heroTag: 'start',
            child: Icon(Icons.play_arrow),
            onPressed: () {
              MusicPlayerService().startService();
            },
          ),
          SizedBox(height: 8.0),
          FloatingActionButton(
            heroTag: 'stop',
            child: Icon(Icons.stop),
            onPressed: () {
              MusicPlayerService().stopService();
            },
          ),
          SizedBox(height: 8.0),
          FloatingActionButton(
            heroTag: 'check',
            child: Icon(Icons.check),
            onPressed: () async {
              print(await MusicPlayerService().isServiceRunning());
            },
          ),
        ],
      ),
    );
  }

  @override
  void onCompleted(int musicListId, int musicId) {
    setState(() {
      _isPlaying = false;
    });
  }

  @override
  void onNext(int musicListId, int musicId) {
    setState(() {
      _isPlaying = true;
      _currentMusic = _musicList?.firstWhere((music) => music.id == musicId);
    });
  }

  @override
  void onPause(int musicListId, int musicId) {
    setState(() {
      _isPlaying = false;
    });
  }

  @override
  void onPlay(int musicListId, int musicId) {
    setState(() {
      _isPlaying = true;
      _currentMusic = _musicList?.firstWhere((music) => music.id == musicId);
    });
  }

  @override
  void onPrevious(int musicListId, int musicId) {
    setState(() {
      _isPlaying = true;
      _currentMusic = _musicList?.firstWhere((music) => music.id == musicId);
    });
  }

  @override
  void onStop(int musicListId, int musicId) {
    setState(() {
      _isPlaying = false;
    });
  }
}
