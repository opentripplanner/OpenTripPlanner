package org.opentripplanner.index;

import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import graphql.Scalars;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLTypeReference;
import org.joda.time.LocalDate;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.index.model.TripTimeShort;
import org.opentripplanner.profile.StopCluster;
import org.opentripplanner.routing.edgetype.SimpleTransfer;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.vertextype.TransitVertex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

public class IndexGraphQLSchema {

    public GraphQLOutputType agencyType = new GraphQLTypeReference("Agency");
    public GraphQLOutputType clusterType = new GraphQLTypeReference("Cluster");
    public GraphQLOutputType patternType = new GraphQLTypeReference("Pattern");
    public GraphQLOutputType routeType = new GraphQLTypeReference("Route");
    public GraphQLOutputType stoptimeType = new GraphQLTypeReference("Stoptime");
    public GraphQLOutputType stopType = new GraphQLTypeReference("Stop");
    public GraphQLOutputType tripType = new GraphQLTypeReference("Trip");
    public GraphQLOutputType stopAtDistanceType = new GraphQLTypeReference("StopAtDistance");
    public GraphQLObjectType queryType;
    public GraphQLSchema indexSchema;

    public IndexGraphQLSchema(GraphIndex index) {

        stopAtDistanceType = GraphQLObjectType.newObject()
            .name("stopAtDistance")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stop")
                .type(stopType)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("distance")
                .type(Scalars.GraphQLFloat)
                .build())
            .build();

