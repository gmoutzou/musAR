
package com.gmoutzou.musar.app.model3d.ui.ARAppMenu;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.LinearLayout;


public class ARAppMenuView extends LinearLayout
{
    int horizontalClipping = 0;

    public ARAppMenuView(Context context)
    {
        super(context);
    }
    
    public ARAppMenuView(Context context, AttributeSet attribute)
    {
        super(context, attribute);
    }
    
    @Override
    public void onDraw(Canvas canvas)
    {
        canvas.clipRect(0, 0, horizontalClipping, canvas.getHeight());
        super.onDraw(canvas);
    }

    public void setHorizontalClipping(int hClipping)
    {
        horizontalClipping = hClipping;
        invalidate();
    }
}
