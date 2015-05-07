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

package org.opentripplanner.graph_builder.module.shapefile;

import org.opengis.feature.simple.SimpleFeature;

/**
 * A converter for extracting string attributes from features. The converter supports the
 * specification of a default value, which is useful if you are reading from data sources that might
 * contain blank (or null) values (e.g., if you're reading street name values, you might want to
 * default to "Unnamed street" instead of null or " ").
 * 
 * @author nicholasbs
 * 
 */
public class StringAttributeFeatureConverter extends AttributeFeatureConverter<String> {

    private String _defaultValue;

    public StringAttributeFeatureConverter() {

    }

    public StringAttributeFeatureConverter(String attributeName, String defaultValue) {
        super(attributeName);
        _defaultValue = defaultValue;
    }

    public StringAttributeFeatureConverter(String attributeName) {
        super(attributeName);
        _defaultValue = null;
    }

    /**
     * The default value to assign to features with null or empty (" ") values.
     */
    public void setDefaultValue(String defaultValue) {
        _defaultValue = defaultValue;
    }

    @Override
    public String convert(SimpleFeature feature) {
        String attr = (String) feature.getAttribute(getAttributeName());
        // Since dBase (used in shapefiles) has poor/no null support, null strings are sometimes
        // stored as a single space " "
        if (attr == null || attr.equals(" ")) {
            attr = _defaultValue;
        }
        return attr;
    }

}
