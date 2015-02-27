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
import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.services.shapefile.SimpleFeatureConverter;

/*
 * Read safety factors directly from shapefiles (contributed by Guillaume Barreau)
 */
public class DirectSafetyReader implements SimpleFeatureConverter<P2<Double>> {
    private String safetyAttributeName;

    public static final P2<Double> oneone = new P2<Double>(1.0, 1.0);

    @Override
    public P2<Double> convert(SimpleFeature feature) {
        Double d = (Double) feature.getAttribute(safetyAttributeName);
        if (d == null) {
            return oneone;
        }
        return new P2<Double>(d, d);
    }

    public void setSafetyAttributeName(String safetyAttributeName) {
        this.safetyAttributeName = safetyAttributeName;
    }
}
