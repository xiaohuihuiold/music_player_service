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

class _HomePageState extends State<HomePage> {
  List<MusicInfo> _musicList;

  @override
  void initState() {
    super.initState();
    MusicPlayerService().addServiceStatusCallback((status) {
      print('服务状态:$status');
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
          onTap: () {
            MusicScanner.getAlbumByAlbumId(musicInfo.albumId).then((value) {
              print(value.toJson());
            });
            MusicScanner.getArtistByArtistId(musicInfo.artistId).then((value) {
              print(value.toJson());
            });
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
}
