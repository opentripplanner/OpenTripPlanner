/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.updater.stoptime;

import java.util.HashMap;
import java.util.Map;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Graph;

/**
 * A synchronized cache of trip patterns that are added to the graph due to GTFS-realtime messages.
 */
public class TripPatternCache {
    
    private int counter = 0;

    private final Map<StopPattern, TripPattern> cache = new HashMap<>();
    
    /**
     * Get cached trip pattern or create one if it doesn't exist yet. If a trip pattern is created, vertices
     * and edges for this trip pattern are also created in the graph.
     * 
     * @param stopPattern stop pattern to retrieve/create trip pattern
     * @param route route of new trip pattern in case a new trip pattern will be created
     * @param graph graph to add vertices and edges in case a new trip pattern will be created
     * @return cached or newly created trip pattern
     */
    public synchronized TripPattern getOrCreateTripPattern(final StopPattern stopPattern,
            final Route route, final Graph graph) {
        // Check cache for trip pattern
        TripPattern tripPattern = cache.get(stopPattern);
        
        // Create TripPattern if it doesn't exist yet
        if (tripPattern == null) {
            tripPattern = new TripPattern(route, stopPattern);
            
            // Generate unique code for trip pattern
            tripPattern.code = generateUniqueTripPatternCode(tripPattern);
            
            // Create an empty bitset for service codes (because the new pattern does not contain any trips)
            tripPattern.setServiceCodes(graph.serviceCodes);
            
            // Finish scheduled time table
            tripPattern.scheduledTimetable.finish();
            
            // Create vertices and edges for new TripPattern
            // TODO: purge these vertices and edges once in a while?
            tripPattern.makePatternVerticesAndEdges(graph, graph.index.stopVertexForStop);
            
            // TODO: Add pattern to graph index? 
            
            // Add pattern to cache
            cache.put(stopPattern, tripPattern);
        }
        
        return tripPattern;
    }

    /**
     * Generate unique trip pattern code for real-time added trip pattern. This function roughly
     * follows the format of {@link TripPattern#generateUniqueIds(java.util.Collection)}.
     * 
     * @param tripPattern trip pattern to generate code for
     * @return unique trip pattern code
     */
    private String generateUniqueTripPatternCode(TripPattern tripPattern) {
        AgencyAndId routeId = tripPattern.route.getId();
        String direction = tripPattern.directionId != -1 ? String.valueOf(tripPattern.directionId) : "";
        if (counter == Integer.MAX_VALUE) {
            counter = 0;
        } else {
            counter++;
        }
        // OBA library uses underscore as separator, we're moving toward colon.
        String code = String.format("%s:%s:%s:rt#%d", routeId.getAgencyId(), routeId.getId(), direction, counter);
        return code;
    }

}
