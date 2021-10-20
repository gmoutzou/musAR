package com.gmoutzou.musar.app.video.VideoPlayback;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.annotation.SuppressLint;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;

import com.vuforia.Device;
import com.vuforia.ImageTarget;
import com.vuforia.Matrix44F;
import com.vuforia.Renderer;
import com.vuforia.State;
import com.vuforia.Tool;
import com.vuforia.TrackableResult;
import com.vuforia.VIDEO_BACKGROUND_REFLECTION;
import com.vuforia.Vec2F;
import com.vuforia.Vec3F;
import com.vuforia.Vuforia;

import com.gmoutzou.musar.app.video.ARApplication.ARAppRenderer;
import com.gmoutzou.musar.app.video.ARApplication.ARAppRendererControl;
import com.gmoutzou.musar.app.video.ARApplication.ARApplicationSession;
import com.gmoutzou.musar.app.utils.ARMath;
import com.gmoutzou.musar.app.utils.ARUtils;
import com.gmoutzou.musar.app.utils.Texture;
import com.gmoutzou.musar.app.video.VideoPlayback.VideoPlayerHelper.MEDIA_STATE;
import com.gmoutzou.musar.app.video.VideoPlayback.VideoPlayerHelper.MEDIA_TYPE;

public class VideoPlaybackRenderer implements GLSurfaceView.Renderer, ARAppRendererControl {
    private static final String LOGTAG = "VideoPlaybackRenderer";

    ARApplicationSession vuforiaAppSession;

    private int videoPlaybackShaderID = 0;
    private int videoPlaybackVertexHandle = 0;
    private int videoPlaybackNormalHandle = 0;
    private int videoPlaybackTexCoordHandle = 0;
    private int videoPlaybackMVPMatrixHandle = 0;
    private int videoPlaybackTexSamplerOESHandle = 0;

    int videoPlaybackTextureID[] = new int[VideoPlayback.NUM_TARGETS];

    private int keyframeShaderID = 0;
    private int keyframeVertexHandle = 0;
    private int keyframeNormalHandle = 0;
    private int keyframeTexCoordHandle = 0;
    private int keyframeMVPMatrixHandle = 0;
    private int keyframeTexSampler2DHandle = 0;

    private float videoQuadTextureCoords[] = {0.0f, 0.0f, 1.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 1.0f,};
    private float videoQuadTextureCoordsTransformedHercules[] = {0.0f, 0.0f,
            1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f,};
    private float videoQuadTextureCoordsTransformedDionysos[] = {0.0f, 0.0f,
            1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f,};
    private float videoQuadTextureCoordsTransformedAthena[] = {0.0f, 0.0f,
            1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f,};

    Vec3F targetPositiveDimensions[] = new Vec3F[VideoPlayback.NUM_TARGETS];

    static int NUM_QUAD_VERTEX = 4;
    static int NUM_QUAD_INDEX = 6;

    double quadVerticesArray[] = {-1.0f, -1.0f, 0.0f, 1.0f, -1.0f, 0.0f, 1.0f,
            1.0f, 0.0f, -1.0f, 1.0f, 0.0f};

    double quadTexCoordsArray[] = {0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f,
            1.0f};

    double quadNormalsArray[] = {0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1,};

    short quadIndicesArray[] = {0, 1, 2, 2, 3, 0};

    Buffer quadVertices, quadTexCoords, quadIndices, quadNormals;

    public boolean mIsActive = false;

    private float[][] mTexCoordTransformationMatrix = null;
    private VideoPlayerHelper mVideoPlayerHelper[] = null;
    private String mMovieName[] = null;
    private MEDIA_TYPE mCanRequestType[] = null;
    private int mSeekPosition[] = null;
    private boolean mShouldPlayImmediately[] = null;
    private long mLostTrackingSince[] = null;
    private boolean mLoadRequested[] = null;

    VideoPlayback mActivity;

    Matrix44F modelViewMatrix[] = new Matrix44F[VideoPlayback.NUM_TARGETS];

    private Vector<Texture> mTextures;

    boolean isTracking[] = new boolean[VideoPlayback.NUM_TARGETS];
    MEDIA_STATE currentStatus[] = new MEDIA_STATE[VideoPlayback.NUM_TARGETS];

    float videoQuadAspectRatio[] = new float[VideoPlayback.NUM_TARGETS];
    float keyframeQuadAspectRatio[] = new float[VideoPlayback.NUM_TARGETS];

    private ARAppRenderer mARAppRenderer;


