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

import java.util.HashMap;
import java.util.Map;

import org.opengis.feature.simple.SimpleFeature;
import org.opentripplanner.graph_builder.services.shapefile.SimpleFeatureConverter;

public class CaseBasedBooleanConverter implements SimpleFeatureConverter<Boolean> {
    public boolean defaultValue = false;

    private Map<String, Boolean> values = new HashMap<String, Boolean>();

    private String attributeName;

    public CaseBasedBooleanConverter() {

    }

    public CaseBasedBooleanConverter(String attributeName) {
        this.attributeName = attributeName;
    }

    public CaseBasedBooleanConverter(String attributeName,
            Boolean defaultValue) {
        this.attributeName = attributeName;
        this.defaultValue = defaultValue; 
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public void setDefaultValue(Boolean defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void setValues(Map<String, Boolean> values) {
        this.values = values; 
    }

    @Override
    public Boolean convert(SimpleFeature feature) {
        String key = feature.getAttribute(attributeName).toString();
        Boolean value = values.get(key);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }
}
