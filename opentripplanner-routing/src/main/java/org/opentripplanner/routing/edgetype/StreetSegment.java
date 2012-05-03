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

package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.core.RoutingRequest;

/*
 * StreetSegment will hold the information for a single piece of street covered by around six 
 * TurnEdges. It should replace StreetVertex, and be refrenced from the edges themselves.
 * 
 * It should be subclassed for specific kinds of street segments, like stairs or roundabouts.
 */
public interface StreetSegment {

    public double getWeight(RoutingRequest options);

}
