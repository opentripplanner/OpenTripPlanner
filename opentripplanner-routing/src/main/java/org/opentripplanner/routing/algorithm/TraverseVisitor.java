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

package org.opentripplanner.routing.algorithm;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;

public interface TraverseVisitor {

    /** Called when A* explores an edge */
    void visitEdge(Edge edge, State state);

    /** Called when A* dequeues a vertex */
    void visitVertex(State state);

    /** Called when A* enqueues a vertex */
    void visitEnqueue(State state);

}
