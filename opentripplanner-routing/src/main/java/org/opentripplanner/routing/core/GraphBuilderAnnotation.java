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

package org.opentripplanner.routing.core;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;

import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents noteworthy events or errors that occur during the graphbuilding process.
 * 
 * This is in the routing subproject (rather than graphbuilder) to avoid making routing depend on the entire graphbuilder subproject. Graphbuilder
 * already depends on routing.
 * 
 * Typically we want to create an annotation object, store it in the graph that is being built, and log it at the same time. Automatically logging in
 * the annotation object constructor or in the Graph will lead to the wrong compilation unit/line number being reported in the logs. It seems that we
 * cannot modify the behavior of the logger to report a log event one stack frame higher than usual because the true logging mechanism is behind a
 * facade. We cannot invert the process and log an annotation object which would attach itself to a graph upon creation because the logger methods
 * only accept strings. Thus, a static register method on this class that creates an annotation, adds it to a graph, and returns a message string for
 * that annotation.
 * 
 * @author andrewbyrd
 */
public class GraphBuilderAnnotation implements Serializable {

    private static final long serialVersionUID = 20111201L; // YYYYMMDD

    private static final Logger LOG = LoggerFactory.getLogger(GraphBuilderAnnotation.class);

    private Object[] refs;

    private Variety variety;

    public GraphBuilderAnnotation(Variety variety, Object... refs) {
        this.refs = refs;
        this.variety = variety;
    }

    public static String register(Graph graph, Variety variety, Object... refs) {
        GraphBuilderAnnotation gba = new GraphBuilderAnnotation(variety, refs);
        graph.addBuilderAnnotation(gba);
        return "(annotation registered) " + gba.toString();
    }

    public Collection<Object> getReferencedObjects() {
        return Arrays.asList(refs);
    }

    public String toString() {
        return variety.getMessage(refs);
    }

    public enum Variety {
        GRAPHWIDE("Graph-wide: %s"),
        TRIP_DEGENERATE("Trip %s has fewer than two stops.  We will not use it for routing. "
                + "This is probably an error in your data"), 
        TRIP_OVERTAKING("Possible GTFS feed error: Trip %s overtakes trip %s (which has the same "
                + "stops) at stop index %i. This will be handled correctly but inefficiently."), 
        TRIP_DUPLICATE("Possible GTFS feed error: Duplicate trip (skipping). New: %s Existing: %s"), 
        TRIP_DUPLICATE_DEPARTURE("Possible GTFS feed error: Duplicate first departure time. New "
                + "trip: %s Existing trip: %s This will be handled correctly but inefficiently."), 
        CONFLICTING_BIKE_TAGS("Conflicting tags bicycle:[yes|designated] and cycleway: "
                + "dismount on way %s, assuming dismount"), 
        TURN_RESTRICTION_UNKNOWN("Invalid turn restriction at %s"), 
        TURN_RESTRICTION_BAD("Invalid turn restriction at %s"), 
        TURN_RESTRICTION_EXCEPTION("Turn restriction with bicycle exception at node %s from %s"), 
        STOP_UNLINKED("Stop %s not near any streets; it will not be usable"), 
        VERTEX_SHAPE_ERROR("Transit edge %s has shape geometry which is far from its "
                + "start/end vertices.  This could be caused by bad shape geometry, or "
                + "by incorrect use of defaultAgencyId"), 
        BOGUS_EDGE_GEOMETRY("Edge %s has bogus geometry (some coordinates are NaN)"), 
        BOGUS_VERTEX_GEOMETRY("Vertex %s has NaN location; this will cause all sorts of problems. "
                + "This is probably caused by a bug in the graph builder, but could "
                + "conceivably happen with extremely bad GTFS or OSM data."), 
        NO_FUTURE_DATES("Agency %s has no calendar dates which are after today; "
                + "no trips will be plannable on this agency"),
        LEVEL_AMBIGUOUS("Could not infer floor number for layer called '%s' at %s. Vertical " +
                "movement will still be possible, but elevator cost might be incorrect. " +
                "Consider an OSM level map."),
        GRAPH_CONNECTIVITY("Removed/depedestrianized disconnected subgraph containing: %s"),
        ELEVATION_FLATTENED("Edge %s was steeper than Baldwin Street and was flattened.");

        private final String formatString;

        Variety(String formatString) {
            this.formatString = formatString;
        }

        public String getMessage(Object... refs) {
            return String.format(formatString, refs);
        }
    }

    public static void logSummary(Iterable<GraphBuilderAnnotation> gbas) {
        // an EnumMap would be nice, but Integers are immutable...
        int[] counts = new int[Variety.values().length];
        LOG.info("Summary (number of each type of annotation):");
        for (GraphBuilderAnnotation gba : gbas)
            ++counts[gba.variety.ordinal()];
        for (Variety v : Variety.values())
            LOG.info("    {} - {}", v.toString(), counts[v.ordinal()]);
    }

}
