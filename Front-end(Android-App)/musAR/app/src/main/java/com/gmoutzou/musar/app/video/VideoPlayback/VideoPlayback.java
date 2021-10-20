package com.gmoutzou.musar.app.video.VideoPlayback;

import com.gmoutzou.musar.R;

import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;

import com.vuforia.CameraDevice;
import com.vuforia.DataSet;
import com.vuforia.HINT;
import com.vuforia.ObjectTracker;
import com.vuforia.State;
import com.vuforia.STORAGE_TYPE;
import com.vuforia.Tracker;
import com.vuforia.TrackerManager;
import com.vuforia.Vuforia;

import com.gmoutzou.musar.app.video.ARApplication.ARApplicationControl;
import com.gmoutzou.musar.app.video.ARApplication.ARApplicationException;
import com.gmoutzou.musar.app.video.ARApplication.ARApplicationSession;
import com.gmoutzou.musar.app.utils.LoadingDialogHandler;
import com.gmoutzou.musar.app.utils.ARApplicationGLView;
import com.gmoutzou.musar.app.utils.Texture;
import com.gmoutzou.musar.app.video.VideoPlayback.VideoPlayerHelper.MEDIA_STATE;
import com.gmoutzou.musar.app.video.ui.ARAppMenu.ARAppMenu;
import com.gmoutzou.musar.app.video.ui.ARAppMenu.ARAppMenuGroup;
import com.gmoutzou.musar.app.video.ui.ARAppMenu.ARAppMenuInterface;

