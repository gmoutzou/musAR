package com.gmoutzou.musar.app.model3d.Object3D;

import com.gmoutzou.musar.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.gmoutzou.musar.app.utils.InfoDialogHandler;
import com.gmoutzou.musar.app.video.VideoPlayback.VideoPlayback;
import com.vuforia.CameraDevice;
import com.vuforia.DataSet;
import com.vuforia.ObjectTracker;
import com.vuforia.State;
import com.vuforia.STORAGE_TYPE;
import com.vuforia.Trackable;
import com.vuforia.Tracker;
import com.vuforia.TrackerManager;
import com.vuforia.Vuforia;

import com.gmoutzou.musar.app.model3d.ARApplication.ARApplicationControl;
import com.gmoutzou.musar.app.model3d.ARApplication.ARApplicationException;
import com.gmoutzou.musar.app.model3d.ARApplication.ARApplicationSession;
import com.gmoutzou.musar.app.utils.LoadingDialogHandler;
import com.gmoutzou.musar.app.utils.RotationGestureDetector;
import com.gmoutzou.musar.app.utils.ARApplicationGLView;
import com.gmoutzou.musar.app.utils.Texture;
import com.gmoutzou.musar.app.model3d.ui.ARAppMenu.ARAppMenu;
import com.gmoutzou.musar.app.model3d.ui.ARAppMenu.ARAppMenuGroup;
import com.gmoutzou.musar.app.model3d.ui.ARAppMenu.ARAppMenuInterface;


