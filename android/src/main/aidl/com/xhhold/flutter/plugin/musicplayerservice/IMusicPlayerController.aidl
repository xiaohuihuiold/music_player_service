package com.xhhold.flutter.plugin.musicplayerservice;


interface IMusicPlayerController {
    /**
     * 初始化通知栏
     */
    void initNotification(String title,String text,String sub,String largeIcon,String smallIcon,String play,String pause,String previous,String next);

    /**
     * 设置播放列表
     */
    void initMusicList(in List list);

    /**
     * 播放
     */
    void play(int index);

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

    void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
            double aDouble, String aString);
}
