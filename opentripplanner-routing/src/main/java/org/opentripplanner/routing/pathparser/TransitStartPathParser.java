package org.opentripplanner.routing.pathparser;

import org.opentripplanner.routing.automata.DFA;
import org.opentripplanner.routing.automata.Nonterminal;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.opentripplanner.routing.vertextype.OffboardVertex;
import org.opentripplanner.routing.vertextype.OnboardVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

import static org.opentripplanner.routing.automata.Nonterminal.*;

/**
 * Some goals to build out for this path parser: Reject paths that use crosswalks to Reject paths
 * that take shortcuts through stops, by disallowing series of STL and requiring a transit trip
 * after entering a station. Reject paths that reach the destination with a rented bicycle. Disallow
 * boarding transit without first parking a car. Reject breaking no-through-traffic rules on driving
 * legs.
 * 
 * Despite the name, this actually *ends* at a transit station -- it's used with 
 * TransitStartPathServiceImpl to implement start-at-transit (first/last/next/prev trip) 
 */
public class TransitStartPathParser extends PathParser {

    private static final int STATION = 1;

    private static final int TRANSIT = 2;

    // 3,4,5 come from StreetEdge.java

    private static final DFA DFA;
    static {
        Nonterminal bikeNonStreet = star(choice(StreetEdge.CLASS_CROSSING,
                StreetEdge.CLASS_OTHERPATH));

        // (C|O)*(S+O(C|O)*)*(S*(C|O)*) -- the inverse of S+C+S+
        Nonterminal optionalNontransitLeg = seq(bikeNonStreet,
                star(plus(StreetEdge.CLASS_STREET),
                     star(StreetEdge.CLASS_CROSSING),
                     StreetEdge.CLASS_OTHERPATH,
                     bikeNonStreet),
                seq(star(StreetEdge.CLASS_STREET), bikeNonStreet));

        Nonterminal itinerary = seq(star(STATION), optionalNontransitLeg, star(STATION));
        DFA = itinerary.toDFA().minimize();
    }

    @Override
    protected DFA getDFA() {
        return DFA;
    }

    @Override
    public int terminalFor(State state) {
        Vertex v = state.getVertex();
        if (v instanceof StreetVertex || v instanceof StreetLocation) {
            TraverseModeSet modes = state.getOptions().getModes();
            if (modes.contains(TraverseMode.BICYCLE)
                    && (!modes.contains(TraverseMode.WALK) || !state.isBikeRenting())) {
                Edge edge = state.getBackEdge();
                if (edge instanceof StreetEdge) {
                    int cls = ((StreetEdge) edge).getStreetClass();
                    return cls & StreetEdge.CROSSING_CLASS_MASK;
                } else {
                    return StreetEdge.CLASS_OTHERPATH;
                }
            } else {
                return StreetEdge.CLASS_OTHERPATH;
            }
        }
        if (v instanceof OnboardVertex)
            return TRANSIT;
        if (v instanceof OffboardVertex)
            return STATION;
        if (v instanceof BikeRentalStationVertex)
            return StreetEdge.CLASS_OTHERPATH;
        else
            throw new RuntimeException("failed to tokenize path");
    }

}
