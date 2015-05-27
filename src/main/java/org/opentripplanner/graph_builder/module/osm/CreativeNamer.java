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

package org.opentripplanner.graph_builder.module.osm;

import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.LocalizedString;

/**
 * A CreativeNamer makes up names for ways that don't have one in the OSM data set.
 * It does this by substituting the values of OSM tags into a template.
 */
public class CreativeNamer {

    /**
     * A creative name pattern is a template which may contain variables of the form {{tag_name}}.
     * When a way's creative name is created, the value of its tag tag_name is substituted for the
     * variable.
     * 
     * For example, "Highway with surface {{surface}}" might become "Highway with surface gravel"
     */
    private String creativeNamePattern;

    public CreativeNamer(String pattern) {
        this.creativeNamePattern = pattern;
    }

    public CreativeNamer() {
    }
   
    public I18NString generateCreativeName(OSMWithTags way) {
        return new LocalizedString(creativeNamePattern, way);
    }

    public void setCreativeNamePattern(String creativeNamePattern) {
        this.creativeNamePattern = creativeNamePattern;
    }

    public String getCreativeNamePattern() {
        return creativeNamePattern;
    }

}
