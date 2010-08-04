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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
import org.opentripplanner.graph_builder.services.StreetUtils;
import org.opentripplanner.graph_builder.services.shapefile.FeatureSourceFactory;
import org.opentripplanner.graph_builder.services.shapefile.SimpleFeatureConverter;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.EndpointVertex;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.OutEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.edgetype.StreetVertex;
import org.opentripplanner.routing.edgetype.TurnEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.PrecisionModel;

/**
 * Loads a shapefile into an edge-based graph.
 *
 */
public class ShapefileStreetGraphBuilderImpl implements GraphBuilder {
    private static Logger log = LoggerFactory.getLogger(ShapefileStreetGraphBuilderImpl.class);

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

            HashMap<Coordinate, P2<EndpointVertex>> intersectionsByLocation = new HashMap<Coordinate, P2<EndpointVertex>>();

            SimpleFeatureConverter<P2<Double>> safetyConverter = _schema.getBicycleSafetyConverter();

            SimpleFeatureConverter<Boolean> slopeOverrideCoverter = _schema.getSlopeOverrideConverter();

            SimpleFeatureConverter<Boolean> featureSelector = _schema.getFeatureSelector();
            
            //keep track of features that are duplicated so we don't have duplicate streets
            HashSet<Object> seen = new HashSet<Object>();

            List<SimpleFeature> featureList = new ArrayList<SimpleFeature>();
            Iterator<SimpleFeature> it2 = features.iterator();
            while (it2.hasNext()) {
                SimpleFeature feature = it2.next();
                if (featureSelector != null && ! featureSelector.convert(feature)) {
                    continue;
                }
                featureList.add(feature);
            }
            features.close(it2);

            for (SimpleFeature feature : featureList) {
                LineString geom = toLineString((Geometry) feature.getDefaultGeometry());

                Object o = streetIdConverter.convert(feature);
                String id = "" + o;
                if (o != null && seen.contains(id)) {
                    continue;
                }
                seen.add(id);
                String name = streetNameConverter.convert(feature);
                Coordinate[] coordinates = geom.getCoordinates();

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

                P2<EndpointVertex> startIntersection = intersectionsByLocation.get(startCoordinate);
                if (startIntersection == null) {
                    EndpointVertex in = new EndpointVertex(startIntersectionName + " in", startCoordinate.x,
                            startCoordinate.y, startIntersectionName);
                    EndpointVertex out = new EndpointVertex(startIntersectionName + " out", startCoordinate.x,
                            startCoordinate.y, startIntersectionName);
                    
                    in = (EndpointVertex) graph.addVertex(in);
                    out = (EndpointVertex) graph.addVertex(out);
                    
                    startIntersection = new P2<EndpointVertex>(in, out);
                    intersectionsByLocation.put(startCoordinate, startIntersection);
                }

                P2<EndpointVertex> endIntersection = intersectionsByLocation.get(endCoordinate);
                if (endIntersection == null) {
                    EndpointVertex in = new EndpointVertex(endIntersectionName + " in", endCoordinate.x,
                            endCoordinate.y, endIntersectionName);
                    EndpointVertex out = new EndpointVertex(endIntersectionName + " out", endCoordinate.x,
                                    endCoordinate.y, endIntersectionName);
                    in = (EndpointVertex) graph.addVertex(in);
                    out = (EndpointVertex) graph.addVertex(out);
                    endIntersection = new P2<EndpointVertex>(in, out);
                    intersectionsByLocation.put(endCoordinate, endIntersection);
                }

                double length = 0;
                for (int i = 0; i < coordinates.length - 1; ++i) {
                    length += JTS.orthodromicDistance(coordinates[i],
                            coordinates[i + 1], worldCRS);
                }
                
                StreetVertex street = new StreetVertex(id, geom, name, length, false);
                graph.addVertex(street);
                //StreetVertexImpl backStreet = new StreetVertexImpl(id, (LineString) geom.reverse(), name, length, false);
                /* reverse is sometimes missing(?!) */
                coordinates = geom.getCoordinates();
                
                Coordinate[] coordinatesCopy = Arrays.asList(coordinates).toArray(new Coordinate[0]);
                Collections.reverse(Arrays.asList(coordinatesCopy));
                LineString reversed = new GeometryFactory().createLineString(coordinatesCopy);
                StreetVertex backStreet = new StreetVertex(id, reversed, name, length, true);
                graph.addVertex(backStreet);
                
                graph.addEdge(new OutEdge(street, endIntersection.getFirst()));
                graph.addEdge(new FreeEdge(startIntersection.getSecond(), street));
                
                graph.addEdge(new FreeEdge(endIntersection.getSecond(), backStreet));
                graph.addEdge(new OutEdge(backStreet, startIntersection.getFirst()));

                boolean slopeOverride = slopeOverrideCoverter.convert(feature);
                street.setSlopeOverride(slopeOverride);
                backStreet.setSlopeOverride(slopeOverride);

                P2<StreetTraversalPermission> permissions = permissionConverter.convert(feature);

                street.setTraversalPermission(permissions.getFirst());
                backStreet.setTraversalPermission(permissions.getSecond());

                P2<Double> effectiveLength;
                if (safetyConverter != null) {
                    effectiveLength = safetyConverter.convert(feature);
                    if (effectiveLength != null) {
                        street.setBicycleSafetyEffectiveLength(effectiveLength.getFirst() * length);
                        backStreet.setBicycleSafetyEffectiveLength(effectiveLength.getSecond() * length);
                    }
                }
            }
            /* generate turns */
            
            for (P2<EndpointVertex> vertices: intersectionsByLocation.values()) {
                Vertex in = vertices.getFirst();
                Vertex out = vertices.getSecond();
                for (Edge e : graph.getIncoming(in)) {
                    StreetVertex v1 = (StreetVertex) e.getFromVertex();
                    for (Edge e2 : graph.getOutgoing(out)) {
                        StreetVertex v2 = (StreetVertex) e2.getToVertex();
                        if (v1 != v2 && v1.getEdgeId() != v2.getEdgeId()) { 
                            graph.addEdge(new TurnEdge(v1, v2));                            
                        }
                    }
                }
            }
            features.close(it2);

            StreetUtils.unify(graph, intersectionsByLocation.values());
        } catch (Exception ex) {
            throw new IllegalStateException("error loading shapefile street data", ex);
        }       
    }

    private HashMap<Coordinate, TreeSet<String>> getCoordinatesToStreetNames(
            FeatureCollection<SimpleFeatureType, SimpleFeature> features) {
        HashMap<Coordinate, TreeSet<String>> coordinateToStreets = new HashMap<Coordinate, TreeSet<String>>();
        SimpleFeatureConverter<String> streetNameConverter = _schema.getNameConverter();

        SimpleFeatureConverter<Boolean> featureSelector = _schema.getFeatureSelector();
        Iterator<SimpleFeature> it = features.iterator();
        while (it.hasNext()) {

            SimpleFeature feature = it.next();
            if (featureSelector != null && !featureSelector.convert(feature)) {
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
