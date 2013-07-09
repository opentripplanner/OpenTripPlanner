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

package org.opentripplanner.routing.edgetype.factory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.routing.edgetype.TableTripPattern;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStopArrive;
import org.opentripplanner.routing.vertextype.TransitStopDepart;

/** Retains graph-wide information between GTFSPatternHopFactory runs on different feeds. */
public class GtfsStopContext {

    public HashSet<AgencyAndId> stops = new HashSet<AgencyAndId>();

    public Map<Stop, Vertex> stopNodes = new HashMap<Stop, Vertex>();

    public Map<Stop, TransitStopArrive> stopArriveNodes = new HashMap<Stop, TransitStopArrive>();

    public Map<Stop, TransitStopDepart> stopDepartNodes = new HashMap<Stop, TransitStopDepart>();

    public Map<T2<Stop, Trip>, Vertex> patternArriveNodes = new HashMap<T2<Stop, Trip>, Vertex>();

    public Map<T2<Stop, Trip>, Vertex> patternDepartNodes = new HashMap<T2<Stop, Trip>, Vertex>(); // exemplar
                                                                                                   // trip
    public HashMap<AgencyAndId, Integer> serviceIds = new HashMap<AgencyAndId, Integer>();

    public HashMap<TableTripPattern, Integer> tripPatternIds = new HashMap<TableTripPattern, Integer>();

}
