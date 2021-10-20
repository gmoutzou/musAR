
package com.gmoutzou.musar.app.model3d.ARApplication;

import com.gmoutzou.musar.R;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.WindowManager;

import com.vuforia.CameraDevice;
import com.vuforia.Device;
import com.vuforia.INIT_ERRORCODE;
import com.vuforia.INIT_FLAGS;
import com.vuforia.State;
import com.vuforia.Vuforia;
import com.vuforia.Vuforia.UpdateCallbackInterface;

public class ARApplicationSession implements UpdateCallbackInterface
{
    
    private static final String LOGTAG = "SampleAppSession";

    private Activity mActivity;
    private ARApplicationControl mSessionControl;

    private boolean mStarted = false;
    private boolean mCameraRunning = false;

    private InitVuforiaTask mInitVuforiaTask;
    private InitTrackerTask mInitTrackerTask;
    private LoadTrackerTask mLoadTrackerTask;
    private StartVuforiaTask mStartVuforiaTask;
    private ResumeVuforiaTask mResumeVuforiaTask;

    private final Object mLifecycleLock = new Object();

    private int mVuforiaFlags = 0;

    private int mCamera = CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_DEFAULT;
    

    public ARApplicationSession(ARApplicationControl sessionControl)
    {
        mSessionControl = sessionControl;
    }

    public void initAR(Activity activity, int screenOrientation)
    {
        ARApplicationException vuforiaException = null;
        mActivity = activity;
        
        if ((screenOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR)
            && (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO))
            screenOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR;

        OrientationEventListener orientationEventListener = new OrientationEventListener(mActivity) {
            @Override
            public void onOrientationChanged(int i) {
                int activityRotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
                if(mLastRotation != activityRotation)
                {
                    mLastRotation = activityRotation;
                }
            }
            int mLastRotation = -1;
        };
        
        if (orientationEventListener.canDetectOrientation()) {
            orientationEventListener.enable();
        }

        mActivity.setRequestedOrientation(screenOrientation);
        mActivity.getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mVuforiaFlags = INIT_FLAGS.GL_20;

        if (mInitVuforiaTask != null)
        {
            String logMessage = "Cannot initialize SDK twice";
            vuforiaException = new ARApplicationException(
                ARApplicationException.VUFORIA_ALREADY_INITIALIZATED,
                logMessage);
            Log.e(LOGTAG, logMessage);
        }
        
        if (vuforiaException == null)
        {
            try {
                mInitVuforiaTask = new InitVuforiaTask();
                mInitVuforiaTask.execute();
            }
            catch (Exception e)
            {
                String logMessage = "Initializing Vuforia SDK failed";
                vuforiaException = new ARApplicationException(
                    ARApplicationException.INITIALIZATION_FAILURE,
                    logMessage);
                Log.e(LOGTAG, logMessage);
            }
        }

        if (vuforiaException != null)
        {
            mSessionControl.onInitARDone(vuforiaException);
        }
    }

    private void startCameraAndTrackers(int camera) throws ARApplicationException
    {
        String error;
        if(mCameraRunning)
        {
        	error = "Camera already running, unable to open again";
        	Log.e(LOGTAG, error);
            throw new ARApplicationException(
                ARApplicationException.CAMERA_INITIALIZATION_FAILURE, error);
        }
        
        mCamera = camera;
        if (!CameraDevice.getInstance().init(camera))
        {
            error = "Unable to open camera device: " + camera;
            Log.e(LOGTAG, error);
            throw new ARApplicationException(
                ARApplicationException.CAMERA_INITIALIZATION_FAILURE, error);
        }
               
        if (!CameraDevice.getInstance().selectVideoMode(
            CameraDevice.MODE.MODE_DEFAULT))
        {
            error = "Unable to set video mode";
            Log.e(LOGTAG, error);
            throw new ARApplicationException(
                ARApplicationException.CAMERA_INITIALIZATION_FAILURE, error);
        }
        
        if (!CameraDevice.getInstance().start())
        {
            error = "Unable to start camera device: " + camera;
            Log.e(LOGTAG, error);
            throw new ARApplicationException(
                ARApplicationException.CAMERA_INITIALIZATION_FAILURE, error);
        }
        
        mSessionControl.doStartTrackers();
        
        mCameraRunning = true;
    }