public class Object3D extends Activity implements ARApplicationControl,
        ARAppMenuInterface, RotationGestureDetector.OnRotationGestureListener {
    private static final String LOGTAG = "Object3D";

    ARApplicationSession vuforiaAppSession;

    private DataSet mCurrentDataset;
    private int mCurrentDatasetSelectionIndex = 0;
    private int mStartDatasetsIndex = 0;
    private int mDatasetsNumber = 0;
    private ArrayList<String> mDatasetStrings = new ArrayList<String>();

    private ARApplicationGLView mGlView;

    private Object3DRenderer mRenderer;

    private GestureDetector mGestureDetector;
    private RotationGestureDetector mRotationDetector;
    private ScaleGestureDetector mScaleDetector;

    private Vector<Texture> mTextures;

    private boolean mSwitchDatasetAsap = false;
    private boolean mFlash = false;
    private boolean mContAutofocus = true;
    private boolean mExtendedTracking = false;
    private boolean mRotation = false;
    private boolean mScale = false;

    private float scale = 0.003f;

    private View mFocusOptionView;
    private View mFlashOptionView;

    private RelativeLayout mUILayout;
    private LinearLayout infoLayout;
    private TextView tvInfo;
    private int infoTextCode = -1;

    private ARAppMenu mARAppMenu;

    LoadingDialogHandler loadingDialogHandler = new LoadingDialogHandler(this);
    InfoDialogHandler infoDialogHandler = new InfoDialogHandler(this);

    private AlertDialog mErrorDialog;

    boolean mIsDroidDevice = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOGTAG, "onCreate");
        super.onCreate(savedInstanceState);

        vuforiaAppSession = new ARApplicationSession(this);

        initCameraLayout();
        mDatasetStrings.add("musARProject.xml");

        vuforiaAppSession
                .initAR(this, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mGestureDetector = new GestureDetector(this, new GestureListener());
        mRotationDetector = new RotationGestureDetector(this);
        mScaleDetector = new ScaleGestureDetector(this, new ScaleListener());

        mTextures = new Vector<Texture>();
        loadTextures();

        mIsDroidDevice = android.os.Build.MODEL.toLowerCase().startsWith(
                "droid");

    }

    private class GestureListener extends
            GestureDetector.SimpleOnGestureListener {
        private final Handler autofocusHandler = new Handler();

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            boolean result = CameraDevice.getInstance().setFocusMode(
                    CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO);
            if (!result)
                Log.e("SingleTapUp", "Unable to trigger focus");

            autofocusHandler.postDelayed(new Runnable() {
                public void run() {
                    if (mContAutofocus) {
                        final boolean autofocusResult = CameraDevice.getInstance().setFocusMode(
                                CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO);

                        if (!autofocusResult)
                            Log.e("SingleTapUp", "Unable to re-enable continuous auto-focus");
                    }
                }
            }, 1000L);

            return true;
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scale *= detector.getScaleFactor();
            scale = Math.max(0.001f, Math.min(scale, 0.01f));
            //Log.d("SCALE LISTENER", "Scale: " + Float.toString(scale));
            mRenderer.setScale(scale);
            return true;
        }
    }

    private void loadTextures() {
        mTextures.add(Texture.loadTextureFromApk("hercules.jpg",
                getAssets()));
        mTextures.add(Texture.loadTextureFromApk("dionysos.jpg",
                getAssets()));
        mTextures.add(Texture.loadTextureFromApk("athena.jpg",
                getAssets()));
    }

    @Override
    protected void onResume() {
        Log.d(LOGTAG, "onResume");
        super.onResume();

        if (mIsDroidDevice) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        vuforiaAppSession.onResume();
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        Log.d(LOGTAG, "onConfigurationChanged");
        super.onConfigurationChanged(config);

        vuforiaAppSession.onConfigurationChanged();
    }

    @Override
    protected void onPause() {
        Log.d(LOGTAG, "onPause");
        super.onPause();

        if (mGlView != null) {
            mGlView.setVisibility(View.INVISIBLE);
            mGlView.onPause();
        }

        if (mFlashOptionView != null && mFlash) {
            setMenuToggle(mFlashOptionView, false);
        }

        try {
            vuforiaAppSession.pauseAR();
        } catch (ARApplicationException e) {
            Log.e(LOGTAG, e.getString());
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(LOGTAG, "onDestroy");
        super.onDestroy();

        try {
            vuforiaAppSession.stopAR();
        } catch (ARApplicationException e) {
            Log.e(LOGTAG, e.getString());
        }

        mTextures.clear();
        mTextures = null;

        System.gc();
    }

    private void initApplicationAR() {
        int depthSize = 16;
        int stencilSize = 0;
        boolean translucent = Vuforia.requiresAlpha();

        mGlView = new ARApplicationGLView(this);
        mGlView.init(translucent, depthSize, stencilSize);

        mRenderer = new Object3DRenderer(this, vuforiaAppSession);
        mRenderer.setTextures(mTextures);
        mGlView.setRenderer(mRenderer);
    }

    private void initCameraLayout() {
        mUILayout = (RelativeLayout) View.inflate(this, R.layout.camera_overlay,
                null);

        mUILayout.setVisibility(View.VISIBLE);
        mUILayout.setBackgroundColor(Color.BLACK);

        loadingDialogHandler.mLoadingDialogContainer = mUILayout
                .findViewById(R.id.loading_indicator);
        infoDialogHandler.mInfoDialogContainer = mUILayout
                .findViewById(R.id.info_layout);
        infoDialogHandler.mExpandLayout = mUILayout.findViewById(R.id.expandable_layout);
        tvInfo = mUILayout.findViewById(R.id.info_tv);
        tvInfo.setMovementMethod(new ScrollingMovementMethod());
        infoDialogHandler.mInfoDialogContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!infoDialogHandler.mExpandLayout.isExpanded()) {
                    if (infoTextCode >= 0) {
                        String text = "";
                        try {
                            InputStream inputStream = getAssets().open(
                                    (Object3DRenderer.MESH_OBJECT_3DMODELS[infoTextCode])
                                            .replaceAll(".obj", ".txt"));
                            int size = inputStream.available();
                            byte[] buffer = new byte[size];
                            inputStream.read(buffer);
                            inputStream.close();
                            text = new String(buffer);
                        } catch (IOException e) {
                            Log.d(LOGTAG, "Unable to read from text file!");
                        }
                        tvInfo.setText(text);
                        showInfoText(true);
                    }
                } else {
                    showInfoText(false);
                }
            }
        });

        showProgressIndicator(true);
        addContentView(mUILayout, new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
    }

    public void showProgressIndicator(boolean show) {
        if (loadingDialogHandler != null) {
            if (show) {
                loadingDialogHandler
                        .sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);
            } else {
                loadingDialogHandler
                        .sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);
            }
        }
    }

    public void showInfoLayout(int target) {
        if (infoDialogHandler != null) {
            if (target >= 0 && target < Object3DRenderer.TARGET_NAMES.length) {
                infoDialogHandler
                        .sendEmptyMessage(infoDialogHandler.SHOW_INFO_DIALOG);
                infoTextCode = target;
            } else {
                infoDialogHandler
                        .sendEmptyMessage(infoDialogHandler.HIDE_INFO_DIALOG);
                infoTextCode = -1;
            }
        }
    }

    public void showInfoText(boolean show) {
        if (show) {
            infoDialogHandler
                    .sendEmptyMessage(infoDialogHandler.SHOW_INFO_TEXT);
        } else {
            infoDialogHandler
                    .sendEmptyMessage(infoDialogHandler.HIDE_INFO_TEXT);
        }
    }

    @Override
    public boolean doLoadTrackersData() {
        TrackerManager tManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) tManager
                .getTracker(ObjectTracker.getClassType());
        if (objectTracker == null)
            return false;

        if (mCurrentDataset == null)
            mCurrentDataset = objectTracker.createDataSet();

        if (mCurrentDataset == null)
            return false;

        if (!mCurrentDataset.load(
                mDatasetStrings.get(mCurrentDatasetSelectionIndex),
                STORAGE_TYPE.STORAGE_APPRESOURCE))
            return false;

        if (!objectTracker.activateDataSet(mCurrentDataset))
            return false;

        int numTrackables = mCurrentDataset.getNumTrackables();
        for (int count = 0; count < numTrackables; count++) {
            Trackable trackable = mCurrentDataset.getTrackable(count);
            if (isExtendedTrackingActive()) {
                trackable.startExtendedTracking();
            }

            String name = "Current Dataset : " + trackable.getName();
            trackable.setUserData(name);
            Log.d(LOGTAG, "UserData:Set the following user data " + (String) trackable.getUserData());
        }

        return true;
    }

    @Override
    public boolean doUnloadTrackersData() {
        boolean result = true;

        TrackerManager tManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) tManager
                .getTracker(ObjectTracker.getClassType());
        if (objectTracker == null)
            return false;

        if (mCurrentDataset != null && mCurrentDataset.isActive()) {
            if (objectTracker.getActiveDataSet(0).equals(mCurrentDataset)
                    && !objectTracker.deactivateDataSet(mCurrentDataset)) {
                result = false;
            } else if (!objectTracker.destroyDataSet(mCurrentDataset)) {
                result = false;
            }

            mCurrentDataset = null;
        }

        return result;
    }

    @Override
    public void onVuforiaResumed() {
        if (mGlView != null) {
            mGlView.setVisibility(View.VISIBLE);
            mGlView.onResume();
        }
    }

    @Override
    public void onVuforiaStarted() {
        mRenderer.updateConfiguration();

        if (mContAutofocus) {
            if (!CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO)) {
                if (!CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO)) {
                    CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_NORMAL);
                }
                setMenuToggle(mFocusOptionView, false);
            } else {
                setMenuToggle(mFocusOptionView, true);
            }
        } else {
            setMenuToggle(mFocusOptionView, false);
        }
    }

    @Override
    public void onInitARDone(ARApplicationException exception) {
        if (exception == null) {
            initApplicationAR();

            mRenderer.setActive(true);
            addContentView(mGlView, new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT));
            mUILayout.bringToFront();
            mUILayout.setBackgroundColor(Color.TRANSPARENT);
            vuforiaAppSession.startAR(CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_DEFAULT);
            mARAppMenu = new ARAppMenu(this, this, "3D Model",
                    mGlView, mUILayout, null);
            setSampleAppMenuSettings();
        } else {
            Log.e(LOGTAG, exception.getString());
            showInitializationErrorMessage(exception.getString());
        }
    }

    public void showInitializationErrorMessage(String message) {
        final String errorMessage = message;
        runOnUiThread(new Runnable() {
            public void run() {
                if (mErrorDialog != null) {
                    mErrorDialog.dismiss();
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(
                        Object3D.this);
                builder
                        .setMessage(errorMessage)
                        .setTitle(getString(R.string.INIT_ERROR))
                        .setCancelable(false)
                        .setIcon(0)
                        .setPositiveButton(getString(R.string.button_OK),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        finish();
                                    }
                                });

                mErrorDialog = builder.create();
                mErrorDialog.show();
            }
        });
    }

    @Override
    public void onVuforiaUpdate(State state) {
        if (mSwitchDatasetAsap) {
            mSwitchDatasetAsap = false;
            TrackerManager tm = TrackerManager.getInstance();
            ObjectTracker ot = (ObjectTracker) tm.getTracker(ObjectTracker
                    .getClassType());
            if (ot == null || mCurrentDataset == null
                    || ot.getActiveDataSet(0) == null) {
                Log.d(LOGTAG, "Failed to swap datasets");
                return;
            }

            doUnloadTrackersData();
            doLoadTrackersData();
        }
    }

    @Override
    public boolean doInitTrackers() {
        boolean result = true;

        TrackerManager tManager = TrackerManager.getInstance();
        Tracker tracker;

        tracker = tManager.initTracker(ObjectTracker.getClassType());
        if (tracker == null) {
            Log.e(
                    LOGTAG,
                    "Tracker not initialized. Tracker already initialized or the camera is already started");
            result = false;
        } else {
            Log.i(LOGTAG, "Tracker successfully initialized");
        }
        return result;
    }

    @Override
    public boolean doStartTrackers() {
        boolean result = true;

        Tracker objectTracker = TrackerManager.getInstance().getTracker(
                ObjectTracker.getClassType());
        if (objectTracker != null)
            objectTracker.start();

        return result;
    }

    @Override
    public boolean doStopTrackers() {
        boolean result = true;

        Tracker objectTracker = TrackerManager.getInstance().getTracker(
                ObjectTracker.getClassType());
        if (objectTracker != null)
            objectTracker.stop();

        return result;
    }

    @Override
    public boolean doDeinitTrackers() {
        boolean result = true;

        TrackerManager tManager = TrackerManager.getInstance();
        tManager.deinitTracker(ObjectTracker.getClassType());

        return result;
    }

    @Override
    public void OnRotation(RotationGestureDetector rotationDetector) {
        float angle = rotationDetector.getAngle();
        mRenderer.setModelRotating(true);
        mRenderer.setCurrentRotation(angle);
        //Log.d("RotationGestureDetector", "Rotation: " + Float.toString(angle));
    }

    @Override
    public void OnStopRotation(RotationGestureDetector rotationDetector) {
        mRenderer.setModelRotating(false);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isRotationActive()) {
            mRotationDetector.onTouchEvent(event);
        }
        if (isScaleActive()) {
            mScaleDetector.onTouchEvent(event);
        }

        if (mARAppMenu != null && mARAppMenu.processEvent(event))
            return true;

        return mGestureDetector.onTouchEvent(event);
    }

    boolean isExtendedTrackingActive() {
        return mExtendedTracking;
    }

    boolean isRotationActive() {
        return mRotation;
    }

    boolean isScaleActive() {
        return mScale;
    }

    final public static int CMD_BACK = -1;
    final public static int CMD_EXTENDED_TRACKING = 1;
    final public static int CMD_AUTOFOCUS = 2;
    final public static int CMD_FLASH = 3;
    final public static int CMD_CAMERA_FRONT = 4;
    final public static int CMD_CAMERA_REAR = 5;
    final public static int CMD_DATASET_START_INDEX = 6;
    final public static int CMD_ROTATION = 7;
    final public static int CMD_GROUND_PLANE = 8;
    final public static int CMD_SCALE = 9;

    private void setSampleAppMenuSettings() {
        ARAppMenuGroup group;

        group = mARAppMenu.addGroup("", false);
        group.addTextItem(getString(R.string.menu_back), -1);

        group = mARAppMenu.addGroup("3D OBJECT MANIPULATION", true);
        group.addSelectionItem(getString(R.string.menu_rotation),
                CMD_ROTATION, false);
        group.addSelectionItem(getString(R.string.menu_ground_plane),
                CMD_GROUND_PLANE, false);
        group.addSelectionItem(getString(R.string.menu_scale),
                CMD_SCALE, false);

        group = mARAppMenu.addGroup("CAMERA", true);
        mFocusOptionView = group.addSelectionItem(getString(R.string.menu_contAutofocus),
                CMD_AUTOFOCUS, mContAutofocus);
        mFlashOptionView = group.addSelectionItem(
                getString(R.string.menu_flash), CMD_FLASH, false);
        mStartDatasetsIndex = CMD_DATASET_START_INDEX;
        mDatasetsNumber = mDatasetStrings.size();

        CameraInfo ci = new CameraInfo();
        boolean deviceHasFrontCamera = false;
        boolean deviceHasBackCamera = false;

        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, ci);
            if (ci.facing == CameraInfo.CAMERA_FACING_FRONT)
                deviceHasFrontCamera = true;
            else if (ci.facing == CameraInfo.CAMERA_FACING_BACK)
                deviceHasBackCamera = true;
        }

        if (deviceHasBackCamera && deviceHasFrontCamera) {
            group.addRadioItem(getString(R.string.menu_camera_front),
                    CMD_CAMERA_FRONT, false);
            group.addRadioItem(getString(R.string.menu_camera_back),
                    CMD_CAMERA_REAR, true);
        }

        mARAppMenu.attachMenu();
    }

    private void setMenuToggle(View view, boolean value) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            ((Switch) view).setChecked(value);
        } else {
            ((CheckBox) view).setChecked(value);
        }
    }

    @Override
    public boolean menuProcess(int command) {

        boolean result = true;

        switch (command) {
            case CMD_BACK:
                finish();
                break;

            case CMD_ROTATION:
                mRotation = !mRotation;
                break;

            case CMD_GROUND_PLANE:
                mRenderer.setGroundPlane(!mRenderer.isGroundPlane());
                break;

            case CMD_SCALE:
                mScale = !mScale;
                break;

            case CMD_FLASH:
                result = CameraDevice.getInstance().setFlashTorchMode(!mFlash);

                if (result) {
                    mFlash = !mFlash;
                } else {
                    showToast(getString(mFlash ? R.string.menu_flash_error_off
                            : R.string.menu_flash_error_on));
                    Log.e(LOGTAG,
                            getString(mFlash ? R.string.menu_flash_error_off
                                    : R.string.menu_flash_error_on));
                }
                break;

            case CMD_AUTOFOCUS:

                if (mContAutofocus) {
                    result = CameraDevice.getInstance().setFocusMode(
                            CameraDevice.FOCUS_MODE.FOCUS_MODE_NORMAL);

                    if (result) {
                        mContAutofocus = false;
                    } else {
                        showToast(getString(R.string.menu_contAutofocus_error_off));
                        Log.e(LOGTAG,
                                getString(R.string.menu_contAutofocus_error_off));
                    }
                } else {
                    result = CameraDevice.getInstance().setFocusMode(
                            CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO);

                    if (result) {
                        mContAutofocus = true;
                    } else {
                        showToast(getString(R.string.menu_contAutofocus_error_on));
                        Log.e(LOGTAG,
                                getString(R.string.menu_contAutofocus_error_on));
                    }
                }

                break;

            case CMD_CAMERA_FRONT:
            case CMD_CAMERA_REAR:

                if (mFlashOptionView != null && mFlash) {
                    setMenuToggle(mFlashOptionView, false);
                }

                vuforiaAppSession.stopCamera();

                vuforiaAppSession
                        .startAR(command == CMD_CAMERA_FRONT ? CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_FRONT
                                : CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_BACK);

                break;

            case CMD_EXTENDED_TRACKING:
                for (int tIdx = 0; tIdx < mCurrentDataset.getNumTrackables(); tIdx++) {
                    Trackable trackable = mCurrentDataset.getTrackable(tIdx);

                    if (!mExtendedTracking) {
                        if (!trackable.startExtendedTracking()) {
                            Log.e(LOGTAG,
                                    "Failed to start extended tracking target");
                            result = false;
                        } else {
                            Log.d(LOGTAG,
                                    "Successfully started extended tracking target");
                        }
                    } else {
                        if (!trackable.stopExtendedTracking()) {
                            Log.e(LOGTAG,
                                    "Failed to stop extended tracking target");
                            result = false;
                        } else {
                            Log.d(LOGTAG,
                                    "Successfully started extended tracking target");
                        }
                    }
                }

                if (result)
                    mExtendedTracking = !mExtendedTracking;

                break;

            default:
                if (command >= mStartDatasetsIndex
                        && command < mStartDatasetsIndex + mDatasetsNumber) {
                    mSwitchDatasetAsap = true;
                    mCurrentDatasetSelectionIndex = command
                            - mStartDatasetsIndex;
                }
                break;
        }

        return result;
    }

    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
