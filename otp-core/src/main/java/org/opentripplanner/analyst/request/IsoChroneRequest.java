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

package org.opentripplanner.analyst.request;

import java.util.Arrays;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * A request for an isochrone vector.
 * 
 * @author laurent
 */
public class IsoChroneRequest {

    @Getter
    private final List<Integer> cutoffSecList;

    @Getter
    @Setter
    private boolean includeDebugGeometry;

    @Getter
    @Setter
    private int precisionMeters = 200;

    @Getter
    @Setter
    private int maxTimeSec = 0;

    @Getter
    private int minCutoffSec = Integer.MAX_VALUE;

    @Getter
    private int maxCutoffSec = 0;

    public IsoChroneRequest(List<Integer> cutoffSecList) {
        this.cutoffSecList = cutoffSecList;
        for (Integer cutoffSec : cutoffSecList) {
            if (cutoffSec > maxCutoffSec)
                maxCutoffSec = cutoffSec;
            if (cutoffSec < minCutoffSec)
                minCutoffSec = cutoffSec;
        }
    }

    @Override
    public int hashCode() {
        return cutoffSecList.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof IsoChroneRequest) {
            IsoChroneRequest otherReq = (IsoChroneRequest) other;
            return this.cutoffSecList.equals(otherReq.cutoffSecList);
        }
        return false;
    }

    public String toString() {
        return String.format("<isochrone request, cutoff=%s sec, precision=%d meters>",
                Arrays.toString(cutoffSecList.toArray()), precisionMeters);
    }
}
