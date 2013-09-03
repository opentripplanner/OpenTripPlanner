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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.opentripplanner.routing.automata.AutomatonState;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.PreAlightEdge;
import org.opentripplanner.routing.edgetype.PreBoardEdge;
import org.opentripplanner.routing.edgetype.SimpleTransfer;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.edgetype.TimedTransferEdge;
import org.opentripplanner.routing.edgetype.TransferEdge;
import org.opentripplanner.routing.edgetype.TransitBoardAlight;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.impl.LongDistancePathService.Parser;

public class LongDistancePathServiceTest {
    @Test
    public final void testPathParser() {
        // Create a long distance path parser
        Parser parser = new Parser();
        
        { // Test street only path (allowed)
            List<Class<? extends Edge>> path = new ArrayList<Class<? extends Edge>>();
            path.add(PlainStreetEdge.class);
            path.add(PlainStreetEdge.class);
            path.add(PlainStreetEdge.class);
            path.add(PlainStreetEdge.class);
            assertTrue(parsePath(parser, path));
        }

        { // Test street-transit-street (allowed)
            List<Class<? extends Edge>> path = new ArrayList<Class<? extends Edge>>();
            path.add(PlainStreetEdge.class);
            path.add(StreetTransitLink.class);
            path.add(PreBoardEdge.class);
            path.add(TransitBoardAlight.class);
            path.add(PatternHop.class);
            path.add(TransitBoardAlight.class);
            path.add(PreAlightEdge.class);
            path.add(StreetTransitLink.class);
            path.add(PlainStreetEdge.class);
            assertTrue(parsePath(parser, path));
        }
        
        { // Test onboard-transit-street (allowed)
            List<Class<? extends Edge>> path = new ArrayList<Class<? extends Edge>>();
            path.add(PatternHop.class);
            path.add(PatternHop.class);
            path.add(TransitBoardAlight.class);
            path.add(PreAlightEdge.class);
            path.add(StreetTransitLink.class);
            path.add(PlainStreetEdge.class);
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
            path.add(PlainStreetEdge.class);
            path.add(StreetTransitLink.class);
            path.add(PreBoardEdge.class);
            path.add(TransitBoardAlight.class);
            path.add(PatternHop.class);
            path.add(TransitBoardAlight.class);
            path.add(PreAlightEdge.class);
            path.add(StreetTransitLink.class);
            path.add(PlainStreetEdge.class);
            path.add(StreetTransitLink.class);
            path.add(PreBoardEdge.class);
            path.add(TransitBoardAlight.class);
            path.add(PatternHop.class);
            path.add(TransitBoardAlight.class);
            path.add(PreAlightEdge.class);
            path.add(StreetTransitLink.class);
            path.add(PlainStreetEdge.class);
            assertFalse(parsePath(parser, path));
        }
        
        { // Test street-transit-transfer-transit-street (allowed)
            List<Class<? extends Edge>> path = new ArrayList<Class<? extends Edge>>();
            path.add(PlainStreetEdge.class);
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
            path.add(PlainStreetEdge.class);
            assertTrue(parsePath(parser, path));
        }
        
        { // Test street-transit-simpletransfer-transit-street (allowed)
            List<Class<? extends Edge>> path = new ArrayList<Class<? extends Edge>>();
            path.add(PlainStreetEdge.class);
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
            path.add(PlainStreetEdge.class);
            assertTrue(parsePath(parser, path));
        }
        
        { // Test street-transit-timed transfer-transit-street (allowed)
            List<Class<? extends Edge>> path = new ArrayList<Class<? extends Edge>>();
            path.add(PlainStreetEdge.class);
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
            path.add(PlainStreetEdge.class);
            assertTrue(parsePath(parser, path));
        }
        
        { // Test street-transit (allowed)
            List<Class<? extends Edge>> path = new ArrayList<Class<? extends Edge>>();
            path.add(PlainStreetEdge.class);
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
            path.add(PlainStreetEdge.class);
            assertTrue(parsePath(parser, path));
        }
        
    }

    private boolean parsePath(Parser parser, List<Class<? extends Edge>> path) {
        // Assumes only the backedge is used for the terminal
        boolean accept = false;

        // Start in state 0
        int currentState = 0;
        for (Class<? extends Edge> edgeClass : path) {
            // Create dummy state with edge as back edge
            State state = mock(State.class);
            Edge edge = mock(edgeClass);
            when(state.getBackEdge()).thenReturn(edge);

            // Get terminal of state
            int terminal = parser.terminalFor(state);
            // First state?
            currentState = parser.transition(currentState, terminal);
            if (currentState == AutomatonState.REJECT) {
                return false;
            }
        }

        if (parser.accepts(currentState)) {
            accept = true;
        }

        return accept;
    }

}
