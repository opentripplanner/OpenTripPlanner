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

package org.opentripplanner.routing.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.opentripplanner.routing.automata.AutomatonState;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.*;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.impl.GraphPathFinder.Parser;
import org.opentripplanner.routing.vertextype.TransitStation;
import org.opentripplanner.routing.vertextype.TransitStop;

public class GraphPathFinderTest {
    @Test
    public final void testTerminalFor() {
        // Create a long distance path parser
        Parser parser = new Parser();

        State emptyState = mock(State.class);

        State streetState = mock(State.class);
        when(streetState.getBackEdge()).thenReturn(mock(StreetEdge.class));

        State linkState = mock(State.class);
        when(linkState.getBackEdge()).thenReturn(mock(StreetTransitLink.class));

        State stationState = mock(State.class);
        when(stationState.getBackEdge()).thenReturn(mock(PreAlightEdge.class));

        State onboardState = mock(State.class);
        when(onboardState.getBackEdge()).thenReturn(mock(PatternHop.class));

        State transferState = mock(State.class);
        when(transferState.getBackEdge()).thenReturn(mock(TransferEdge.class));

        State stationStopState = mock(State.class);
        when(stationStopState.getBackEdge()).thenReturn(mock(StationStopEdge.class));
        when(stationStopState.getVertex()).thenReturn(mock(TransitStop.class));

        State stopStationState = mock(State.class);
        when(stopStationState.getBackEdge()).thenReturn(mock(StationStopEdge.class));
        when(stopStationState.getVertex()).thenReturn(mock(TransitStation.class));

        try {
            parser.terminalFor(emptyState);
            fail();
        } catch (Throwable throwable) {
            assertEquals(RuntimeException.class, throwable.getClass()); // A back edge must be given
        }

        assertEquals(Parser.STREET, parser.terminalFor(streetState));
        assertEquals(Parser.LINK, parser.terminalFor(linkState));
        assertEquals(Parser.STATION, parser.terminalFor(stationState));
        assertEquals(Parser.ONBOARD, parser.terminalFor(onboardState));
        assertEquals(Parser.TRANSFER, parser.terminalFor(transferState));
        assertEquals(Parser.STATION_STOP, parser.terminalFor(stationStopState));
        assertEquals(Parser.STOP_STATION, parser.terminalFor(stopStationState));
    }

