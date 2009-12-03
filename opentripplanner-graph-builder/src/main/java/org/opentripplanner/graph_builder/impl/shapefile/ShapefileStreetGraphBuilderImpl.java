package org.opentripplanner.graph_builder.impl.shapefile;

import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import org.geotools.data.DefaultQuery;
import org.geotools.data.FeatureSource;
import org.geotools.factory.Hints;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.graph_builder.services.shapefile.FeatureSourceFactory;
import org.opentripplanner.graph_builder.services.shapefile.SimpleFeatureConverter;
import org.opentripplanner.routing.core.GenericVertex;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.Street;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.vertextypes.Intersection;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.PrecisionModel;

public class ShapefileStreetGraphBuilderImpl implements GraphBuilder {

    private FeatureSourceFactory _featureSourceFactory;

    private ShapefileStreetSchema _schema;

    public void setFeatureSourceFactory(FeatureSourceFactory factory) {
        _featureSourceFactory = factory;
    }

    public void setSchema(ShapefileStreetSchema schema) {
        _schema = schema;
    }

    @Override
    public void buildGraph(Graph graph) {

        try {

            FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = _featureSourceFactory
                    .getFeatureSource();
            CoordinateReferenceSystem sourceCRS = featureSource.getInfo().getCRS();

            Hints hints = new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);
            CRSAuthorityFactory factory = ReferencingFactoryFinder.getCRSAuthorityFactory("EPSG",
                    hints);
            CoordinateReferenceSystem worldCRS = factory
                    .createCoordinateReferenceSystem("EPSG:4326");

            DefaultQuery query = new DefaultQuery();
            query.setCoordinateSystem(sourceCRS);
            query.setCoordinateSystemReproject(worldCRS);

            FeatureCollection<SimpleFeatureType, SimpleFeature> features = featureSource
                    .getFeatures(query);

            HashMap<Coordinate, TreeSet<String>> coordinateToStreetNames = getCoordinatesToStreetNames(features);

            features = featureSource.getFeatures(query);

            HashMap<String, HashMap<Coordinate, Integer>> intersectionNameToId = new HashMap<String, HashMap<Coordinate, Integer>>();

            SimpleFeatureConverter<String> streetIdConverter = _schema.getIdConverter();
            SimpleFeatureConverter<String> streetNameConverter = _schema.getNameConverter();
            SimpleFeatureConverter<P2<StreetTraversalPermission>> permissionConverter = _schema
                    .getPermissionConverter();

            Iterator<SimpleFeature> it2 = features.iterator();
            while (it2.hasNext()) {

                SimpleFeature feature = it2.next();
                LineString geom = toLineString((Geometry) feature.getDefaultGeometry());

                String id = streetIdConverter.convert(feature);
                String name = streetNameConverter.convert(feature);
                Coordinate[] coordinates = geom.getCoordinates();

                // FIXME: this rounding is a total hack, to work around
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
                    System.out.println(name);
                }

                String endIntersectionName = getIntersectionName(coordinateToStreetNames,
                        intersectionNameToId, endCoordinate);
                Vertex startCorner = new GenericVertex(startIntersectionName, startCoordinate.x,
                        startCoordinate.y, startIntersectionName);
                startCorner = graph.addVertex(startCorner);
                startCorner.setType(Intersection.class);
                Vertex endCorner = new GenericVertex(endIntersectionName, endCoordinate.x,
                        endCoordinate.y, endIntersectionName);
                endCorner = graph.addVertex(endCorner);
                endCorner.setType(Intersection.class);

                double length = JTS.orthodromicDistance(coordinates[0],
                        coordinates[coordinates.length - 1], worldCRS);

                Street street = new Street(startCorner, endCorner, id, name, length);
                street.setGeometry(geom);
                graph.addEdge(street);
                Street backStreet = new Street(endCorner, startCorner, id, name, length);
                backStreet.setGeometry(geom.reverse());
                graph.addEdge(backStreet);

                P2<StreetTraversalPermission> pair = permissionConverter
                        .convert(feature);

                street.setTraversalPermission(pair.getFirst());
                backStreet.setTraversalPermission(pair.getSecond());
            }

            features.close(it2);
        } catch (Exception ex) {
            throw new IllegalStateException("error loading shapefile street data", ex);
        }
    }

    private HashMap<Coordinate, TreeSet<String>> getCoordinatesToStreetNames(
            FeatureCollection<SimpleFeatureType, SimpleFeature> features) {
        HashMap<Coordinate, TreeSet<String>> coordinateToStreets = new HashMap<Coordinate, TreeSet<String>>();
        SimpleFeatureConverter<String> streetNameConverter = _schema.getNameConverter();

        Iterator<SimpleFeature> it = features.iterator();
        while (it.hasNext()) {

            SimpleFeature feature = it.next();
            LineString geom = toLineString((Geometry) feature.getDefaultGeometry());

            for (Coordinate coord : geom.getCoordinates()) {
                // FIXME: this rounding is a total hack, to work around
                // http://jira.codehaus.org/browse/GEOT-2811
                Coordinate rounded = new Coordinate(Math.round(coord.x * 1048576) / 1048576.0, Math
                        .round(coord.y * 1048576) / 1048576.0);

                TreeSet<String> streets = coordinateToStreets.get(rounded);
                if (streets == null) {
                    streets = new TreeSet<String>();
                    coordinateToStreets.put(rounded, streets);
                }
                String streetName = streetNameConverter.convert(feature);
                streets.add(streetName);
            }
        }

        features.close(it);
        return coordinateToStreets;
    }

    private String getIntersectionName(HashMap<Coordinate, TreeSet<String>> coordinateToStreets,
            HashMap<String, HashMap<Coordinate, Integer>> intersectionNameToId,
            Coordinate coordinate) {
        
        TreeSet<String> streets = coordinateToStreets.get(coordinate);
        if (streets == null) {
            return "null";
        }
        
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
            GeometryFactory factory = new GeometryFactory(new PrecisionModel(
                    PrecisionModel.FLOATING), 4326);
            return factory.createLineString(coords);
        } else {
            throw new RuntimeException("found a geometry feature that's not a linestring: " + g);
        }
    }

}
