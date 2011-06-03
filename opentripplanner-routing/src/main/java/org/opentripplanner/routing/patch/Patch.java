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

package org.opentripplanner.routing.patch;

import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;

public interface Patch {
	public String getNotes();
	
	public void apply(Graph graph);
	public void remove(Graph graph);
	
	public TraverseResult addTraverseResult(Edge edge, State state, TraverseOptions options);
	public TraverseResult addTraverseResultBack(Edge edge, State state, TraverseOptions options);
	
	public TraverseResult filterTraverseResults(TraverseResult result);
	public boolean activeDuring(long startTime, long time);

	
}
