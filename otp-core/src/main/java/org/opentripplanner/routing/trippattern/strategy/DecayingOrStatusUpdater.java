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

import org.opentripplanner.routing.edgetype.TableTripPattern;
import org.opentripplanner.routing.trippattern.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DecayingOrStatusUpdater implements ITripTimesUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(DecayingOrStatusUpdater.class);

    @Override
    public TripTimes updateTimes(ScheduledTripTimes scheduledTimes, TableTripPattern pattern ,TripUpdateList updateList) {
        TripTimes newTimes = null;
        if (updateList.isCancellation()) {
            newTimes = new CanceledTripTimes(scheduledTimes);
        }
        else if(updateList.hasDelay()) {
            // 'stop' Index as in transit stop (not 'end', not 'hop')
            int stopIndex = updateList.findUpdateStopIndex(pattern);
            if (stopIndex == TripUpdateList.MATCH_FAILED) {
                LOG.warn("Unable to match update block to stopIds.");
                return null;
            }
            int delay = updateList.getUpdates().get(0).getArriveDelay();
            newTimes = new DecayingDelayTripTimes(scheduledTimes, stopIndex, delay);
            if(newTimes.timesIncreasing()){
                return newTimes;
            }
            return null;
        }
        else {
            // 'stop' Index as in transit stop (not 'end', not 'hop')
            int stopIndex = updateList.findUpdateStopIndex(pattern);
            if (stopIndex == TripUpdateList.MATCH_FAILED) {
                LOG.warn("Unable to match update block to stopIds.");
                return null;
            }
            newTimes = new UpdatedTripTimes(scheduledTimes, updateList, stopIndex);
            if ( ! newTimes.timesIncreasing()) {
                LOG.warn("Resulting UpdatedTripTimes has non-increasing times. " +
                        "Falling back on DecayingDelayTripTimes.");
                LOG.warn(updateList.toString());
                LOG.warn(newTimes.toString());
                int delay = newTimes.getDepartureDelay(stopIndex);
                // maybe decay should be applied on top of the update (wrap Updated in Decaying),
                // starting at the end of the update block
                newTimes = new DecayingDelayTripTimes(scheduledTimes, stopIndex, delay);
                LOG.warn(newTimes.toString());
                if ( ! newTimes.timesIncreasing()) {
                    LOG.error("Even these trip times are non-increasing. Underlying schedule problem?");
                    return null;
                }
            }
        }

        return newTimes;
    }

}
