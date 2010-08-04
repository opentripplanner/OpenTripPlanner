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

package org.opentripplanner.graph_builder.services.shapefile;

import org.opengis.feature.simple.SimpleFeature;

/**
 * Interface for converters from an opengis @{link org.opengis.feature.simple.SimpleFeature} 
 * to an object of type T
 * 
 * @param <T> the type to convert to.
 */
public interface SimpleFeatureConverter<T> {
    public T convert(SimpleFeature feature);
}
