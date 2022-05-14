package im.youme.youmertcengine;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.youme.voiceengine.MemberChange;
import com.youme.voiceengine.NativeEngine;
import com.youme.voiceengine.ScreenRecorder;
import com.youme.voiceengine.YouMeCallBackInterface;
import com.youme.voiceengine.YouMeConst;
import com.youme.voiceengine.api;
import com.youme.voiceengine.mgr.YouMeManager;

import java.util.ArrayList;
import java.util.HashMap;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.plugin.common.StandardMessageCodec;

/**
 * YoumeRtcEnginePlugin
 */
@SuppressWarnings("ALL")
public class YoumeRtcEnginePlugin implements FlutterPlugin, MethodCallHandler, ActivityAware, EventChannel.StreamHandler, AudioManager.OnAudioFocusChangeListener {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private final String TAG = "YoumeRtcEnginePlugin";
    private MethodChannel channel;
    private Activity mActivity;
    private final Registrar mRegistrar;

    private Handler mEventHandler = new Handler(Looper.getMainLooper());
    private HashMap<String, SurfaceView> mRendererViews;
    private EventChannel.EventSink sink;
    private boolean mixAudio = false;
    private Intent forgroundIntent;

    private final static String YOUME_ON_EVENT = "YOUME_ON_EVENT";
    private final static String YOUME_ON_RESTAPI = "YOUME_ON_RESTAPI";
    private final static String YOUME_ON_MEMBER_CHANGE = "YOUME_ON_MEMBER_CHANGE";
    private final static String YOUME_ON_STATISTIC_UPDATE = "YOUME_ON_STATISTIC_UPDATE";

    // 用于存放异步事件
    private ArrayList<Result> initResults = new ArrayList<>();
    private ArrayList<Result> joinResults = new ArrayList<>();
    private ArrayList<Result> leaveResults = new ArrayList<>();
    private ArrayList<Result> restapiResults = new ArrayList<>();

    // 共享桌面分辨率
    private int shareWidth = 720;
    private int shareHeight = 1280;
    private int shareFps = 15;

    // 屏幕旋转
    private OrientationReciver mOrientationReciver;
    private boolean isInChannel;

    private <T> T readArgument(MethodCall call, String key, T defaultValue) {
        return call.hasArgument(key) ? (T) call.argument(key) : defaultValue;
    }

    void addView(SurfaceView view, int id) {
        mRendererViews.put("" + id, view);
    }

    private void removeView(int id) {
        mRendererViews.remove("" + id);
    }

    private SurfaceView getView(int id) {
        return mRendererViews.get("" + id);
    }

    public YoumeRtcEnginePlugin() {
        this.sink = null;
        this.mRendererViews = new HashMap<>();
        this.mRegistrar = null;
        api.SetCallback(mRtcEventHandler);
        YouMeManager.Init(getActiveContext());
    }

