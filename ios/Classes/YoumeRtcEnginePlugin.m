#import "YoumeRtcEnginePlugin.h"

#import "YoumeConst.h"

@interface YoumeRendererView ()
@property(nonatomic, strong) UIView *renderView;
@property(nonatomic, assign) int64_t viewId;
@end

@implementation YoumeRendererView
- (instancetype)initWithFrame:(CGRect)frame viewIdentifier:(int64_t)viewId {
    if (self = [super init]) {
        self.renderView = [[UIView alloc] initWithFrame:frame];
        self.renderView.backgroundColor = [UIColor blackColor];
        self.viewId = viewId;
    }
    return self;
}

- (nonnull UIView *)view {
    return self.renderView;
}
@end

@interface YoumeRenderViewFactory : NSObject <FlutterPlatformViewFactory>
@end

@interface YoumeRtcEnginePlugin () <FlutterStreamHandler, VoiceEngineCallback>
@property (strong, nonatomic) YMVoiceService *engine;

@property (retain, atomic) NSMutableArray<FlutterResult>* sdkInitPromise;
@property (retain, atomic) NSMutableArray<FlutterResult>* joinPromise;
@property (retain, atomic) NSMutableArray<FlutterResult>* leavePromise;

@property(strong, nonatomic) FlutterMethodChannel *methodChannel;
@property(strong, nonatomic) FlutterEventChannel *eventChannel;
@property(strong, nonatomic) FlutterEventSink eventSink;

@property(strong, nonatomic) NSMutableDictionary *rendererViews;
@end

#pragma mark - flutter

@implementation YoumeRtcEnginePlugin
#pragma mark - renderer views

+ (instancetype)sharedPlugin {
    static YoumeRtcEnginePlugin *plugin = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        plugin = [[YoumeRtcEnginePlugin alloc] init];
    });
    return plugin;
}

+ (void)addView:(UIView *)view id:(NSNumber *)viewId {
    if (!viewId) {
        return;
    }
    if (view) {
        [[[YoumeRtcEnginePlugin sharedPlugin] rendererViews] setObject:view forKey:viewId];
    } else {
        [self removeViewForId:viewId];
    }
}

+ (void)removeViewForId:(NSNumber *)viewId {
    if (!viewId) {
        return;
    }
    [[[YoumeRtcEnginePlugin sharedPlugin] rendererViews] removeObjectForKey:viewId];
}

+ (UIView *)viewForId:(NSNumber *)viewId {
    if (!viewId) {
        return nil;
    }
    return [[[YoumeRtcEnginePlugin sharedPlugin] rendererViews] objectForKey:viewId];
}

- (NSMutableDictionary *)rendererViews {
    if (!_rendererViews) {
        _rendererViews = [[NSMutableDictionary alloc] init];
    }
    return _rendererViews;
}

#pragma mark - Flutter

+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
    FlutterMethodChannel* channel = [FlutterMethodChannel
                                     methodChannelWithName:@"youme_rtc_engine"
                                     binaryMessenger:[registrar messenger]];
    FlutterEventChannel *eventChannel = [FlutterEventChannel
                                         eventChannelWithName:@"youme_rtc_engine_event"
                                         binaryMessenger:registrar.messenger];
    YoumeRtcEnginePlugin* instance = [[YoumeRtcEnginePlugin alloc] init];
    instance.methodChannel = channel;
    instance.eventChannel = eventChannel;
    
    [registrar addMethodCallDelegate:instance channel:channel];
}

- (id) init {
    self = [super init];
    self.sdkInitPromise = [[NSMutableArray alloc] init];
    self.joinPromise    = [[NSMutableArray alloc] init];
    self.leavePromise   = [[NSMutableArray alloc] init];
    return self;
}


//销毁引擎实例
- (void)dealloc {
//    [[YMVoiceService getInstance] unInit];
    [self.methodChannel setMethodCallHandler:nil];
    [self.eventChannel setStreamHandler:nil];
}

//导出常量
- (NSDictionary *)constantsToExport {
    return @{};
}

- (NSError *) makeNSError:(NSDictionary *)options {
    NSError *error = [NSError errorWithDomain:@"YouMeRTCEngineDomain"
                                         code:[options[@"code"] integerValue]
                                     userInfo:options[@"message"]];
    return error;
}

#pragma mark - FlutterStreamHandler
// plugin implements flutterStreamHandler protocol

