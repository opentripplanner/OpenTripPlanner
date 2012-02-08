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

package org.opentripplanner.graph_builder.impl.reach;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.graph.Vertex;

public class ReachState extends State {
    public ReachState(Vertex v, TraverseOptions opt) {
        super(v, opt);
    }

    public ArrayList<ReachState> children;
    
    public Double height = null;

    public void addChild(ReachState s) {
        if (children == null) {
            children = new ArrayList<ReachState>();
        }
        children.add(s);
    }
    
    public Collection<ReachState> getChildren() {
        if (children == null) {
            return Collections.emptyList();
        }
        return children;
    }
}
