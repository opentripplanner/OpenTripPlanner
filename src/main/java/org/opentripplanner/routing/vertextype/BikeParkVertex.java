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

package org.opentripplanner.routing.vertextype;

import org.opentripplanner.common.MavenVersion;
import org.opentripplanner.routing.bike_park.BikePark;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.util.NonLocalizedString;

/**
 * A vertex for a bike park.
 * 
 * Connected to streets by StreetBikeParkLink. Transition for parking the bike is handled by
 * BikeParkEdge.
 * 
 * Bike park-and-ride and "OV-fiets mode" development has been funded by GoAbout
 * (https://goabout.com/).
 * 
 * @author laurent
 * @author GoAbout
 * 
 */
public class BikeParkVertex extends Vertex {

    private static final long serialVersionUID = MavenVersion.VERSION.getUID();

    private int spacesAvailable;

    private String id;

    public BikeParkVertex(Graph g, BikePark bikePark) {
        //TODO: localize bikepark
        super(g, "bike park " + bikePark.id, bikePark.x, bikePark.y, new NonLocalizedString(bikePark.name));
        this.setId(bikePark.id);
        this.setSpacesAvailable(bikePark.spacesAvailable);
    }

    public int getSpacesAvailable() {
        return spacesAvailable;
    }

    public void setSpacesAvailable(int spaces) {
        this.spacesAvailable = spaces;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
