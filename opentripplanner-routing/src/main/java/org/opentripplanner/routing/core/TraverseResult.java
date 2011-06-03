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

    private final EdgeNarrative edgeNarrative;

    /**
     * Optional next result that allows {@link Edge} to return multiple results from
     * {@link Edge#traverse(State, TraverseOptions)} or
     * {@link Edge#traverseBack(State, TraverseOptions)}
     */
    private TraverseResult nextResult;

    public TraverseResult(double weight, State sprime, EdgeNarrative edgeNarrative) {
        this.weight = weight;
        this.state = sprime;
        this.edgeNarrative = edgeNarrative;
    }

    public EdgeNarrative getEdgeNarrative() {
        return edgeNarrative;
    }

    public String toString() {
        return this.weight + " " + this.state;
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
        if (this.getNextResult() != null)
            throw new IllegalStateException("this result already has a next result set");
        nextResult = existingResultChain;
        return this;
    }
}