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

package org.opentripplanner.routing.trippattern.strategy;

import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.TableTripPattern;
import org.opentripplanner.routing.trippattern.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ContinuesDelayTTUpdater implements ITripTimesUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(ContinuesDelayTTUpdater.class);

    @Override
    public TripTimes updateTimes(ScheduledTripTimes scheduledTimes, TableTripPattern pattern, TripUpdateList updateList) {
        ContinuesDelayTripTimes continuesDelayTT = new ContinuesDelayTripTimes(scheduledTimes);
        int minHopForCoherency = 0;
        for(Update update : updateList.getUpdates()){
            int arrivalHop = findArrivalHop(update, pattern);
            if(arrivalHop >=0 ){
                continuesDelayTT.insertUpdate(arrivalHop, true, update);
                minHopForCoherency = arrivalHop;
            }else{
                //test if the we missed the first stop
                if(pattern.getStop(0).getId().equals(update.getStopId())){
                    continuesDelayTT.insertUpdate(0, false, update);
                }else{
                    LOG.warn("stop id " + update.getStopId() + " from the update couldn't be found in the trip pattern");
                }
            }
        }
        if(continuesDelayTT.timesIncreasing(minHopForCoherency)){
            return continuesDelayTT;
        }
        return null;
    }

    private int findArrivalHop(Update update, TableTripPattern tripPattern){
        List<PatternHop> patternHops = tripPattern.getPatternHops();
        for(int i = 0; i < patternHops.size(); i++){
            PatternHop patternHop = patternHops.get(i);
            if(patternHop.getEndStop().getId().equals(update.getStopId())){
                return i;
            }
        }
        return -1;
    }

}
