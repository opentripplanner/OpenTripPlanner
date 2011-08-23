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

import java.util.Set;

import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.patch.Alert;

import com.vividsolutions.jts.geom.Geometry;

/**
 * An EdgeNarrative holds the data needed to generate a human-readable
 * set of directions for an edge.
 * @author novalis
 *
 */
public interface EdgeNarrative {

    public Vertex getFromVertex();

    public Vertex getToVertex();

    public TraverseMode getMode();

    public String getName();

    /* True if the name is automatically generated rather than coming from OSM or GTFS */
    public boolean hasBogusName();

    public Geometry getGeometry();

    public double getDistance();

    public Trip getTrip();

    public boolean isRoundabout();

    public Set<Alert> getNotes();

}