        clusterType = GraphQLObjectType.newObject()
            .name("Cluster")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("id")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .dataFetcher(environment ->
                    ((StopCluster) environment.getSource()).id)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("name")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .dataFetcher(environment ->
                    ((StopCluster) environment.getSource()).name)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("lat")
                .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                .dataFetcher(environment ->
                    (float) (((StopCluster) environment.getSource()).lat))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("lon")
                .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                .dataFetcher(environment ->
                    (float) (((StopCluster) environment.getSource()).lon))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stops")
                .type(new GraphQLList(new GraphQLNonNull(stopType)))
                .dataFetcher(environment -> ((StopCluster) environment.getSource()).children)
                .build())
            .build();

        stopType = GraphQLObjectType.newObject()
            .name("Stop")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("id")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .dataFetcher(environment ->
                    GtfsLibrary.convertIdToString(((Stop) environment.getSource()).getId()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("name")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("lat")
                .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                .dataFetcher(environment ->
                    (float) (((Stop) environment.getSource()).getLat()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("lon")
                .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                .dataFetcher(environment ->
                    (float) (((Stop) environment.getSource()).getLon()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("code")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("desc")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("zoneId")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("url")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("locationType")
                .type(Scalars.GraphQLInt) //TODO:enum
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("parentStation")
                .type(Scalars.GraphQLString) //TODO: get actual station type
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("wheelchairBoarding") //TODO:enum
                .type(Scalars.GraphQLInt)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("direction")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("timezone")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("vehicleType")
                .type(Scalars.GraphQLInt) //TODO:enum
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("platformCode")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("cluster")
                .type(clusterType)
                .dataFetcher(environment ->
                    index.stopClusterForStop.get((Stop) environment.getSource()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("routes")
                .type(new GraphQLList(new GraphQLNonNull(routeType)))
                .dataFetcher(environment ->
                    index.patternsForStop.get((Stop) environment.getSource())
                        .stream()
                        .map(pattern -> pattern.route)
                        .distinct()
                        .collect(Collectors.toList()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("patterns")
                .type(new GraphQLList(patternType))
                .dataFetcher(environment ->
                    index.patternsForStop.get((Stop) environment.getSource()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("transfers")               //TODO: add max distance as parameter?
                .type(new GraphQLList(stopAtDistanceType))
                .dataFetcher(environment ->
                    index.stopVertexForStop.get((Stop) environment.getSource()).getOutgoing()
                        .stream()
                        .filter(edge -> edge instanceof SimpleTransfer)
                        .map(edge -> new ImmutableMap.Builder<String, Object>()
                            .put("stop", ((TransitVertex) edge.getToVertex()).getStop())
                            .put("distance", edge.getDistance())
                            .build())
                        .collect(Collectors.toList())
                )
                .build())
            .build(); //TODO: add stoptimes

        stoptimeType = GraphQLObjectType.newObject()
            .name("Stoptime")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stop")
                .type(stopType)
                .dataFetcher(environment ->
                    index.stopForId.get(((TripTimeShort) environment.getSource()).stopId))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("scheduledArrival")
                .type(Scalars.GraphQLInt)
                .dataFetcher(environment ->
                    ((TripTimeShort) environment.getSource()).scheduledArrival)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("realtimeArrival")
                .type(Scalars.GraphQLInt)
                .dataFetcher(environment ->
                    ((TripTimeShort) environment.getSource()).realtimeArrival)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("arrivalDelay")
                .type(Scalars.GraphQLInt)
                .dataFetcher(environment ->
                    ((TripTimeShort) environment.getSource()).arrivalDelay)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("scheduledDeparture")
                .type(Scalars.GraphQLInt)
                .dataFetcher(environment ->
                    ((TripTimeShort) environment.getSource()).scheduledDeparture)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("realtimeDeparture")
                .type(Scalars.GraphQLInt)
                .dataFetcher(environment ->
                    ((TripTimeShort) environment.getSource()).realtimeArrival)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("departureDelay")
                .type(Scalars.GraphQLInt)
                .dataFetcher(environment ->
                    ((TripTimeShort) environment.getSource()).departureDelay)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("timepoint")
                .type(Scalars.GraphQLBoolean)
                .dataFetcher(environment ->
                    ((TripTimeShort) environment.getSource()).timepoint)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("realtime")
                .type(Scalars.GraphQLBoolean)
                .dataFetcher(environment ->
                    ((TripTimeShort) environment.getSource()).realtime)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("serviceDay")
                .type(Scalars.GraphQLString)
                .dataFetcher(environment ->
                    new LocalDate((((TripTimeShort) environment.getSource()).serviceDay)).toString())
                .build())
            .build();

        tripType = GraphQLObjectType.newObject()
            .name("Trip")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("id")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .dataFetcher(environment ->
                    GtfsLibrary.convertIdToString(((Trip) environment.getSource()).getId()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("route")
                .type(new GraphQLNonNull(routeType))
                .dataFetcher(environment ->
                    ((Trip) environment.getSource()).getRoute())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("serviceId")
                .type(Scalars.GraphQLString) //TODO:Should be serviceType
                .dataFetcher(environment ->
                    GtfsLibrary.convertIdToString(((Trip) environment.getSource()).getServiceId()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("tripShortName")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("tripHeadsign")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("routeShortName")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("directionId")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("blockId")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("shapeId")
                .type(Scalars.GraphQLString) //TODO: should be shapeType or geometryType
                .dataFetcher(environment ->
                    GtfsLibrary.convertIdToString(((Trip) environment.getSource()).getShapeId()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("wheelchairAccessible")
                .type(Scalars.GraphQLInt) //TODO: enum
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("bikesAllowed")
                .type(Scalars.GraphQLString) //TODO:enum
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("pattern")
                .type(patternType)
                .dataFetcher(environment ->
                    index.patternForTrip.get((Trip) environment.getSource()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stops")
                .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(stopType))))
                .dataFetcher(environment ->
                    index.patternForTrip.get((Trip) environment.getSource()).getStops())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("semanticHash")
                .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(stopType))))
                .dataFetcher(environment ->
                    index.patternForTrip.get((Trip) environment.getSource())
                        .semanticHashString((Trip) environment.getSource()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stoptimes")
                .type(new GraphQLList(stoptimeType))
                .dataFetcher(environment ->
                    TripTimeShort.fromTripTimes(
                        index.patternForTrip.get((Trip) environment.getSource()).scheduledTimetable,
                        (Trip) environment.getSource()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("geometry")
                .type(Scalars.GraphQLString) //TODO: Should be geometry
                .dataFetcher(environment ->
                    index.patternForTrip.get((Trip) environment.getSource())
                        .geometry.getCoordinateSequence())
                .build())
            .build();


        patternType = GraphQLObjectType.newObject()
            .name("Pattern")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("route")
                .type(new GraphQLNonNull(routeType))
                .dataFetcher(environment ->
                    ((TripPattern) environment.getSource()).route)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("directionId")
                .type(Scalars.GraphQLInt)
                .dataFetcher(environment ->
                    ((TripPattern) environment.getSource()).directionId)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("name")
                .type(Scalars.GraphQLString)
                .dataFetcher(environment ->
                    ((TripPattern) environment.getSource()).name)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("id")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .dataFetcher(environment ->
                    ((TripPattern) environment.getSource()).code)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("headsign")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .dataFetcher(environment ->
                    ((TripPattern) environment.getSource()).getDirection())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("trips")
                .type(new GraphQLList(new GraphQLNonNull(tripType)))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stops")
                .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(stopType))))
                .build())
                // TODO: add stoptimes
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("semanticHash")
                .type(Scalars.GraphQLString)
                .dataFetcher(environment ->
                    ((TripPattern) environment.getSource()).semanticHashString(null))
                .build())
            .build();


        routeType = GraphQLObjectType.newObject()
            .name("Route")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("id")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .dataFetcher(environment ->
                    GtfsLibrary.convertIdToString(((Route) environment.getSource()).getId()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("agency")
                .type(agencyType)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("shortName")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("longName")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("type")
                .type(Scalars.GraphQLInt) // TODO:Replace with enum
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("desc")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("url")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("color")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("textColor")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("bikesAllowed")
                .type(Scalars.GraphQLInt) // TODO:Replace with enum
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("patterns")
                .type(new GraphQLList(patternType))
                .dataFetcher(environment ->
                    index.patternsForRoute.get((Route) environment.getSource()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stops")
                .type(new GraphQLList(stopType))
                .dataFetcher(environment ->
                    index.patternsForRoute.get((Route) environment.getSource())
                        .stream()
                        .map(TripPattern::getStops)
                        .flatMap(Collection::stream)
                        .distinct()
                        .collect(Collectors.toList())
                )
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("trips")
                .type(new GraphQLList(tripType))
                .dataFetcher(environment ->
                    index.patternsForRoute.get((Route) environment.getSource())
                        .stream()
                        .map(TripPattern::getTrips)
                        .flatMap(Collection::stream)
                        .distinct()
                        .collect(Collectors.toList())
                )
                .build())
            .build();

        agencyType = GraphQLObjectType.newObject()
            .name("Agency")
            .description("Agency in the graph")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("id")
                .description("Agency id")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("name")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("url")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("timezone")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("lang")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("phone")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("fareUrl")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("routes")
                .type(new GraphQLList(routeType))
                .dataFetcher(environment -> index.routeForId.values().stream()
                    .filter(route -> route.getAgency() == environment.getSource())
                    .collect(Collectors.toList()))
                .build())
            .build();

        queryType = GraphQLObjectType.newObject()
            .name("QueryType")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("agencies")
                .type(new GraphQLList(agencyType))
                .dataFetcher(environment ->
                    new ArrayList<>(index.agencyForId.values()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("agency")
                .type(agencyType)
                .argument(GraphQLArgument.newArgument()
                    .name("id")
                    .type(new GraphQLNonNull(Scalars.GraphQLString))
                    .build())
                .dataFetcher(environment ->
                    index.agencyForId.get(environment.getArgument("id")))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stops")
                .type(new GraphQLList(stopType))
                .dataFetcher(environment ->
                    new ArrayList<>(index.stopForId.values()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stopsByBbox")
                .type(new GraphQLList(stopType))
                .argument(GraphQLArgument.newArgument()
                    .name("minLat")
                    .type(Scalars.GraphQLFloat)
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("minLon")
                    .type(Scalars.GraphQLFloat)
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("maxLat")
                    .type(Scalars.GraphQLFloat)
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("maxLon")
                    .type(Scalars.GraphQLFloat)
                    .build())
                .dataFetcher(environment ->
                    index.graph.streetIndex.getTransitStopForEnvelope(
                        new Envelope(
                            new Coordinate((double) (float) environment.getArgument("minLon"),
                                (double) (float) environment.getArgument("minLat")),
                            new Coordinate((double) (float) environment.getArgument("maxLon"),
                                (double) (float) environment.getArgument("maxLat"))
                        ))
                        .stream()
                        .map(TransitVertex::getStop)
                        .collect(Collectors.toList())
                )
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stopsByRadius")
                .type(new GraphQLList(stopAtDistanceType)) // Expects a TransitVertex object
                .argument(GraphQLArgument.newArgument()
                    .name("lat")
                    .type(Scalars.GraphQLFloat)
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("lon")
                    .type(Scalars.GraphQLFloat)
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("radius")
                    .type(Scalars.GraphQLFloat)
                    .build())
                .dataFetcher(environment -> {
                    Coordinate coordinate = new Coordinate(
                        (double) (float) environment.getArgument("lon"),
                        (double) (float) environment.getArgument("lat")
                    );
                    return index.graph.streetIndex.getNearbyTransitStops(coordinate, (double) (float) environment.getArgument("radius"))
                        .stream()
                        .map(vertex -> new ImmutableMap.Builder<String, Object>()
                            .put("stop", vertex.getStop())
                            .put("distance", (float) SphericalDistanceLibrary.fastDistance(vertex.getCoordinate(), coordinate))
                            .build())
                        .collect(Collectors.toList());
                })
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stop")
                .type(stopType)
                .argument(GraphQLArgument.newArgument()
                    .name("id")
                    .type(new GraphQLNonNull(Scalars.GraphQLString))
                    .build())
                .dataFetcher(environment ->
                    index.stopForId.get(
                        GtfsLibrary.convertIdFromString(environment.getArgument("id"))))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("routes")
                .type(new GraphQLList(routeType))
                .dataFetcher(environment ->
                    new ArrayList<>(index.routeForId.values()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("route")
                .type(routeType)
                .argument(GraphQLArgument.newArgument()
                    .name("id")
                    .type(new GraphQLNonNull(Scalars.GraphQLString))
                    .build())
                .dataFetcher(environment ->
                    index.routeForId.get(
                        GtfsLibrary.convertIdFromString(environment.getArgument("id"))))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("trips")
                .type(new GraphQLList(tripType))
                .dataFetcher(environment ->
                    new ArrayList<>(index.tripForId.values()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("trip")
                .type(tripType)
                .argument(GraphQLArgument.newArgument()
                    .name("id")
                    .type(new GraphQLNonNull(Scalars.GraphQLString))
                    .build())
                .dataFetcher(environment ->
                    index.tripForId.get(
                        GtfsLibrary.convertIdFromString(environment.getArgument("id"))))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("patterns")
                .type(new GraphQLList(patternType))
                .dataFetcher(environment ->
                    new ArrayList<>(index.patternForId.values()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("pattern")
                .type(patternType)
                .argument(GraphQLArgument.newArgument()
                    .name("id")
                    .type(new GraphQLNonNull(Scalars.GraphQLString))
                    .build())
                .dataFetcher(environment ->
                    index.patternForId.get(
                        environment.getArgument("id")))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("clusters")
                .type(new GraphQLList(clusterType))
                .dataFetcher(environment ->
                    new ArrayList<>(index.stopClusterForId.values()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("cluster")
                .type(clusterType)
                .argument(GraphQLArgument.newArgument()
                    .name("id")
                    .type(new GraphQLNonNull(Scalars.GraphQLString))
                    .build())
                .dataFetcher(environment ->
                    index.stopClusterForId.get(
                        environment.getArgument("id")))
                .build())
            .build();

        indexSchema = GraphQLSchema.newSchema()
            .query(queryType)
            .build();
    }
}
