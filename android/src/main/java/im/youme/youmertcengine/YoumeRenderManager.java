package im.youme.youmertcengine;

import android.content.Context;

import com.youme.voiceengine.video.SurfaceViewRenderer;

import java.util.concurrent.ConcurrentHashMap;


public class YoumeRenderManager {
    public static YoumeRenderManager sYoumeManager;

    private Context context;

    public String getLocalUserId() {
        return localUserId;
    }

    private String localUserId = null;
    private boolean inited;
    private ConcurrentHashMap<String, Integer> userResolutions;
    public String currentShareUserId = "";

    public static YoumeRenderManager getInstance() {
        if (sYoumeManager == null) {
            synchronized (YoumeRenderManager.class) {
                if (sYoumeManager == null) {
                    sYoumeManager = new YoumeRenderManager();
                    sYoumeManager.userResolutions = new ConcurrentHashMap<>();
                }
            }
        }
        return sYoumeManager;
    }


    public int init(Context context) {
        this.context = context;
        return 0;
    }

    public void inited() {
        this.inited = true;
    }

    public void updateJoinChannelInfo(String userid, String channel) {
        this.localUserId = userid;
        VideoRendererManager.getInstance().init();
        VideoRendererManager.getInstance().setLocalUserId(userid);
    }

    public void leaveChannel() {
        VideoRendererManager.getInstance().deleteAllRender();
    }

    public VideoRendererManager.RenderInfo addRenderInfo(final String userid) {
        SurfaceViewRenderer sView = new SurfaceViewRenderer(context);
        return VideoRendererManager.getInstance().addRender(userid, sView);
    }

    public void deleteRenderInfo(String userid, VideoRendererManager.RenderInfo renderInfo) {
        VideoRendererManager.getInstance().deleteRender(userid, renderInfo);
        if (renderInfo != null && renderInfo.view != null) {
            renderInfo.view.release();
        }
    }

    public void updateResolution(String userid, int resolution) {
        userResolutions.put(userid, resolution);
    }

    public VideoResolution getResolution(String userid) {
        VideoResolution res = new VideoResolution();
        if (userResolutions.containsKey(userid)) {
            int intRes = userResolutions.get(userid).intValue();
            res.width = intRes << 16;
            res.height = intRes & 0xffff;
        }
        return res;
    }
}