public class VideoPlayback extends Activity implements
        ARApplicationControl, ARAppMenuInterface
{
    private static final String LOGTAG = "VideoPlayback";
    
    ARApplicationSession vuforiaAppSession;
    
    Activity mActivity;

    private GestureDetector mGestureDetector = null;
    private SimpleOnGestureListener mSimpleListener = null;

    public static final int NUM_TARGETS = 3;
    public static final int HERCULES = 0;
    public static final int DIONYSOS = 1;
    public static final int ATHENA = 2;

    private VideoPlayerHelper mVideoPlayerHelper[] = null;
    private int mSeekPosition[] = null;
    private boolean mWasPlaying[] = null;
    private String mMovieName[] = null;

    private boolean mReturningFromFullScreen = false;

    private ARApplicationGLView mGlView;

    private VideoPlaybackRenderer mRenderer;

    private Vector<Texture> mTextures;
    
    DataSet mDataSet = null;
    
    private RelativeLayout mUILayout;
    
    private boolean mPlayFullscreenVideo = false;
    
    private ARAppMenu mARAppMenu;
    
    private LoadingDialogHandler loadingDialogHandler = new LoadingDialogHandler(
        this);

    private AlertDialog mErrorDialog;
    
    boolean mIsDroidDevice = false;
    boolean mIsInitialized = false;

    protected void onCreate(Bundle savedInstanceState)
    {
        Log.d(LOGTAG, "onCreate");
        super.onCreate(savedInstanceState);
        
        vuforiaAppSession = new ARApplicationSession(this);
        
        mActivity = this;
        
        startLoadingAnimation();
        
        vuforiaAppSession
            .initAR(this, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mTextures = new Vector<Texture>();
        loadTextures();

        mSimpleListener = new SimpleOnGestureListener();
        mGestureDetector = new GestureDetector(getApplicationContext(),
            mSimpleListener);
        
        mVideoPlayerHelper = new VideoPlayerHelper[NUM_TARGETS];
        mSeekPosition = new int[NUM_TARGETS];
        mWasPlaying = new boolean[NUM_TARGETS];
        mMovieName = new String[NUM_TARGETS];

        for (int i = 0; i < NUM_TARGETS; i++)
        {
            mVideoPlayerHelper[i] = new VideoPlayerHelper();
            mVideoPlayerHelper[i].init();
            mVideoPlayerHelper[i].setActivity(this);
        }
        
        mMovieName[HERCULES] = "VideoPlayback/hercules.mp4";
        mMovieName[DIONYSOS] = "VideoPlayback/dionysos.mp4";
        mMovieName[ATHENA] = "VideoPlayback/athena.mp4";

        mGestureDetector.setOnDoubleTapListener(new OnDoubleTapListener()
        {
            public boolean onDoubleTap(MotionEvent e)
            {
               return false;
            }
            
            
            public boolean onDoubleTapEvent(MotionEvent e)
            {
                return false;
            }

            public boolean onSingleTapConfirmed(MotionEvent e)
            {
                boolean isSingleTapHandled = false;
                for (int i = 0; i < NUM_TARGETS; i++)
                {
                    if (mRenderer!= null && mRenderer.isTapOnScreenInsideTarget(i, e.getX(),
                        e.getY()))
                    {
                        if (mVideoPlayerHelper[i].isPlayableOnTexture())
                        {
                            if ((mVideoPlayerHelper[i].getStatus() == MEDIA_STATE.PAUSED)
                                || (mVideoPlayerHelper[i].getStatus() == MEDIA_STATE.READY)
                                || (mVideoPlayerHelper[i].getStatus() == MEDIA_STATE.STOPPED)
                                || (mVideoPlayerHelper[i].getStatus() == MEDIA_STATE.REACHED_END))
                            {
                                pauseAll(i);

                                if ((mVideoPlayerHelper[i].getStatus() == MEDIA_STATE.REACHED_END))
                                    mSeekPosition[i] = 0;
                                
                                mVideoPlayerHelper[i].play(mPlayFullscreenVideo,
                                    mSeekPosition[i]);
                                mSeekPosition[i] = VideoPlayerHelper.CURRENT_POSITION;
                            } else if (mVideoPlayerHelper[i].getStatus() == MEDIA_STATE.PLAYING)
                            {
                                mVideoPlayerHelper[i].pause();
                            }
                        } else if (mVideoPlayerHelper[i].isPlayableFullscreen())
                        {
                            mVideoPlayerHelper[i].play(true,
                                VideoPlayerHelper.CURRENT_POSITION);
                        }
                        
                        isSingleTapHandled = true;

                        break;
                    }
                }
                
                return isSingleTapHandled;
            }
        });
    }

    private void loadTextures()
    {
        mTextures.add(Texture.loadTextureFromApk(
            "VideoPlayback/hercules.png", getAssets()));
        mTextures.add(Texture.loadTextureFromApk(
            "VideoPlayback/dionysos.png", getAssets()));
        mTextures.add(Texture.loadTextureFromApk(
                "VideoPlayback/athena.png", getAssets()));
        mTextures.add(Texture.loadTextureFromApk("VideoPlayback/play.png",
            getAssets()));
        mTextures.add(Texture.loadTextureFromApk("VideoPlayback/busy.png",
            getAssets()));
        mTextures.add(Texture.loadTextureFromApk("VideoPlayback/error.png",
            getAssets()));
    }

    protected void onResume()
    {
        Log.d(LOGTAG, "onResume");
        super.onResume();

        if (mIsDroidDevice)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        
        try
        {
            vuforiaAppSession.resumeAR();
        } catch (ARApplicationException e)
        {
            Log.e(LOGTAG, e.getString());
        }

        if (mGlView != null)
        {
            mGlView.setVisibility(View.VISIBLE);
            mGlView.onResume();
        }

        if (mRenderer != null)
        {
            for (int i = 0; i < NUM_TARGETS; i++)
            {
                if (!mReturningFromFullScreen)
                {
                    mRenderer.requestLoad(i, mMovieName[i], mSeekPosition[i],
                        false);
                } else
                {
                    mRenderer.requestLoad(i, mMovieName[i], mSeekPosition[i],
                        mWasPlaying[i]);
                }
            }
        }
        
        mReturningFromFullScreen = false;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == 1)
        {
            
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            
            if (resultCode == RESULT_OK)
            {
                String movieBeingPlayed = data.getStringExtra("movieName");
                mReturningFromFullScreen = true;

                for (int i = 0; i < NUM_TARGETS; i++)
                {
                    if (movieBeingPlayed.compareTo(mMovieName[i]) == 0)
                    {
                        mSeekPosition[i] = data.getIntExtra(
                            "currentSeekPosition", 0);
                        mWasPlaying[i] = false;
                    }
                }
            }
        }
    }
    
    
    public void onConfigurationChanged(Configuration config)
    {
        Log.d(LOGTAG, "onConfigurationChanged");
        super.onConfigurationChanged(config);
        
        vuforiaAppSession.onConfigurationChanged();
    }

    protected void onPause()
    {
        Log.d(LOGTAG, "onPause");
        super.onPause();
        
        if (mGlView != null)
        {
            mGlView.setVisibility(View.INVISIBLE);
            mGlView.onPause();
        }

        for (int i = 0; i < NUM_TARGETS; i++)
        {
            if (mVideoPlayerHelper[i].isPlayableOnTexture())
            {
                mSeekPosition[i] = mVideoPlayerHelper[i].getCurrentPosition();
                mWasPlaying[i] = mVideoPlayerHelper[i].getStatus() == MEDIA_STATE.PLAYING ? true
                    : false;
            }

            if (mVideoPlayerHelper[i] != null)
                mVideoPlayerHelper[i].unload();
        }
        
        mReturningFromFullScreen = false;
        
        try
        {
            vuforiaAppSession.pauseAR();
        } catch (ARApplicationException e)
        {
            Log.e(LOGTAG, e.getString());
        }
    }

    protected void onDestroy()
    {
        Log.d(LOGTAG, "onDestroy");
        super.onDestroy();
        
        for (int i = 0; i < NUM_TARGETS; i++)
        {
            if (mVideoPlayerHelper[i] != null)
                mVideoPlayerHelper[i].deinit();
            mVideoPlayerHelper[i] = null;
        }
        
        try {
            vuforiaAppSession.stopAR();
        } catch (ARApplicationException e) {
            Log.e(LOGTAG, e.getString());
        }

        mTextures.clear();
        mTextures = null;
        
        System.gc();
    }

    private void pauseAll(int except)
    {
        for (int i = 0; i < NUM_TARGETS; i++)
        {
            if (i != except)
            {
                if (mVideoPlayerHelper[i].isPlayableOnTexture())
                {
                    mVideoPlayerHelper[i].pause();
                }
            }
        }
    }

    public void onBackPressed()
    {
        pauseAll(-1);
        super.onBackPressed();
    }
    
    
    private void startLoadingAnimation()
    {
        mUILayout = (RelativeLayout) View.inflate(this, R.layout.camera_overlay,
            null);
        
        mUILayout.setVisibility(View.VISIBLE);
        mUILayout.setBackgroundColor(Color.BLACK);

        loadingDialogHandler.mLoadingDialogContainer = mUILayout
            .findViewById(R.id.loading_indicator);

        loadingDialogHandler
            .sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);

        addContentView(mUILayout, new LayoutParams(LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT));
    }

    private void initApplicationAR()
    {
        int depthSize = 16;
        int stencilSize = 0;
        boolean translucent = Vuforia.requiresAlpha();
        
        mGlView = new ARApplicationGLView(this);
        mGlView.init(translucent, depthSize, stencilSize);
        
        mRenderer = new VideoPlaybackRenderer(this, vuforiaAppSession);
        mRenderer.setTextures(mTextures);

        for (int i = 0; i < NUM_TARGETS; i++)
        {
            mRenderer.setVideoPlayerHelper(i, mVideoPlayerHelper[i]);
            mRenderer.requestLoad(i, mMovieName[i], 0, false);
        }
        
        mGlView.setRenderer(mRenderer);
        
        for (int i = 0; i < NUM_TARGETS; i++)
        {
            float[] temp = { 0f, 0f, 0f };
            mRenderer.targetPositiveDimensions[i].setData(temp);
            mRenderer.videoPlaybackTextureID[i] = -1;
        }
        
    }

    public boolean onTouchEvent(MotionEvent event)
    {
        boolean result = false;
        if ( mARAppMenu != null )
            result = mARAppMenu.processEvent(event);

        if (!result)
            mGestureDetector.onTouchEvent(event);
        
        return result;
    }
    
    
    @Override
    public boolean doInitTrackers()
    {
        boolean result = true;

        TrackerManager trackerManager = TrackerManager.getInstance();
        Tracker tracker = trackerManager.initTracker(ObjectTracker
            .getClassType());
        if (tracker == null)
        {
            Log.d(LOGTAG, "Failed to initialize ObjectTracker.");
            result = false;
        }
        
        return result;
    }
    
    
    @Override
    public boolean doLoadTrackersData()
    {
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
            .getTracker(ObjectTracker.getClassType());
        if (objectTracker == null)
        {
            Log.d(
                LOGTAG,
                "Failed to load tracking data set because the ObjectTracker has not been initialized.");
            return false;
        }

        mDataSet = objectTracker.createDataSet();
        if (mDataSet == null)
        {
            Log.d(LOGTAG, "Failed to create a new tracking data.");
            return false;
        }

        if (!mDataSet.load("musARProjectV.xml",
            STORAGE_TYPE.STORAGE_APPRESOURCE))
        {
            Log.d(LOGTAG, "Failed to load data set.");
            return false;
        }

        if (!objectTracker.activateDataSet(mDataSet))
        {
            Log.d(LOGTAG, "Failed to activate data set.");
            return false;
        }
        
        Log.d(LOGTAG, "Successfully loaded and activated data set.");
        return true;
    }
    
    
    @Override
    public boolean doStartTrackers()
    {
        boolean result = true;
        
        Tracker objectTracker = TrackerManager.getInstance().getTracker(
            ObjectTracker.getClassType());
        if (objectTracker != null)
        {
            objectTracker.start();
            Vuforia.setHint(HINT.HINT_MAX_SIMULTANEOUS_IMAGE_TARGETS, 2);
        } else
            result = false;
        
        return result;
    }
    
    
    @Override
    public boolean doStopTrackers()
    {
        boolean result = true;
        
        Tracker objectTracker = TrackerManager.getInstance().getTracker(
            ObjectTracker.getClassType());
        if (objectTracker != null)
            objectTracker.stop();
        else
            result = false;
        
        return result;
    }
    
    
    @Override
    public boolean doUnloadTrackersData()
    {
        boolean result = true;

        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
            .getTracker(ObjectTracker.getClassType());
        if (objectTracker == null)
        {
            Log.d(
                LOGTAG,
                "Failed to destroy the tracking data set because the ObjectTracker has not been initialized.");
            return false;
        }
        
        if (mDataSet != null)
        {
            if (objectTracker.getActiveDataSet(0) == mDataSet
                && !objectTracker.deactivateDataSet(mDataSet))
            {
                Log.d(
                    LOGTAG,
                    "Failed to destroy the tracking data set because the data set could not be deactivated.");
                result = false;
            } else if (!objectTracker.destroyDataSet(mDataSet))
            {
                Log.d(LOGTAG,
                    "Failed to destroy the tracking data set.");
                result = false;
            }
            
            mDataSet = null;
        }
        
        return result;
    }
    
    
    @Override
    public boolean doDeinitTrackers()
    {
        boolean result = true;

        TrackerManager trackerManager = TrackerManager.getInstance();
        trackerManager.deinitTracker(ObjectTracker.getClassType());
        
        return result;
    }
    
    
    @Override
    public void onInitARDone(ARApplicationException exception)
    {
        
        if (exception == null)
        {
            initApplicationAR();
            
            mRenderer.mIsActive = true;

            addContentView(mGlView, new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));

            mUILayout.bringToFront();

            loadingDialogHandler
                .sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);

            mUILayout.setBackgroundColor(Color.TRANSPARENT);
            
            try
            {
                vuforiaAppSession.startAR(CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_DEFAULT);
            } catch (ARApplicationException e)
            {
                Log.e(LOGTAG, e.getString());
            }
            
            boolean result = CameraDevice.getInstance().setFocusMode(
                CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO);
            
            if (!result)
                Log.e(LOGTAG, "Unable to enable continuous autofocus");
            
            mARAppMenu = new ARAppMenu(this, this, "Video Playback",
                mGlView, mUILayout, null);
            setSampleAppMenuSettings();
            
            mIsInitialized = true;
            
        } else
        {
            Log.e(LOGTAG, exception.getString());
            showInitializationErrorMessage(exception.getString());
        }
        
    }

    public void showInitializationErrorMessage(String message)
    {
        final String errorMessage = message;
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                if (mErrorDialog != null)
                {
                    mErrorDialog.dismiss();
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(
                    VideoPlayback.this);
                builder
                    .setMessage(errorMessage)
                    .setTitle(getString(R.string.INIT_ERROR))
                    .setCancelable(false)
                    .setIcon(0)
                    .setPositiveButton("OK",
                        new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int id)
                            {
                                finish();
                            }
                        });
                
                mErrorDialog = builder.create();
                mErrorDialog.show();
            }
        });
    }
    
    
    @Override
    public void onVuforiaUpdate(State state)
    {
    }
    
    final private static int CMD_BACK = -1;
    final private static int CMD_FULLSCREEN_VIDEO = 1;

    private void setSampleAppMenuSettings()
    {
        ARAppMenuGroup group;
        
        group = mARAppMenu.addGroup("", false);
        group.addTextItem(getString(R.string.menu_back), -1);
        
        group = mARAppMenu.addGroup("", true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        {
            group.addSelectionItem(getString(R.string.menu_playFullscreenVideo),
                CMD_FULLSCREEN_VIDEO, mPlayFullscreenVideo);
        }
        
        mARAppMenu.attachMenu();
    }
    
    
    @Override
    public boolean menuProcess(int command)
    {
        boolean result = true;
        switch (command)
        {
            case CMD_BACK:
                finish();
                break;
            
            case CMD_FULLSCREEN_VIDEO:
                mPlayFullscreenVideo = !mPlayFullscreenVideo;
                
                for(int i = 0; i < mVideoPlayerHelper.length; i++)
                {
                    if (mVideoPlayerHelper[i].getStatus() == MEDIA_STATE.PLAYING)
                    {
                        mVideoPlayerHelper[i].pause();
                        
                        mVideoPlayerHelper[i].play(true,
                            mSeekPosition[i]);
                    }
                }
                break;
            
        }
        return result;
    }
}