- (FlutterError *_Nullable)onCancelWithArguments:(id _Nullable)params {
    _eventSink = nil;
    return nil;
}

- (FlutterError *_Nullable)onListenWithArguments:(id _Nullable)params eventSink:(FlutterEventSink)events {
    _eventSink = events;
    return nil;
}

- (void)sendEvent:(NSString *)name params:(NSDictionary *)params {
    NSLog(@"plugin handleEventChannel: %@, argus: %@", name, params);
    if (_eventSink) {
        NSMutableDictionary *dict = [[NSMutableDictionary alloc] initWithDictionary:params];
        dict[@"event"] = name;
        _eventSink([dict copy]);
    }
}

- (void)sdkInit:(NSDictionary *)params result:(FlutterResult)result{
    if([params[@"serverMode"] integerValue] > 0)
    {
        int serverMode = (int)[params[@"serverMode"] integerValue];
        [[YMVoiceService getInstance] setServerMode:serverMode];
        if(serverMode == 7 || serverMode == 4)
        {
            [[YMVoiceService getInstance] setServerIP: params[@"serverIP"] port:(int)[params[@"serverPort"] integerValue]];
        }
    }
    
    if([params[@"testServer"] boolValue]) [[YMVoiceService getInstance] setTestServer:true];
    
    [_eventChannel setStreamHandler:self];
    
    NSString *appKey = [self stringFromArguments:params key:@"appKey"];
    NSString *secretKey = [self stringFromArguments:params key:@"secretKey"];
    NSInteger region = [self intFromArguments:params key:@"region"];
    NSString *regionExt = [self stringFromArguments:params key:@"regionExt"];
    
    [YoumeConst share].engine    = [YMVoiceService getInstance];
    [YoumeConst share].appKey    = appKey;
    [YoumeConst share].secretKey = secretKey;
    [YoumeConst share].region    = (int)region;
    [YoumeConst share].extRegion = regionExt;
    
    self.engine = [YMVoiceService getInstance];
    
    int code = [self.engine initSDK:self
                                appkey:appKey
                            appSecret:secretKey
                            regionId:(YOUME_RTC_SERVER_REGION_t)region
                    serverRegionName:regionExt];
    NSLog(@"init, code: %d", code);
    if(code == 0){
        [self.sdkInitPromise addObject:result];
    }else{
        result([FlutterError errorWithCode:[@(code) stringValue] message:@"param error" details:@""]);
    }
}

- (void)joingChannel:(NSDictionary *)params result:(FlutterResult)result{
    NSString *channelId = [self stringFromArguments:params key:@"channel"];
    NSString *userID    = [self stringFromArguments:params key:@"userid"];
    NSInteger userRole  = [self intFromArguments:params key:@"role"];
    NSString *token     = [self stringFromArguments:params key:@"token"];
    BOOL     checkRoomExist     = params[@"checkRoomExist"] != nil ? [self boolFromArguments:params key:@"checkRoomExist"] : false;
    BOOL useVideo       = [self boolFromArguments:params key:@"useVideo"];
    BOOL autoRecvStream = params[@"autoRecvStream"] != nil ? [self boolFromArguments:params key:@"autoRecvStream"] : true ;
    
    if(useVideo){
        [self setVideoParams:params];
    }
    
    //保存一下uid 在自定义视图使用
    [YoumeConst share].localUid = userID;
    
    // 设置音视频质量统计数据的回调频率
    [self.engine setAVStatisticInterval: 5000];
    // 设置视频无渲染帧超时等待时间，单位毫秒
    [self.engine setVideoNoFrameTimeout: 5000];
    // 开启自动同步本地状态给其它成员
    [self.engine setAutoSendStatus: true ];
    //[self.engine setVideoNetAdjustmode: 1];
    
    //开启讲话音量回调
    [self.engine setMicLevelCallback:10];
    //开启远端语音音量回调
    [self.engine setFarendVoiceLevelCallback:10];
    [self.engine setVadCallbackEnabled:true];
    
    if(token != nil)
    {
        [self.engine setToken: token];
    }
    
    int code = [self.engine joinChannelSingleMode:userID channelID:channelId userRole:(YouMeUserRole_t)userRole autoRecv:autoRecvStream?true: false];
    if(code == 0){
        [self.joinPromise addObject:result];
    }else{
        result([FlutterError errorWithCode:[@(code) stringValue] message:@"param error" details:@""]);
    }
}

