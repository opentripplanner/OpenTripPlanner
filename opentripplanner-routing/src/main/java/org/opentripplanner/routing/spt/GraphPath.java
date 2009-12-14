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

package org.opentripplanner.routing.spt;

import java.util.Collections;
import java.util.ListIterator;
import java.util.Vector;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;

public class GraphPath {
    public Vector<SPTVertex> vertices;

    public Vector<SPTEdge> edges;

    public GraphPath() {
        this.vertices = new Vector<SPTVertex>();
        this.edges = new Vector<SPTEdge>();
    }

    public void optimize() {
        State state = vertices.lastElement().state;
        State state0 = vertices.firstElement().state;
        if (state0.getTime() >= state.getTime()) {
            // reversed paths are already optimized, because preferences are asymmetric -- people
            // want to arrive as late as possible, but also want to leave as late as possible.
            return;
        }
        TraverseOptions options = vertices.lastElement().options;
        ListIterator<SPTEdge> iterator = edges.listIterator(vertices.size() - 1);
        // The following line makes it so that the last edge of a trip is always allowed to be a
        // transfer. See http://opentripplanner.org/ticket/87
        state.setTransferAllowed(true);
        while (iterator.hasPrevious()) {
            SPTEdge edge = iterator.previous();
            TraverseResult result = edge.payload.traverseBack(state, options);
            assert (result != null);
            state = result.state;
            edge.fromv.state = state;
        }
    }

    public String toString() {
        return vertices.toString();
    }

    public void reverse() {
        Collections.reverse(vertices);
        Collections.reverse(edges);
        for (SPTEdge e : edges) {
            SPTVertex tmp = e.fromv;
            e.fromv = e.tov;
            e.tov = tmp;
        }
    }
}