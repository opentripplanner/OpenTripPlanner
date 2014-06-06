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

package org.opentripplanner.graph_builder.impl.osm;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Describes how unnamed OSM ways are to be named based on the tags they possess.
 * The CreativeNamer will be applied to ways that match the OSMSpecifier.
 * @author novalis
 */
@Data @NoArgsConstructor @AllArgsConstructor
public class CreativeNamerPicker {
    private OSMSpecifier specifier;
    private CreativeNamer namer;
}
