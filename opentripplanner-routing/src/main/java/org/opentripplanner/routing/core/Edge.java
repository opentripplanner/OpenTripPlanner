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

package org.opentripplanner.routing.core;

import java.util.List;

import org.opentripplanner.routing.patch.Patch;
import org.opentripplanner.routing.spt.GraphPath;

/**
 * This represents an edge transition function in the graph.
 * 
 * <h2>A note about multiple traverse results:</h2>
 * 
 * The {@link TraverseResult} supports a simple chaining mechanism through
 * {@link TraverseResult#addToExistingResultChain(TraverseResult)} and
 * {@link TraverseResult#getNextResult()} that allows one to construct a chain of multiple traverse
 * result objects that can be returned from an {@link Edge} traversal operation. It's important to
 * note that while this can be a powerful mechanism to allow for more dynamic edge-traversal
 * behavior, care must be taken when implementing.
 * 
 * Specifically, we currently forbid an edge from returning multiple results in both
 * {@link #traverse(State, TraverseOptions)} and {@link #traverseBack(State, TraverseOptions)}. If
 * one traverse operations returns multiple results, the inverse operation must always return a
 * single result. We've set this rule primarily to support the reverse path optimization in
 * {@link GraphPath#optimize()}.
 * 
 * If you think of a compelling reason where you need multiple traverse results in BOTH directions,
 * let us know.
 */
public interface Edge {

    public Vertex getFromVertex();

    public State traverse(State s0);

    /**
     * Alternate traversal method that provides a lower bound on the changes in weight and time that
     * will occur when traversing an edge. Used by RemainingWeightHeuristics.
     */
    public State optimisticTraverse(State s0);

    /**
     * Intended to replace optimisticTraverse, on the grounds that the lower bound on an edge's weight 
     * should not be path-dependent, so the state is not needed.
     * 
     * @return a lower bound on this edge's weight, given these {@link TraverseOptions}.
     * Double.POSITIVE_INFINITY if the edge cannot be traversed with the given {@link TraverseOptions}.
     */
    public double weightLowerBound(TraverseOptions options);

    public void addPatch(Patch patch);

    public List<Patch> getPatches();

    public void removePatch(Patch patch);
}
