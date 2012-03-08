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

package org.opentripplanner.graph_builder.services.osm;

import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.graph.Graph;

/**
 * For when CreativeNamePicker/WayPropertySet is just not powerful enough.
 * 
 * @author novalis
 * 
 */
public interface CustomNamer {
    public String name(OSMWithTags way, String defaultName);

    public void nameWithEdge(OSMWithTags way, PlainStreetEdge edge);

    public void postprocess(Graph graph);
}
