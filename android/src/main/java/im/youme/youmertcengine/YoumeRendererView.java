package im.youme.youmertcengine;

import android.view.SurfaceView;
import android.view.View;

import io.flutter.plugin.platform.PlatformView;

public class YoumeRendererView implements PlatformView {
    private final SurfaceView mSurfaceView;
    private final long uid;

    YoumeRendererView(SurfaceView surfaceView, int uid) {
        this.mSurfaceView = surfaceView;
        this.uid = uid;
    }

    @Override
    public View getView() {
        return mSurfaceView;
    }

    @Override
    public void dispose() { }
}