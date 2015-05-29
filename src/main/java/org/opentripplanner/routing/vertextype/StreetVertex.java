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

import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;

import com.vividsolutions.jts.geom.Coordinate;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.LocalizedString;
import org.opentripplanner.util.NonLocalizedString;

import java.util.*;

/**
 * Abstract base class for vertices in the street layer of the graph.
 * This includes both vertices representing intersections or points (IntersectionVertices) 
 * and Elevator*Vertices.
 */
public abstract class StreetVertex extends Vertex {

    private static final long serialVersionUID = 1L;

    public StreetVertex(Graph g, String label, Coordinate coord, I18NString streetName) {
        this(g, label, coord.x, coord.y, streetName);
    }

    public StreetVertex(Graph g, String label, double x, double y, I18NString streetName) {
        super(g, label, x, y, streetName);
    }

    /**
     * Creates intersection name out of all outgoing names
     *
     * This can be:
     *  - name of the street if it is only 1
     *  - unnamedStreed (localized in requested language) if it doesn't have a name
     *  - corner of 0 and 1 (localized corner of zero and first street in the corner)
     *
     * @param locale Wanted locale
     * @return already localized street names and non-localized corner of x and unnamedStreet
     */
    public I18NString getIntersectionName(Locale locale) {
        I18NString calculatedName = null;
        // generate names for corners when no name was given
        Set<String> uniqueNameSet = new HashSet<String>();
        for (Edge e : getOutgoing()) {
            if (e instanceof StreetEdge) {
                uniqueNameSet.add(e.getName(locale));
            }
        }
        List<String> uniqueNames = new ArrayList<String>(uniqueNameSet);

        if (uniqueNames.size() > 1) {
            calculatedName = new LocalizedString("corner", new String[]{uniqueNames.get(0),
                uniqueNames.get(1)});
        } else if (uniqueNames.size() == 1) {
            calculatedName = new NonLocalizedString(uniqueNames.get(0));
        } else {
            calculatedName = new LocalizedString("unnamedStreet", (String[]) null);
        }
        return calculatedName;
    }
}