- (void)setVideoParams:(NSDictionary *)params{
    int      fps                = params[@"fps"] != nil ? (int)[self intFromArguments:params key:@"fps"] : 15;
    int      previewFps         = params[@"previewFps"] != nil ? (int)[self intFromArguments:params key:@"previewFps"] : 30;
    int      previewHeight      = params[@"previewHeight"] != nil ? (int)[self intFromArguments:params key:@"previewHeight"] : 640;
    int      previewWidth       = params[@"previewWidth"] != nil ?(int)[self intFromArguments:params key:@"previewWidth"] : 480;
    int      sendWidth          = params[@"sendWidth"] != nil ?(int)[self intFromArguments:params key:@"sendWidth"] : 480;
    int      sendHeight         = params[@"sendHeight"] != nil ?(int)[self intFromArguments:params key:@"sendHeight"] : 640;
    int      secondStreamWidth  = (int)[self intFromArguments:params key:@"secondStreamWidth"];
    int      secondStreamHeight = (int)[self intFromArguments:params key:@"secondStreamHeight"];
    
    int      bitRateMin         = (int)[self intFromArguments:params key:@"bitRateMin"] ;
    int      bitRateMax         = (int)[self intFromArguments:params key:@"bitRateMax"];
    int      bitRateForSecondMin= (int)[self intFromArguments:params key:@"secondStreamBitRateMin"];
    int      bitRateForSecondMax= (int)[self intFromArguments:params key:@"secondStreamBitRateMax"];
    int      fpsForSecond       = (int)[self intFromArguments:params key:@"secondStreamFPS"] || fps;
    BOOL     VBR                = [self boolFromArguments:params key:@"VBR"] ;
    BOOL     secondStreamVBR    = [self boolFromArguments:params key:@"secondStreamVBR"] ;
    
    [self.engine setVideoFps:fps];
    [self.engine setVideoLocalResolutionWidth:previewWidth height:previewHeight];
    [self.engine setVideoNetResolutionWidth:sendWidth height:sendHeight];
    [self.engine setVideoNetResolutionWidthForSecond:secondStreamWidth height:secondStreamHeight];
    [self.engine setVideoPreviewFps:previewFps];
    [self.engine setVBR: VBR ? true: false];
    [self.engine setVBRForSecond: secondStreamVBR ? true : false];
    [self.engine setVideoCodeBitrateForSecond:bitRateForSecondMax minBitrate:bitRateForSecondMin];
    [self.engine setVideoFpsForSecond: fpsForSecond];
    [self.engine setVideoCodeBitrate: bitRateMax minBitrate: bitRateMin];
}

#pragma mark - api

