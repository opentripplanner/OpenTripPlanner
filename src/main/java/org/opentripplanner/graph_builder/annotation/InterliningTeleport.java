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

package org.opentripplanner.graph_builder.annotation;

import org.onebusaway.gtfs.model.Trip;

public class InterliningTeleport extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "Interlining trip '%s' implies teleportation of %d meters. Skipping block '%s'.";

    final Trip prevTrip;
    final int distance;
    final String blockId;

    public InterliningTeleport(Trip prevTrip, int distance, String blockId){
    	this.prevTrip = prevTrip;
    	this.distance = distance;
    	this.blockId = blockId;
    }
    
    @Override
    public String getMessage() {
        return String.format(FMT, prevTrip, distance, blockId);
    }

}
