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

package org.opentripplanner.routing.edgetype;

import java.io.Serializable;

import org.onebusaway.gtfs.model.Trip;

/** 
 * A vehicle's wait between the end of one run and the beginning of another run on the same block 
 * */
public class InterlineDwellData implements Serializable {

    private static final long serialVersionUID = 1L;

    public int dwellTime;

    public int patternIndex;

    public Trip trip;
    
    public InterlineDwellData(int dwellTime, int patternIndex, Trip trip) {
        this.dwellTime = dwellTime;
        this.patternIndex = patternIndex;
        this.trip = trip;
    }
}