    public void startAR(int camera)
    {
        mCamera = camera;
        ARApplicationException vuforiaException = null;

        try {
            mStartVuforiaTask = new StartVuforiaTask();
            mStartVuforiaTask.execute();
        }
        catch (Exception e)
        {
            String logMessage = "Starting Vuforia failed";
            vuforiaException = new ARApplicationException(
                    ARApplicationException.CAMERA_INITIALIZATION_FAILURE,
                    logMessage);
            Log.e(LOGTAG, logMessage);
        }

        if (vuforiaException != null)
        {
            mSessionControl.onInitARDone(vuforiaException);
        }
    }

    public void stopAR() throws ARApplicationException
    {
        if (mInitVuforiaTask != null
            && mInitVuforiaTask.getStatus() != InitVuforiaTask.Status.FINISHED)
        {
            mInitVuforiaTask.cancel(true);
            mInitVuforiaTask = null;
        }
        
        if (mLoadTrackerTask != null
            && mLoadTrackerTask.getStatus() != LoadTrackerTask.Status.FINISHED)
        {
            mLoadTrackerTask.cancel(true);
            mLoadTrackerTask = null;
        }
        
        mInitVuforiaTask = null;
        mLoadTrackerTask = null;
        
        mStarted = false;
        
        stopCamera();

        synchronized (mLifecycleLock)
        {
            
            boolean unloadTrackersResult;
            boolean deinitTrackersResult;

            unloadTrackersResult = mSessionControl.doUnloadTrackersData();

            deinitTrackersResult = mSessionControl.doDeinitTrackers();

            Vuforia.deinit();
            
            if (!unloadTrackersResult)
                throw new ARApplicationException(
                    ARApplicationException.UNLOADING_TRACKERS_FAILURE,
                    "Failed to unload trackers\' data");
            
            if (!deinitTrackersResult)
                throw new ARApplicationException(
                    ARApplicationException.TRACKERS_DEINITIALIZATION_FAILURE,
                    "Failed to deinitialize trackers");
            
        }
    }

    private void resumeAR()
    {
        ARApplicationException vuforiaException = null;

        try {
            mResumeVuforiaTask = new ResumeVuforiaTask();
            mResumeVuforiaTask.execute();
        }
        catch (Exception e)
        {
            String logMessage = "Resuming Vuforia failed";
            vuforiaException = new ARApplicationException(
                    ARApplicationException.INITIALIZATION_FAILURE,
                    logMessage);
            Log.e(LOGTAG, logMessage);
        }

        if (vuforiaException != null)
        {
            mSessionControl.onInitARDone(vuforiaException);
        }
    }

    public void pauseAR() throws ARApplicationException
    {
        if (mStarted)
        {
            stopCamera();
        }
        
        Vuforia.onPause();
    }

    @Override
    public void Vuforia_onUpdate(State s)
    {
        mSessionControl.onVuforiaUpdate(s);
    }

    public void onConfigurationChanged()
    {
        if (mStarted)
        {
            Device.getInstance().setConfigurationChanged();
        }
    }

    public void onResume()
    {
        if (mResumeVuforiaTask == null
                || mResumeVuforiaTask.getStatus() == ResumeVuforiaTask.Status.FINISHED)
        {
            resumeAR();
        }
    }

    public void onPause()
    {
        Vuforia.onPause();
    }

    public void onSurfaceChanged(int width, int height)
    {
        Vuforia.onSurfaceChanged(width, height);
    }

    public void onSurfaceCreated()
    {
        Vuforia.onSurfaceCreated();
    }

