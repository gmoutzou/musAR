
package com.gmoutzou.musar.app.model3d.ui.ARAppMenu;

import com.gmoutzou.musar.R;

import java.util.ArrayList;

import android.app.Activity;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ARAppMenu
{
    
    protected static final String SwipeSettingsInterface = null;
    private GestureListener mGestureListener;
    private GestureDetector mGestureDetector;
    private ARAppMenuAnimator mMenuAnimator;
    private Activity mActivity;
    private ARAppMenuInterface mMenuInterface;
    private GLSurfaceView mMovableView;
    private ARAppMenuView mParentMenuView;
    private LinearLayout mMovableListView;
    private ArrayList<ARAppMenuGroup> mSettingsItems = new ArrayList<ARAppMenuGroup>();
    
    private ArrayList<View> mAdditionalViews;
    private float mInitialAdditionalViewsX[];
    private int mScreenWidth;
    private int mListViewWidth = 0;

    boolean mSwipingMenu = false;

    boolean mStartMenuDisplaying = false;
    
    float mGingerbreadMenuClipping = 0;
    
    private static float SETTINGS_MENU_SCREEN_PERCENTAGE = .80f;
    private static float SETTINGS_MENU_SCREEN_MIN_PERCENTAGE_TO_SHOW = .1f;
    
    boolean mIsBelowICS = Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH;

    public ARAppMenu(ARAppMenuInterface menuInterface,
                     Activity activity, String menuTitle, GLSurfaceView movableView,
                     RelativeLayout parentView, ArrayList<View> additionalViewsToHide)
    {
        mMenuInterface = menuInterface;
        mActivity = activity;
        mMovableView = movableView;
        mAdditionalViews = additionalViewsToHide;
        
        LayoutInflater inflater = LayoutInflater.from(mActivity);
        mParentMenuView = (ARAppMenuView) inflater.inflate(
            R.layout.ar_app_menu_layer, null, false);
        parentView.addView(mParentMenuView);
        
        mMovableListView = (LinearLayout) mParentMenuView
            .findViewById(R.id.settings_menu);
        mMovableListView.setBackgroundColor(Color.WHITE);
        
        TextView title = (TextView) mMovableListView
            .findViewById(R.id.settings_menu_title);
        title.setText(menuTitle);
        
        mMovableView.setVisibility(View.VISIBLE);
        
        if (mAdditionalViews != null && mAdditionalViews.size() > 0)
        {
            mInitialAdditionalViewsX = new float[mAdditionalViews.size()];
        }
        
        mGestureListener = new GestureListener();
        mGestureDetector = new GestureDetector(mActivity, mGestureListener);
        
        if (!mIsBelowICS)
            mMenuAnimator = new ARAppMenuAnimator(this);
        
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenWidth = metrics.widthPixels;

        ViewTreeObserver vto = mMovableView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener()
        {
            @SuppressWarnings("deprecation")
            @Override
            public void onGlobalLayout()
            {
                int menuWidth = Math.min(mMovableView.getWidth(), mMovableView.getHeight());
                mListViewWidth = (int) (menuWidth * SETTINGS_MENU_SCREEN_PERCENTAGE);
                
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                    mListViewWidth, RelativeLayout.LayoutParams.MATCH_PARENT);
                
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                mParentMenuView.setLayoutParams(params);
                
                setMenuDisplaying(false);
                mGestureListener.setMaxSwipe(mListViewWidth);
                
                LinearLayout.LayoutParams groupParams = new LinearLayout.LayoutParams(
                    mListViewWidth, LinearLayout.LayoutParams.WRAP_CONTENT);
                for (ARAppMenuGroup group : mSettingsItems)
                {
                    group.getMenuLayout().setLayoutParams(groupParams);
                }
                
                mMovableView.getViewTreeObserver()
                    .removeGlobalOnLayoutListener(this);
                
            }
        });
        
    }
    
    public boolean processEvent(MotionEvent event)
    {
        boolean result = false;
        result = mGestureDetector.onTouchEvent(event);
        
        if (event.getAction() == MotionEvent.ACTION_UP && !result)
        {
            setSwipingMenu(false);

            if (!isMenuDisplaying()
                || getViewX(mMovableView) < (mScreenWidth * SETTINGS_MENU_SCREEN_PERCENTAGE))
            {
                if (isMenuDisplaying()
                    || getViewX(mMovableView) < (mScreenWidth * SETTINGS_MENU_SCREEN_MIN_PERCENTAGE_TO_SHOW))
                {
                    hideMenu();
                } else
                {
                    showMenu();
                }
            }
        }
        
        return result;
    }
    
    private void startViewsAnimation(boolean display)
    {
        float targetX = display ? mGestureListener.getMaxSwipe() : 0;
        
        mMenuAnimator.setStartEndX(getViewX(mMovableView), targetX);
        mMenuAnimator.start();
        
        if (mAdditionalViews != null)
        {
            for (int i = 0; i < mAdditionalViews.size(); i++)
            {
                setViewX(mAdditionalViews.get(i), mInitialAdditionalViewsX[i]
                    + targetX);
            }
        }
    }
    
    
    public void setSwipingMenu(boolean isSwiping)
    {
        mSwipingMenu = isSwiping;
    }
    
    
    public boolean isMenuDisplaying()
    {
        return mStartMenuDisplaying;
    }
    
    
    public void setMenuDisplaying(boolean isMenuDisplaying)
    {
        mParentMenuView.setFocusable(isMenuDisplaying);
        mParentMenuView.setFocusableInTouchMode(isMenuDisplaying);
        mParentMenuView.setClickable(isMenuDisplaying);
        mParentMenuView.setEnabled(isMenuDisplaying);
        
        mStartMenuDisplaying = isMenuDisplaying;
        
    }

    public void hide()
    {
        setViewX(mMovableView, 0);
        
        mParentMenuView.setHorizontalClipping(0);
        mParentMenuView.setVisibility(View.GONE);
        
        if (mAdditionalViews != null && !mIsBelowICS)
        {
            for (int i = 0; i < mAdditionalViews.size(); i++)
            {
                setViewX(mAdditionalViews.get(i), mInitialAdditionalViewsX[i]);
            }
        }
        
    }
    
    private void setViewX(View view, float x)
    {
        if (!mIsBelowICS)
            view.setX(x);
        else
            mGingerbreadMenuClipping = x;
    }
    
    private float getViewX(View view)
    {
        float x = 0;
        if (!mIsBelowICS)
            x = view.getX();
        else
            x = mGingerbreadMenuClipping;
        
        return x;
    }
    
    public void showMenu()
    {
        if (!mIsBelowICS)
        {
            startViewsAnimation(true);
        } else
        {
            setAnimationX(mGestureListener.getMaxSwipe());
            setMenuDisplaying(true);
        }
    }
    
    public void hideMenu()
    {
        if (!mIsBelowICS)
        {
            if (!mMenuAnimator.isRunning())
            {
                startViewsAnimation(false);
                setMenuDisplaying(false);
            }
        } else
        {
            hide();
            setMenuDisplaying(false);
        }
    }
    
    public ARAppMenuGroup addGroup(String string, boolean hasTitle)
    {
        ARAppMenuGroup newGroup = new ARAppMenuGroup(mMenuInterface,
            mActivity, this, hasTitle, string, 700);
        mSettingsItems.add(newGroup);
        return mSettingsItems.get(mSettingsItems.size() - 1);
    }

    public void attachMenu()
    {
        
        for (ARAppMenuGroup group : mSettingsItems)
        {
            mMovableListView.addView(group.getMenuLayout());
        }
        
        View newView = new View(mActivity);
        newView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT));
        newView.setBackgroundColor(Color.parseColor("#000000"));
        mMovableListView.addView(newView);
        hide();
        setMenuDisplaying(false);
        
    }

    public void setAnimationX(float animtationX)
    {
        mParentMenuView.setVisibility(View.VISIBLE);
        setViewX(mMovableView, animtationX);
        
        mParentMenuView.setHorizontalClipping((int) animtationX);
        
        if (mAdditionalViews != null)
        {
            for (int i = 0; i < mAdditionalViews.size(); i++)
            {
                setViewX(mAdditionalViews.get(i), mInitialAdditionalViewsX[i]
                    + animtationX);
            }
        }
    }
    
    public void setDockMenu(boolean isDocked)
    {
        setMenuDisplaying(isDocked);
        if (!isDocked)
            hideMenu();
    }

    private class GestureListener extends
        GestureDetector.SimpleOnGestureListener
    {
        int DISTANCE_TRESHOLD = 10;
        int VELOCITY_TRESHOLD = 2000;
        float mMaxXSwipe;

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
            float distanceX, float distanceY)
        {
            if (Math.abs(distanceX) > DISTANCE_TRESHOLD && !mSwipingMenu)
            {
                mSwipingMenu = true;
                mParentMenuView.setVisibility(View.VISIBLE);
                
                if (mAdditionalViews != null && !mIsBelowICS
                    && !mStartMenuDisplaying)
                {
                    for (int i = 0; i < mAdditionalViews.size(); i++)
                    {
                        mInitialAdditionalViewsX[i] = getViewX(mAdditionalViews
                            .get(i));
                    }
                }
            }
            
            if (mSwipingMenu && mMovableView != null
                && (getViewX(mMovableView) - distanceX > 0))
            {
                float deltaX = Math.min(mMaxXSwipe, getViewX(mMovableView)
                    - distanceX);
                
                setViewX(mMovableView, deltaX);
                
                mParentMenuView.setHorizontalClipping((int) deltaX);
                
                if (mAdditionalViews != null && !mIsBelowICS)
                {
                    for (int i = 0; i < mAdditionalViews.size(); i++)
                    {
                        setViewX(mAdditionalViews.get(i),
                            mInitialAdditionalViewsX[i] + deltaX);
                    }
                }
                
            }
            
            if (mMaxXSwipe <= getViewX(mMovableView))
            {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable(){

                    @Override
                    public void run()
                    {
                        showMenu();
                    }
                    
                }, 100L);
                
            }
            
            return false;
        }
        
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
            float velocityY)
        {
            if (velocityX > VELOCITY_TRESHOLD && !isMenuDisplaying())
            {
                showMenu();
            }
            return false;
        }
        
        @Override
        public boolean onSingleTapUp(MotionEvent e)
        {
            boolean consumeTapUp = isMenuDisplaying();
            hideMenu();
            
            return consumeTapUp;
        }
        
        @Override
        public boolean onDoubleTap(MotionEvent e)
        {
            if (!isMenuDisplaying())
            {
                if (!mIsBelowICS)
                {
                    startViewsAnimation(true);
                } else
                {
                    setAnimationX(mMaxXSwipe);
                    setMenuDisplaying(true);
                }
            }
            return true;
        }

        public void setMaxSwipe(float maxXSwipe)
        {
            mMaxXSwipe = maxXSwipe;
            if (!mIsBelowICS)
            {
                mMenuAnimator.setMaxX(mMaxXSwipe);
                mMenuAnimator.setStartEndX(0.0f, mMaxXSwipe);
            }
        }
        
        public float getMaxSwipe()
        {
            return mMaxXSwipe;
        }
    }
}
