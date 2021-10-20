
package com.gmoutzou.musar.app.video.VideoPlayback;

import java.util.concurrent.locks.ReentrantLock;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.AssetFileDescriptor;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

public class VideoPlayerHelper implements OnPreparedListener,
    OnBufferingUpdateListener, OnCompletionListener, OnErrorListener
{
    private static final String LOGTAG = "VideoPlayerHelper";
    
    public static final int CURRENT_POSITION = -1;
    private MediaPlayer mMediaPlayer = null;
    private MEDIA_TYPE mVideoType = MEDIA_TYPE.UNKNOWN;
    private SurfaceTexture mSurfaceTexture = null;
    private int mCurrentBufferingPercentage = 0;
    private String mMovieName = "";
    private byte mTextureID = 0;
    Intent mPlayerHelperActivityIntent = null;
    private Activity mParentActivity = null;
    private MEDIA_STATE mCurrentState = MEDIA_STATE.NOT_READY;
    private boolean mShouldPlayImmediately = false;
    private int mSeekPosition = CURRENT_POSITION;
    private ReentrantLock mMediaPlayerLock = null;
    private ReentrantLock mSurfaceTextureLock = null;

    public enum MEDIA_STATE
    {
        REACHED_END     (0),
        PAUSED          (1),
        STOPPED         (2),
        PLAYING         (3),
        READY           (4),
        NOT_READY       (5),
        ERROR           (6);

        private int type;
        
        
        MEDIA_STATE(int i)
        {
            this.type = i;
        }
        
        
        public int getNumericType()
        {
            return type;
        }
    }

    public enum MEDIA_TYPE
    {
        ON_TEXTURE              (0),
        FULLSCREEN              (1),
        ON_TEXTURE_FULLSCREEN   (2),
        UNKNOWN                 (3);

        private int type;
        
        
        MEDIA_TYPE(int i)
        {
            this.type = i;
        }
        
        
        public int getNumericType()
        {
            return type;
        }
    }

    public boolean init()
    {
        mMediaPlayerLock = new ReentrantLock();
        mSurfaceTextureLock = new ReentrantLock();
        
        return true;
    }

    public boolean deinit()
    {
        unload();
        
        mSurfaceTextureLock.lock();
        mSurfaceTexture = null;
        mSurfaceTextureLock.unlock();
        
        return true;
    }

    @SuppressLint("NewApi")
    public boolean load(String filename, MEDIA_TYPE requestedType,
        boolean playOnTextureImmediately, int seekPosition)
    {
        boolean canBeOnTexture = false;
        boolean canBeFullscreen = false;
        
        boolean result = false;
        mMediaPlayerLock.lock();
        mSurfaceTextureLock.lock();

        if ((mCurrentState == MEDIA_STATE.READY) || (mMediaPlayer != null))
        {
            Log.d(LOGTAG, "Already loaded");
        } else
        {
            if (((requestedType == MEDIA_TYPE.ON_TEXTURE) ||
                (requestedType == MEDIA_TYPE.ON_TEXTURE_FULLSCREEN)) &&
                
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH))
            {
                if (mSurfaceTexture == null)
                {
                    Log.d(LOGTAG,
                        "Can't load file to ON_TEXTURE because the Surface Texture is not ready");
                } else
                {
                    try
                    {
                        mMediaPlayer = new MediaPlayer();

                        AssetFileDescriptor afd = mParentActivity.getAssets()
                            .openFd(filename);
                        mMediaPlayer.setDataSource(afd.getFileDescriptor(),
                            afd.getStartOffset(), afd.getLength());
                        afd.close();

                        //mMediaPlayer.setDataSource("/sdcard/myMovie.m4v");
                        
                        mMediaPlayer.setOnPreparedListener(this);
                        mMediaPlayer.setOnBufferingUpdateListener(this);
                        mMediaPlayer.setOnCompletionListener(this);
                        mMediaPlayer.setOnErrorListener(this);
                        mMediaPlayer
                            .setAudioStreamType(AudioManager.STREAM_MUSIC);
                        mMediaPlayer.setSurface(new Surface(mSurfaceTexture));
                        canBeOnTexture = true;
                        mShouldPlayImmediately = playOnTextureImmediately;
                        mMediaPlayer.prepareAsync();
                    } catch (Exception e)
                    {
                        Log.e(LOGTAG, "Error while creating the MediaPlayer: "
                            + e.toString());
                        
                        mCurrentState = MEDIA_STATE.ERROR;
                        mMediaPlayerLock.unlock();
                        mSurfaceTextureLock.unlock();
                        return false;
                    }
                }
            } else
            {
                try
                {
                    AssetFileDescriptor afd = mParentActivity.getAssets()
                        .openFd(filename);
                    afd.close();
                } catch (Exception e)
                {
                    Log.d(LOGTAG, "File does not exist");
                    mCurrentState = MEDIA_STATE.ERROR;
                    mMediaPlayerLock.unlock();
                    mSurfaceTextureLock.unlock();
                    return false;
                }
            }

            if ((requestedType == MEDIA_TYPE.FULLSCREEN)
                || (requestedType == MEDIA_TYPE.ON_TEXTURE_FULLSCREEN))
            {
                mPlayerHelperActivityIntent = new Intent(mParentActivity,
                    FullscreenPlayback.class);
                mPlayerHelperActivityIntent
                    .setAction(android.content.Intent.ACTION_VIEW);
                canBeFullscreen = true;
            }

            mMovieName = filename;
            mSeekPosition = seekPosition;
            
            if (canBeFullscreen && canBeOnTexture)
                mVideoType = MEDIA_TYPE.ON_TEXTURE_FULLSCREEN;
            else if (canBeFullscreen)
            {
                mVideoType = MEDIA_TYPE.FULLSCREEN;
                mCurrentState = MEDIA_STATE.READY;
            }
            else if (canBeOnTexture)
                mVideoType = MEDIA_TYPE.ON_TEXTURE;
            else
                mVideoType = MEDIA_TYPE.UNKNOWN;
            
            result = true;
        }
        
        mSurfaceTextureLock.unlock();
        mMediaPlayerLock.unlock();
        
        return result;
    }

    public boolean unload()
    {
        mMediaPlayerLock.lock();
        if (mMediaPlayer != null)
        {
            try
            {
                mMediaPlayer.stop();
            } catch (Exception e)
            {
                mMediaPlayerLock.unlock();
                Log.e(LOGTAG, "Could not start playback");
            }
            
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        mMediaPlayerLock.unlock();
        
        mCurrentState = MEDIA_STATE.NOT_READY;
        mVideoType = MEDIA_TYPE.UNKNOWN;
        return true;
    }

    public boolean isPlayableOnTexture()
    {
        if ((mVideoType == MEDIA_TYPE.ON_TEXTURE)
            || (mVideoType == MEDIA_TYPE.ON_TEXTURE_FULLSCREEN))
            return true;
        
        return false;
    }

    public boolean isPlayableFullscreen()
    {
        if ((mVideoType == MEDIA_TYPE.FULLSCREEN)
            || (mVideoType == MEDIA_TYPE.ON_TEXTURE_FULLSCREEN))
            return true;
        
        return false;
    }

    MEDIA_STATE getStatus()
    {
        return mCurrentState;
    }

    public int getVideoWidth()
    {
        if (!isPlayableOnTexture())
        {
            //Log.d( LOGTAG, "Cannot get the video width if it is not playable on texture");
            return -1;
        }
        
        if ((mCurrentState == MEDIA_STATE.NOT_READY)
            || (mCurrentState == MEDIA_STATE.ERROR))
        {
            //Log.d( LOGTAG, "Cannot get the video width if it is not ready");
            return -1;
        }
        
        int result = -1;
        mMediaPlayerLock.lock();
        if (mMediaPlayer != null)
            result = mMediaPlayer.getVideoWidth();
        mMediaPlayerLock.unlock();
        
        return result;
    }

    public int getVideoHeight()
    {
        if (!isPlayableOnTexture())
        {
            //Log.d( LOGTAG,"Cannot get the video height if it is not playable on texture");
            return -1;
        }
        
        if ((mCurrentState == MEDIA_STATE.NOT_READY)
            || (mCurrentState == MEDIA_STATE.ERROR))
        {
            //Log.d( LOGTAG, "Cannot get the video height if it is not ready");
            return -1;
        }
        
        int result = -1;
        mMediaPlayerLock.lock();
        if (mMediaPlayer != null)
            result = mMediaPlayer.getVideoHeight();
        mMediaPlayerLock.unlock();
        
        return result;
    }

    public float getLength()
    {
        if (!isPlayableOnTexture())
        {
            //Log.d( LOGTAG, "Cannot get the video length if it is not playable on texture");
            return -1;
        }
        
        if ((mCurrentState == MEDIA_STATE.NOT_READY)
            || (mCurrentState == MEDIA_STATE.ERROR))
        {
            //Log.d( LOGTAG, "Cannot get the video length if it is not ready");
            return -1;
        }
        
        int result = -1;
        mMediaPlayerLock.lock();
        if (mMediaPlayer != null)
            result = mMediaPlayer.getDuration() / 1000;
        mMediaPlayerLock.unlock();
        
        return result;
    }

    public boolean play(boolean fullScreen, int seekPosition)
    {
        if (fullScreen)
        {
            if (!isPlayableFullscreen())
            {
                Log.d(LOGTAG, "Cannot play this video fullscreen, it was not requested on load");
                return false;
            }
            
            if (isPlayableOnTexture())
            {
                mMediaPlayerLock.lock();
                
                if (mMediaPlayer == null)
                {
                    mMediaPlayerLock.unlock();
                    return false;
                }
                
                mPlayerHelperActivityIntent.putExtra(
                    "shouldPlayImmediately", true);
                
                try
                {
                    mMediaPlayer.pause();
                } catch (Exception e)
                {
                    mMediaPlayerLock.unlock();
                    Log.e(LOGTAG, "Could not pause playback");
                }
                
                if (seekPosition != CURRENT_POSITION)
                    mPlayerHelperActivityIntent.putExtra("currentSeekPosition",
                        seekPosition);
                else
                    mPlayerHelperActivityIntent.putExtra("currentSeekPosition",
                        mMediaPlayer.getCurrentPosition());
                
                mMediaPlayerLock.unlock();
            } else
            {
                mPlayerHelperActivityIntent.putExtra("currentSeekPosition", 0);
                mPlayerHelperActivityIntent.putExtra("shouldPlayImmediately",
                    true);
                
                if (seekPosition != CURRENT_POSITION)
                    mPlayerHelperActivityIntent.putExtra("currentSeekPosition",
                        seekPosition);
                else
                    mPlayerHelperActivityIntent.putExtra("currentSeekPosition",
                        0);
            }

            mPlayerHelperActivityIntent.putExtra("requestedOrientation",
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            mPlayerHelperActivityIntent.putExtra("movieName", mMovieName);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                mParentActivity.startActivity(mPlayerHelperActivityIntent);
            else
                mParentActivity.startActivityForResult(mPlayerHelperActivityIntent, 1);
            return true;
        } else
        {
            if (!isPlayableOnTexture())
            {
                Log.d(
                    LOGTAG,
                    "Cannot play this video on texture, it was either not requested on load or is not supported on this plattform");
                return false;
            }
            
            if ((mCurrentState == MEDIA_STATE.NOT_READY)
                || (mCurrentState == MEDIA_STATE.ERROR))
            {
                Log.d(LOGTAG, "Cannot play this video if it is not ready");
                return false;
            }
            
            mMediaPlayerLock.lock();
            if (seekPosition != CURRENT_POSITION)
            {
                try
                {
                    mMediaPlayer.seekTo(seekPosition);
                } catch (Exception e)
                {
                    mMediaPlayerLock.unlock();
                    Log.e(LOGTAG, "Could not seek to position");
                }
            } else
            {
                if (mCurrentState == MEDIA_STATE.REACHED_END)
                {
                    try
                    {
                        mMediaPlayer.seekTo(0);
                    } catch (Exception e)
                    {
                        mMediaPlayerLock.unlock();
                        Log.e(LOGTAG, "Could not seek to position");
                    }
                }
            }

            try
            {
                mMediaPlayer.start();
            } catch (Exception e)
            {
                mMediaPlayerLock.unlock();
                Log.e(LOGTAG, "Could not start playback");
            }
            mCurrentState = MEDIA_STATE.PLAYING;
            
            mMediaPlayerLock.unlock();
            
            return true;
        }
    }

    public boolean pause()
    {
        if (!isPlayableOnTexture())
        {
            //Log.d( LOGTAG, "Cannot pause this video since it is not on texture");
            return false;
        }
        
        if ((mCurrentState == MEDIA_STATE.NOT_READY)
            || (mCurrentState == MEDIA_STATE.ERROR))
        {
            //Log.d( LOGTAG, "Cannot pause this video if it is not ready");
            return false;
        }
        
        boolean result = false;
        
        mMediaPlayerLock.lock();
        if (mMediaPlayer != null)
        {
            if (mMediaPlayer.isPlaying())
            {
                try
                {
                    mMediaPlayer.pause();
                } catch (Exception e)
                {
                    mMediaPlayerLock.unlock();
                    Log.e(LOGTAG, "Could not pause playback");
                }
                mCurrentState = MEDIA_STATE.PAUSED;
                result = true;
            }
        }
        mMediaPlayerLock.unlock();
        
        return result;
    }

    public boolean stop()
    {
        if (!isPlayableOnTexture())
        {
            Log.d( LOGTAG, "Cannot stop this video since it is not on texture");
            return false;
        }
        
        if ((mCurrentState == MEDIA_STATE.NOT_READY)
            || (mCurrentState == MEDIA_STATE.ERROR))
        {
            //Log.d( LOGTAG, "Cannot stop this video if it is not ready");
            return false;
        }
        
        boolean result = false;
        
        mMediaPlayerLock.lock();
        if (mMediaPlayer != null)
        {
            mCurrentState = MEDIA_STATE.STOPPED;
            try
            {
                mMediaPlayer.stop();
            } catch (Exception e)
            {
                mMediaPlayerLock.unlock();
                Log.e(LOGTAG, "Could not stop playback");
            }
            
            result = true;
        }
        mMediaPlayerLock.unlock();
        
        return result;
    }

    @SuppressLint("NewApi")
    public byte updateVideoData()
    {
        if (!isPlayableOnTexture())
        {
            //Log.d( LOGTAG, "Cannot update the data of this video since it is not on texture");
            return -1;
        }
        
        byte result = -1;
        
        mSurfaceTextureLock.lock();
        if (mSurfaceTexture != null)
        {
            if (mCurrentState == MEDIA_STATE.PLAYING)
                mSurfaceTexture.updateTexImage();
            
            result = mTextureID;
        }
        mSurfaceTextureLock.unlock();
        
        return result;
    }

    public boolean seekTo(int position)
    {
        if (!isPlayableOnTexture())
        {
            //Log.d( LOGTAG, "Cannot seek-to on this video since it is not on texture");
            return false;
        }
        
        if ((mCurrentState == MEDIA_STATE.NOT_READY)
            || (mCurrentState == MEDIA_STATE.ERROR))
        {
            //Log.d( LOGTAG, "Cannot seek-to on this video if it is not ready");
            return false;
        }
        
        boolean result = false;
        mMediaPlayerLock.lock();
        if (mMediaPlayer != null)
        {
            try
            {
                mMediaPlayer.seekTo(position);
            } catch (Exception e)
            {
                mMediaPlayerLock.unlock();
                Log.e(LOGTAG, "Could not seek to position");
            }
            result = true;
        }
        mMediaPlayerLock.unlock();
        
        return result;
    }

    public int getCurrentPosition()
    {
        if (!isPlayableOnTexture())
        {
            //Log.d( LOGTAG, "Cannot get the current playback position of this video since it is not on texture");
            return -1;
        }
        
        if ((mCurrentState == MEDIA_STATE.NOT_READY)
            || (mCurrentState == MEDIA_STATE.ERROR))
        {
            //Log.d( LOGTAG, "Cannot get the current playback position of this video if it is not ready");
            return -1;
        }
        
        int result = -1;
        mMediaPlayerLock.lock();
        if (mMediaPlayer != null)
            result = mMediaPlayer.getCurrentPosition();
        mMediaPlayerLock.unlock();
        
        return result;
    }

    public boolean setVolume(float value)
    {
        if (!isPlayableOnTexture())
        {
            //Log.d( LOGTAG, "Cannot set the volume of this video since it is not on texture");
            return false;
        }
        
        if ((mCurrentState == MEDIA_STATE.NOT_READY)
            || (mCurrentState == MEDIA_STATE.ERROR))
        {
            //Log.d( LOGTAG, "Cannot set the volume of this video if it is not ready");
            return false;
        }
        
        boolean result = false;
        mMediaPlayerLock.lock();
        if (mMediaPlayer != null)
        {
            mMediaPlayer.setVolume(value, value);
            result = true;
        }
        mMediaPlayerLock.unlock();
        
        return result;
    }

    public int getCurrentBufferingPercentage()
    {
        return mCurrentBufferingPercentage;
    }

    public void onBufferingUpdate(MediaPlayer arg0, int arg1)
    {
        //Log.d( LOGTAG, "onBufferingUpdate " + arg1);
        
        mMediaPlayerLock.lock();
        if (mMediaPlayer != null)
        {
            if (arg0 == mMediaPlayer)
                mCurrentBufferingPercentage = arg1;
        }
        mMediaPlayerLock.unlock();
    }

    public void setActivity(Activity newActivity)
    {
        mParentActivity = newActivity;
    }

    public void onCompletion(MediaPlayer arg0)
    {
        mCurrentState = MEDIA_STATE.REACHED_END;
    }

    @SuppressLint("NewApi")
    public boolean setupSurfaceTexture(int TextureID)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        {
            mSurfaceTextureLock.lock();
            mSurfaceTexture = new SurfaceTexture(TextureID);
            mTextureID = (byte) TextureID;
            mSurfaceTextureLock.unlock();
            
            return true;
        } else
            return false;
    }
    
    
    @SuppressLint("NewApi")
    public void getSurfaceTextureTransformMatrix(float[] mtx)
    {
        mSurfaceTextureLock.lock();
        if (mSurfaceTexture != null)
            mSurfaceTexture.getTransformMatrix(mtx);
        mSurfaceTextureLock.unlock();
    }

    public void onPrepared(MediaPlayer mediaplayer)
    {
        mCurrentState = MEDIA_STATE.READY;

        if (mShouldPlayImmediately)
            play(false, mSeekPosition);
        
        mSeekPosition = 0;
    }
    
    
    public boolean onError(MediaPlayer mp, int what, int extra)
    {
        
        if (mp == mMediaPlayer)
        {
            String errorDescription;
            
            switch (what)
            {
                case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                    errorDescription = "The video is streamed and its container is not valid for progressive playback";
                    break;
                case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                    errorDescription = "Media server died";
                    break;
                case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                    errorDescription = "Unspecified media player error";
                    break;
                default:
                    errorDescription = "Unknown error " + what;
            }
            
            Log.e(LOGTAG,
                "Error while opening the file. Unloading the media player ("
                    + errorDescription + ", " + extra + ")");
            
            unload();
            
            mCurrentState = MEDIA_STATE.ERROR;
            
            return true;
        }

        return false;
    }
}
