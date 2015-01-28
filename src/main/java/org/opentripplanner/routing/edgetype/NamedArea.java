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

import java.io.Serializable;

import com.vividsolutions.jts.geom.Geometry;
import org.opentripplanner.util.I18NString;

/**
 * A named area is a subset of an area with a certain set of properties
 * (name, safety, etc). Its originalEdges may include some edges which are 
 * crossable (because they separate it from another contiguous and
 * routeable area).
 * 
 */
public class NamedArea implements Serializable {
    private static final long serialVersionUID = 3570078249065754760L;

    private Geometry originalEdges;

    private I18NString name;

    private double bicycleSafetyMultiplier;

    private int streetClass;

    private StreetTraversalPermission permission;

    public String getName() {
        return name.toString();
    }

    public I18NString getRawName() {
        return name;
    }

    public void setName(I18NString name) {
        this.name = name;
    }

    public Geometry getPolygon() {
        return originalEdges;
    }

    public void setOriginalEdges(Geometry originalEdges) {
        this.originalEdges = originalEdges;
    }

    public double getBicycleSafetyMultiplier() {
        return bicycleSafetyMultiplier;
    }

    public void setBicycleSafetyMultiplier(double bicycleSafetyMultiplier) {
        this.bicycleSafetyMultiplier = bicycleSafetyMultiplier;
    }

    public StreetTraversalPermission getPermission() {
        return permission;
    }

    public int getStreetClass() {
        return streetClass;
    }

    public void setStreetClass(int streetClass) {
        this.streetClass = streetClass;
    }

    public void setPermission(StreetTraversalPermission permission) {
        this.permission = permission;
    }
}
