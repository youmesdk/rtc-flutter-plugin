import 'dart:async';

import 'package:flutter/services.dart';

class YoumeRtcEngine {
  // 普通方法调用channel
  static const MethodChannel _channel = MethodChannel('youme_rtc_engine');
  // 事件传递channel
  static const EventChannel _eventChannel = EventChannel('youme_rtc_engine_event');

  static var _eventCallback;
  // 事件监听期handler，用以移除监听
  static StreamSubscription<dynamic>? _sink;

  /// 初始化
  static Future<Map<Object?, Object?>> init(String appKey, String secretKey, int region,
    [String regionExt = ""]) async {
    // init sdk
    Map<Object?, Object?> ret = await _channel.invokeMethod('init', {
      "appKey": appKey,
      "secretKey": secretKey,
      "region": region,
      "regionExt": regionExt
    });

    // addEventChannelHandler
    _addEventChannelHandler();
    return ret;
  }

  static void addEventChannelHandler(var eventCallback) {
    _eventCallback = eventCallback;
  }

  // 添加eventChannel监听
  static void _addEventChannelHandler() async {
    _sink = _eventChannel
        .receiveBroadcastStream()
        .listen(_eventListener, onError: onError);
  }

  // 移除eventChannel监听
  static void _removeEventChannelHandler() async {
    await _sink?.cancel();
  }

  static void onError(dynamic err) {}

  static void _eventListener(dynamic event) {
    _eventCallback(event);
  }

  /// 设置镜像模式
  /// [isMirror]  true ,表示镜像模式，false 表示非镜像模式
  static Future<void> setLocalVideoPreviewMirror(bool isMirror) async {
    await _channel
        .invokeMethod('setLocalVideoPreviewMirror', {'isMirror': isMirror});
  }

  /// 设置mic是否静音
  /// [mute]  true ,表示静音，false 表示非静音
  static Future<void> setMicrophoneMute(bool mute) async {
    await _channel.invokeMethod('setMicrophoneMute', {'mute': mute});
  }

  /// 设置扬声器是否静音
  /// [mute]  true ,表示静音，false 表示非静音
  static Future<void> setSpeakerMute(bool mute) async {
    await _channel.invokeMethod('setSpeakerMute', {'mute': mute});
  }

  /// [useTcp]  true ,表示使用tcp传输媒体数据，false 表示使用udp，默认为false
  static Future<void> setTCP(bool useTcp) async {
    await _channel.invokeMethod('setTCP', {'useTcp': useTcp});
  }

  ///  设置网络适配模式，1为手动适配模式，0是自动适配模式，默认为0， 进频道前设置
  static Future<void> setVideoNetAdjustmode(int adjustmode) async {
    await _channel
        .invokeMethod('setVideoNetAdjustmode', {'adjustmode': adjustmode});
  }

  /// 开启屏幕常亮
  static Future<void> keepScreenOn() async {
    await _channel.invokeMethod('keepScreenOn');
  }

  /// 取消屏幕常亮
  static Future<void> cancelScreenOn() async {
    await _channel.invokeMethod('cancelScreenOn');
  }

  /// 开启摄像头
  static Future<void> startCapturer(bool switchWithHeightIfLandscape) async {
    await _channel.invokeMethod('startCapturer',
        {'switchWithHeightIfLandscape': switchWithHeightIfLandscape});
  }

  /// 关闭摄像头
  static Future<void> stopCapturer() async {
    await _channel.invokeMethod('stopCapturer');
  }

  /// 设置是否从听筒输出，默认是没有耳机从扬声器输出，插入耳机从耳机输出。如需强制听筒输出，需要传入false
  static Future<void> outputToSpeaker(bool outputToSpeaker) async {
    await _channel
        .invokeMethod('outputToSpeaker', {'outputToSpeaker': outputToSpeaker});
  }

  /// 设置是否同步自己的设备开关状态
  static Future<void> setAutoSendStatus(bool sync) async {
    await _channel.invokeMethod('setAutoSendStatus', {'sync': sync});
  }

  /// 控制其它人的麦克风开关
  static Future<void> setOtherMicMute(String userid, bool mute) async {
    await _channel
        .invokeMethod('setOtherMicMute', {'userid': userid, 'mute': mute});
  }

