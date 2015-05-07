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
import org.opentripplanner.graph_builder.services.shapefile.SimpleFeatureConverter;

/**
 * A converter which converts null-valued attributes to either true or false (and all others to the other)
 * 
 * @author novalis
 * 
 */
public class NullBooleanConverter implements SimpleFeatureConverter<Boolean> {

    private String attributeName;
    
    private boolean nullIsTrue = false;

    public NullBooleanConverter() {
    }

    public NullBooleanConverter(String attributeName, boolean nullIsTrue) {
        this.attributeName = attributeName;
        this.nullIsTrue = nullIsTrue;
    }
    
    @Override
    public Boolean convert(SimpleFeature feature) {
        Object value = feature.getAttribute(attributeName);

        if (value == null || value.equals("")) {
            return nullIsTrue;
        } else {
            return !nullIsTrue;
        }
    }

    /**
     * @param attributeName the attribute name to set
     */
    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    /**
     * @return the attribute name
     */
    public String getAttributeName() {
        return attributeName;
    }

    /**
     * @param nullIsTrue whether a null value for this attribue converts to false
     */
    public void setNullIsTrue(boolean nullIsTrue) {
        this.nullIsTrue = nullIsTrue;
    }

    /**
     * @return whether a null value for this attribute converts to true
     */
    public boolean getNullIsTrue() {
        return nullIsTrue;
    }

}
