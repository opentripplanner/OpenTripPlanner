package org.opentripplanner.routing.pathparser;

import org.opentripplanner.routing.automata.DFA;
import org.opentripplanner.routing.automata.Nonterminal;

import static org.opentripplanner.routing.automata.Nonterminal.*;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.ElevatorOffboardVertex;
import org.opentripplanner.routing.vertextype.ElevatorOnboardVertex;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.TransitStop;


public class InitialStopSearchPathParser extends PathParser {

    protected static final int STREET = 1;
    protected static final int TRANSITSTOP = 2;
    protected static final int OTHER = 3;

    private static DFA dfa;

    static {
        Nonterminal complete = seq(optional(OTHER), star(STREET), TRANSITSTOP);
        dfa = complete.toDFA().minimize();
    }

    @Override
    public int terminalFor(State state) {
        Vertex v = state.getVertex();

        if (v instanceof IntersectionVertex || v instanceof ElevatorOffboardVertex || v instanceof ElevatorOnboardVertex)
            return STREET;

        else if (v instanceof TransitStop)
            return TRANSITSTOP;

        else return OTHER;
    }

    @Override
    protected DFA getDFA() {
        return dfa;
    }

}
