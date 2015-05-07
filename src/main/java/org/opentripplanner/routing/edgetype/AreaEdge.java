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

import com.vividsolutions.jts.geom.LineString;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.NonLocalizedString;
import org.opentripplanner.routing.vertextype.IntersectionVertex;


public class AreaEdge extends StreetWithElevationEdge implements EdgeWithCleanup {
    private static final long serialVersionUID = 6761687673982054612L;
    private AreaEdgeList area;

    public AreaEdge(IntersectionVertex startEndpoint, IntersectionVertex endEndpoint,
            LineString geometry, I18NString name, double length, StreetTraversalPermission permissions,
            boolean back, AreaEdgeList area) {
        super(startEndpoint, endEndpoint, geometry, name, length, permissions, back);
        this.area = area;
        area.addEdge(this);
    }

    //For testing only
    public AreaEdge(IntersectionVertex startEndpoint,
            IntersectionVertex endEndpoint, LineString geometry, String name,
            double length, StreetTraversalPermission permissions, boolean back,
            AreaEdgeList area) {
        this(startEndpoint, endEndpoint, geometry, new NonLocalizedString(name),
                length, permissions, back, area);
    }

    public AreaEdgeList getArea() {
        return area;
    }

    @Override
    public void detach() {
        area.removeEdge(this);
    }

    public void setArea(AreaEdgeList area) {
        this.area = area;
    }
}