  /// 设置是否屏蔽指定userid的视频
  static Future<void> maskVideoByUserId(String userid, bool block) async {
    await _channel
        .invokeMethod('maskVideoByUserId', {'userid': userid, 'block': block});
  }

  /// 切换前后摄像头
  static Future<void> switchCamera() async {
    await _channel.invokeMethod('switchCamera');
  }

  /// 设置屏幕旋转角度，只支持 0，1，2，3，分别对应 0° ，90° ，180°， 270°
  static Future<void> screenRotationChange() async {
    await _channel.invokeMethod('screenRotationChange');
  }

  /// 切换前后摄像头
  static Future<void> applicationInBackground() async {
    await _channel.invokeMethod('applicationInBackground');
  }

  /// 切换前后摄像头
  static Future<void> applicationInFront() async {
    await _channel.invokeMethod('applicationInFront');
  }

  /// 踢出指定 userid
  static Future<void> kickOtherFromChannel(
      String userid, String channel, int forbidSecond) async {
    await _channel.invokeMethod('kickOtherFromChannel',
        {'userid': userid, 'channel': channel, 'forbidSecond': forbidSecond});
  }

  ///
  static Future<void> setUsersVideoInfo(List userStreamInfo) async {
    await _channel
        .invokeMethod('setUsersVideoInfo', {'userStreamInfo': userStreamInfo});
  }

  /// 设置大流码率
  static Future<void> setVideoCodeBitrate(
      int minBitRateKbps, int maxBitRateKbps) async {
    await _channel.invokeMethod('setVideoCodeBitrate',
        {'minBitRateKbps': minBitRateKbps, 'maxBitRateKbps': maxBitRateKbps});
  }

  /// 设置小流码率
  static Future<void> setVideoCodeBitrateForSecond(
      int minBitRateKbps, int maxBitRateKbps) async {
    await _channel.invokeMethod('setVideoCodeBitrateForSecond',
        {'minBitRateKbps': minBitRateKbps, 'maxBitRateKbps': maxBitRateKbps});
  }

  /// 设置大流fps
  static Future<void> setVideoFps(int fps) async {
    await _channel.invokeMethod("setVideoFps", {'fps': fps});
  }

  /// 设置小流fps
  static Future<void> setVideoFpsForSecond(int fps) async {
    await _channel.invokeMethod('setVideoFpsForSecond', {'fps': fps});
  }

  /// 设置预览fps
  static Future<void> setVideoPreviewFps(int fps) async {
    await _channel.invokeMethod('setVideoPreviewFps', {'fps': fps});
  }

  /// 设置统计信息间隔
  static Future<void> setAVStatisticInterval(int interval) async {
    await _channel
        .invokeMethod('setAVStatisticInterval', {'interval': interval});
  }

  /// 设置是否开启美颜 [isOpen] true - 开启， false - 不开启
  static Future<void> openBeautify(bool isOpen) async {
    await _channel.invokeMethod('openBeautify', {'isOpen': isOpen});
  }

  /// 设置美颜等级，有效返回 0.0  - 1.0
  static Future<void> setBeautyLevel(double level) async {
    await _channel.invokeMethod('setBeautyLevel', {'level': level});
  }

  /// 开始录制屏幕
  static Future<void> startScreenRecorder() async {
    await _channel.invokeMethod('startScreenRecorder');
  }

  /// 停止录制屏幕
  static Future<void> stopScreenRecorder() async {
    await _channel.invokeMethod('stopScreenRecorder');
  }

  static Future<int> initSDK() async {
    int code = await _channel.invokeMethod('initSDK', {});
    return code;
  }

  static Future<Map<Object?, Object?>> joinChannel(
    String channelId, String userId, int userRole) async {
    Map<Object?, Object?> ret = await _channel.invokeMethod('joinChannel',
        {'channel': channelId, 'userid': userId, 'role': userRole});
    return ret;
  }

  static Future<Map<Object?, Object?>> leaveChannel() async {
    Map<Object?, Object?> ret = await _channel.invokeMethod('leaveChannel');
    return ret;
  }

}
