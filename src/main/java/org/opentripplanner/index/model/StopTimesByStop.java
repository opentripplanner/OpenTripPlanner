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

package org.opentripplanner.index.model;

import com.google.common.collect.Lists;
import org.onebusaway.gtfs.model.Stop;

import java.util.List;

/**
 * Some stopTimes all from the same stop.
 */
public class StopTimesByStop {

    private StopShort stop;

    private List<TripTimeShort> times = Lists.newArrayList();

    public StopTimesByStop(Stop stop) {
        this.stop = new StopShort(stop);
    }

    public void addTime(TripTimeShort time) {
        times.add(time);
    }

    public StopShort getStop() {
        return stop;
    }

    public List<TripTimeShort> getTimes() {
        return times;
    }

}