- (void)handleMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result {
    NSString *method = call.method;
    NSDictionary *params = call.arguments;
    NSLog(@"plugin handleMethodCall: %@, argus: %@", method, params);
    
    if ([@"getPlatformVersion" isEqualToString:call.method]) {
        result([@"iOS " stringByAppendingString:[[UIDevice currentDevice] systemVersion]]);
    } else if ([@"create" isEqualToString:call.method]) {
        result(nil);
    } else if ([@"init" isEqualToString:call.method]) {
        [self sdkInit:params result: result];
    } else if ([@"joinChannel" isEqualToString:call.method]) {
        [self joingChannel:params result: result];
    } else if ([@"leaveChannel" isEqualToString:call.method]) {
        int code = [self.engine leaveChannelAll];
        if(code == 0){
            [self.leavePromise addObject:result];
        }else{
            result([FlutterError errorWithCode:[@(code) stringValue] message:@"param error" details:@""]);
        }
    } else if ([@"setLocalVideoPreviewMirror" isEqualToString:call.method]) {
    } else if ([@"setMicrophoneMute" isEqualToString:call.method]) {
        BOOL mute = [self boolFromArguments:params key:@"mute"];
        [self.engine setMicrophoneMute:mute];
        result(nil);
    } else if ([@"setSpeakerMute" isEqualToString:call.method]) {
        BOOL mute = [self boolFromArguments:params key:@"mute"];
        [self.engine setSpeakerMute:mute];
        result(nil);
    } else if ([@"setTCP" isEqualToString:call.method]) {
        BOOL useTCP = [self boolFromArguments:params key:@"useTcp"];
        int code = [self.engine setTCPMode:useTCP];
        result([@(code) stringValue]);
    } else if ([@"setVideoNetAdjustmode" isEqualToString:call.method]) {
    } else if ([@"keepScreenOn" isEqualToString:call.method]) {
    } else if ([@"cancelScreenOn" isEqualToString:call.method]) {
    } else if ([@"startCapturer" isEqualToString:call.method]) {

    } else if ([@"stopCapturer" isEqualToString:call.method]) {

    } else if ([@"outputToSpeaker" isEqualToString:call.method]) {
        BOOL outputToSpeaker = [self boolFromArguments:params key:@"outputToSpeaker"];
        int code = [self.engine setOutputToSpeaker: outputToSpeaker];
        result([@(code) stringValue]);
    } else if ([@"setAutoSendStatus" isEqualToString:call.method]) {
    } else if ([@"setOtherMicMute" isEqualToString:call.method]) {
        NSString *userID    = [self stringFromArguments:params key:@"userid"];
        BOOL mute = [self boolFromArguments:params key:@"mute"];
        [self.engine setOtherMicMute: userID mute: mute];
        result(nil);
    } else if ([@"maskVideoByUserId" isEqualToString:call.method]) {
    } else if ([@"switchCamera" isEqualToString:call.method]) {
    } else if ([@"screenRotationChange" isEqualToString:call.method]) {
    } else if ([@"applicationInBackground" isEqualToString:call.method]) {
    } else if ([@"applicationInFront" isEqualToString:call.method]) {
    } else if ([@"kickOtherFromChannel" isEqualToString:call.method]) {
        NSString *userID    = [self stringFromArguments:params key:@"userid"];
        NSString *channel   = [self stringFromArguments:params key:@"channel"];
        int forbidSecond    = (int)[self intFromArguments:params key:@"forbidSecond"];
        int code = [self.engine kickOtherFromChannel: userID channelID: channel lastTime:forbidSecond];
        result([@(code) stringValue]);
    } else if ([@"setUsersVideoInfo" isEqualToString:call.method]) {
    } else if ([@"setVideoCodeBitrate" isEqualToString:call.method]) {
    } else if ([@"setVideoCodeBitrateForSecond" isEqualToString:call.method]) {
    } else if ([@"setVideoFps" isEqualToString:call.method]) {
    } else if ([@"setVideoFpsForSecond" isEqualToString:call.method]) {
    } else if ([@"setVideoPreviewFps" isEqualToString:call.method]) {
    } else if ([@"setAVStatisticInterval" isEqualToString:call.method]) {
        int interval = (int)[self intFromArguments:params key:@"interval"];
        [self.engine setAVStatisticInterval: interval];
        result(nil);
    } else if ([@"openBeautify" isEqualToString:call.method]) {
    } else if ([@"setBeautyLevel" isEqualToString:call.method]) {
    } else if ([@"startScreenRecorder" isEqualToString:call.method]) {
    } else if ([@"stopScreenRecorder" isEqualToString:call.method]) {
    } else {
        result(FlutterMethodNotImplemented);
    }
}



#pragma mark - <VoiceEngineCallback>
/**
 *  功能描述: 一般事件通知
 *  @param eventType 事件类型
 *  @param iErrorCode 错误码
 *  @param roomid 时间发生的房间号
 *  @param param 其他参数，根据eventType不同而不同
 */
