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

import org.opentripplanner.routing.algorithm.NegativeWeightException;

import com.vividsolutions.jts.geom.Geometry;

public interface Edge {

    public Vertex getFromVertex();

    public Vertex getToVertex();

    public TraverseResult traverse(State s0, TraverseOptions wo) throws NegativeWeightException;
        
    public TraverseResult traverseBack(State s0, TraverseOptions wo) throws NegativeWeightException;

    public TraverseMode getMode();

    public String getName();

    public String getDirection();

    public Geometry getGeometry();

    public double getDistance();
}