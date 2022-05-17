import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:youme_rtc_engine/youme_rtc_engine.dart';
import 'dart:math';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  int _initCode = -1;
  int joinChannelState = 0;

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    int initCode = 0;
    //监听SDK服务事件
    YoumeRtcEngine.addEventChannelHandler((event)=>{
      // ignore: avoid_print
      print("recv event:$event")
    });
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      var appKey = "【https://console.youme.im/user/login】注册后获取";
      var appSecret =
          "【https://console.youme.im/user/login】注册后获取";
      Map<Object?, Object?> ret = await YoumeRtcEngine.init(
        appKey,
        appSecret,
        0,
        "",
      );
      // ignore: avoid_print
      print("init ret:$ret");
    } on PlatformException {
      initCode = -2;
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _initCode = initCode;
    });
  }

  Future<void> joinChannel() async {
    int code=0;
    var rng = Random();
    try {
      var channelId = "aaaaa";
      var userId = "userId-${rng.nextInt(100000)}";
      var userRole = 1;
      Map<Object?, Object?> ret = await YoumeRtcEngine.joinChannel(channelId, userId, userRole);
      code = ret["code"] as int;
      print("joinChannel code:${ret}");
      //开启麦克风
      YoumeRtcEngine.setMicrophoneMute(false);
      //开启声音播放
      YoumeRtcEngine.setSpeakerMute(false);
    } catch (e) {
      print("joinChannel exception:${e}");
      print(e);
      code = -1;
    }

    setState(() {
      joinChannelState = code;
    });
  }

  static TextStyle textStyle = const TextStyle(fontSize: 18, color: Colors.blue);

  Widget mainBody() {
    return Column(children: <Widget>[
      Row(children: <Widget>[
        Center(child: Text("init state: $_initCode"))
      ]),
      Row(children: <Widget>[
        OutlinedButton(
          child: Text('Join Channel: aaa, state: $joinChannelState',
              style: textStyle),
          onPressed: joinChannel,
        ),
      ]),
      Row(children: const <Widget>[]),
      Row(children: const <Widget>[]),
      Row(children: const <Widget>[]),
    ]);
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('YouMe RTC'),
        ),
        body: Center(
          child: mainBody(),
        ),
      ),
    );
  }
}