- (void)onYouMeEvent:(YouMeEvent_t)eventType errcode:(YouMeErrorCode_t)iErrorCode roomid:(NSString *)roomid param:(NSString *)param
{
    NSString* copyedRoomID = [roomid copy];
    NSString* copyParam = [param copy];
    dispatch_async (dispatch_get_main_queue (), ^{
        switch (eventType) {
            case YOUME_EVENT_INIT_OK:
            {
                if(self.sdkInitPromise.count > 0)
                {
                    FlutterResult promise = [self.sdkInitPromise objectAtIndex:0];
                    [self.sdkInitPromise removeObjectAtIndex:0];
                    promise(@{@"eventType": @(eventType),
                              @"code":@(iErrorCode),
                              @"channel": copyedRoomID,
                              @"param":copyParam});
                }
            }
                break;
            case YOUME_EVENT_INIT_FAILED:
            {
                if(self.sdkInitPromise.count > 0)
                {
                    FlutterResult promise = [self.sdkInitPromise objectAtIndex:0];
                    [self.sdkInitPromise removeObjectAtIndex:0];
                    promise([FlutterError errorWithCode:[@(iErrorCode) stringValue] message:@"param error" details:@""]);
                }
            }
                break;
            case YOUME_EVENT_JOIN_OK:
            {
                if(self.joinPromise.count > 0)
                {
                    FlutterResult promise = [self.joinPromise objectAtIndex:0];
                    [self.joinPromise removeObjectAtIndex:0];
                    promise(@{@"eventType": @(eventType),
                              @"code":@(iErrorCode),
                              @"channel": copyedRoomID,
                              @"param":copyParam});
                }
            }
                break;
            case YOUME_EVENT_JOIN_FAILED:
            {
                if(self.joinPromise.count > 0)
                {
                    FlutterResult promise = [self.joinPromise objectAtIndex:0];
                    [self.joinPromise removeObjectAtIndex:0];
                    promise([FlutterError errorWithCode:[@(iErrorCode) stringValue] message:@"param error" details:@""]);
                }
            }
                break;
            case YOUME_EVENT_LEAVED_ALL:
            {
                [[YMVoiceService getInstance] deleteAllRender];
                [[YMVoiceService getInstance] removeAllOverlayVideo];
                if(self.leavePromise.count > 0)
                {
                    FlutterResult promise = [self.leavePromise objectAtIndex:0];
                    [self.leavePromise removeObjectAtIndex:0];
                    promise(@{@"eventType": @(eventType),
                              @"code":@(iErrorCode),
                              @"channel": copyedRoomID,
                              @"param":copyParam});
                }
            }
                break;
            case YOUME_EVENT_OTHERS_VIDEO_ON:
            {
                /* 非自动接收模式不需要再做这个hack
                 dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(0.100 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
                 //NSLog(@"init count: %@ %d",copyParam, [self.engine getRenderCount:copyParam]);
                 if([self.engine getRenderCount:copyParam] == 0)
                 {
                 if([YoumeConst share].currentShareUserId == nil || ![[YoumeConst share].currentShareUserId isEqualToString:copyParam] ){
                 [self.engine maskVideoByUserId:copyParam mask:true];
                 NSLog(@"mask user video: %@", copyParam);
                 }else{
                 NSLog(@"user id: %@ is in share, do not mask", copyParam);
                 }
                 }else{
                 NSLog(@"already bunded");
                 }
                 });
                 */
            }
            default:
            {
                if(eventType == 223 && param.length > 6 ){ //YOUME_EVENT_OTHERS_SHARE_INPUT_START
                    [YoumeConst share].currentShareUserId = [param substringToIndex:(param.length - 6)];
                    [self.engine maskVideoByUserId:[YoumeConst share].currentShareUserId mask:false]; //接收开启屏幕共享的用户的流量
                }else if(eventType == 224){ //YOUME_EVENT_OTHERS_SHARE_INPUT_STOP
                    [YoumeConst share].currentShareUserId = nil;
                }
                NSMutableDictionary *params = @{}.mutableCopy;
                params[@"eventType"] = [NSNumber numberWithInteger:eventType];
                params[@"code"] = [NSNumber numberWithInteger:iErrorCode];
                params[@"channel"] = copyedRoomID;
                params[@"param"] = copyParam;
                [self sendEvent:YOUME_ON_EVENT params:params];
            }
                break;
        }
    });
    
}

/**
 *  功能描述:RestAPI回调
 *  @param requestID 调用查询时返回的requestID，可以用于标识一个查询
 *  @param iErrorCode 查询结果的错误码
 *  @param strQuery 查询的请求,json字符串
 *  @param strResult 查询的结果，json字符串
 */
- (void)onRequestRestAPI: (int)requestID iErrorCode:(YouMeErrorCode_t) iErrorCode  query:(NSString*) strQuery  result:(NSString*) strResult 
{
    NSString* strQueryCopy  = [strQuery copy];
    NSString* strResultCopy = [strResult copy];
    dispatch_async (dispatch_get_main_queue (), ^{
        NSMutableDictionary *params = @{}.mutableCopy;
        params[@"code"] = [NSNumber numberWithInteger:iErrorCode];
        params[@"query"] = strQueryCopy;
        params[@"result"] = strResultCopy;
        [self sendEvent:YOUME_ON_RESTAPI params:params];
    });
}

