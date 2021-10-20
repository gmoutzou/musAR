
package com.gmoutzou.musar.app.video.ARApplication;

import com.vuforia.State;

public interface ARApplicationControl
{

    boolean doInitTrackers();

    boolean doLoadTrackersData();

    boolean doStartTrackers();

    boolean doStopTrackers();

    boolean doUnloadTrackersData();

    boolean doDeinitTrackers();

    void onInitARDone(ARApplicationException e);

    void onVuforiaUpdate(State state);
    
}
