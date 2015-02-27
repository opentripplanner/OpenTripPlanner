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

import java.io.File;

import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opentripplanner.graph_builder.services.shapefile.FeatureSourceFactory;

public class ShapefileFeatureSourceFactoryImpl implements FeatureSourceFactory {

    private File _path;
    private ShapefileDataStore dataStore;

    public ShapefileFeatureSourceFactoryImpl() {
        
    }
    
    public ShapefileFeatureSourceFactoryImpl(File path) {
        _path = path;
    }

    public void setPath(File path) {
        _path = path;
    }

    @Override
    public FeatureSource<SimpleFeatureType, SimpleFeature> getFeatureSource() {

        try {
            dataStore = new ShapefileDataStore(_path.toURI().toURL());

            String typeNames[] = dataStore.getTypeNames();
            String typeName = typeNames[0];

            return dataStore.getFeatureSource(typeName);
        } catch (Exception ex) {
            throw new IllegalStateException("error creating feature source from shapefile: path="
                    + _path, ex);
        }
    }
    
    @Override
    public void cleanup() {
        dataStore.dispose();
    }

    @Override
    public void checkInputs() {
        if (!_path.canRead()) {
            throw new RuntimeException("Can't read Shapefile path: " + _path);
        }
    }
}
