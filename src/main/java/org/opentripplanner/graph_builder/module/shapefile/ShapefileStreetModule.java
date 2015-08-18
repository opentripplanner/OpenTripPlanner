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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.factory.Hints;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.services.DefaultStreetEdgeFactory;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.graph_builder.services.StreetEdgeFactory;
import org.opentripplanner.graph_builder.services.shapefile.FeatureSourceFactory;
import org.opentripplanner.graph_builder.services.shapefile.SimpleFeatureConverter;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.notes.StreetNotesService;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import org.opentripplanner.util.NonLocalizedString;

/**
 * Loads a shapefile into an edge-based graph.
 *
 */
public class ShapefileStreetModule implements GraphBuilderModule {
    private static Logger log = LoggerFactory.getLogger(ShapefileStreetModule.class);

    private FeatureSourceFactory _featureSourceFactory;

    private ShapefileStreetSchema _schema;

    public StreetEdgeFactory edgeFactory = new DefaultStreetEdgeFactory();

    public List<String> provides() {
        return Arrays.asList("streets");
    }

    public List<String> getPrerequisites() {
        return Collections.emptyList();
    }
    
    public void setFeatureSourceFactory(FeatureSourceFactory factory) {
        _featureSourceFactory = factory;
    }

    public void setSchema(ShapefileStreetSchema schema) {
        _schema = schema;
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {

        try {

            FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = _featureSourceFactory
                    .getFeatureSource();
            CoordinateReferenceSystem sourceCRS = featureSource.getInfo().getCRS();

            Hints hints = new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);
            CRSAuthorityFactory factory = ReferencingFactoryFinder.getCRSAuthorityFactory("EPSG",
                    hints);
            CoordinateReferenceSystem worldCRS = factory
                    .createCoordinateReferenceSystem("EPSG:4326");

            Query query = new Query();
            query.setCoordinateSystem(sourceCRS);
            query.setCoordinateSystemReproject(worldCRS);

            FeatureCollection<SimpleFeatureType, SimpleFeature> features = featureSource
                    .getFeatures(query);

            features = featureSource.getFeatures(query);

            HashMap<String, HashMap<Coordinate, Integer>> intersectionNameToId = new HashMap<String, HashMap<Coordinate, Integer>>();

            SimpleFeatureConverter<String> streetIdConverter = _schema.getIdConverter();
            SimpleFeatureConverter<String> streetNameConverter = _schema.getNameConverter();
            SimpleFeatureConverter<P2<StreetTraversalPermission>> permissionConverter = _schema
                    .getPermissionConverter();
            SimpleFeatureConverter<String> noteConverter = _schema.getNoteConverter();

            HashMap<Coordinate, IntersectionVertex> intersectionsByLocation = 
                    new HashMap<Coordinate, IntersectionVertex>();

            SimpleFeatureConverter<P2<Double>> safetyConverter = _schema.getBicycleSafetyConverter();

            SimpleFeatureConverter<Boolean> slopeOverrideCoverter = _schema.getSlopeOverrideConverter();

            SimpleFeatureConverter<Boolean> featureSelector = _schema.getFeatureSelector();
            
            // Keep track of features that are duplicated so we don't have duplicate streets
            Set<Object> seen = new HashSet<Object>();

            List<SimpleFeature> featureList = new ArrayList<SimpleFeature>();
            FeatureIterator<SimpleFeature> it2 = features.features();
            while (it2.hasNext()) {
                SimpleFeature feature = it2.next();
                if (featureSelector != null && ! featureSelector.convert(feature)) {
                    continue;
                }
                featureList.add(feature);
            }
            it2.close();
            it2 = null;

            HashMap<Coordinate, TreeSet<String>> coordinateToStreetNames = getCoordinatesToStreetNames(featureList);
            
            for (SimpleFeature feature : featureList) {
                if (feature.getDefaultGeometry() == null) {
                    log.warn("feature has no geometry: " + feature.getIdentifier());
                    continue;
                }
                LineString geom = toLineString((Geometry) feature.getDefaultGeometry());

                Object o = streetIdConverter.convert(feature);
                String label = "" + o;
                if (o != null && seen.contains(label)) {
                    continue;
                }
                seen.add(label);
                String name = streetNameConverter.convert(feature);
                Coordinate[] coordinates = geom.getCoordinates();

                if (coordinates.length < 2) {
                    //not a real linestring
                    log.warn("Bad geometry for street with label " + label + " name " + name);
                    continue;
                }
                
                // this rounding is a total hack, to work around
                // http://jira.codehaus.org/browse/GEOT-2811
                Coordinate startCoordinate = new Coordinate(
                        Math.round(coordinates[0].x * 1048576) / 1048576.0, Math
                                .round(coordinates[0].y * 1048576) / 1048576.0);
                Coordinate endCoordinate = new Coordinate(Math
                        .round(coordinates[coordinates.length - 1].x * 1048576) / 1048576.0, Math
                        .round(coordinates[coordinates.length - 1].y * 1048576) / 1048576.0);

                String startIntersectionName = getIntersectionName(coordinateToStreetNames,
                        intersectionNameToId, startCoordinate);

                if (startIntersectionName == "null") {
                    log.warn("No intersection name for " + name);
                }

                String endIntersectionName = getIntersectionName(coordinateToStreetNames,
                        intersectionNameToId, endCoordinate);

                IntersectionVertex startIntersection = intersectionsByLocation.get(startCoordinate);
                if (startIntersection == null) {
                    startIntersection = new IntersectionVertex(graph, startIntersectionName, startCoordinate.x,
                            startCoordinate.y, new NonLocalizedString(startIntersectionName));
                    intersectionsByLocation.put(startCoordinate, startIntersection);
                }

                IntersectionVertex endIntersection = intersectionsByLocation.get(endCoordinate);
                if (endIntersection == null) {
                    endIntersection = new IntersectionVertex(graph, endIntersectionName, endCoordinate.x,
                            endCoordinate.y, new NonLocalizedString(endIntersectionName));
                    intersectionsByLocation.put(endCoordinate, endIntersection);
                }

                double length = 0;
                for (int i = 0; i < coordinates.length - 1; ++i) {
                    length += JTS.orthodromicDistance(coordinates[i],
                            coordinates[i + 1], worldCRS);
                }
                P2<StreetTraversalPermission> permissions = permissionConverter.convert(feature);

                // TODO Set appropriate car speed from shapefile source.
                StreetEdge street = edgeFactory.createEdge(startIntersection, endIntersection,
                        geom, new NonLocalizedString(name), length, permissions.first, false);
                LineString reversed = (LineString) geom.reverse();
                StreetEdge backStreet = edgeFactory.createEdge(endIntersection, startIntersection,
                        reversed, new NonLocalizedString(name), length, permissions.second, true);
                backStreet.shareData(street);

                if (noteConverter != null) {
                	String note = noteConverter.convert(feature);
                	if (note != null && note.length() > 0) {
				Alert noteAlert = Alert.createSimpleAlerts(note);
				graph.streetNotesService.addStaticNote(street, noteAlert, StreetNotesService.ALWAYS_MATCHER);
				graph.streetNotesService.addStaticNote(backStreet, noteAlert, StreetNotesService.ALWAYS_MATCHER);
                	}
                }

                boolean slopeOverride = slopeOverrideCoverter.convert(feature);
                street.setSlopeOverride(slopeOverride);
                backStreet.setSlopeOverride(slopeOverride);

                if (safetyConverter != null) {
                    P2<Double> safetyFactors = safetyConverter.convert(feature);
                    if (safetyFactors != null) {
                        street.setBicycleSafetyFactor(safetyFactors.first.floatValue());
                        backStreet.setBicycleSafetyFactor(safetyFactors.second.floatValue());
                    }
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("error loading shapefile street data", ex);
        } finally {
            _featureSourceFactory.cleanup();
        }       
    }

    private HashMap<Coordinate, TreeSet<String>> getCoordinatesToStreetNames(
            List<SimpleFeature> features) {
        HashMap<Coordinate, TreeSet<String>> coordinateToStreets = new HashMap<Coordinate, TreeSet<String>>();
        SimpleFeatureConverter<String> streetNameConverter = _schema.getNameConverter();

        SimpleFeatureConverter<Boolean> featureSelector = _schema.getFeatureSelector();
        Iterator<SimpleFeature> it = features.iterator();
        while (it.hasNext()) {
            SimpleFeature feature = it.next();
            if (featureSelector != null && !featureSelector.convert(feature)) {
                continue;
            }
            if (feature.getDefaultGeometry() == null) {
                log.warn("feature has no geometry: " + feature.getIdentifier());
                continue;
            }
            LineString geom = toLineString((Geometry) feature.getDefaultGeometry());

            for (Coordinate coord : geom.getCoordinates()) {
                // this rounding is a total hack, to work around
                // http://jira.codehaus.org/browse/GEOT-2811
                Coordinate rounded = new Coordinate(Math.round(coord.x * 1048576) / 1048576.0, Math
                        .round(coord.y * 1048576) / 1048576.0);

                TreeSet<String> streets = coordinateToStreets.get(rounded);
                if (streets == null) {
                    streets = new TreeSet<String>();
                    coordinateToStreets.put(rounded, streets);
                }
                String streetName = streetNameConverter.convert(feature);
                if (streetName == null) {
                	throw new IllegalStateException("Unexpectedly got null for a street name for feature at " + coord);
                }
                streets.add(streetName);
            }
        }

        return coordinateToStreets;
    }

    private String getIntersectionName(HashMap<Coordinate, TreeSet<String>> coordinateToStreets,
            HashMap<String, HashMap<Coordinate, Integer>> intersectionNameToId,
            Coordinate coordinate) {

        TreeSet<String> streets = coordinateToStreets.get(coordinate);
        if (streets == null) {
            return "null";
        }

        //TODO: localize this and return localized string
        String intersection = streets.first() + " at " + streets.last();

        HashMap<Coordinate, Integer> possibleIntersections = intersectionNameToId.get(intersection);
        if (possibleIntersections == null) {
            possibleIntersections = new HashMap<Coordinate, Integer>();
            possibleIntersections.put(coordinate, 1);
            intersectionNameToId.put(intersection, possibleIntersections);
            return intersection;
        }
        Integer index = possibleIntersections.get(coordinate);
        if (index == null) {
            int max = 0;
            for (Integer value : possibleIntersections.values()) {
                if (value > max)
                    max = value;
            }
            possibleIntersections.put(coordinate, max + 1);
            index = max + 1;
        }
        if (index > 1) {
            intersection += " #" + possibleIntersections.get(coordinate);
        }
        return intersection;
    }

    private LineString toLineString(Geometry g) {
        if (g instanceof LineString) {
            return (LineString) g;
        } else if (g instanceof MultiLineString) {
            MultiLineString ml = (MultiLineString) g;

            Coordinate[] coords = ml.getCoordinates();
            return GeometryUtils.getGeometryFactory().createLineString(coords);
        } else {
            throw new RuntimeException("found a geometry feature that's not a linestring: " + g);
        }
    }

    @Override
    public void checkInputs() {
        _featureSourceFactory.checkInputs();
    }
}
