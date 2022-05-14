package im.youme.youmertcengine;

import android.content.Context;
import android.view.SurfaceView;

import io.flutter.plugin.common.MessageCodec;
import io.flutter.plugin.platform.PlatformView;
import io.flutter.plugin.platform.PlatformViewFactory;
import com.youme.voiceengine.mgr.YouMeManager;


public class YoumeRenderViewFactory extends PlatformViewFactory {
    private final YoumeRtcEnginePlugin mEnginePlugin;

    public YoumeRenderViewFactory(MessageCodec<Object> createArgsCodec, YoumeRtcEnginePlugin enginePlugin) {
        super(createArgsCodec);
        this.mEnginePlugin = enginePlugin;
    }

    @Override
    public PlatformView create(Context context, int viewId, Object args) {
        //TODO: 这里还需要实现
        return null;
    }
}