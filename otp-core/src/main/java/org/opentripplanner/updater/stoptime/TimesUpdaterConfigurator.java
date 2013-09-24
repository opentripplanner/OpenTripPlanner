/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

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
