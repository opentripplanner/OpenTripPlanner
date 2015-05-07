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

import java.beans.PropertyEditorSupport;

import org.opentripplanner.common.model.P2;

public class SafetyFeaturesEditor extends PropertyEditorSupport {
    private P2<Double> value;

    public void setAsText(String safetyFeatures) {
        String[] strings = safetyFeatures.split(",");
        value = new P2<Double>(Double.parseDouble(strings[0]), Double.parseDouble(strings[1]));
    }

    public String getAsText() {
        return value.first + ", " + value.second;
    }

    public Object getValue() {
        return value;
    }

    @SuppressWarnings("unchecked")
    public void setValue(Object object) {
        value = (P2<Double>) object;
    }
}
