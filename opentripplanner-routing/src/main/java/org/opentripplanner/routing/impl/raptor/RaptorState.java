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

package org.opentripplanner.routing.impl.raptor;

import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.trippattern.TripTimes;

/* RaptorStates are always at some transit stop;
 * they either got there via a walk, in which case walkPath != null, or
 * via a transit hop, in which case boardStop etc have been set.
*/

public class RaptorState {
    /* dominance characteristics */
    double walkDistance;
    int nBoardings;
    int arrivalTime;
    int waitingTime;

    public RaptorState() {
    }
    
    /* if this state has just boarded transit, this is the boarding location */
    RaptorStop boardStop;
    int boardStopSequence = -1; //this is the index in this route
    RaptorRoute route;
    public int patternIndex = -1; 
    public TripTimes tripTimes = null;

    /* if has walked to transit,  */
    State walkPath;
    
    
    /* path info */
    RaptorState parent;
    public ServiceDay serviceDay;
    public RaptorStop stop;
    
    public String toString() {
        return "at " + stop + " boarded at " + boardStop + " on " + route + " time " + arrivalTime;
    }
    
}
