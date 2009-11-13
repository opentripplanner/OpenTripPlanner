package org.opentripplanner.routing.edgetype.loader;

import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import org.geotools.data.DefaultQuery;
import org.geotools.data.FeatureSource;
import org.geotools.factory.Hints;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
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

public class ShapefileStreetLoader {
    /**
     * Load a shape file of streets, possibly reprojecting it. This is intended to work with NYC's
     * LION data.
     */
    Graph graph;

    FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;

    CoordinateReferenceSystem sourceCRS = null;

    static {
        System.setProperty("org.geotools.referencing.forceXY", "true");
    }

    /**
     * 
     * @param graph
     *            A Graph to load streets into
     * @param featureSource
     *            A source for streets Attempts to get the CRS from the data.
     */
    public ShapefileStreetLoader(Graph graph,
            FeatureSource<SimpleFeatureType, SimpleFeature> featureSource) {

        this.graph = graph;
        this.featureSource = featureSource;
        sourceCRS = featureSource.getInfo().getCRS();
    }

    public ShapefileStreetLoader(Graph graph,
            FeatureSource<SimpleFeatureType, SimpleFeature> featureSource, int sourceSRID) {
        this.graph = graph;
        this.featureSource = featureSource;

        try {
            sourceCRS = CRS.decode("EPSG:" + sourceSRID);
        } catch (NoSuchAuthorityCodeException e) {
            throw new RuntimeException(e);
        } catch (FactoryException e) {
            throw new RuntimeException(e);
        }
    }

    public static LineString toLineString(Geometry g) {
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

    public void load() throws Exception {
        // fixme: what logger? Logger.log("loading shapes from " + shapefile);

        Hints hints = new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);
        CRSAuthorityFactory factory = ReferencingFactoryFinder
                .getCRSAuthorityFactory("EPSG", hints);
        CoordinateReferenceSystem worldCRS = factory.createCoordinateReferenceSystem("EPSG:4326");

        DefaultQuery query = new DefaultQuery();
        query.setCoordinateSystem(sourceCRS);
        query.setCoordinateSystemReproject(worldCRS);

        FeatureCollection<SimpleFeatureType, SimpleFeature> features = featureSource
                .getFeatures(query);
        Iterator<SimpleFeature> i = features.iterator();

        try {

            HashMap<Coordinate, TreeSet<String>> coordinateToStreets = new HashMap<Coordinate, TreeSet<String>>();

            while (i.hasNext()) {

                SimpleFeature feature = i.next();
                LineString geom = toLineString((Geometry) feature.getDefaultGeometry());

                for (Coordinate coord : geom.getCoordinates()) {
                    // FIXME: this rounding is a total hack, to work around
                    // http://jira.codehaus.org/browse/GEOT-2811
                    Coordinate rounded = new Coordinate(Math.round(coord.x * 1048576) / 1048576.0,
                            Math.round(coord.y * 1048576) / 1048576.0);

                    TreeSet<String> streets = coordinateToStreets.get(rounded);
                    if (streets == null) {
                        streets = new TreeSet<String>();
                        coordinateToStreets.put(rounded, streets);
                    }
                    streets.add((String) feature.getAttribute("Street"));
                }
            }

            features.close(i);
            features = featureSource.getFeatures(query);

            HashMap<String, HashMap<Coordinate, Integer>> intersectionNameToId = new HashMap<String, HashMap<Coordinate, Integer>>();

            i = features.iterator();
            while (i.hasNext()) {

                SimpleFeature feature = i.next();
                LineString geom = toLineString((Geometry) feature.getDefaultGeometry());

                String id = "" + feature.getAttribute("StreetCode");
                String name = "" + feature.getAttribute("Street");
                Coordinate[] coordinates = geom.getCoordinates();
                String trafDir = (String) feature.getAttribute("TrafDir");

                // FIXME: this rounding is a total hack, to work around
                // http://jira.codehaus.org/browse/GEOT-2811
                Coordinate startCoordinate = new Coordinate(
                        Math.round(coordinates[0].x * 1048576) / 1048576.0, Math
                                .round(coordinates[0].y * 1048576) / 1048576.0);
                Coordinate endCoordinate = new Coordinate(Math
                        .round(coordinates[coordinates.length - 1].x * 1048576) / 1048576.0, Math
                        .round(coordinates[coordinates.length - 1].y * 1048576) / 1048576.0);

                String startIntersectionName = getIntersectionName(coordinateToStreets,
                        intersectionNameToId, startCoordinate);

                if (startIntersectionName == "null") {
                    System.out.println(name);
                }
                String endIntersectionName = getIntersectionName(coordinateToStreets,
                        intersectionNameToId, endCoordinate);
                Vertex startCorner = new GenericVertex(startIntersectionName, startCoordinate.x,
                        startCoordinate.y);
                startCorner = graph.addVertex(startCorner);
                startCorner.setType(Intersection.class);
                Vertex endCorner = new GenericVertex(endIntersectionName, endCoordinate.x,
                        endCoordinate.y);
                endCorner = graph.addVertex(endCorner);
                endCorner.setType(Intersection.class);

                double length = JTS.orthodromicDistance(coordinates[0],
                        coordinates[coordinates.length - 1], worldCRS);

                // TODO: The following assumes the street direction convention
                // used in NYC
                // This code should either be moved or generalized (if
                // possible).

                Street street = new Street(startCorner, endCorner, id, name, length);
                street.setGeometry(geom);
                graph.addEdge(street);
                Street backStreet = new Street(endCorner, startCorner, id, name, length);
                backStreet.setGeometry(geom.reverse());
                graph.addEdge(backStreet);
                
                if (trafDir.equals("W")) {
                    // traffic flows With geometry
                    street.setTraversalPermission(StreetTraversalPermission.ALL);
                    backStreet.setTraversalPermission(StreetTraversalPermission.PEDESTRIAN_ONLY);
                } else if (trafDir.equals("A")) {
                    backStreet.setTraversalPermission(StreetTraversalPermission.ALL);
                    street.setTraversalPermission(StreetTraversalPermission.PEDESTRIAN_ONLY);
                } else if (trafDir.equals("T")) {
                    backStreet.setTraversalPermission(StreetTraversalPermission.ALL);
                    street.setTraversalPermission(StreetTraversalPermission.ALL);
                } else {
                    // no cars allowed
                    backStreet.setTraversalPermission(StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE_ONLY);
                    street.setTraversalPermission(StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE_ONLY);
                }

            }
        } finally {
            features.close(i);
        }

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
}
