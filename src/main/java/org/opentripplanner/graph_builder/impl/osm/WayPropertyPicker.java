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

/**
 * Associates an OSMSpecifier with some WayProperties. The WayProperties will be applied an OSM way when the OSMSpecifier
 * matches it better than any other OSMSpecifier in the same WayPropertySet. WayPropertyPickers may be mixins, in which
 * case they do not need to beat out all the other WayPropertyPickers. Instead, their safety values will be
 * applied to all ways that they match multiplicatively.
 */
public class WayPropertyPicker {

    private OSMSpecifier specifier;

    private WayProperties properties;

    private boolean safetyMixin;

    public WayPropertyPicker() {
    }

    public WayPropertyPicker(OSMSpecifier specifier, WayProperties properties, boolean mixin) {
        this.specifier = specifier;
        this.properties = properties;
        this.safetyMixin = mixin;
    }

    public void setSpecifier(OSMSpecifier specifier) {
        this.specifier = specifier;
    }

    public OSMSpecifier getSpecifier() {
        return specifier;
    }

    public void setProperties(WayProperties properties) {
        this.properties = properties;
    }

    public WayProperties getProperties() {
        return properties;
    }

    public void setSafetyMixin(boolean mixin) {
        this.safetyMixin = mixin;
    }

    /**
     * If this value is true, and this picker's specifier applies to a given way, then this picker is never
     * chosen as the most applicable value, and the final safety will be multiplied by this value.
     * More than one mixin may apply.
     */
    public boolean isSafetyMixin() {
        return safetyMixin;
    }
}
