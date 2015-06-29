package org.opentripplanner.routing.pathparser;

import org.opentripplanner.routing.automata.DFA;
import org.opentripplanner.routing.automata.Nonterminal;

import static org.opentripplanner.routing.automata.Nonterminal.*;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.TransitStop;

/**
 * Path parser to propagate profile results to the street
 * Reverse of InitialStopSearchPathParser, and uses the same grammar so extends it.
 *
 */
public class ProfilePropagationPathParser extends InitialStopSearchPathParser {
    private static final DFA pppDfa;

    static {
        Nonterminal nt = seq(optional(TRANSITSTOP), star(STREET), optional(OTHER));
        pppDfa = nt.toDFA().minimize();
    }

    @Override
    public DFA getDFA () {
        return pppDfa;
    }
}