/**
 *  功能描述:获取频道用户列表回调
 *  @param channelID 频道号
 *  @param changeList 更改列表,NSArray<MemberChangeOC*>*
 *  @param isUpdate true-表示进房间后，有人进出的通知，false表示进房间时的当前user列表。
 */
- (void)onMemberChange:(NSString*) channelID changeList:(NSArray<MemberChangeOC*>*) changeList isUpdate:(bool) isUpdate
{
    NSLog(@"onMemberChange");
    dispatch_async (dispatch_get_main_queue (), ^{
        NSMutableDictionary *params = @{}.mutableCopy;
        params[@"code"] = [NSNumber numberWithInteger:0];
        params[@"channel"] = channelID;
        params[@"isUpdate"] = [NSNumber numberWithBool:isUpdate];
        NSMutableArray<NSMutableDictionary*> *memberList = [[NSMutableArray alloc] initWithCapacity: 10];
        for (int i = 0; i < [changeList count]; i++) {
            NSMutableDictionary *member = @{}.mutableCopy;
            member[@"userid"] = [changeList[i].userID copy];
            member[@"isJoin"] = [NSNumber numberWithBool: changeList[i].isJoin];
            [memberList addObject:member];
        }
        params[@"memberList"] = memberList;
        [self sendEvent:YOUME_ON_MEMBER_CHANGE params:params];
    });
}


/**
 *  功能描述:房间内广播消息回调
 */
- (void)onBroadcast:(YouMeBroadcast_t)bc strChannelID:(NSString*)channelID strParam1:(NSString*)param1 strParam2:(NSString*)param2 strContent:(NSString*)content
{}


/**
 *  功能描述: 音视频通话码率、丢包率回调，目前主要用于检测某个用户的网络状况
 *  @param avType     统计数据类型
 *  @param userID   对应的用户ID
 *  @param value    统计数据数值
 */
- (void) onAVStatistic:(YouMeAVStatisticType_t)avType  userID:(NSString*)userID  value:(int) value
{
    dispatch_async(dispatch_get_main_queue(), ^{
        NSMutableDictionary *params = @{}.mutableCopy;
        switch(avType){
            case YOUME_AVS_AUDIO_PACKET_UP_LOSS_HALF:       //音频上行的服务器丢包率，千分比
            case YOUME_AVS_VIDEO_PACKET_UP_LOSS_HALF:       //视频上行的服务器丢包率，千分比
            case YOUME_AVS_AUDIO_PACKET_DOWN_LOSS_RATE:     //音频下行丢包率,千分比
            case YOUME_AVS_VIDEO_PACKET_DOWN_LOSS_RATE:     //视频下行丢包率,千分比
            case YOUME_AVS_RECV_DATA_STAT:     //下行带宽,单位Bps
            case YOUME_AVS_VIDEO_BLOCK:        //视频卡顿
            case YOUME_AVS_AUDIO_DELAY_MS:
            case YOUME_AVS_VIDEO_DELAY_MS:
                params[@"avType"] = [NSNumber numberWithInteger:avType];
                params[@"userId"] =  [NSString stringWithString:userID];
                params[@"value"] = [NSNumber numberWithInteger:value];
                [self sendEvent:YOUME_ON_STATISTIC_UPDATE params:params];
                break;
            default:
                break;
        }
    });
}

///合流相关音视频数据回调
/**
 *  功能描述: 音频数据回调
 */
- (void)onAudioFrameCallback: (NSString*)userId data:(void*) data len:(int)len timestamp:(uint64_t)timestamp
{
    
}
/**
 *  功能描述: 音频合流数据回调
 */
- (void)onAudioFrameMixedCallback: (void*)data len:(int)len timestamp:(uint64_t)timestamp
{
    
}
/**
 *  功能描述: 视频数据回调
 */
- (void)onVideoFrameCallback: (NSString*)userId data:(void*) data len:(int)len width:(int)width height:(int)height fmt:(int)fmt timestamp:(uint64_t)timestamp
{
    
}
/**
 *  功能描述: 视频合流数据回调(需要设置了合流画面和本地视频参与合流)
 */
- (void)onVideoFrameMixedCallback: (void*) data len:(int)len width:(int)width height:(int)height fmt:(int)fmt timestamp:(uint64_t)timestamp
{
    
}

