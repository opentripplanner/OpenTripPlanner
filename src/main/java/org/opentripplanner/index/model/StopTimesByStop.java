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

    private List<StopTimesInPattern> patterns = Lists.newArrayList();

    public StopTimesByStop(Stop stop, boolean groupByParent, List<StopTimesInPattern> stopTimesInPattern) {
        this.stop = new StopShort(stop);
        if (stop.getParentStation() != null && groupByParent) {
            this.stop.id.setId(stop.getParentStation());
            this.stop.cluster = null;
            // TODO we only know lat and lon match because it's an MTA convention
        }
        this.patterns = stopTimesInPattern;
    }

    /**
     * Stop which these arrival-departures are supplied for. If groupByParent = true, this will be a parent station
     * (if parent stations are given in GTFS).
     */
    public StopShort getStop() {
        return stop;
    }

    /**
     * List of groups of arrival-departures, separated out by TripPattern
     */
    public List<StopTimesInPattern> getPatterns() {
        return patterns;
    }

    public void addPatterns(List<StopTimesInPattern> stip) {
        patterns.addAll(stip);
    }
}