    @Test
    public final void testPathParser() {
        // Create a long distance path parser
        Parser parser = new Parser();

        { // Test street only path (allowed)
            List<Class<? extends Edge>> path = new ArrayList<Class<? extends Edge>>();
            path.add(StreetEdge.class);
            path.add(StreetEdge.class);
            path.add(StreetEdge.class);
            path.add(StreetEdge.class);
            assertTrue(parsePath(parser, path));
        }

        { // Test street-transit-street (allowed)
            List<Class<? extends Edge>> path = new ArrayList<Class<? extends Edge>>();
            path.add(StreetEdge.class);
            path.add(StreetTransitLink.class);
            path.add(PreBoardEdge.class);
            path.add(TransitBoardAlight.class);
            path.add(PatternHop.class);
            path.add(TransitBoardAlight.class);
            path.add(PreAlightEdge.class);
            path.add(StreetTransitLink.class);
            path.add(StreetEdge.class);
            assertTrue(parsePath(parser, path));
        }

        { // Test onboard-transit-street (allowed)
            List<Class<? extends Edge>> path = new ArrayList<Class<? extends Edge>>();
            path.add(PatternHop.class);
            path.add(PatternHop.class);
            path.add(TransitBoardAlight.class);
            path.add(PreAlightEdge.class);
            path.add(StreetTransitLink.class);
            path.add(StreetEdge.class);
            assertTrue(parsePath(parser, path));
        }

        { // Test onboard-transit (allowed)
            List<Class<? extends Edge>> path = new ArrayList<Class<? extends Edge>>();
            path.add(PatternHop.class);
            path.add(PatternHop.class);
            path.add(TransitBoardAlight.class);
            path.add(PreAlightEdge.class);
            assertTrue(parsePath(parser, path));
        }

        { // Test street-transit-street-transit-street (not allowed)
            List<Class<? extends Edge>> path = new ArrayList<Class<? extends Edge>>();
            path.add(StreetEdge.class);
            path.add(StreetTransitLink.class);
            path.add(PreBoardEdge.class);
            path.add(TransitBoardAlight.class);
            path.add(PatternHop.class);
            path.add(TransitBoardAlight.class);
            path.add(PreAlightEdge.class);
            path.add(StreetTransitLink.class);
            path.add(StreetEdge.class);
            path.add(StreetTransitLink.class);
            path.add(PreBoardEdge.class);
            path.add(TransitBoardAlight.class);
            path.add(PatternHop.class);
            path.add(TransitBoardAlight.class);
            path.add(PreAlightEdge.class);
            path.add(StreetTransitLink.class);
            path.add(StreetEdge.class);
            assertFalse(parsePath(parser, path));
        }

        { // Test street-transit-transfer-transit-street (allowed)
            List<Class<? extends Edge>> path = new ArrayList<Class<? extends Edge>>();
            path.add(StreetEdge.class);
            path.add(StreetTransitLink.class);
            path.add(PreBoardEdge.class);
            path.add(TransitBoardAlight.class);
            path.add(PatternHop.class);
            path.add(TransitBoardAlight.class);
            path.add(PreAlightEdge.class);
            path.add(TransferEdge.class);
            path.add(PreBoardEdge.class);
            path.add(TransitBoardAlight.class);
            path.add(PatternHop.class);
            path.add(TransitBoardAlight.class);
            path.add(PreAlightEdge.class);
            path.add(StreetTransitLink.class);
            path.add(StreetEdge.class);
            assertTrue(parsePath(parser, path));
        }

        { // Test street-transit-transfer-transfer-transit-street (not allowed)
            List<Class<? extends Edge>> path = new ArrayList<Class<? extends Edge>>();
            path.add(StreetEdge.class);
            path.add(StreetTransitLink.class);
            path.add(PreBoardEdge.class);
            path.add(TransitBoardAlight.class);
            path.add(PatternHop.class);
            path.add(TransitBoardAlight.class);
            path.add(PreAlightEdge.class);
            path.add(TransferEdge.class);
            path.add(TransferEdge.class);
            path.add(PreBoardEdge.class);
            path.add(TransitBoardAlight.class);
            path.add(PatternHop.class);
            path.add(TransitBoardAlight.class);
            path.add(PreAlightEdge.class);
            path.add(StreetTransitLink.class);
            path.add(StreetEdge.class);
            assertFalse(parsePath(parser, path));
        }

        { // Test street-transit-simpletransfer-transit-street (allowed)
            List<Class<? extends Edge>> path = new ArrayList<Class<? extends Edge>>();
            path.add(StreetEdge.class);
            path.add(StreetTransitLink.class);
            path.add(PreBoardEdge.class);
            path.add(TransitBoardAlight.class);
            path.add(PatternHop.class);
            path.add(TransitBoardAlight.class);
            path.add(PreAlightEdge.class);
            path.add(SimpleTransfer.class);
            path.add(PreBoardEdge.class);
            path.add(TransitBoardAlight.class);
            path.add(PatternHop.class);
            path.add(TransitBoardAlight.class);
            path.add(PreAlightEdge.class);
            path.add(StreetTransitLink.class);
            path.add(StreetEdge.class);
            assertTrue(parsePath(parser, path));
        }

        { // Test street-transit-timed transfer-transit-street (allowed)
            List<Class<? extends Edge>> path = new ArrayList<Class<? extends Edge>>();
            path.add(StreetEdge.class);
            path.add(StreetTransitLink.class);
            path.add(PreBoardEdge.class);
            path.add(TransitBoardAlight.class);
            path.add(PatternHop.class);
            path.add(TransitBoardAlight.class);
            path.add(TimedTransferEdge.class);
            path.add(TransitBoardAlight.class);
            path.add(PatternHop.class);
            path.add(TransitBoardAlight.class);
            path.add(PreAlightEdge.class);
            path.add(StreetTransitLink.class);
            path.add(StreetEdge.class);
            assertTrue(parsePath(parser, path));
        }

        { // Test street-transit (allowed)
            List<Class<? extends Edge>> path = new ArrayList<Class<? extends Edge>>();
            path.add(StreetEdge.class);
            path.add(StreetTransitLink.class);
            path.add(PreBoardEdge.class);
            path.add(TransitBoardAlight.class);
            path.add(PatternHop.class);
            path.add(TransitBoardAlight.class);
            path.add(TimedTransferEdge.class);
            path.add(TransitBoardAlight.class);
            path.add(PatternHop.class);
            path.add(TransitBoardAlight.class);
            path.add(PreAlightEdge.class);
            assertTrue(parsePath(parser, path));
        }

        { // Test transit-street (allowed)
            List<Class<? extends Edge>> path = new ArrayList<Class<? extends Edge>>();
            path.add(PreBoardEdge.class);
            path.add(TransitBoardAlight.class);
            path.add(PatternHop.class);
            path.add(TransitBoardAlight.class);
            path.add(TimedTransferEdge.class);
            path.add(TransitBoardAlight.class);
            path.add(PatternHop.class);
            path.add(TransitBoardAlight.class);
            path.add(PreAlightEdge.class);
            path.add(StreetTransitLink.class);
            path.add(StreetEdge.class);
            assertTrue(parsePath(parser, path));
        }

        { // Test transfer-transit-transfer (allowed)
            List<Class<? extends Edge>> path = new ArrayList<Class<? extends Edge>>();
            path.add(TransferEdge.class);
            path.add(PreBoardEdge.class);
            path.add(TransitBoardAlight.class);
            path.add(PatternHop.class);
            path.add(TransitBoardAlight.class);
            path.add(TimedTransferEdge.class);
            path.add(TransitBoardAlight.class);
            path.add(PatternHop.class);
            path.add(TransitBoardAlight.class);
            path.add(PreAlightEdge.class);
            path.add(TransferEdge.class);
            assertTrue(parsePath(parser, path));
        }

        { // Test onboard-transfer (allowed)
            List<Class<? extends Edge>> path = new ArrayList<Class<? extends Edge>>();
            path.add(PatternHop.class);
            path.add(TransitBoardAlight.class);
            path.add(PreAlightEdge.class);
            path.add(TransferEdge.class);
            assertTrue(parsePath(parser, path));
        }

        { // Test transit (allowed)
            List<Class<? extends Edge>> path = new ArrayList<Class<? extends Edge>>();
            path.add(PreBoardEdge.class);
            path.add(TransitBoardAlight.class);
            path.add(PatternHop.class);
            path.add(TransitBoardAlight.class);
            path.add(PreAlightEdge.class);
            assertTrue(parsePath(parser, path));
        }

        { // Test parent station-transit (allowed)
            List<Class<? extends Edge>> path = new ArrayList<Class<? extends Edge>>();
            path.add(StationStopEdge.class);
            path.add(PreBoardEdge.class);
            path.add(TransitBoardAlight.class);
            path.add(PatternHop.class);
            path.add(TransitBoardAlight.class);
            path.add(PreAlightEdge.class);
            assertTrue(parsePath(parser, path));
        }

        { // Test parent station-parent station-transit (not allowed)
            List<Class<? extends Edge>> path = new ArrayList<Class<? extends Edge>>();
            path.add(StationStopEdge.class);
            path.add(StationStopEdge.class);
            path.add(PreBoardEdge.class);
            path.add(TransitBoardAlight.class);
            path.add(PatternHop.class);
            path.add(TransitBoardAlight.class);
            path.add(PreAlightEdge.class);
            assertFalse(parsePath(parser, path));
        }

        { // Test parent station-transit-parent station (allowed)
            List<Class<? extends Edge>> path = new ArrayList<Class<? extends Edge>>();
            path.add(StationStopEdge.class);
            path.add(PreBoardEdge.class);
            path.add(TransitBoardAlight.class);
            path.add(PatternHop.class);
            path.add(TransitBoardAlight.class);
            path.add(PreAlightEdge.class);
            path.add(StationStopEdge.class);
            assertTrue(parsePath(parser, path));
        }

        { // Test parent station-transfer-transit-street (allowed)
            List<Class<? extends Edge>> path = new ArrayList<Class<? extends Edge>>();
            path.add(StationStopEdge.class);
            path.add(TransferEdge.class);
            path.add(PreBoardEdge.class);
            path.add(TransitBoardAlight.class);
            path.add(PatternHop.class);
            path.add(PatternHop.class);
            path.add(PatternHop.class);
            path.add(TransitBoardAlight.class);
            path.add(PreAlightEdge.class);
            path.add(StreetTransitLink.class);
            path.add(StreetEdge.class);
            path.add(StreetEdge.class);
            path.add(StreetEdge.class);
            assertTrue(parsePath(parser, path));
        }

        { // Test parent station-street-parent station (allowed)
            List<Class<? extends Edge>> path = new ArrayList<Class<? extends Edge>>();
            path.add(StationStopEdge.class);
            path.add(StreetTransitLink.class);
            path.add(StreetEdge.class);
            path.add(StreetEdge.class);
            path.add(StreetEdge.class);
            path.add(StreetTransitLink.class);
            path.add(StationStopEdge.class);
            assertTrue(parsePath(parser, path));
        }

        { // Test pre board-pre alight (not allowed)
            List<Class<? extends Edge>> path = new ArrayList<Class<? extends Edge>>();
            path.add(PreBoardEdge.class);
            path.add(PreAlightEdge.class);
            assertFalse(parsePath(parser, path));
        }

        { // Test street link-street link (not allowed; not necessary in long distance mode)
            List<Class<? extends Edge>> path = new ArrayList<Class<? extends Edge>>();
            path.add(StreetTransitLink.class);
            path.add(StreetTransitLink.class);
            assertFalse(parsePath(parser, path));
        }

        { // Test station stop-station stop (not allowed)
            List<Class<? extends Edge>> path = new ArrayList<Class<? extends Edge>>();
            path.add(StationStopEdge.class);
            path.add(StationStopEdge.class);
            assertFalse(parsePath(parser, path));
        }

        { // Test transfer (allowed)
            List<Class<? extends Edge>> path = new ArrayList<Class<? extends Edge>>();
            path.add(TransferEdge.class);
            assertTrue(parsePath(parser, path));
        }

    }