    private class InitVuforiaTask extends AsyncTask<Void, Integer, Boolean>
    {
        private int mProgressValue = -1;
        protected Boolean doInBackground(Void... params)
        {
            synchronized (mLifecycleLock)
            {
                Vuforia.setInitParameters(mActivity, mVuforiaFlags, mActivity.getString(R.string.vuforia_key));
                
                do
                {
                    mProgressValue = Vuforia.init();
                    publishProgress(mProgressValue);

                } while (!isCancelled() && mProgressValue >= 0
                    && mProgressValue < 100);

                return (mProgressValue > 0);
            }
        }
        
        
        protected void onProgressUpdate(Integer... values)
        {
        }
        
        
        protected void onPostExecute(Boolean result)
        {
            Log.d(LOGTAG, "InitVuforiaTask.onPostExecute: execution "
                    + (result ? "successful" : "failed"));
            ARApplicationException vuforiaException = null;
            
            if (result)
            {
                try {
                    mInitTrackerTask = new InitTrackerTask();
                    mInitTrackerTask.execute();
                }
                catch (Exception e)
                {
                    String logMessage = "Failed to initialize tracker.";
                    vuforiaException = new ARApplicationException(
                            ARApplicationException.TRACKERS_INITIALIZATION_FAILURE,
                            logMessage);
                    Log.e(LOGTAG, logMessage);
                }
            } else
            {
                String logMessage;

                logMessage = getInitializationErrorString(mProgressValue);

                Log.e(LOGTAG, "InitVuforiaTask.onPostExecute: " + logMessage
                    + " Exiting.");

                vuforiaException = new ARApplicationException(
                    ARApplicationException.INITIALIZATION_FAILURE,
                    logMessage);
            }

            if (vuforiaException != null)
            {
                mSessionControl.onInitARDone(vuforiaException);
            }
        }
    }

    private class ResumeVuforiaTask extends AsyncTask<Void, Void, Void>
    {
        protected Void doInBackground(Void... params)
        {
            synchronized (mLifecycleLock)
            {
                Vuforia.onResume();
            }

            return null;
        }

        protected void onPostExecute(Void result)
        {
            Log.d(LOGTAG, "ResumeVuforiaTask.onPostExecute");

            if (mStarted && !mCameraRunning)
            {
                startAR(mCamera);
                mSessionControl.onVuforiaResumed();
            }
        }
    }

    private class InitTrackerTask extends AsyncTask<Void, Integer, Boolean>
    {
        protected  Boolean doInBackground(Void... params)
        {
            synchronized (mLifecycleLock)
            {
                return mSessionControl.doInitTrackers();
            }
        }

        protected void onPostExecute(Boolean result)
        {

            ARApplicationException vuforiaException = null;
            Log.d(LOGTAG, "InitTrackerTask.onPostExecute: execution "
                + (result ? "successful" : "failed"));

            if (result)
            {
                try {
                    mLoadTrackerTask = new LoadTrackerTask();
                    mLoadTrackerTask.execute();
                }
                catch (Exception e)
                {
                    String logMessage = "Failed to load tracker data.";
                    Log.e(LOGTAG, logMessage);

                    vuforiaException = new ARApplicationException(
                            ARApplicationException.LOADING_TRACKERS_FAILURE,
                            logMessage);
                }
            }
            else
            {
                String logMessage = "Failed to load tracker data.";
                Log.e(LOGTAG, logMessage);
                vuforiaException = new ARApplicationException(
                        ARApplicationException.TRACKERS_INITIALIZATION_FAILURE,
                        logMessage);
            }

            if (vuforiaException != null)
            {
                mSessionControl.onInitARDone(vuforiaException);
            }
        }
    }

    private class LoadTrackerTask extends AsyncTask<Void, Void, Boolean>
    {
        protected Boolean doInBackground(Void... params)
        {
            synchronized (mLifecycleLock)
            {
                return mSessionControl.doLoadTrackersData();
            }
        }
        