    private YoumeRtcEnginePlugin(Registrar registrar) {
        this.sink = null;
        this.mRendererViews = new HashMap<>();
        this.mRegistrar = registrar;
        api.SetCallback(mRtcEventHandler);
        YouMeManager.Init(getActiveContext());
    }

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "youme_rtc_engine");
        channel.setMethodCallHandler(this);
        final EventChannel eventChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), "youme_rtc_engine_event");
        eventChannel.setStreamHandler(this);
        YoumeRenderViewFactory factory = new YoumeRenderViewFactory(StandardMessageCodec.INSTANCE, this);
        flutterPluginBinding.getPlatformViewRegistry().registerViewFactory("YoumeRendererView", factory);
    }

    // This static function is optional and equivalent to onAttachedToEngine. It supports the old
    // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
    // plugin registration via this function while apps migrate to use the new Android APIs
    // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
    //
    // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
    // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
    // depending on the user's project. onAttachedToEngine or registerWith must both be defined
    // in the same class.
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "youme_rtc_engine");
        final EventChannel eventChannel = new EventChannel(registrar.messenger(), "youme_rtc_engine_event");
        YoumeRtcEnginePlugin plugin = new YoumeRtcEnginePlugin(registrar);
        channel.setMethodCallHandler(plugin);
        eventChannel.setStreamHandler(plugin);
        YoumeRenderViewFactory factory = new YoumeRenderViewFactory(StandardMessageCodec.INSTANCE, plugin);
        registrar.platformViewRegistry().registerViewFactory("YoumeRendererView", factory);
    }

    private Activity getActiveContext() {
        if (mRegistrar != null) {
            return mRegistrar.activity();
        }
        return mActivity;
    }

    private Context getApplicationContext() {
        return getActiveContext().getApplicationContext();
    }

    private HashMap<String, Object> getResultMap(int event, int code, String channel, String param) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("eventType", event);
        map.put("code", code);
        map.put("channel", channel);
        map.put("param", param);
        return map;
    }

    private YouMeCallBackInterface mRtcEventHandler = new YouMeCallBackInterface() {
        @Override
        public void onEvent(final int eventType, final int code, final String channel, final Object param) {
            if(YoumeRtcEnginePlugin.this.getActiveContext() != null)
                YoumeRtcEnginePlugin.this.getActiveContext().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (eventType) {
                        case YouMeConst.YouMeEvent.YOUME_EVENT_INIT_OK: {
                            YoumeRenderManager.getInstance().inited();
                            if(!initResults.isEmpty()){
                                initResults.get(0).success(getResultMap(eventType, code, channel, param.toString()));
                                initResults.remove(0);
                            }
                            break;
                        }
                        case YouMeConst.YouMeEvent.YOUME_EVENT_INIT_FAILED: {
                            isInChannel = false;
                            if(!initResults.isEmpty()){
                                initResults.get(0).error(String.valueOf(eventType), channel, null);
                                initResults.remove(0);
                            }
                            break;
                        }
                        case YouMeConst.YouMeEvent.YOUME_EVENT_JOIN_FAILED: {
                            if(!joinResults.isEmpty()){
                                joinResults.get(0).error(String.valueOf(eventType), channel, null);
                                joinResults.remove(0);
                            }
                            break;
                        }
                        case YouMeConst.YouMeEvent.YOUME_EVENT_JOIN_OK: {
                            if(!joinResults.isEmpty()){
                                joinResults.get(0).success(getResultMap(eventType, code, channel, param.toString()));
                                joinResults.remove(0);
                            }
                            break;
                        }
                        case YouMeConst.YouMeEvent.YOUME_EVENT_LEAVED_ALL: {
                            if(!leaveResults.isEmpty()){
                                leaveResults.get(0).success(getResultMap(eventType, code, channel, param.toString()));
                                leaveResults.remove(0);
                            }
                            break;
                        }
                        case YouMeConst.YouMeEvent.YOUME_EVENT_OTHERS_VIDEO_SHUT_DOWN: {
                            api.deleteRenderByUserID(param.toString());
                            sendEvent(YOUME_ON_EVENT, getResultMap(eventType, code, channel, param.toString()));
                            break;
                        }
                        case YouMeConst.YouMeEvent.YOUME_EVENT_OTHERS_VIDEO_ON: {
                            Log.d(TAG, "video _on: "+param.toString());
                            YoumeRenderManager.getInstance().updateResolution(param.toString(), code);
                            sendEvent(YOUME_ON_EVENT, getResultMap(eventType, code, channel, param.toString()));
                            break;
                        }
                        default: {
                            if(eventType == 223 && param.toString().length() >6){ //YOUME_EVENT_OTHERS_SHARE_INPUT_START
                                String userID =  param.toString().substring(0,param.toString().length() - 6);
                                YoumeRenderManager.getInstance().currentShareUserId = userID;
                                api.maskVideoByUserId(userID, false);
                            }else if(eventType == 224){ //YOUME_EVENT_OTHERS_SHARE_INPUT_STOP
                                if(YoumeRenderManager.getInstance().currentShareUserId != null && YoumeRenderManager.getInstance().currentShareUserId.equals(param.toString())) {
                                    // 先检查是否是当前在共享的用户发出的stop事件，避免意外停止接收共享
                                    YoumeRenderManager.getInstance().currentShareUserId = "";
                                }
                            }
                            sendEvent(YOUME_ON_EVENT, getResultMap(eventType, code, channel, param.toString()));
                        }
                    }
                }
            });
        }

        @Override
        public void onRequestRestAPI(final int requestID, final int code, final String query, final String result) {
            if(YoumeRtcEnginePlugin.this.getActiveContext() != null)
                YoumeRtcEnginePlugin.this.getActiveContext().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("code", 0);
                    map.put("query", query);
                    map.put("result", result);
                    sendEvent(YOUME_ON_RESTAPI, map);
                }
            });
        }

        @Override
        public void onMemberChange(final String channel, final MemberChange[] changeList, final boolean isUpdate) {
            if(YoumeRtcEnginePlugin.this.getActiveContext() != null)
                YoumeRtcEnginePlugin.this.getActiveContext().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("code", 0);
                    map.put("channel", channel);
                    map.put("isUpdate", isUpdate);
                    ArrayList<HashMap<String, Object>> userList = new ArrayList<HashMap<String, Object>>(15);
                    for (int i = 0; i < changeList.length; i++) {
                        HashMap<String, Object> user = new HashMap<>();
                        user.put("userid", changeList[i].userID);
                        user.put("isJoin", changeList[i].isJoin);
                        userList.add(user);
                    }
                    map.put("memberList", userList);
                    sendEvent(YOUME_ON_MEMBER_CHANGE, map);
                }
            });
        }

        @Override
        public void onBroadcast(int i, String s, String s1, String s2, String s3) {

        }

        /**
         *  功能描述: 音视频通话码率、丢包率回调，目前主要用于检测某个用户的网络状况
         *  @param avType   统计数据类型
         *  @param userID   对应的用户ID
         *  @param value    统计数据数值
         */
        @Override
        public void onAVStatistic(final int avType,  final String userID, final int value) {
            if(YoumeRtcEnginePlugin.this.getActiveContext() != null)
                YoumeRtcEnginePlugin.this.getActiveContext().runOnUiThread(new Runnable(){
                @Override
                public void run(){
                    HashMap<String, Object> map = new HashMap<>();
                    switch(avType){
                        case YouMeConst.YouMeAVStatisticType.YOUME_AVS_AUDIO_PACKET_UP_LOSS_HALF:   //音频上行的服务器丢包率,千分比
                        case YouMeConst.YouMeAVStatisticType.YOUME_AVS_VIDEO_PACKET_UP_LOSS_HALF:   //视频上行的服务器丢包率,千分比
                        case YouMeConst.YouMeAVStatisticType.YOUME_AVS_AUDIO_PACKET_DOWN_LOSS_RATE:   //音频下行丢包率,千分比
                        case YouMeConst.YouMeAVStatisticType.YOUME_AVS_VIDEO_PACKET_DOWN_LOSS_RATE:   //视频下行丢包率,千分比
                        case YouMeConst.YouMeAVStatisticType.YOUME_AVS_RECV_DATA_STAT:   //下行带宽,单位Bps
                        case YouMeConst.YouMeAVStatisticType.YOUME_AVS_VIDEO_BLOCK:   //视频卡顿
                        case YouMeConst.YouMeAVStatisticType.YOUME_AVS_AUDIO_DELAY_MS:
                        case YouMeConst.YouMeAVStatisticType.YOUME_AVS_VIDEO_DELAY_MS:
                            map.put("avType", avType);
                            map.put("userId", userID);
                            map.put("value", value);
                            sendEvent(YOUME_ON_STATISTIC_UPDATE, map);
                            break;
                        default:
                            break;
                    }
                }
            });
        }

        @Override
        public void onAVStatisticNew(int i, String s, int i1, String s1) {

        }

        @Override
        public void onTranslateTextComplete(int i, int i1, String s, int i2, int i3) {

        }
    };

    private void sendEvent(final String eventName, final HashMap<String, Object> map) {
        map.put("event", eventName);
        mEventHandler.post(new Runnable() {
            @Override
            public void run() {
                if (sink != null) {
                    sink.success(map);
                }
            }
        });
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        Context context = getActiveContext();
        switch (call.method) {
            case "init":
                init(call, result);
                break;
            case "joinChannel":
                joinChannel(call, result);
                break;
            case "leaveChannel":
                leaveChannel(call, result);
            case "setLocalVideoPreviewMirror":
                setLocalVideoPreviewMirror(call, result);
                break;
            case "setMicrophoneMute":
                setMicrophoneMute(call, result);
                break;
            case "setSpeakerMute":
                setSpeakerMute(call, result);
                break;
            case "setTCP":
                setTCP(call, result);
                break;
            case "setVideoNetAdjustmode":
                setVideoNetAdjustmode(call, result);
                break;
            case "keepScreenOn":
                keepScreenOn(call, result);
                break;
            case "cancelScreenOn":
                cancelScreenOn(call, result);
                break;
            case "startCapturer":
                startCapturer(call, result);
                break;
            case "stopCapturer":
                stopCapturer(call, result);
                break;
            case "outputToSpeaker":
                outputToSpeaker(call, result);
                break;
            case "setAutoSendStatus":
                setAutoSendStatus(call, result);
                break;
            case "setOtherMicMute":
                setOtherMicMute(call, result);
                break;
            case "maskVideoByUserId":
                maskVideoByUserId(call, result);
                break;
            case "switchCamera":
                switchCamera(call, result);
                break;
            case "screenRotationChange":
                screenRotationChange(call, result);
                break;
            case "applicationInBackground":
                applicationInBackground(call, result);
                break;
            case "applicationInFront":
                applicationInFront(call, result);
                break;
            case "kickOtherFromChannel":
                kickOtherFromChannel(call, result);
                break;
            case "setUsersVideoInfo":
                setUsersVideoInfo(call, result);
                break;
            case "setVideoCodeBitrate":
                setVideoCodeBitrate(call, result);
                break;
            case "setVideoCodeBitrateForSecond":
                setVideoCodeBitrateForSecond(call, result);
                break;
            case "setVideoFps":
                setVideoFps(call, result);
                break;
            case "setVideoFpsForSecond":
                setVideoFpsForSecond(call, result);
                break;
            case "setVideoPreviewFps":
                setVideoPreviewFps(call, result);
                break;
            case "setAVStatisticInterval":
                setAVStatisticInterval(call, result);
                break;
            case "openBeautify":
                openBeautify(call, result);
                break;
            case "setBeautyLevel":
                setBeautyLevel(call, result);
                break;
            case "startScreenRecorder":
                startScreenRecorder(call, result);
                break;
            case "stopScreenRecorder":
                stopScreenRecorder(call, result);
                break;
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }

    @Override
    public void onListen(Object arguments, EventChannel.EventSink events) {
        this.sink = events;
    }

    @Override
    public void onCancel(Object arguments) {
        this.sink = null;
    }

    private void init(MethodCall call, Result result) {
        try {
            YouMeManager.Init(getActiveContext());
            if (call.hasArgument("serverMode")) {
                int serverMode = call.argument("serverMode");
                NativeEngine.setServerMode(serverMode); // 0 正常，1 测试，7 私服务
                if (serverMode == 7 || serverMode == 4) {
                    String ip = call.argument("serverIP");
                    int port = call.argument("serverPort");
                    NativeEngine.setServerIpPort(ip, port);
                }
            }

            YoumeRenderManager.getInstance().init(getApplicationContext());
            api.SetCallback(mRtcEventHandler);
            String appKey = call.argument("appKey");
            String secretKey = call.argument("secretKey");
            int region = call.argument("region");

            String regionExt = "";
            if (call.hasArgument("regionExt")) {
                regionExt = call.argument("regionExt");
            }
            int code = api.init(appKey, secretKey, region, regionExt);
            if (code != 0) {
                result.error(code + "", "init error", code);
            } else {
                initResults.add(result);
            }
        } catch (Exception e) {
            result.error("1001", "init error", e);
        }
    }

    private void joinChannel(MethodCall call, Result result) {
        try {
            String userid = readArgument(call, "userid", "");
            String channel = readArgument(call, "channel", "");
            int role = readArgument(call, "role", 1);
            boolean autoRecv = readArgument(call, "autoRecvStream", true); //是否自动接收视频流

            api.setAVStatisticInterval(5000);
            api.setMicLevelCallback(10); // 开启讲话音量回调
            api.setFarendVoiceLevelCallback(10); // 开启远端语音音量回调
            api.setVadCallbackEnabled(true); //开启vad
            
            api.setAutoSendStatus(true);
            //设置视频通话相关参数
            if(call.hasArgument("useVideo") && (boolean) call.argument("useVideo")) {
                setVideoParam(call);
            }

            YoumeRenderManager.getInstance().updateJoinChannelInfo(userid, channel);
            //是否允许其他APP音频同时运行
            mixAudio = call.hasArgument("allowMixAudio") && (boolean) call.argument("allowMixAudio");
            if (!mixAudio) {//不允许
                AudioManager audioManager = (AudioManager) getActiveContext().getSystemService(getActiveContext().AUDIO_SERVICE);
                if (audioManager != null) {
                    audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                }
            }
            // 设置android 前台service 标题信息，视频通话中把app放入后台可以激活通知栏的显示
            RTCService.contentTitle = readArgument(call, "serviceTitle", "RTC Service is running");
            RTCService.contentText = readArgument(call, "serviceContent", "");

            api.setToken(readArgument(call, "token", ""));
            int code = api.joinChannelSingleMode(userid,channel,role, autoRecv);
            if (code != 0) {
                result.error(code + "", "call join fail", "");
            } else {
                isInChannel = true;
                joinResults.add(result);
            }
        } catch (Exception e) {
            result.error("1002", e.getMessage(), "");
        }
    }

    private void leaveChannel(MethodCall call, Result result){
        try {
            isInChannel = false;
            YoumeRenderManager.getInstance().leaveChannel();
            int code = api.leaveChannelAll();
            YoumeRenderManager.getInstance().currentShareUserId = "";
            removeOrientationListener();

            if (code != 0) {
                result.error(code + "", "call leave fail", "");
            } else {
                leaveResults.add(result);
            }
        } catch (Exception e) {
            result.error( "1003", "call leave fail", "");
        }
    }

    private void setVideoParam(MethodCall call){
        ScreenRecorder.init(getActiveContext());
        api.setVideoFrameCallback(VideoRendererManager.getInstance());
        api.SetVideoCallback(); // 激活视频数据接收的回调
        api.setVideoFps(readArgument(call, "fps", 15));
        api.setVideoLocalResolution(readArgument(call, "previewWidth", 480), readArgument(call, "previewHeight", 640));
        api.setVideoNetResolution(readArgument(call, "sendWidth", 480), readArgument(call, "sendHeight", 640));
        api.setVideoPreviewFps(readArgument(call, "previewFps", 30));
        shareWidth = readArgument(call, "shareWidth", 720);
        shareHeight = readArgument(call, "shareHeight", 1280);
        shareFps = readArgument(call, "shareFps", 15);
        ScreenRecorder.setResolution(shareWidth, shareHeight);
        api.setVideoNetResolutionForShare(shareWidth, shareHeight);
        ScreenRecorder.setFps(shareFps);
        addOrientationListener();

        if (call.hasArgument("secondStreamWidth") && readArgument(call, "secondStreamWidth", 0) > 0) {
            api.setVideoNetResolutionForSecond(readArgument(call, "secondStreamWidth", 0), readArgument(call, "secondStreamHeight", 0));
            api.setVideoCodeBitrateForSecond(readArgument(call, "secondStreamBitRateMax", 0), readArgument(call, "secondStreamBitRateMin", 0));
            api.setVideoFpsForSecond(readArgument(call, "secondStreamFPS", 15));
        }

        if (call.hasArgument("bitRateMin")) {
            api.setVideoCodeBitrate(readArgument(call, "bitRateMax", 0), readArgument(call, "bitRateMin", 0));
        }
        api.setVBR(readArgument(call, "VBR", false));
        api.setVBRForSecond(readArgument(call, "secondStreamVBR", false));
    }

    /**
     * 设置镜像模式
     * isMirror  true ,表示镜像模式，false 表示非镜像模式
     */
    private void setLocalVideoPreviewMirror(MethodCall call, Result result) {
        boolean isMirror = call.argument("isMirror");
        api.setlocalVideoPreviewMirror(isMirror);
        result.success(null);
    }

    /**
     * 设置mic是否静音
     * mute  true ,表示静音，false 表示非静音
     */
    private void setMicrophoneMute(MethodCall call, Result result) {
        boolean isMute = call.argument("mute");
        api.setMicrophoneMute(isMute);
        result.success(null);
    }

    /**
     * 设置扬声器是否静音
     * mute  true ,表示静音，false 表示非静音
     */
    private void setSpeakerMute(MethodCall call, Result result) {
        boolean mute = call.argument("mute");
        api.setSpeakerMute(mute);
        result.success(null);
    }

    /**
     * useTcp  true ,表示使用tcp传输媒体数据，false 表示使用udp，默认为false
     */
    private void setTCP(MethodCall call, Result result) {
        boolean useTcp = call.argument("useTcp");
        api.setTCPMode(useTcp);
        result.success(null);
    }

    /**
     * 设置网络适配模式，1为手动适配模式，0是自动适配模式，默认为0， 进频道前设置
     * adjustmode
     */
    private void setVideoNetAdjustmode(MethodCall call, Result result) {
        int adjustmode = call.argument("adjustmode");
        api.setVideoNetAdjustmode(adjustmode);
        result.success(null);
    }

    /**
     * 开启屏幕常亮
     */
    private void keepScreenOn(MethodCall call, Result result) {
        final Activity currentActivity = getActiveContext();
        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                currentActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        });
        result.success(null);
    }

    /**
     * 取消屏幕常亮
     */
    private void cancelScreenOn(MethodCall call, Result result) {
        final Activity currentActivity = getActiveContext();
        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(currentActivity != null) currentActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        });
        result.success(null);
    }

    /**
     * 开启摄像头
     */
    private void startCapturer(MethodCall call, Result result) {
        // 横竖屏分辨率交换自动设置
        // 如果设置为true，那么在屏幕发生横竖屏变化时，再通知给screenRotationChange，
        // 就可以实现内置横竖屏自动切换width 和 height分辨率
        boolean switchWithHeightIfLandscape = call.argument("switchWithHeightIfLandscape");
        if (switchWithHeightIfLandscape) {
            api.screenRotationChange();
        }
        api.startCapturer();
        result.success(null);
    }

    /**
     * 关闭摄像头
     */
    private void stopCapturer(MethodCall call, Result result) {
        api.stopCapturer();
        result.success(null);
    }

    /**
     * 设置是否从听筒输出，默认是没有耳机从扬声器输出，插入耳机从耳机输出。如需强制听筒输出，需要传入false
     */
    private void outputToSpeaker(MethodCall call, Result result) {
        boolean outputToSpeaker = call.argument("outputToSpeaker");
        api.setOutputToSpeaker(outputToSpeaker);
        result.success(null);
    }

    /**
     * 设置是否同步自己的设备开关状态
     */
    private void setAutoSendStatus(MethodCall call, Result result) {
        boolean sync = call.argument("sync");
        api.setAutoSendStatus(sync);
        result.success(null);
    }

    /**
     * 控制其它人的麦克风开关
     */
    private void setOtherMicMute(MethodCall call, Result result) {
        String userid = call.argument("userid");
        boolean mute = call.argument("mute");
        api.setOtherMicMute(userid, mute);
        result.success(null);
    }

    /**
     * 设置是否屏蔽指定userid的视频
     */
    private void maskVideoByUserId(MethodCall call, Result result) {
        String userid = call.argument("userid");
        boolean block = call.argument("block");
        api.maskVideoByUserId(userid, block);
        result.success(null);
    }

    /**
     * 切换前后摄像头
     */
    private void switchCamera(MethodCall call, Result result) {
        api.switchCamera();
        result.success(null);
    }

    /**
     * 设置屏幕旋转角度，只支持 0，1，2，3，分别对应 0° ，90° ，180°， 270°
     */
    private void screenRotationChange(MethodCall call, Result result) {
        api.screenRotationChange();
        result.success(null);
    }

    /**
     * app放到后台
     */
    private void applicationInBackground(MethodCall call, Result result) {
        if (isInChannel)
        {
            try{
                RTCService.mContext = getApplicationContext();
                RTCService.mActivity = getActiveContext();
                if(RTCService.mContext != null && RTCService.mActivity != null) {
                    forgroundIntent = new Intent(getActiveContext(), RTCService.class);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        RTCService.mActivity.startForegroundService(forgroundIntent);
                    } else {
                        RTCService.mActivity.startService(forgroundIntent);
                    }
                }
            }catch (Throwable e){
                e.printStackTrace();
            }
        }
        VideoRendererManager.getInstance().pauseRender();
    }

    /**
     * 切换前后摄像头
     */
    private void applicationInFront(MethodCall call, Result result) {
        VideoRendererManager.getInstance().resumeRender();
        if(forgroundIntent != null && getActiveContext() != null){
            try {
                getActiveContext().stopService(forgroundIntent);
            }catch (Throwable e){
                e.printStackTrace();
            }
        }
    }

    /**
     * 踢出指定 userid
     */
    private void kickOtherFromChannel(MethodCall call, Result result) {
        String userid = call.argument("userid");
        String channel = call.argument("channel");
        int forbidSecond = call.argument("forbidSecond");
        api.kickOtherFromChannel(userid, channel, forbidSecond);
        result.success(null);
    }

    /**
     * 指定订阅列表
     */
    private void setUsersVideoInfo(MethodCall call, Result result) {
//        ReadableArray usersStreamInfo = call.argument("userStreamInfo");
//        ArrayList<String> userIds =  new ArrayList<>();
//        ArrayList<Integer> streamIds = new ArrayList<>();
//        for (int i=0; i< usersStreamInfo.size(); i++) {
//            ReadableMap userInfo = usersStreamInfo.getMap(i);
//            userIds.add(userInfo.getString("userID"));
//            streamIds.add(userInfo.getInt("streamID"));
//        }
//        if (userIds.size()>0 && streamIds.size() >0) {
//            int[] ids = new int[streamIds.size()];
//            for (int i = 0; i < ids.length; i++) {
//                ids[i] = (int) streamIds.get(i);
//            }
//            api.setUsersVideoInfo(userIds.toArray(new String[0]), ids);
//        } else {
//            Log.e(TAG, "setUsersVideoInfo: error" );
//        }
    }

    /**
     * 设置大流码率
     */
    private void setVideoCodeBitrate(MethodCall call, Result result) {
        int minBitRateKbps = call.argument("minBitRateKbps");
        int maxBitRateKbps = call.argument("maxBitRateKbps");
        api.setVideoCodeBitrate(minBitRateKbps, maxBitRateKbps);
        result.success(null);
    }

    /**
     * 设置小流码率
     */
    private void setVideoCodeBitrateForSecond(MethodCall call, Result result) {
        int minBitRateKbps = call.argument("minBitRateKbps");
        int maxBitRateKbps = call.argument("maxBitRateKbps");
        api.setVideoCodeBitrateForSecond(minBitRateKbps, maxBitRateKbps);
        result.success(null);
    }

    /**
     * 设置大流fps
     */
    private void setVideoFps(MethodCall call, Result result) {
        int fps = call.argument("fps");
        api.setVideoFps(fps);
        result.success(null);
    }

    /**
     * 设置小流fps
     */
    private void setVideoFpsForSecond(MethodCall call, Result result) {
        int fps = call.argument("fps");
        api.setVideoFpsForSecond(fps);
        result.success(null);
    }

    /**
     * 设置预览fps
     */
    private void setVideoPreviewFps(MethodCall call, Result result) {
        int fps = call.argument("fps");
        api.setVideoPreviewFps(fps);
        result.success(null);
    }

    /**
     * 设置预览fps
     */
    private void setAVStatisticInterval(MethodCall call, Result result) {
        int interval = call.argument("interval");
        api.setAVStatisticInterval(interval);
        result.success(null);
    }

    /**
     * 设置是否开启美颜
     */
    private void openBeautify(MethodCall call, Result result) {
        boolean isOpen = call.argument("isOpen");
        api.openBeautify(isOpen);
        result.success(null);
    }


    /**
     * 设置美颜等级，有效返回 0.0  - 1.0
     */
    private void setBeautyLevel(MethodCall call, Result result) {
        Float level = call.argument("level");
        api.setBeautyLevel(level);
        result.success(null);
    }

    /**
     * 开始屏幕录制
     */
    public void startScreenRecorder(MethodCall call, Result result) {
        // TODO: 需要异步消息来知道调用结果
        ScreenRecorder.startScreenRecorder();
        result.success(null);
    }

    /**
     * 停止屏幕录制
     */
    private void stopScreenRecorder(MethodCall call, Result result) {
        ScreenRecorder.stopScreenRecorder();
        api.stopInputVideoFrameForShare();
        result.success(null);
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        mActivity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
    }

    @Override
    public void onDetachedFromActivity() {
        mActivity = null;
    }

    private void addOrientationListener() {
        removeOrientationListener();
        mOrientationReciver = new OrientationReciver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.CONFIGURATION_CHANGED");
        getActiveContext().registerReceiver(mOrientationReciver, intentFilter);
    }

    private void removeOrientationListener() {
        if (mOrientationReciver != null) {
            getActiveContext().unregisterReceiver(mOrientationReciver);
            mOrientationReciver = null;
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {

    }

    private class OrientationReciver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int rotation = getActiveContext().getWindowManager().getDefaultDisplay().getRotation() * 90;
            if (rotation == 90 || rotation == 270) {
                ScreenRecorder.orientationChange(rotation, shareHeight, shareWidth);
                api.setVideoNetResolutionForShare(shareHeight, shareWidth);
            } else {
                ScreenRecorder.orientationChange(rotation, shareWidth, shareHeight);
                api.setVideoNetResolutionForShare(shareWidth, shareHeight);
            }
        }
    }
}
