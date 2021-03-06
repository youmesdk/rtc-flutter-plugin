#include "include/youme_rtc_engine/youme_rtc_engine_plugin.h"
#include "IYouMeVoiceEngine.h"
#include "YouMeConstDefine.h"

// This must be included before many other Windows headers.
#include <windows.h>

// For getPlatformVersion; remove unless needed for your plugin implementation.
#include <VersionHelpers.h>

#include <flutter/method_channel.h>
#include <flutter/plugin_registrar_windows.h>
#include <flutter/standard_method_codec.h>

#include <map>
#include <memory>
#include <sstream>

using flutter::EncodableMap;
using flutter::EncodableValue;

namespace
{

    class YoumeRtcEnginePlugin : public flutter::Plugin, public IYouMeEventCallback, public IYouMeMemberChangeCallback, IYouMeAVStatisticCallback
    {
    public:
        static void RegisterWithRegistrar(flutter::PluginRegistrarWindows* registrar);

        YoumeRtcEnginePlugin();
        void onEvent(const YouMeEvent event, const YouMeErrorCode error, const char* channel, const char* param) override;
        void onMemberChange(const char* channel, const char* listMemberChangeJson, bool bUpdate) override;
        void onAVStatistic(YouMeAVStatisticType type, const char* userID, int value) override;

        virtual ~YoumeRtcEnginePlugin();

    private:
        std::vector<std::unique_ptr<flutter::MethodResult<flutter::EncodableValue>>> initPromises;
        std::vector<std::unique_ptr<flutter::MethodResult<flutter::EncodableValue>>> joinPromises;
        std::vector<std::unique_ptr<flutter::MethodResult<flutter::EncodableValue>>> leavePromises;

    private:
        // Called when a method is called on this plugin's channel from Dart.
        void HandleMethodCall(
            const flutter::MethodCall<flutter::EncodableValue>& method_call,
            std::unique_ptr<flutter::MethodResult<flutter::EncodableValue>> result);
        void init(const flutter::MethodCall<flutter::EncodableValue>& method_call,
            std::unique_ptr<flutter::MethodResult<flutter::EncodableValue>>& result);
        void joinChannel(const flutter::MethodCall<flutter::EncodableValue>& method_call,
            std::unique_ptr<flutter::MethodResult<flutter::EncodableValue>>& result);
        void leaveChannel(const flutter::MethodCall<flutter::EncodableValue>& method_call,
            std::unique_ptr<flutter::MethodResult<flutter::EncodableValue>>& result);
    };

    // static
    void YoumeRtcEnginePlugin::RegisterWithRegistrar(
        flutter::PluginRegistrarWindows* registrar)
    {
        auto channel =
            std::make_unique<flutter::MethodChannel<flutter::EncodableValue>>(
                registrar->messenger(), "youme_rtc_engine",
                &flutter::StandardMethodCodec::GetInstance());

        auto plugin = std::make_unique<YoumeRtcEnginePlugin>();

        channel->SetMethodCallHandler(
            [plugin_pointer = plugin.get()](const auto& call, auto result)
        {
            plugin_pointer->HandleMethodCall(call, std::move(result));
        });

        registrar->AddPlugin(std::move(plugin));
    }

    YoumeRtcEnginePlugin::YoumeRtcEnginePlugin() {}

    YoumeRtcEnginePlugin::~YoumeRtcEnginePlugin() {}

