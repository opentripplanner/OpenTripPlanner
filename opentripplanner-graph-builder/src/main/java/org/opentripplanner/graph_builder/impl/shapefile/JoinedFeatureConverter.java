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

package org.opentripplanner.graph_builder.impl.shapefile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opentripplanner.graph_builder.services.shapefile.FeatureSourceFactory;
import org.opentripplanner.graph_builder.services.shapefile.SimpleFeatureConverter;

/**
 * This is a simple feature converter that gets features from a secondary feature source. This is
 * useful if you have (say) bike lane data in another file.
 */
public class JoinedFeatureConverter<T> implements SimpleFeatureConverter<T> {

    private SimpleFeatureConverter<T> converter;

    private String joinedKey;

    private String mainKey;

    private FeatureSource<SimpleFeatureType, SimpleFeature> joinedSource;

    private HashMap<String, SimpleFeature> cache;

    public JoinedFeatureConverter() {
    }

    public JoinedFeatureConverter(String mainKey, String joinedKey,
            SimpleFeatureConverter<T> converter,
            FeatureSource<SimpleFeatureType, SimpleFeature> joinedSource) {
        this.mainKey = mainKey;
        this.joinedKey = joinedKey;
        this.converter = converter;
        this.joinedSource = joinedSource;
    }

    @Override
    public T convert(SimpleFeature feature) {
        ensureCached();
        String mainKeyValue = feature.getAttribute(this.mainKey).toString();
        SimpleFeature joinedFeature = cache.get(mainKeyValue);

        if (joinedFeature == null) {
            return null;
        } else {
            return converter.convert(joinedFeature);
        }
    }

    /** We have to cache all the features in the supplemenetal file, because
     * if we try to load them on the fly, Geotools wigs out.
     */
    private void ensureCached() {
        if (cache != null) {
            return;
        }
        cache = new HashMap<String, SimpleFeature>();
        try {
            FeatureCollection<SimpleFeatureType, SimpleFeature> features = joinedSource
                    .getFeatures();
            Iterator<SimpleFeature> it = features.iterator();
            while (it.hasNext()) {
                SimpleFeature feature = it.next();
                String joinedKeyValue = feature.getAttribute(joinedKey).toString();
                cache.put(joinedKeyValue, feature);
            }
            features.close(it);

        } catch (IOException e) {
            throw new RuntimeException("Could not cache values for joined shapefile", e);
        }
    }

    public void setConverter(SimpleFeatureConverter<T> converter) {
        this.converter = converter;
    }

    public void setJoinedKey(String joinedKey) {
        this.joinedKey = joinedKey;
    }

    public void setMainKey(String mainKey) {
        this.mainKey = mainKey;
    }

    public void setJoinedSourceFactory(FeatureSourceFactory factory) {
        this.joinedSource = factory.getFeatureSource();
    }

    public void setJoinedSource(FeatureSource<SimpleFeatureType, SimpleFeature> joinedSource) {
        this.joinedSource = joinedSource;
    }
}
