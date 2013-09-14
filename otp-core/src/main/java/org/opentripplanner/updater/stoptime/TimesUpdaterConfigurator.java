package org.opentripplanner.updater.stoptime;

import org.opentripplanner.routing.trippattern.strategy.ContinuesDelayTTUpdater;
import org.opentripplanner.routing.trippattern.strategy.DecayingOrStatusUpdater;
import org.opentripplanner.routing.trippattern.strategy.ITripTimesUpdater;

import java.util.prefs.Preferences;

public class TimesUpdaterConfigurator {

    public static ITripTimesUpdater getConfigurator(Preferences preferences){
        //DecayingOrStatus is the default updater
        String updaterType = preferences.get("tripTimeUpdater", null);
        if(updaterType != null){
            if (updaterType.equals("decayingOrStatus")) {
                return new DecayingOrStatusUpdater();
            } else if (updaterType.equals("continuesDelay")) {
                return new ContinuesDelayTTUpdater();
            }
        }
        return new DecayingOrStatusUpdater();
    }

}