    void YoumeRtcEnginePlugin::HandleMethodCall(
        const flutter::MethodCall<flutter::EncodableValue>& method_call,
        std::unique_ptr<flutter::MethodResult<flutter::EncodableValue>> result)
    {
        const auto* params = std::get_if<EncodableMap>(method_call.arguments());
        if (method_call.method_name().compare("getPlatformVersion") == 0)
        {
            std::ostringstream version_stream;
            version_stream << "Windows ";
            if (IsWindows10OrGreater())
            {
                version_stream << "10+";
            }
            else if (IsWindows8OrGreater())
            {
                version_stream << "8";
            }
            else if (IsWindows7OrGreater())
            {
                version_stream << "7";
            }
            result->Success(flutter::EncodableValue(version_stream.str()));
        }else if (method_call.method_name().compare("init") == 0)
        {
            init(method_call, result);
        }else if (method_call.method_name().compare("joinChannel") == 0)
        {
            joinChannel(method_call, result);
        }else if (method_call.method_name().compare("leaveChannel") == 0)
        {
            leaveChannel(method_call, result);
        }else if (method_call.method_name().compare("setMicrophoneMute") == 0)
        {
            if(params){
                auto p_it = params->find(EncodableValue("mute"));
                if (p_it != params->end())
                {
                    bool mute = std::get<bool>(p_it->second);
                    IYouMeVoiceEngine::getInstance()->setMicrophoneMute(mute);
                    result->Success("");
                }
            }
        }else if (method_call.method_name().compare("setSpeakerMute") == 0)
        {
            if(params){
                auto p_it = params->find(EncodableValue("mute"));
                if (p_it != params->end())
                {
                    bool mute = std::get<bool>(p_it->second);
                    IYouMeVoiceEngine::getInstance()->setSpeakerMute(mute);
                    result->Success("");
                }
            }
        }else if (method_call.method_name().compare("outputToSpeaker") == 0)
        {
            if(params){
                auto p_it = params->find(EncodableValue("outputToSpeaker"));
                if (p_it != params->end())
                {
                    bool outputToSpeaker = std::get<bool>(p_it->second);
                    IYouMeVoiceEngine::getInstance()->setOutputToSpeaker(outputToSpeaker);
                    result->Success("");
                }
            }
        }else if (method_call.method_name().compare("setOtherMicMute") == 0)
        {
            if(params){
                bool mute = true;
                std::string userid;
                auto p_it = params->find(EncodableValue("mute"));
                if (p_it != params->end())
                {
                    mute = std::get<bool>(p_it->second);
                }
                auto u_it = params->find(EncodableValue("userid"));
                if (u_it != params->end())
                {
                    userid = std::get<std::string>(u_it->second);
                }

                IYouMeVoiceEngine::getInstance()->setOtherMicMute(userid.c_str(), mute);
                result->Success("");
            }
        }else if (method_call.method_name().compare("kickOtherFromChannel") == 0)
        {
            if(params){
                int forbidSecond = 15;
                std::string userid;
                std::string channel;
                auto p_it = params->find(EncodableValue("forbidSecond"));
                if (p_it != params->end())
                {
                    forbidSecond = std::get<int>(p_it->second);
                }
                auto u_it = params->find(EncodableValue("userid"));
                if (u_it != params->end())
                {
                    userid = std::get<std::string>(u_it->second);
                }
                auto s_it = params->find(EncodableValue("channel"));
                if (s_it != params->end())
                {
                    channel = std::get<std::string>(s_it->second);
                }

                IYouMeVoiceEngine::getInstance()->kickOtherFromChannel(userid.c_str(), channel.c_str(), forbidSecond);
                result->Success("");
            }
        }
        else
        {
            result->NotImplemented();
        }
    }

