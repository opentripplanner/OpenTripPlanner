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

import org.onebusaway.gtfs.model.Trip;

public abstract class AbstractEdge implements DirectEdge, Serializable {

    private static final long serialVersionUID = 1L;

    protected Vertex fromv;

    protected Vertex tov;

    public String toString() {
        return fromv + "-> " + tov;
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
    
    @Override
    public int hashCode() {
        return fromv.hashCode() * 31 + tov.hashCode();
    }

    @Override
    public boolean isRoundabout() {
        return false;
    }
}
