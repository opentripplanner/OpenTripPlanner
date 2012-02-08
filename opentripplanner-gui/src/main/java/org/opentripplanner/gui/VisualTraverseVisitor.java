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

package org.opentripplanner.gui;

import org.opentripplanner.routing.algorithm.GenericAStar;
import org.opentripplanner.routing.algorithm.TraverseVisitor;
import org.opentripplanner.routing.algorithm.strategies.GenericAStarFactory;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Edge;

public class VisualTraverseVisitor implements TraverseVisitor {

    private ShowGraph gui;

    private final int SLEEP_AFTER = 50;
    
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
        if (--sleepAfter <= 0) {
            sleepAfter = SLEEP_AFTER;
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
//        seen.add(state.getVertex());
//        gui.setHighlightedVertices(seen);
//        gui.highlightVertex(state.getVertex());
    }

    @Override
    public void visitEnqueue(State state) {
//        Edge e = state.getBackEdge();
//        if (e instanceof Edge) {
//            gui.enqueueHighlightedEdge((Edge) e);
//        }
    }
    
    public GenericAStarFactory getAStarSearchFactory() {
        return new GenericAStarFactory() {

            @Override
            public GenericAStar createAStarInstance() {
                GenericAStar astar = new GenericAStar();
                astar.setTraverseVisitor(VisualTraverseVisitor.this);
                return astar;
            }
        };
    }

}