    void YoumeRtcEnginePlugin::init(const flutter::MethodCall<flutter::EncodableValue>& method_call,
        std::unique_ptr<flutter::MethodResult<flutter::EncodableValue>>& result)
    {
        const auto* params = std::get_if<EncodableMap>(method_call.arguments());
        if (params != nullptr)
        {
            std::string appKey;
            std::string secretKey;
            int region = 0;
            std::string regionExt;
            auto appkey_it = params->find(EncodableValue("appKey"));
            if (appkey_it != params->end())
            {
                appKey = std::get<std::string>(appkey_it->second);
            }
            auto secretKey_it = params->find(EncodableValue("secretKey"));
            if (secretKey_it != params->end())
            {
                secretKey = std::get<std::string>(secretKey_it->second);
            }
            auto regionExt_it = params->find(EncodableValue("regionExt"));
            if (regionExt_it != params->end())
            {
                regionExt = std::get<std::string>(regionExt_it->second);
            }
            auto region_it = params->find(EncodableValue("region"));
            if (region_it != params->end())
            {
                region = std::get<int>(region_it->second);
            }
            YouMeErrorCode_t code = IYouMeVoiceEngine::getInstance()->init(this, appKey.c_str(), secretKey.c_str(), (YOUME_RTC_SERVER_REGION)region, regionExt.c_str());
            if (code != 0)
            {
                result->Error(std::to_string(code), "param error");
            }
            else
            {
                initPromises.push_back(std::move(result));
            }
        }
    }
    void YoumeRtcEnginePlugin::joinChannel(const flutter::MethodCall<flutter::EncodableValue>& method_call,
        std::unique_ptr<flutter::MethodResult<flutter::EncodableValue>>& result)
    {
        const auto* params = std::get_if<EncodableMap>(method_call.arguments());
        if (params != nullptr)
        {
            std::string channel;
            std::string userid;
            int userRole = 0;
            std::string token;
            bool autoRecvStream = true;
            // ????????????????????????????????????????????????
            IYouMeVoiceEngine::getInstance()->setAVStatisticInterval(5000);
            // ?????????????????????????????????????????????????????????
            IYouMeVoiceEngine::getInstance()->setVideoNoFrameTimeout(5000);
            // ?????????????????????????????????????????????
            IYouMeVoiceEngine::getInstance()->setAutoSendStatus(true);
            //????????????????????????
            IYouMeVoiceEngine::getInstance()->setMicLevelCallback(10);
            //??????????????????????????????
            IYouMeVoiceEngine::getInstance()->setFarendVoiceLevelCallback(10);
            IYouMeVoiceEngine::getInstance()->setVadCallbackEnabled(true);

            auto channel_it = params->find(EncodableValue("channel"));
            if (channel_it != params->end())
            {
                channel = std::get<std::string>(channel_it->second);
            }
            auto userid_it = params->find(EncodableValue("userid"));
            if (userid_it != params->end())
            {
                userid = std::get<std::string>(userid_it->second);
            }
            auto token_it = params->find(EncodableValue("token"));
            if (token_it != params->end())
            {
                token = std::get<std::string>(token_it->second);
            }
            auto userRole_it = params->find(EncodableValue("role"));
            if (userRole_it != params->end())
            {
                userRole = std::get<int>(userRole_it->second);
            }
            YouMeErrorCode_t code = IYouMeVoiceEngine::getInstance()->joinChannelSingleMode(userid.c_str(),channel.c_str(),(YouMeUserRole_t)userRole, autoRecvStream);
            if (code != 0)
            {
                result->Error(std::to_string(code), "param error");
            }
            else
            {
                joinPromises.push_back(std::move(result));
            }
        }
    }

    void YoumeRtcEnginePlugin::leaveChannel(const flutter::MethodCall<flutter::EncodableValue>& method_call,
        std::unique_ptr<flutter::MethodResult<flutter::EncodableValue>>& result)
    {
        YouMeErrorCode_t code = IYouMeVoiceEngine::getInstance()->leaveChannelAll();
        if (code != 0)
        {
            result->Error(std::to_string(code), "param error");
        }
        else
        {
            leavePromises.push_back(std::move(result));
        }
    }

