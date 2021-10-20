package com.gmoutzou.musar.app.utils;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.view.View;

import com.github.aakira.expandablelayout.ExpandableLayout;

public final class InfoDialogHandler extends Handler
{
    private final WeakReference<Activity> mActivity;
    public static final int HIDE_INFO_DIALOG = 0;
    public static final int SHOW_INFO_DIALOG = 1;
    public static final int HIDE_INFO_TEXT = 10;
    public static final int SHOW_INFO_TEXT = 11;

    public View mInfoDialogContainer;
    public ExpandableLayout mExpandLayout;


    public InfoDialogHandler(Activity activity)
    {
        mActivity = new WeakReference<Activity>(activity);
    }


    public void handleMessage(Message msg)
    {
        Activity imageTargets = mActivity.get();
        if (imageTargets == null)
        {
            return;
        }

        switch (msg.what) {
            case HIDE_INFO_DIALOG:
                if (mInfoDialogContainer.getVisibility() == View.VISIBLE) {
                    mInfoDialogContainer.setVisibility(View.GONE);
                    if (mExpandLayout.isExpanded()) {
                        mExpandLayout.collapse();
                    }
                }
                break;
            case SHOW_INFO_DIALOG:
                if (mInfoDialogContainer.getVisibility() != View.VISIBLE) {
                    mInfoDialogContainer.setVisibility(View.VISIBLE);
                }
                break;
            case HIDE_INFO_TEXT:
                mExpandLayout.collapse();
                break;
            case SHOW_INFO_TEXT:
                mExpandLayout.expand();
                break;
            default:
                break;
        }
    }
}