    public VideoPlaybackRenderer(VideoPlayback activity,
                                 ARApplicationSession session) {

        mActivity = activity;
        vuforiaAppSession = session;
        mVideoPlayerHelper = new VideoPlayerHelper[VideoPlayback.NUM_TARGETS];
        mMovieName = new String[VideoPlayback.NUM_TARGETS];
        mCanRequestType = new MEDIA_TYPE[VideoPlayback.NUM_TARGETS];
        mSeekPosition = new int[VideoPlayback.NUM_TARGETS];
        mShouldPlayImmediately = new boolean[VideoPlayback.NUM_TARGETS];
        mLostTrackingSince = new long[VideoPlayback.NUM_TARGETS];
        mLoadRequested = new boolean[VideoPlayback.NUM_TARGETS];
        mTexCoordTransformationMatrix = new float[VideoPlayback.NUM_TARGETS][16];

        for (int i = 0; i < VideoPlayback.NUM_TARGETS; i++) {
            mVideoPlayerHelper[i] = null;
            mMovieName[i] = "";
            mCanRequestType[i] = MEDIA_TYPE.ON_TEXTURE_FULLSCREEN;
            mSeekPosition[i] = 0;
            mShouldPlayImmediately[i] = false;
            mLostTrackingSince[i] = -1;
            mLoadRequested[i] = false;
        }

        for (int i = 0; i < VideoPlayback.NUM_TARGETS; i++)
            targetPositiveDimensions[i] = new Vec3F();

        for (int i = 0; i < VideoPlayback.NUM_TARGETS; i++)
            modelViewMatrix[i] = new Matrix44F();

        mARAppRenderer = new ARAppRenderer(this, mActivity, Device.MODE.MODE_AR, false, 0.01f, 5f);

    }

    public void setVideoPlayerHelper(int target,
                                     VideoPlayerHelper newVideoPlayerHelper) {
        mVideoPlayerHelper[target] = newVideoPlayerHelper;
    }