- (void)onVideoFrameCallbackForGLES:(NSString*)userId  pixelBuffer:(CVPixelBufferRef)pixelBuffer timestamp:(uint64_t)timestamp
{
    
}

- (void)onVideoFrameMixedCallbackForGLES:(CVPixelBufferRef)pixelBuffer timestamp:(uint64_t)timestamp
{
    
}

/**
 *  功能描述: 视频渲染回调，添加自定义滤镜
 */
- (int)onVideoRenderFilterCallback:(int)textureId width:(int)width height:(int)height rotation:(int)rotation mirror:(int)mirror
{
    return 0;
}

- (void)onCustomDataCallback: (const void*)data len:(int)len timestamp:(uint64_t)timestamp
{
    
}

- (void)onPcmDataMix:(int)channelNum samplingRateHz:(int)samplingRateHz bytesPerSample:(int)bytesPerSample data:(void *)data dataSizeInByte:(int)dataSizeInByte {
    
}


- (void)onPcmDataRecord:(int)channelNum samplingRateHz:(int)samplingRateHz bytesPerSample:(int)bytesPerSample data:(void *)data dataSizeInByte:(int)dataSizeInByte {
    
}


- (void)onPcmDataRemote:(int)channelNum samplingRateHz:(int)samplingRateHz bytesPerSample:(int)bytesPerSample data:(void *)data dataSizeInByte:(int)dataSizeInByte {
    
}


- (void)onTranslateTextComplete:(YouMeErrorCode_t)errorcode requestID:(unsigned int)requestID text:(NSString *)text srcLangCode:(YouMeLanguageCode_t)srcLangCode destLangCode:(YouMeLanguageCode_t)destLangCode {
    
}


- (void)onVideoPreDecodeDataForUser:(const char *)userId data:(const void *)data len:(int)dataSizeInByte ts:(uint64_t)timestamp {
    
}


/**
 *  功能描述: 接收到声音的PCM回调，用于外部播放
 */
- (void)onPcmData: (int)channelNum samplingRateHz:(int)samplingRateHz bytesPerSample:(int)bytesPerSample data:(void*) data dataSizeInByte:(int)dataSizeInByte
{
    
}

- (dispatch_queue_t)methodQueue {
    return dispatch_get_main_queue();
}

- (NSArray<NSString *> *)supportedEvents {
    return @[
        YOUME_ON_RESTAPI,
        YOUME_ON_EVENT,
        YOUME_ON_MEMBER_CHANGE,
        YOUME_ON_STATISTIC_UPDATE
    ];
}


#pragma mark - helper

- (NSString *)stringFromArguments:(NSDictionary *)params key:(NSString *)key {
    if (![params isKindOfClass:[NSDictionary class]]) {
        return nil;
    }
    
    NSString *value = [params valueForKey:key];
    if (![value isKindOfClass:[NSString class]]) {
        return nil;
    } else {
        return value;
    }
}

- (NSInteger)intFromArguments:(NSDictionary *)params key:(NSString *)key {
    if (![params isKindOfClass:[NSDictionary class]]) {
        return 0;
    }
    
    NSNumber *value = [params valueForKey:key];
    if (![value isKindOfClass:[NSNumber class]]) {
        return 0;
    } else {
        return [value integerValue];
    }
}

- (double)doubleFromArguments:(NSDictionary *)params key:(NSString *)key {
    if (![params isKindOfClass:[NSDictionary class]]) {
        return 0;
    }
    
    NSNumber *value = [params valueForKey:key];
    if (![value isKindOfClass:[NSNumber class]]) {
        return 0;
    } else {
        return [value doubleValue];
    }
}

- (BOOL)boolFromArguments:(NSDictionary *)params key:(NSString *)key {
    if (![params isKindOfClass:[NSDictionary class]]) {
        return NO;
    }
    
    NSNumber *value = [params valueForKey:key];
    if (![value isKindOfClass:[NSNumber class]]) {
        return NO;
    } else {
        return [value boolValue];
    }
}

- (NSDictionary *)dictionaryFromArguments:(NSDictionary *)params key:(NSString *)key {
    if (![params isKindOfClass:[NSDictionary class]]) {
        return nil;
    }
    
    NSDictionary *value = [params valueForKey:key];
    if (![value isKindOfClass:[NSDictionary class]]) {
        return nil;
    } else {
        return value;
    }
}
@end
