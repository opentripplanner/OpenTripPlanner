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

package org.opentripplanner.visualizer;

import org.opentripplanner.routing.algorithm.TraverseVisitor;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VisualTraverseVisitor implements TraverseVisitor {
    private static final Logger LOG = LoggerFactory.getLogger(VisualTraverseVisitor.class);

    private ShowGraph gui;

    private final int SLEEP_AFTER = 50;
    private final int SLEEP_LEN = 2;
    
    private int sleepAfter = SLEEP_AFTER;
    
    public VisualTraverseVisitor(ShowGraph gui) {
        this.gui = gui;
    }

    @Override
    public void visitEdge(Edge edge, State state) {
        gui.enqueueHighlightedEdge(edge);
        //gui.highlightVertex(state.getVertex());
    }

    @Override
    public void visitVertex(State state) {
    	// every SLEEP_AFTER visits of a vertex, sleep for SLEEP_LEN
    	// this slows down the search so it animates prettily
        if (--sleepAfter <= 0) {
            sleepAfter = SLEEP_AFTER;
            try {
                Thread.sleep(SLEEP_LEN);
            } catch (InterruptedException e) {
                LOG.warn("interrupted", e);
            }
        }
        gui.addNewSPTEdge( state );
    }

    @Override
    public void visitEnqueue(State state) {
//        Edge e = state.getBackEdge();
//        if (e instanceof Edge) {
//            gui.enqueueHighlightedEdge((Edge) e);
//        }
    }
    
}