        protected void onPostExecute(Boolean result)
        {
            
            ARApplicationException vuforiaException = null;
            
            Log.d(LOGTAG, "LoadTrackerTask.onPostExecute: execution "
                + (result ? "successful" : "failed"));
            
            if (!result)
            {
                String logMessage = "Failed to load tracker data.";
                Log.e(LOGTAG, logMessage);
                vuforiaException = new ARApplicationException(
                    ARApplicationException.LOADING_TRACKERS_FAILURE,
                    logMessage);
            } else
            {
                System.gc();
                
                Vuforia.registerCallback(ARApplicationSession.this);

                mStarted = true;
            }

            mSessionControl.onInitARDone(vuforiaException);
        }
    }

    private class StartVuforiaTask extends AsyncTask<Void, Void, Boolean>
    {
        ARApplicationException vuforiaException = null;
        protected Boolean doInBackground(Void... params)
        {
            synchronized (mLifecycleLock)
            {
                try {
                    startCameraAndTrackers(mCamera);
                }
                catch (ARApplicationException e)
                {
                    Log.e(LOGTAG, "StartVuforiaTask.doInBackground: Could not start AR with exception: " + e);
                    vuforiaException = e;
                }
            }

            return true;
        }

        protected void onPostExecute(Boolean result)
        {
            Log.d(LOGTAG, "StartVuforiaTask.onPostExecute: execution "
                + (result ? "successful" : "failed"));

            mSessionControl.onVuforiaStarted();

            if (vuforiaException != null)
            {
                mSessionControl.onInitARDone(vuforiaException);
            }
        }
    }

    private String getInitializationErrorString(int code)
    {
        if (code == INIT_ERRORCODE.INIT_DEVICE_NOT_SUPPORTED)
            return mActivity.getString(R.string.INIT_ERROR_DEVICE_NOT_SUPPORTED);
        if (code == INIT_ERRORCODE.INIT_NO_CAMERA_ACCESS)
            return mActivity.getString(R.string.INIT_ERROR_NO_CAMERA_ACCESS);
        if (code == INIT_ERRORCODE.INIT_LICENSE_ERROR_MISSING_KEY)
            return mActivity.getString(R.string.INIT_LICENSE_ERROR_MISSING_KEY);
        if (code == INIT_ERRORCODE.INIT_LICENSE_ERROR_INVALID_KEY)
            return mActivity.getString(R.string.INIT_LICENSE_ERROR_INVALID_KEY);
        if (code == INIT_ERRORCODE.INIT_LICENSE_ERROR_NO_NETWORK_TRANSIENT)
            return mActivity.getString(R.string.INIT_LICENSE_ERROR_NO_NETWORK_TRANSIENT);
        if (code == INIT_ERRORCODE.INIT_LICENSE_ERROR_NO_NETWORK_PERMANENT)
            return mActivity.getString(R.string.INIT_LICENSE_ERROR_NO_NETWORK_PERMANENT);
        if (code == INIT_ERRORCODE.INIT_LICENSE_ERROR_CANCELED_KEY)
            return mActivity.getString(R.string.INIT_LICENSE_ERROR_CANCELED_KEY);
        if (code == INIT_ERRORCODE.INIT_LICENSE_ERROR_PRODUCT_TYPE_MISMATCH)
            return mActivity.getString(R.string.INIT_LICENSE_ERROR_PRODUCT_TYPE_MISMATCH);
        else
        {
            return mActivity.getString(R.string.INIT_LICENSE_ERROR_UNKNOWN_ERROR);
        }
    }

    public void stopCamera()
    {
        if (mCameraRunning)
        {
            mSessionControl.doStopTrackers();
            mCameraRunning = false;
            CameraDevice.getInstance().stop();
            CameraDevice.getInstance().deinit();
        }
    }

    private boolean isARRunning()
    {
        return mStarted;
    }
}
