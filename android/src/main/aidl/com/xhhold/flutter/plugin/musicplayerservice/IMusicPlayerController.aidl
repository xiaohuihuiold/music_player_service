package com.xhhold.flutter.plugin.musicplayerservice;

import com.xhhold.flutter.plugin.musicplayerservice.IMusicPlayerCallback;

interface IMusicPlayerController {
    /**
     * 初始化通知栏
     */
    void initNotification(in String title,in String text,in String sub,in String largeIcon,in String smallIcon,in String play,in String pause,in String previous,in String next);

    /**
     * 设置播放列表
     */
    void initMusicList(in int id,in List list);

    /**
     * 播放
     */
    void play(in int index,in boolean program);

    /**
     * 暂停
     */
    void pause();

    /**
     * 上一曲
     */
    void previous();

    /**
     * 下一曲
     */
    void next();

    /**
     * 停止
     */
    void stop();

    /**
     * 设置进度
     */
    void seek(in int time);

    /**
     * 获取音频id
     */
    int getMediaPlayerId();

    /**
     * 获取总长度
     */
    int getDuration();

    /**
     * 获取当前位置
     */
    int getPosition();

    /**
     * 获取是否正在播放
     */
    boolean isPlaying();

    /**
     * 获取音乐列表id
     */
    int getMusicListId();

    /**
     * 获取音乐id
     */
    int getMusicId();

    /**
     * 获取播放模式
     */
    int getMusicMode();

    /**
     * 设置播放模式
     */
    void setMusicMode(in int mode);

    /**
     * 添加播放器回调
     */
    void setPlayerCallback(in IMusicPlayerCallback callback);
}