    void YoumeRtcEnginePlugin::onEvent(const YouMeEvent event, const YouMeErrorCode error, const char* channel, const char* param)
    {
        std::string strChannel = channel;
        std::string strParam = param;
        switch (event)
        {
            // case?????????????????????????????????????????????????????????????????????YouMeEvent
        case YOUME_EVENT_INIT_OK:
            //"???????????????";
            if (!initPromises.empty()) {
                const auto promise = initPromises.erase(initPromises.begin());
                auto r = flutter::EncodableMap::map();
                r[EncodableValue("eventType")] = event;
                r[EncodableValue("code")] = (int)error;
                r[EncodableValue("channel")] = strChannel;
                r[EncodableValue("param")] = strParam;
                promise->get()->Success(r);
            }
            break;
        case YOUME_EVENT_INIT_FAILED:
            // "??????????????????????????????" + errorCode;
            if (!initPromises.empty()) {
                const auto promise = initPromises.erase(initPromises.begin());
                promise->get()->Error(std::to_string((int)error), "init fail");
            }
            break;
        case YOUME_EVENT_JOIN_OK:
            //"??????????????????";
            if (!joinPromises.empty()) {
                const auto promise = joinPromises.erase(joinPromises.begin());
                auto r = flutter::EncodableMap::map();
                r[EncodableValue("eventType")] = event;
                r[EncodableValue("code")] = (int)error;
                r[EncodableValue("channel")] = strChannel;
                r[EncodableValue("param")] = strParam;
                promise->get()->Success(r);
            }
            break;
        case YOUME_EVENT_LEAVED_ALL:
            // "??????????????????";
            if (!joinPromises.empty()) {
                const auto promise = joinPromises.erase(joinPromises.begin());
                auto r = flutter::EncodableMap::map();
                r[EncodableValue("eventType")] = event;
                r[EncodableValue("code")] = (int)error;
                r[EncodableValue("channel")] = strChannel;
                r[EncodableValue("param")] = strParam;
                promise->get()->Success(r);
            }
            break;
        case YOUME_EVENT_JOIN_FAILED:
            //????????????????????????
            if (!joinPromises.empty()) {
                const auto promise = joinPromises.erase(joinPromises.begin());
                promise->get()->Error(std::to_string((int)error), strChannel);
            }
            break;
        case YOUME_EVENT_REC_PERMISSION_STATUS:
            //"????????????????????????????????????????????????????????????YOUME_SUCCESS??????????????????YOUME_ERROR_REC_NO_PERMISSION????????????????????????mute???????????????????????????????????????";
            break;
        case YOUME_EVENT_RECONNECTING:
            //"????????????????????????";
            break;
        case YOUME_EVENT_RECONNECTED:
            // "??????????????????";
            break;
        case YOUME_EVENT_OTHERS_MIC_OFF:
            //?????????????????????????????????
            break;
        case YOUME_EVENT_OTHERS_MIC_ON:
            //?????????????????????????????????
            break;
        case YOUME_EVENT_OTHERS_SPEAKER_ON:
            //?????????????????????????????????
            break;
        case YOUME_EVENT_OTHERS_SPEAKER_OFF:
            //??????????????????????????????
            break;
        case YOUME_EVENT_OTHERS_VOICE_ON:
            //????????????????????????
            break;
        case YOUME_EVENT_OTHERS_VOICE_OFF:
            //????????????????????????
            break;
        case YOUME_EVENT_MY_MIC_LEVEL:
            //???????????????????????????????????????error????????????????????????
            break;
        case YOUME_EVENT_MIC_CTR_ON:
            //??????????????????????????????
            break;
        case YOUME_EVENT_MIC_CTR_OFF:
            //??????????????????????????????
            break;
        case YOUME_EVENT_SPEAKER_CTR_ON:
            //??????????????????????????????
            break;
        case YOUME_EVENT_SPEAKER_CTR_OFF:
            //??????????????????????????????
            break;
        case YOUME_EVENT_LISTEN_OTHER_ON:
            //????????????????????????
            break;
        case YOUME_EVENT_LISTEN_OTHER_OFF:
            //??????????????????
            break;
        default:
            //"????????????" + eventType + ",?????????" +
            break;
        }
    }
    void YoumeRtcEnginePlugin::onMemberChange(const char* channel, const char* listMemberChangeJson, bool bUpdate)
    {
    }
    void YoumeRtcEnginePlugin::onAVStatistic(YouMeAVStatisticType type, const char* userID, int value)
    {
    }

} // namespace

void YoumeRtcEnginePluginRegisterWithRegistrar(
    FlutterDesktopPluginRegistrarRef registrar)
{
    YoumeRtcEnginePlugin::RegisterWithRegistrar(
        flutter::PluginRegistrarManager::GetInstance()
        ->GetRegistrar<flutter::PluginRegistrarWindows>(registrar));
}
