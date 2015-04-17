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

package org.opentripplanner.routing.carspeed;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.opentripplanner.routing.carspeed.CarSpeedSnapshot.StreetEdgeCarSpeedProvider;
import org.opentripplanner.routing.edgetype.StreetEdge;

/**
 * Tentative implementation for a week baseline with variable slots size.
 *
 * TODO Handle current/predicted values
 */
public class WeekBaselineCarSpeedProvider implements StreetEdgeCarSpeedProvider {

    private final float averageSpeed;

    private final NavigableMap<Integer, Float> carSpeeds;

    /**
     * @param averageSpeed
     * @param minuteCarSpeed Map of (seconds since week baseline, speed)
     */
    public WeekBaselineCarSpeedProvider(float averageSpeed, Map<Integer, Float> minuteCarSpeed) {
        this.averageSpeed = averageSpeed;
        this.carSpeeds = new TreeMap<>(minuteCarSpeed);
    }

    @Override
    public float getCarSpeed(StreetEdge streetEdge, long timestampMs, float defaultSpeed) {
        int weekOffset = getWeekOffsetSec(timestampMs);

        Map.Entry<Integer, Float> slotSpeed = carSpeeds.floorEntry(weekOffset);
        if (slotSpeed == null) {
            // Can happen if no slots at all, or if first slot start >0
            return averageSpeed;
        }
        Float carSpeed = slotSpeed.getValue();
        if (carSpeed == null) {
            // Empty slot, this is valid
            return averageSpeed;
        }
        return carSpeed;
    }

    private static final long FIRST_MONDAY_EPOCH = 345600; // Mon 5 Jan 1970, 0:00:00 GMT

    private static final long ONE_WEEK_IN_SEC = TimeUnit.DAYS.toSeconds(7);

    /**
     * Note: Computing week offset with a simple modulo is correct, because UNIX/Java epoch assume a
     * constant day duration in ms, even when leap seconds occurs.
     * 
     * @param timestampMs "epoch-in-millisec" timestamp
     * @return Offset in seconds since Monday 0:00:00.0 GMT
     */
    protected static int getWeekOffsetSec(long timestampMs) {
        return (int) ((timestampMs / 1000 - FIRST_MONDAY_EPOCH) % ONE_WEEK_IN_SEC);
    }
}
