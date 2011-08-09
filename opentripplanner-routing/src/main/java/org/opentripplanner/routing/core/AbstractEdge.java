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


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.patch.Alert;
import org.opentripplanner.routing.patch.Patch;

public abstract class AbstractEdge implements DirectEdge, Serializable {

    private static final long serialVersionUID = 1L;

    /* protected rather than private because these are overwritten during deserialization */
    
    protected transient Vertex fromv;

    protected transient Vertex tov;

    private List<Patch> patches;

    public String toString() {
        return getClass().getName() + "(" + fromv + " -> " + tov + ")";
    }

    public AbstractEdge(Vertex v1, Vertex v2) {
        this.fromv = v1;
        this.tov = v2;
    }

    @Override
    public Vertex getFromVertex() {
        return fromv;
    }

    @Override
    public Vertex getToVertex() {
        return tov;
    }

    public void setFromVertex(Vertex fromv) {
        this.fromv = fromv;
    }

    public void setToVertex(Vertex tov) {
        this.tov = tov;
    }

    public Trip getTrip() {
        return null;
    }
    

    public Set<Alert> getNotes() {
    	return null;
    }
    
    @Override
    public int hashCode() {
        return fromv.hashCode() * 31 + tov.hashCode();
    }

    @Override
    public boolean isRoundabout() {
        return false;
    }
    
    @Override
    public State optimisticTraverse (State s0) {
    	return this.traverse(s0);
    }
    
    /* Edge weights are non-negative. Zero is an admissible default lower bound. */
    public double weightLowerBound(TraverseOptions options) {
    	return 0;
    }
        
    /* No edge should take less than zero time to traverse. */
    public double timeLowerBound(TraverseOptions options) {
        return 0;
    }

    @Override
    public void addPatch(Patch patch) {
    	if (patches == null) {
    		patches = new ArrayList<Patch>();
    	}
    	patches.add(patch);
    }

    public List<Patch> getPatches() {
        if (patches == null) {
            return Collections.emptyList();
        }
        return patches;
    }

    @Override
    public void removePatch(Patch patch) {
        if (patches.size() == 1) {
            patches = null;
        } else {
            patches.remove(patch);
        }
    }

    public boolean hasBogusName() {
        return false;
    }
}
