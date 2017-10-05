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

import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.edgetype.TransferEdge;
import org.opentripplanner.routing.vertextype.TransitStationStop;

/** Represents a transfer from a stop */
public class TransferShort {
    /** Type of transfer */
    public String type;

    /** The stop we are connecting to */
    public String toStopId;

    /** the on-street distance of the transfer (meters) */
    public double distance;

    /** Make a transfer from a SimpleTransfer or TransferEdge edge from the graph. */
    public TransferShort(TransferEdge e) {
        type = e.getClass().getSimpleName();
        toStopId = GtfsLibrary.convertIdToString(((TransitStationStop) e.getToVertex()).getStopId());
        distance = e.getDistance();
    }
}
