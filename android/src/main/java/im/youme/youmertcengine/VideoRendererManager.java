package im.youme.youmertcengine;

import android.util.Log;

import com.youme.voiceengine.VideoMgr.VideoFrameCallback;
import com.youme.voiceengine.YouMeConst.YOUME_VIDEO_FMT;
import com.youme.voiceengine.video.SurfaceViewRenderer;
import com.youme.voiceengine.video.VideoBaseRenderer.I420Frame;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 摄像头渲染
 *
 * @author fire
 */
public class VideoRendererManager implements VideoFrameCallback {
    public class RenderInfo {
        public int rotation;
        public SurfaceViewRenderer view;

        public RenderInfo(int rotation, SurfaceViewRenderer view) {
            this.rotation = rotation;
            this.view = view;
        }
    }

    private final static String TAG = "VideoRendererManager";
    private Map<String, Vector> renderers = new ConcurrentHashMap<String, Vector>();
    private static final VideoRendererManager instance = new VideoRendererManager();
    private String localUserId = "";
    private boolean bPauseRender = false;
    private final Object layoutLock = new Object();

    private VideoRendererManager() {
        // 私有构造
    }

    public static VideoRendererManager getInstance() {
        return instance;
    }

    public void init() {
        renderers.clear();
    }

    public void setLocalUserId(String userId) {
        localUserId = userId;
    }

    public void pauseRender() {
        bPauseRender = true;
    }

    public void resumeRender() {
        bPauseRender = false;
    }

    /**
     * 添加渲染源
     *
     * @param view
     * @return
     */
    public RenderInfo addRender(String userId, SurfaceViewRenderer view) {
        //int renderId = api.createRender(userId);
        RenderInfo info = new RenderInfo(0, view);
        synchronized (this.layoutLock) {
            Vector<RenderInfo> renderInfos = renderers.get(userId);
            if (renderInfos != null) {
                renderInfos.add(info);
            } else {
                Vector<RenderInfo> renderInfos1 = new Vector<RenderInfo>();
                renderInfos1.add(info);
                renderers.put(userId, renderInfos1);
            }
            Log.d(TAG, "addRender userId:" + userId);
        }
        return info;
    }

    public Vector<SurfaceViewRenderer> getRender(String userId) {
        synchronized (this.layoutLock) {
            Vector<RenderInfo> renderInfos = renderers.get(userId);
            Vector<SurfaceViewRenderer> surfaceViewRenderers = new Vector<SurfaceViewRenderer>();
            if (renderInfos != null) {
                for (int i = 0; i < renderInfos.size(); i++) {
                    surfaceViewRenderers.add(renderInfos.get(i).view);
                }
                return surfaceViewRenderers;
            } else {
                return null;
            }
        }
    }


    public int deleteRender(String userId, RenderInfo renderInfo) {
        //int ret = api.deleteRender(renderId);
        synchronized (this.layoutLock) {
            if (renderInfo == null) {
                renderers.remove(userId);
            } else {
                Vector<RenderInfo> renderInfos = renderers.get(userId);
                if (renderInfos != null) {
                    renderInfos.remove(renderInfo);
                }
            }
        }
        return 0;
    }

    public void deleteAllRender() {
        synchronized (this.layoutLock) {
            renderers.clear();
        }
    }

    public void onVideoFrameCallback(String userId, byte[] data, int len, int width, int height, int fmt, long timestamp) {
        if (!bPauseRender) {
            Vector<RenderInfo> renderInfos = renderers.get(userId);
            if (renderInfos != null) {
                int[] yuvStrides = {width, width / 2, width / 2};

                int yLen = width * height;
                int uLen = width * height / 4;
                int vLen = width * height / 4;
                byte[] yPlane = new byte[yLen];
                byte[] uPlane = new byte[uLen];
                byte[] vPlane = new byte[vLen];

                System.arraycopy(data, 0, yPlane, 0, yLen);
                System.arraycopy(data, yLen, uPlane, 0, uLen);
                System.arraycopy(data, (yLen + uLen), vPlane, 0, vLen);

                ByteBuffer[] yuvPlanes = {ByteBuffer.wrap(yPlane), ByteBuffer.wrap(uPlane), ByteBuffer.wrap(vPlane)};

                //rotationDegree = 270; // for android
                synchronized (this.layoutLock) {
                    for (int i = 0; i < renderInfos.size(); i++) {
                        I420Frame frame = new I420Frame(width, height, renderInfos.get(i).rotation, yuvStrides, yuvPlanes);
                        renderInfos.get(i).view.renderFrame(frame);
                    }
                }
            }
        }
    }

    public void onVideoFrameMixed(byte[] data, int len, int width, int height, int fmt, long timestamp) {
        if (!bPauseRender) {
            String userId = localUserId;
            Vector<RenderInfo> renderInfos = renderers.get(userId);
            if (renderInfos != null) {
                int[] yuvStrides = {width, width / 2, width / 2};

                int yLen = width * height;
                int uLen = width * height / 4;
                int vLen = width * height / 4;
                byte[] yPlane = new byte[yLen];
                byte[] uPlane = new byte[uLen];
                byte[] vPlane = new byte[vLen];

                System.arraycopy(data, 0, yPlane, 0, yLen);
                System.arraycopy(data, yLen, uPlane, 0, uLen);
                System.arraycopy(data, (yLen + uLen), vPlane, 0, vLen);

                ByteBuffer[] yuvPlanes = {ByteBuffer.wrap(yPlane), ByteBuffer.wrap(uPlane), ByteBuffer.wrap(vPlane)};

                //rotationDegree = 270; // for android
                synchronized (this.layoutLock) {
                    for (int i = 0; i < renderInfos.size(); i++) {
                        I420Frame frame = new I420Frame(width, height, renderInfos.get(i).rotation, yuvStrides, yuvPlanes);
                        renderInfos.get(i).view.renderFrame(frame);
                    }
                }
            }
        }
    }

    public void onVideoFrameCallbackGLES(String userId, int type, int texture, float[] matrix, int width, int height, long timestamp) {
        if (!bPauseRender) {
            Vector<RenderInfo> renderInfos = renderers.get(userId);
            if (renderInfos != null) {
                I420Frame frame = new I420Frame(width, height, 0, texture, matrix, type == YOUME_VIDEO_FMT.VIDEO_FMT_TEXTURE_OES);
                synchronized (this.layoutLock) {
                    for (int i = 0; i < renderInfos.size(); i++) {
                        frame.rotationDegree = renderInfos.get(i).rotation;
                        renderInfos.get(i).view.renderFrame(frame);
                    }
                }
            }
        }
    }

    public void onVideoFrameMixedGLES(int type, int texture, float[] matrix, int width, int height, long timestamp) {
        if (!bPauseRender) {
            String userId = localUserId;
            Vector<RenderInfo> renderInfos = renderers.get(userId);
            if (renderInfos != null) {
                I420Frame frame = new I420Frame(width, height, 0, texture, matrix, type == YOUME_VIDEO_FMT.VIDEO_FMT_TEXTURE_OES);
                synchronized (this.layoutLock) {
                    for (int i = 0; i < renderInfos.size(); i++) {
                        //Log.d(TAG, "renderInfos" + renderInfos.get(i));
                        frame.rotationDegree = renderInfos.get(i).rotation;
                        renderInfos.get(i).view.renderFrame(frame);
                        //info.view.setVisibility(View.VISIBLE);
                    }
                }
            }
        }
    }

    public int onVideoRenderFilterCallback(int var1, int var2, int var3, int var4, int var5) {
        return 0;
    }
}