    /**
     * Check whether a "path" is accepted by the long distance path parser.
     * 
     * Assumes that only the back edge is used to determine the terminal, except in the case of
     * @link{StationStopEdge}. For that special case, the vertex of the first state is specified as
     * @link{TransitStop} while the vertex of the final state is specified as @link{TransitStation}.
     * 
     * @param parser is the long distance path parser
     * @param path is a list of edge classes that represent a path
     * @return true when path is accepted
     */
    private boolean parsePath(Parser parser, List<Class<? extends Edge>> path) {
        // Assume a path that is not accepted
        boolean accepted = false;

        // Start in start state and "walk" through state machine
        int currentState = AutomatonState.START;
        for (int i = 0; i < path.size(); i++) {
            Class<? extends Edge> edgeClass = path.get(i);
            // Create dummy state with edge as back edge
            State state = mock(State.class);
            Edge edge = mock(edgeClass);
            when(state.getBackEdge()).thenReturn(edge);
            if (i == 0) {                                                           // First state
                when(state.getVertex()).thenReturn(mock(TransitStop.class));
            } else if (i == path.size() - 1) {                                      // Final state
                when(state.getVertex()).thenReturn(mock(TransitStation.class));
            }

            // Get terminal of state
            int terminal = parser.terminalFor(state);
            // Make a transition
            currentState = parser.transition(currentState, terminal);
            // Check whether we still are in a valid state
            if (currentState == AutomatonState.REJECT) {
                return false;
            }
        }

        // Check whether this final state is accepted
        if (parser.accepts(currentState)) {
            accepted = true;
        }

        return accepted;
    }
}
