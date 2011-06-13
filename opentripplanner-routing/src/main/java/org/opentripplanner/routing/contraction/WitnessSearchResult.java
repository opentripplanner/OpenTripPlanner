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

package org.opentripplanner.routing.contraction;

import java.util.List;

import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.spt.BasicShortestPathTree;

public class WitnessSearchResult {
    public List<Shortcut> shortcuts;
    public int searchSpace;
    public BasicShortestPathTree spt;
    public Vertex vertex;
    
    /**
     * Result of a local witness search during CH building.
     * 
     * @param shortcuts - the new shortcuts suggested by this search
     * @param spt - an spt containing all states produced during the search
     * @param vertex - the start point of this search V (not U, the taboo vertex)
     * @param searchSpace - number of nodes visited
     */
    public WitnessSearchResult(List<Shortcut> shortcuts, BasicShortestPathTree spt, Vertex vertex, int searchSpace) {
        this.shortcuts = shortcuts;
        this.spt = spt;
        this.vertex = vertex; 
        this.searchSpace = searchSpace;
    }
}
