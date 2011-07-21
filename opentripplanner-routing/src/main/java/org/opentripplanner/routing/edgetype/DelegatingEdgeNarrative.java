/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.edgetype;

import java.util.Set;

import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.core.EdgeNarrative;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.Vertex;

import com.vividsolutions.jts.geom.Geometry;

public abstract class DelegatingEdgeNarrative implements EdgeNarrative {
    protected EdgeNarrative base;

    public DelegatingEdgeNarrative(EdgeNarrative base) {
	this.base = base;
    }

    @Override
    public double getDistance() {
        return base.getDistance();
    }

    @Override
    public Geometry getGeometry() {
        return base.getGeometry();
    }

    @Override
    public TraverseMode getMode() {
        return base.getMode();
    }

    @Override
    public String getName() {
        return base.getName();
    }
    
    public Set<String> getNotes() {
    	return base.getNotes();
    }

    @Override
    public Trip getTrip() {
        return base.getTrip();
    }

    public Vertex getFromVertex() {
        return base.getFromVertex();
    }

    public Vertex getToVertex() {
        return base.getToVertex();
    }

    @Override
    public boolean isRoundabout() {
        return base.isRoundabout();
    }

    public boolean hasBogusName() {
        return base.hasBogusName();
    }
}