    public void requestLoad(int target, String movieName, int seekPosition,
                            boolean playImmediately) {
        mMovieName[target] = movieName;
        mSeekPosition[target] = seekPosition;
        mShouldPlayImmediately[target] = playImmediately;
        mLoadRequested[target] = true;
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        initRendering();
        Vuforia.onSurfaceCreated();
        mARAppRenderer.onSurfaceCreated();

        for (int i = 0; i < VideoPlayback.NUM_TARGETS; i++) {
            if (mVideoPlayerHelper[i] != null) {
                if (!mVideoPlayerHelper[i]
                        .setupSurfaceTexture(videoPlaybackTextureID[i])) {
                    mCanRequestType[i] = MEDIA_TYPE.FULLSCREEN;
                }
                else {
                    mCanRequestType[i] = MEDIA_TYPE.ON_TEXTURE_FULLSCREEN;
                }
                if (mLoadRequested[i]) {
                    mVideoPlayerHelper[i].load(mMovieName[i],
                            mCanRequestType[i], mShouldPlayImmediately[i],
                            mSeekPosition[i]);
                    mLoadRequested[i] = false;
                }
            }
        }
    }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Vuforia.onSurfaceChanged(width, height);
        mARAppRenderer.onConfigurationChanged(mIsActive);
        for (int i = 0; i < VideoPlayback.NUM_TARGETS; i++) {
            if (mLoadRequested[i] && mVideoPlayerHelper[i] != null) {
                mVideoPlayerHelper[i].load(mMovieName[i], mCanRequestType[i],
                        mShouldPlayImmediately[i], mSeekPosition[i]);
                mLoadRequested[i] = false;
            }
        }
    }

    public void onDrawFrame(GL10 gl) {
        if (!mIsActive)
            return;

        for (int i = 0; i < VideoPlayback.NUM_TARGETS; i++) {
            if (mVideoPlayerHelper[i] != null) {
                if (mVideoPlayerHelper[i].isPlayableOnTexture()) {
                    if (mVideoPlayerHelper[i].getStatus() == MEDIA_STATE.PLAYING) {
                        mVideoPlayerHelper[i].updateVideoData();
                    }
                    mVideoPlayerHelper[i]
                            .getSurfaceTextureTransformMatrix(mTexCoordTransformationMatrix[i]);
                    setVideoDimensions(i,
                            mVideoPlayerHelper[i].getVideoWidth(),
                            mVideoPlayerHelper[i].getVideoHeight(),
                            mTexCoordTransformationMatrix[i]);
                }
                setStatus(i, mVideoPlayerHelper[i].getStatus().getNumericType());
            }
        }
        //renderFrame();
        mARAppRenderer.render();

        for (int i = 0; i < VideoPlayback.NUM_TARGETS; i++) {
            if (isTracking(i)) {
                mLostTrackingSince[i] = -1;
            } else {
                if (mLostTrackingSince[i] < 0)
                    mLostTrackingSince[i] = SystemClock.uptimeMillis();
                else {
                    if ((SystemClock.uptimeMillis() - mLostTrackingSince[i]) > 2000) {
                        if (mVideoPlayerHelper[i] != null)
                            mVideoPlayerHelper[i].pause();
                    }
                }
            }
        }
    }


    @SuppressLint("InlinedApi")
    void initRendering() {
        Log.d(LOGTAG, "VideoPlayback VideoPlaybackRenderer initRendering");
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f
                : 1.0f);
        for (Texture t : mTextures) {
            GLES20.glGenTextures(1, t.mTextureID, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t.mTextureID[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                    t.mWidth, t.mHeight, 0, GLES20.GL_RGBA,
                    GLES20.GL_UNSIGNED_BYTE, t.mData);
        }

        for (int i = 0; i < VideoPlayback.NUM_TARGETS; i++) {
            GLES20.glGenTextures(1, videoPlaybackTextureID, i);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    videoPlaybackTextureID[i]);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        }

        videoPlaybackShaderID = ARUtils.createProgramFromShaderSrc(
                VideoPlaybackShaders.VIDEO_PLAYBACK_VERTEX_SHADER,
                VideoPlaybackShaders.VIDEO_PLAYBACK_FRAGMENT_SHADER);
        videoPlaybackVertexHandle = GLES20.glGetAttribLocation(
                videoPlaybackShaderID, "vertexPosition");
        videoPlaybackNormalHandle = GLES20.glGetAttribLocation(
                videoPlaybackShaderID, "vertexNormal");
        videoPlaybackTexCoordHandle = GLES20.glGetAttribLocation(
                videoPlaybackShaderID, "vertexTexCoord");
        videoPlaybackMVPMatrixHandle = GLES20.glGetUniformLocation(
                videoPlaybackShaderID, "modelViewProjectionMatrix");
        videoPlaybackTexSamplerOESHandle = GLES20.glGetUniformLocation(
                videoPlaybackShaderID, "texSamplerOES");

        keyframeShaderID = ARUtils.createProgramFromShaderSrc(
                KeyFrameShaders.KEY_FRAME_VERTEX_SHADER,
                KeyFrameShaders.KEY_FRAME_FRAGMENT_SHADER);
        keyframeVertexHandle = GLES20.glGetAttribLocation(keyframeShaderID,
                "vertexPosition");
        keyframeNormalHandle = GLES20.glGetAttribLocation(keyframeShaderID,
                "vertexNormal");
        keyframeTexCoordHandle = GLES20.glGetAttribLocation(keyframeShaderID,
                "vertexTexCoord");
        keyframeMVPMatrixHandle = GLES20.glGetUniformLocation(keyframeShaderID,
                "modelViewProjectionMatrix");
        keyframeTexSampler2DHandle = GLES20.glGetUniformLocation(
                keyframeShaderID, "texSampler2D");

        keyframeQuadAspectRatio[VideoPlayback.HERCULES] = (float) mTextures
                .get(VideoPlayback.HERCULES).mHeight / (float) mTextures.get(VideoPlayback.HERCULES).mWidth;
        keyframeQuadAspectRatio[VideoPlayback.DIONYSOS] = (float) mTextures.get(VideoPlayback.DIONYSOS).mHeight
                / (float) mTextures.get(VideoPlayback.DIONYSOS).mWidth;
        keyframeQuadAspectRatio[VideoPlayback.ATHENA] = (float) mTextures.get(VideoPlayback.ATHENA).mHeight
                / (float) mTextures.get(VideoPlayback.ATHENA).mWidth;

        quadVertices = fillBuffer(quadVerticesArray);
        quadTexCoords = fillBuffer(quadTexCoordsArray);
        quadIndices = fillBuffer(quadIndicesArray);
        quadNormals = fillBuffer(quadNormalsArray);

        mARAppRenderer.configureVideoBackground();
    }


    private Buffer fillBuffer(double[] array) {
        ByteBuffer bb = ByteBuffer.allocateDirect(4 * array.length); // each
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (double d : array)
            bb.putFloat((float) d);
        bb.rewind();
        return bb;
    }


    private Buffer fillBuffer(short[] array) {
        ByteBuffer bb = ByteBuffer.allocateDirect(2 * array.length); // each
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (short s : array)
            bb.putShort(s);
        bb.rewind();
        return bb;
    }


    private Buffer fillBuffer(float[] array) {
        ByteBuffer bb = ByteBuffer.allocateDirect(4 * array.length); // each
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (float d : array)
            bb.putFloat(d);
        bb.rewind();
        return bb;
    }


    @SuppressLint("InlinedApi")
    @Override
    public void renderFrame() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        State state = Renderer.getInstance().begin();
        mARAppRenderer.renderVideoBackground();
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        int[] viewport = vuforiaAppSession.getViewport();
        GLES20.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);
        if (Renderer.getInstance().getVideoBackgroundConfig().getReflection() == VIDEO_BACKGROUND_REFLECTION.VIDEO_BACKGROUND_REFLECTION_ON) {
            GLES20.glFrontFace(GLES20.GL_CW); // Front camera
        }
        else {
            GLES20.glFrontFace(GLES20.GL_CCW); // Back camera
        }
        float temp[] = {0.0f, 0.0f, 0.0f};
        for (int i = 0; i < VideoPlayback.NUM_TARGETS; i++) {
            isTracking[i] = false;
            targetPositiveDimensions[i].setData(temp);
        }

        for (int tIdx = 0; tIdx < state.getNumTrackableResults(); tIdx++) {
            // Get the trackable:
            TrackableResult trackableResult = state.getTrackableResult(tIdx);

            ImageTarget imageTarget = (ImageTarget) trackableResult
                    .getTrackable();
            int currentTarget = -1;
            if (imageTarget.getName().compareTo("hercules") == 0) {
                currentTarget = VideoPlayback.HERCULES;
            } else if (imageTarget.getName().compareTo("dionysos") == 0) {
                currentTarget = VideoPlayback.DIONYSOS;
            } else if (imageTarget.getName().compareTo("athena") == 0) {
                currentTarget = VideoPlayback.ATHENA;
            }

            modelViewMatrix[currentTarget] = Tool
                    .convertPose2GLMatrix(trackableResult.getPose());

            isTracking[currentTarget] = true;

            targetPositiveDimensions[currentTarget] = imageTarget.getSize();
            temp[0] = targetPositiveDimensions[currentTarget].getData()[0] / 2.0f;
            temp[1] = targetPositiveDimensions[currentTarget].getData()[1] / 2.0f;
            targetPositiveDimensions[currentTarget].setData(temp);
            if ((currentStatus[currentTarget] == VideoPlayerHelper.MEDIA_STATE.READY)
                    || (currentStatus[currentTarget] == VideoPlayerHelper.MEDIA_STATE.REACHED_END)
                    || (currentStatus[currentTarget] == VideoPlayerHelper.MEDIA_STATE.NOT_READY)
                    || (currentStatus[currentTarget] == VideoPlayerHelper.MEDIA_STATE.ERROR)) {
                float[] modelViewMatrixKeyframe = Tool.convertPose2GLMatrix(
                        trackableResult.getPose()).getData();
                float[] modelViewProjectionKeyframe = new float[16];
                // Matrix.translateM(modelViewMatrixKeyframe, 0, 0.0f, 0.0f,
                // targetPositiveDimensions[currentTarget].getData()[0]);
                float ratio = 1.0f;
                if (mTextures.get(currentTarget).mSuccess) {
                    ratio = keyframeQuadAspectRatio[currentTarget];
                }
                else {
                    ratio = targetPositiveDimensions[currentTarget].getData()[1]
                            / targetPositiveDimensions[currentTarget].getData()[0];
                }

                Matrix.scaleM(modelViewMatrixKeyframe, 0,
                        targetPositiveDimensions[currentTarget].getData()[0] / 1.30f,
                        targetPositiveDimensions[currentTarget].getData()[0] * ratio / 1.25f,
                        targetPositiveDimensions[currentTarget].getData()[0] / 1.30f);
                Matrix.multiplyMM(modelViewProjectionKeyframe, 0,
                        vuforiaAppSession.getProjectionMatrix().getData(), 0,
                        modelViewMatrixKeyframe, 0);

                GLES20.glEnable(GLES20.GL_BLEND);
                GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
                GLES20.glUseProgram(keyframeShaderID);
                GLES20.glVertexAttribPointer(keyframeVertexHandle, 3,
                        GLES20.GL_FLOAT, false, 0, quadVertices);
                GLES20.glVertexAttribPointer(keyframeNormalHandle, 3,
                        GLES20.GL_FLOAT, false, 0, quadNormals);
                GLES20.glVertexAttribPointer(keyframeTexCoordHandle, 2,
                        GLES20.GL_FLOAT, false, 0, quadTexCoords);
                GLES20.glEnableVertexAttribArray(keyframeVertexHandle);
                GLES20.glEnableVertexAttribArray(keyframeNormalHandle);
                GLES20.glEnableVertexAttribArray(keyframeTexCoordHandle);
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                        mTextures.get(currentTarget).mTextureID[0]);
                GLES20.glUniformMatrix4fv(keyframeMVPMatrixHandle, 1, false,
                        modelViewProjectionKeyframe, 0);
                GLES20.glUniform1i(keyframeTexSampler2DHandle, 0);
                GLES20.glDrawElements(GLES20.GL_TRIANGLES, NUM_QUAD_INDEX,
                        GLES20.GL_UNSIGNED_SHORT, quadIndices);
                GLES20.glDisableVertexAttribArray(keyframeVertexHandle);
                GLES20.glDisableVertexAttribArray(keyframeNormalHandle);
                GLES20.glDisableVertexAttribArray(keyframeTexCoordHandle);
                GLES20.glUseProgram(0);
                GLES20.glDisable(GLES20.GL_BLEND);
            } else {
                float[] modelViewMatrixVideo = Tool.convertPose2GLMatrix(
                        trackableResult.getPose()).getData();
                float[] modelViewProjectionVideo = new float[16];
                // Matrix.translateM(modelViewMatrixVideo, 0, 0.0f, 0.0f,
                // targetPositiveDimensions[currentTarget].getData()[0]);
                Matrix.scaleM(modelViewMatrixVideo, 0,
                        targetPositiveDimensions[currentTarget].getData()[0],
                        targetPositiveDimensions[currentTarget].getData()[0]
                                * videoQuadAspectRatio[currentTarget],
                        targetPositiveDimensions[currentTarget].getData()[0]);
                Matrix.multiplyMM(modelViewProjectionVideo, 0,
                        vuforiaAppSession.getProjectionMatrix().getData(), 0,
                        modelViewMatrixVideo, 0);

                GLES20.glEnable(GLES20.GL_BLEND);
                GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

                GLES20.glUseProgram(videoPlaybackShaderID);
                GLES20.glVertexAttribPointer(videoPlaybackVertexHandle, 3,
                        GLES20.GL_FLOAT, false, 0, quadVertices);
                GLES20.glVertexAttribPointer(videoPlaybackNormalHandle, 3,
                        GLES20.GL_FLOAT, false, 0, quadNormals);

                if (imageTarget.getName().compareTo("hercules") == 0) {
                    GLES20.glVertexAttribPointer(videoPlaybackTexCoordHandle,
                            2, GLES20.GL_FLOAT, false, 0,
                            fillBuffer(videoQuadTextureCoordsTransformedHercules));
                } else if (imageTarget.getName().compareTo("dionysos") == 0) {
                    GLES20.glVertexAttribPointer(videoPlaybackTexCoordHandle,
                            2, GLES20.GL_FLOAT, false, 0,
                            fillBuffer(videoQuadTextureCoordsTransformedDionysos));
                } else if (imageTarget.getName().compareTo("athena") == 0) {
                    GLES20.glVertexAttribPointer(videoPlaybackTexCoordHandle,
                            2, GLES20.GL_FLOAT, false, 0,
                            fillBuffer(videoQuadTextureCoordsTransformedAthena));
                }

                GLES20.glEnableVertexAttribArray(videoPlaybackVertexHandle);
                GLES20.glEnableVertexAttribArray(videoPlaybackNormalHandle);
                GLES20.glEnableVertexAttribArray(videoPlaybackTexCoordHandle);
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                        videoPlaybackTextureID[currentTarget]);
                GLES20.glUniformMatrix4fv(videoPlaybackMVPMatrixHandle, 1,
                        false, modelViewProjectionVideo, 0);
                GLES20.glUniform1i(videoPlaybackTexSamplerOESHandle, 0);
                GLES20.glDrawElements(GLES20.GL_TRIANGLES, NUM_QUAD_INDEX,
                        GLES20.GL_UNSIGNED_SHORT, quadIndices);
                GLES20.glDisableVertexAttribArray(videoPlaybackVertexHandle);
                GLES20.glDisableVertexAttribArray(videoPlaybackNormalHandle);
                GLES20.glDisableVertexAttribArray(videoPlaybackTexCoordHandle);

                GLES20.glUseProgram(0);
                GLES20.glDisable(GLES20.GL_BLEND);
            }

            if ((currentStatus[currentTarget] == VideoPlayerHelper.MEDIA_STATE.READY)
                    || (currentStatus[currentTarget] == VideoPlayerHelper.MEDIA_STATE.REACHED_END)
                    || (currentStatus[currentTarget] == VideoPlayerHelper.MEDIA_STATE.PAUSED)
                    || (currentStatus[currentTarget] == VideoPlayerHelper.MEDIA_STATE.NOT_READY)
                    || (currentStatus[currentTarget] == VideoPlayerHelper.MEDIA_STATE.ERROR)) {
                float[] modelViewMatrixButton = Tool.convertPose2GLMatrix(
                        trackableResult.getPose()).getData();
                float[] modelViewProjectionButton = new float[16];
                GLES20.glDepthFunc(GLES20.GL_LEQUAL);
                GLES20.glEnable(GLES20.GL_BLEND);
                GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,
                        GLES20.GL_ONE_MINUS_SRC_ALPHA);
                Matrix
                        .translateM(
                                modelViewMatrixButton,
                                0,
                                0.0f,
                                0.0f,
                                targetPositiveDimensions[currentTarget].getData()[1] / 10.98f);
                Matrix
                        .scaleM(
                                modelViewMatrixButton,
                                0,
                                (targetPositiveDimensions[currentTarget].getData()[1] / 4.5f),
                                (targetPositiveDimensions[currentTarget].getData()[1] / 4.5f),
                                (targetPositiveDimensions[currentTarget].getData()[1] / 4.5f));
                Matrix.multiplyMM(modelViewProjectionButton, 0,
                        vuforiaAppSession.getProjectionMatrix().getData(), 0,
                        modelViewMatrixButton, 0);

                GLES20.glUseProgram(keyframeShaderID);
                GLES20.glVertexAttribPointer(keyframeVertexHandle, 3,
                        GLES20.GL_FLOAT, false, 0, quadVertices);
                GLES20.glVertexAttribPointer(keyframeNormalHandle, 3,
                        GLES20.GL_FLOAT, false, 0, quadNormals);
                GLES20.glVertexAttribPointer(keyframeTexCoordHandle, 2,
                        GLES20.GL_FLOAT, false, 0, quadTexCoords);
                GLES20.glEnableVertexAttribArray(keyframeVertexHandle);
                GLES20.glEnableVertexAttribArray(keyframeNormalHandle);
                GLES20.glEnableVertexAttribArray(keyframeTexCoordHandle);
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

                switch (currentStatus[currentTarget]) {
                    case READY:
                        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                                mTextures.get(VideoPlayback.NUM_TARGETS).mTextureID[0]);
                        break;
                    case REACHED_END:
                        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                                mTextures.get(VideoPlayback.NUM_TARGETS).mTextureID[0]);
                        break;
                    case PAUSED:
                        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                                mTextures.get(VideoPlayback.NUM_TARGETS).mTextureID[0]);
                        break;
                    case NOT_READY:
                        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                                mTextures.get(VideoPlayback.NUM_TARGETS + 1).mTextureID[0]);
                        break;
                    case ERROR:
                        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                                mTextures.get(VideoPlayback.NUM_TARGETS + 2).mTextureID[0]);
                        break;
                    default:
                        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                                mTextures.get(VideoPlayback.NUM_TARGETS + 1).mTextureID[0]);
                        break;
                }
                GLES20.glUniformMatrix4fv(keyframeMVPMatrixHandle, 1, false,
                        modelViewProjectionButton, 0);
                GLES20.glUniform1i(keyframeTexSampler2DHandle, 0);

                GLES20.glDrawElements(GLES20.GL_TRIANGLES, NUM_QUAD_INDEX,
                        GLES20.GL_UNSIGNED_SHORT, quadIndices);

                GLES20.glDisableVertexAttribArray(keyframeVertexHandle);
                GLES20.glDisableVertexAttribArray(keyframeNormalHandle);
                GLES20.glDisableVertexAttribArray(keyframeTexCoordHandle);
                GLES20.glUseProgram(0);
                GLES20.glDepthFunc(GLES20.GL_LESS);
                GLES20.glDisable(GLES20.GL_BLEND);
            }
            ARUtils.checkGLError("VideoPlayback renderFrame");
        }

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        Renderer.getInstance().end();

    }


    boolean isTapOnScreenInsideTarget(int target, float x, float y) {
        Vec3F intersection;
        // Vec3F lineStart = new Vec3F();
        // Vec3F lineEnd = new Vec3F();

        DisplayMetrics metrics = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        intersection = ARMath.getPointToPlaneIntersection(ARMath
                        .Matrix44FInverse(vuforiaAppSession.getProjectionMatrix()),
                modelViewMatrix[target], metrics.widthPixels, metrics.heightPixels,
                new Vec2F(x, y), new Vec3F(0, 0, 0), new Vec3F(0, 0, 1));
        if ((intersection.getData()[0] >= -(targetPositiveDimensions[target]
                .getData()[0]))
                && (intersection.getData()[0] <= (targetPositiveDimensions[target]
                .getData()[0]))
                && (intersection.getData()[1] >= -(targetPositiveDimensions[target]
                .getData()[1]))
                && (intersection.getData()[1] <= (targetPositiveDimensions[target]
                .getData()[1]))) {
            return true;
        }
        else {
            return false;
        }
    }


    void setVideoDimensions(int target, float videoWidth, float videoHeight,
                            float[] textureCoordMatrix)
    {
        videoQuadAspectRatio[target] = videoHeight / videoWidth;

        float mtx[] = textureCoordMatrix;
        float tempUVMultRes[] = new float[2];

        if (target == VideoPlayback.HERCULES) {
            tempUVMultRes = uvMultMat4f(
                    videoQuadTextureCoordsTransformedHercules[0],
                    videoQuadTextureCoordsTransformedHercules[1],
                    videoQuadTextureCoords[0], videoQuadTextureCoords[1], mtx);
            videoQuadTextureCoordsTransformedHercules[0] = tempUVMultRes[0];
            videoQuadTextureCoordsTransformedHercules[1] = tempUVMultRes[1];
            tempUVMultRes = uvMultMat4f(
                    videoQuadTextureCoordsTransformedHercules[2],
                    videoQuadTextureCoordsTransformedHercules[3],
                    videoQuadTextureCoords[2], videoQuadTextureCoords[3], mtx);
            videoQuadTextureCoordsTransformedHercules[2] = tempUVMultRes[0];
            videoQuadTextureCoordsTransformedHercules[3] = tempUVMultRes[1];
            tempUVMultRes = uvMultMat4f(
                    videoQuadTextureCoordsTransformedHercules[4],
                    videoQuadTextureCoordsTransformedHercules[5],
                    videoQuadTextureCoords[4], videoQuadTextureCoords[5], mtx);
            videoQuadTextureCoordsTransformedHercules[4] = tempUVMultRes[0];
            videoQuadTextureCoordsTransformedHercules[5] = tempUVMultRes[1];
            tempUVMultRes = uvMultMat4f(
                    videoQuadTextureCoordsTransformedHercules[6],
                    videoQuadTextureCoordsTransformedHercules[7],
                    videoQuadTextureCoords[6], videoQuadTextureCoords[7], mtx);
            videoQuadTextureCoordsTransformedHercules[6] = tempUVMultRes[0];
            videoQuadTextureCoordsTransformedHercules[7] = tempUVMultRes[1];
        } else if (target == VideoPlayback.DIONYSOS) {
            tempUVMultRes = uvMultMat4f(
                    videoQuadTextureCoordsTransformedDionysos[0],
                    videoQuadTextureCoordsTransformedDionysos[1],
                    videoQuadTextureCoords[0], videoQuadTextureCoords[1], mtx);
            videoQuadTextureCoordsTransformedDionysos[0] = tempUVMultRes[0];
            videoQuadTextureCoordsTransformedDionysos[1] = tempUVMultRes[1];
            tempUVMultRes = uvMultMat4f(
                    videoQuadTextureCoordsTransformedDionysos[2],
                    videoQuadTextureCoordsTransformedDionysos[3],
                    videoQuadTextureCoords[2], videoQuadTextureCoords[3], mtx);
            videoQuadTextureCoordsTransformedDionysos[2] = tempUVMultRes[0];
            videoQuadTextureCoordsTransformedDionysos[3] = tempUVMultRes[1];
            tempUVMultRes = uvMultMat4f(
                    videoQuadTextureCoordsTransformedDionysos[4],
                    videoQuadTextureCoordsTransformedDionysos[5],
                    videoQuadTextureCoords[4], videoQuadTextureCoords[5], mtx);
            videoQuadTextureCoordsTransformedDionysos[4] = tempUVMultRes[0];
            videoQuadTextureCoordsTransformedDionysos[5] = tempUVMultRes[1];
            tempUVMultRes = uvMultMat4f(
                    videoQuadTextureCoordsTransformedDionysos[6],
                    videoQuadTextureCoordsTransformedDionysos[7],
                    videoQuadTextureCoords[6], videoQuadTextureCoords[7], mtx);
            videoQuadTextureCoordsTransformedDionysos[6] = tempUVMultRes[0];
            videoQuadTextureCoordsTransformedDionysos[7] = tempUVMultRes[1];
        } else if (target == VideoPlayback.ATHENA) {
            tempUVMultRes = uvMultMat4f(
                    videoQuadTextureCoordsTransformedAthena[0],
                    videoQuadTextureCoordsTransformedAthena[1],
                    videoQuadTextureCoords[0], videoQuadTextureCoords[1], mtx);
            videoQuadTextureCoordsTransformedAthena[0] = tempUVMultRes[0];
            videoQuadTextureCoordsTransformedAthena[1] = tempUVMultRes[1];
            tempUVMultRes = uvMultMat4f(
                    videoQuadTextureCoordsTransformedAthena[2],
                    videoQuadTextureCoordsTransformedAthena[3],
                    videoQuadTextureCoords[2], videoQuadTextureCoords[3], mtx);
            videoQuadTextureCoordsTransformedAthena[2] = tempUVMultRes[0];
            videoQuadTextureCoordsTransformedAthena[3] = tempUVMultRes[1];
            tempUVMultRes = uvMultMat4f(
                    videoQuadTextureCoordsTransformedAthena[4],
                    videoQuadTextureCoordsTransformedAthena[5],
                    videoQuadTextureCoords[4], videoQuadTextureCoords[5], mtx);
            videoQuadTextureCoordsTransformedAthena[4] = tempUVMultRes[0];
            videoQuadTextureCoordsTransformedAthena[5] = tempUVMultRes[1];
            tempUVMultRes = uvMultMat4f(
                    videoQuadTextureCoordsTransformedAthena[6],
                    videoQuadTextureCoordsTransformedAthena[7],
                    videoQuadTextureCoords[6], videoQuadTextureCoords[7], mtx);
            videoQuadTextureCoordsTransformedAthena[6] = tempUVMultRes[0];
            videoQuadTextureCoordsTransformedAthena[7] = tempUVMultRes[1];
        }
        // textureCoordMatrix = mtx;
    }

    float[] uvMultMat4f(float transformedU, float transformedV, float u,
                        float v, float[] pMat) {
        float x = pMat[0] * u + pMat[4] * v /* + pMat[ 8]*0.f */ + pMat[12]
                * 1.f;
        float y = pMat[1] * u + pMat[5] * v /* + pMat[ 9]*0.f */ + pMat[13]
                * 1.f;
        // float z = pMat[2]*u + pMat[6]*v + pMat[10]*0.f + pMat[14]*1.f;
        // float w = pMat[3]*u + pMat[7]*v + pMat[11]*0.f + pMat[15]*1.f;

        float result[] = new float[2];
        // transformedU = x;
        // transformedV = y;
        result[0] = x;
        result[1] = y;
        return result;
    }


    void setStatus(int target, int value) {
        switch (value) {
            case 0:
                currentStatus[target] = VideoPlayerHelper.MEDIA_STATE.REACHED_END;
                break;
            case 1:
                currentStatus[target] = VideoPlayerHelper.MEDIA_STATE.PAUSED;
                break;
            case 2:
                currentStatus[target] = VideoPlayerHelper.MEDIA_STATE.STOPPED;
                break;
            case 3:
                currentStatus[target] = VideoPlayerHelper.MEDIA_STATE.PLAYING;
                break;
            case 4:
                currentStatus[target] = VideoPlayerHelper.MEDIA_STATE.READY;
                break;
            case 5:
                currentStatus[target] = VideoPlayerHelper.MEDIA_STATE.NOT_READY;
                break;
            case 6:
                currentStatus[target] = VideoPlayerHelper.MEDIA_STATE.ERROR;
                break;
            default:
                currentStatus[target] = VideoPlayerHelper.MEDIA_STATE.NOT_READY;
                break;
        }
    }

    boolean isTracking(int target) {
        return isTracking[target];
    }

    public void setTextures(Vector<Texture> textures) {
        mTextures = textures;
    }
}
