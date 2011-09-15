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

import java.util.ArrayList;
import java.util.List;

import org.opentripplanner.routing.algorithm.GenericAStar;
import org.opentripplanner.routing.algorithm.TraverseVisitor;
import org.opentripplanner.routing.algorithm.strategies.GenericAStarFactory;
import org.opentripplanner.routing.core.DirectEdge;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.Vertex;

public class VisualTraverseVisitor implements TraverseVisitor {

    private ShowGraph gui;

    List<Vertex> seen = new ArrayList<Vertex>();

    public VisualTraverseVisitor(ShowGraph gui) {
        this.gui = gui;
    }

    @Override
    public void visitEdge(Edge edge, State state) {
        if (edge instanceof DirectEdge) {
            gui.enqueueHighlightedEdge((DirectEdge) edge);
        }
        //gui.highlightVertex(state.getVertex());
    }

    @Override
    public void visitVertex(State state) {
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
//        seen.add(state.getVertex());
//        gui.setHighlightedVertices(seen);
//        gui.highlightVertex(state.getVertex());
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
