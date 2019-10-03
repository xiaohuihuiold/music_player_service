package com.xhhold.flutter.plugin.musicplayerservice;

interface IMusicPlayerCallback {
    void onPlay(in int musicListId,in int musicId);
    void onPause(in int musicListId,in int musicId);
    void onPrevious(in int musicListId,in int musicId);
    void onNext(in int musicListId,in int musicId);
    void onStop(in int musicListId,in int musicId);
    void onCompleted(in int musicListId,in int musicId);
}
