
package com.gmoutzou.musar.app.model3d.ARApplication;

import com.vuforia.State;

public interface ARAppRendererControl {

    void renderFrame(State state, float[] projectionMatrix);

}
