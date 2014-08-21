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

package org.opentripplanner.analyst.batch;

import java.io.File;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class ShapefilePopulation extends BasicPopulation {

    private static final Logger LOG = LoggerFactory.getLogger(ShapefilePopulation.class);

    public String labelAttribute;

    public String inputAttribute;
    
    @Override
    public void createIndividuals() {
        String filename = this.sourceFilename;
        LOG.debug("Loading population from shapefile {}", filename);
        LOG.debug("Feature attributes: input data in {}, labeled with {}", inputAttribute, labelAttribute);
        try {
            File file = new File(filename);
            if ( ! file.exists())
                throw new RuntimeException("Shapefile does not exist.");
            FileDataStore store = FileDataStoreFinder.getDataStore(file);
            SimpleFeatureSource featureSource = store.getFeatureSource();

            CoordinateReferenceSystem sourceCRS = featureSource.getInfo().getCRS();
            CoordinateReferenceSystem WGS84 = CRS.decode("EPSG:4326", true);

            Query query = new Query();
            query.setCoordinateSystem(sourceCRS);
            query.setCoordinateSystemReproject(WGS84);
            SimpleFeatureCollection featureCollection = featureSource.getFeatures(query);

            SimpleFeatureIterator it = featureCollection.features();
            int i = 0;
            while (it.hasNext()) {
                SimpleFeature feature = it.next();
                Geometry geom = (Geometry) feature.getDefaultGeometry();
                Point point = null;
                if (geom instanceof Point) {
                    point = (Point) geom;
                } else if (geom instanceof Polygon) {
                    point = ((Polygon) geom).getCentroid();
                } else if (geom instanceof MultiPolygon) {
                    point = ((MultiPolygon) geom).getCentroid();
                } else {
                    throw new RuntimeException("Shapefile must contain either points or polygons.");
                }
                String label;
                if (labelAttribute == null) {
                    label = Integer.toString(i);
                } else {
                    label = feature.getAttribute(labelAttribute).toString();
                }
                double input = 0.0;
                if (inputAttribute != null) {
                    Number n = (Number) feature.getAttribute(inputAttribute);
                    input = n.doubleValue(); 
                }
                Individual individual = new Individual(label, point.getX(), point.getY(), input);
                this.addIndividual(individual);
                i += 1;
            }
            LOG.debug("loaded {} features", i);
            it.close();
        } catch (Exception ex) {
            LOG.error("Error loading population from shapefile: {}", ex.getMessage());
            throw new RuntimeException(ex);
        }
        LOG.debug("Done loading shapefile.");
    }

}
