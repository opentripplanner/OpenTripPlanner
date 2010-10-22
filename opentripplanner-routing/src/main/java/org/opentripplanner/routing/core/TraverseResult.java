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

/**
 * 
 * 
 * @author bdferris
 * 
 */
public class TraverseResult {

    public double weight;

    public State state;

    /**
     * Traditionally, we examine {@link Edge#getFromVertex()} and {@link Edge#getToVertex()} for
     * determining the source and dest vertex in edge traversal. However, in the case of multiple
     * traversal results, we allow the result to override the target vertex.
     */
    private Vertex vertex;

    /**
     * Optional next result that allows {@link Edge} to return multiple results from
     * {@link Edge#traverse(State, TraverseOptions)} or
     * {@link Edge#traverseBack(State, TraverseOptions)}
     */
    private TraverseResult nextResult;

    public TraverseResult(double weight, State sprime) {
        this.weight = weight;
        this.state = sprime;
    }

    public String toString() {
        return this.weight + " " + this.state;
    }

    /**
     * Traditionally, we examine {@link Edge#getFromVertex()} and {@link Edge#getToVertex()} for
     * determining the source and dest vertex in edge traversal. However, in the case of multiple
     * traversal results, we allow the result to override the target vertex.
     * 
     * @return the target vertex for a traversal result, or null if the default edge vertex should
     *         be used.
     */
    public Vertex getVertex() {
        return vertex;
    }

    /**
     * See {@link #getVertex()}.
     * 
     * @param vertex
     *            the target vertext for a traversal result
     */
    public void setVertex(Vertex vertex) {
        this.vertex = vertex;
    }

    /**
     * Optional next result that allows {@link Edge} to return multiple results from
     * {@link Edge#traverse(State, TraverseOptions)} or
     * {@link Edge#traverseBack(State, TraverseOptions)}
     * 
     * @return the next additional result from an edge traversal, or null if no more results
     */
    public TraverseResult getNextResult() {
        return nextResult;
    }

    /**
     * Extend an exiting result chain by appending this result to the existing chain. The usage
     * model looks like this:
     * 
     * <code>
     * TraverseResult result = null;
     * 
     * for( ... ) {
     *   TraverseResult individualResult = ...;
     *   result = individualResult.addToExistingResultChain(result);
     * }
     * 
     * return result;
     * </code>
     * 
     * @param existingResultChain
     *            the tail of an existing result chain, or null if the chain has not been started
     * @return
     */
    public TraverseResult addToExistingResultChain(TraverseResult existingResultChain) {
        if (this.nextResult != null)
            throw new IllegalStateException("this result already has a next result set");
        this.nextResult = existingResultChain;
        return this;
    }
}