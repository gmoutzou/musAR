
package com.gmoutzou.musar.app.model3d.ui.ARAppMenu;

import android.animation.Animator;
import android.animation.ValueAnimator;

public class ARAppMenuAnimator extends ValueAnimator implements
    ValueAnimator.AnimatorUpdateListener, ValueAnimator.AnimatorListener
{
    
    private static long MENU_ANIMATION_DURATION = 300;
    private ARAppMenu mARAppMenu;
    private float mMaxX;
    private float mEndX;
    
    public ARAppMenuAnimator(ARAppMenu menu)
    {
        mARAppMenu = menu;
        setDuration(MENU_ANIMATION_DURATION);
        addUpdateListener(this);
        addListener(this);
    }
    
    @Override
    public void onAnimationUpdate(ValueAnimator animation)
    {
        Float f = (Float) animation.getAnimatedValue();
        mARAppMenu.setAnimationX(f.floatValue());
    }
    
    @Override
    public void onAnimationCancel(Animator animation)
    {
    }
    
    @Override
    public void onAnimationEnd(Animator animation)
    {
        mARAppMenu.setDockMenu(mEndX == mMaxX);
        if (mEndX == 0)
            mARAppMenu.hide();
    }
    
    @Override
    public void onAnimationRepeat(Animator animation)
    {
    }
    
    @Override
    public void onAnimationStart(Animator animation)
    {
    }
    
    public void setStartEndX(float start, float end)
    {
        mEndX = end;
        setFloatValues(start, end);
        setDuration((int) (MENU_ANIMATION_DURATION * (Math.abs(end - start) / mMaxX)));
    }
    
    public void setMaxX(float maxX)
    {
        mMaxX = maxX;
    }
    
}
