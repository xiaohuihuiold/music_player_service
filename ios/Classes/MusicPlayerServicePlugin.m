#import "MusicPlayerServicePlugin.h"
#import <music_player_service/music_player_service-Swift.h>

@implementation MusicPlayerServicePlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftMusicPlayerServicePlugin registerWithRegistrar:registrar];
}
@end
