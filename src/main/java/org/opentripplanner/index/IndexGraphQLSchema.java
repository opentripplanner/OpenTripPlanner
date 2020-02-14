package org.opentripplanner.index;

import com.google.common.collect.ImmutableMap;
import graphql.Scalars;
import graphql.relay.Relay;
import graphql.relay.SimpleListConnection;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLTypeReference;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.index.model.StopTimesInPattern;
import org.opentripplanner.index.model.TripTimeShort;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Notice;
import org.opentripplanner.model.Operator;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopTimeKey;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.StopFinder;
import org.opentripplanner.routing.edgetype.SimpleTransfer;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.trippattern.RealTimeState;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.updater.GtfsRealtimeFuzzyTripMatcher;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class IndexGraphQLSchema {

    public static GraphQLEnumType locationTypeEnum = GraphQLEnumType.newEnum()
        .name("LocationType")
        .description("Identifies whether this stop represents a stop or station.")
        .value("STOP", 0, "A location where passengers board or disembark from a transit vehicle.")
        .value("STATION", 1, "A physical structure or area that contains one or more stop.")
        .value("ENTRANCE", 2)
        .build();

    public static GraphQLEnumType wheelchairBoardingEnum = GraphQLEnumType.newEnum()
        .name("WheelchairBoarding")
        .value("NO_INFORMATION", 0, "There is no accessibility information for the stop.")
        .value("POSSIBLE", 1, "At least some vehicles at this stop can be boarded by a rider in a wheelchair.")
        .value("NOT_POSSIBLE", 2, "Wheelchair boarding is not possible at this stop.")
        .build();

    public static GraphQLEnumType bikesAllowedEnum = GraphQLEnumType.newEnum()
        .name("BikesAllowed")
        .value("NO_INFORMATION", 0, "There is no bike information for the trip.")
        .value("ALLOWED", 1, "The vehicle being used on this particular trip can accommodate at least one bicycle.")
        .value("NOT_ALLOWED", 2, "No bicycles are allowed on this trip.")
        .build();

    public static GraphQLEnumType realtimeStateEnum = GraphQLEnumType.newEnum()
        .name("RealtimeState")
        .value("SCHEDULED", RealTimeState.SCHEDULED, "The trip information comes from the GTFS feed, i.e. no real-time update has been applied.")

        .value("UPDATED", RealTimeState.UPDATED, "The trip information has been updated, but the trip pattern stayed the same as the trip pattern of the scheduled trip.")

        .value("CANCELED", RealTimeState.CANCELED, "The trip has been canceled by a real-time update.")

        .value("ADDED", RealTimeState.ADDED, "The trip has been added using a real-time update, i.e. the trip was not present in the GTFS feed.")

        .value("MODIFIED", RealTimeState.MODIFIED, "The trip information has been updated and resulted in a different trip pattern compared to the trip pattern of the scheduled trip.")
        .build();

    private final GtfsRealtimeFuzzyTripMatcher fuzzyTripMatcher;

    public GraphQLOutputType agencyType = new GraphQLTypeReference("Agency");

    public GraphQLOutputType operatorType = new GraphQLTypeReference("Operator");

    public GraphQLOutputType coordinateType = new GraphQLTypeReference("Coordinates");

    public GraphQLOutputType clusterType = new GraphQLTypeReference("Cluster");

    public GraphQLOutputType noticeType = new GraphQLTypeReference("Notice");

    public GraphQLOutputType patternType = new GraphQLTypeReference("Pattern");

    public GraphQLOutputType routeType = new GraphQLTypeReference("Route");

    public GraphQLOutputType stoptimeType = new GraphQLTypeReference("Stoptime");

    public GraphQLOutputType stopType = new GraphQLTypeReference("Stop");

    public GraphQLOutputType tripType = new GraphQLTypeReference("Trip");

    public GraphQLOutputType stopAtDistanceType = new GraphQLTypeReference("StopAtDistance");

    public GraphQLOutputType stoptimesInPatternType = new GraphQLTypeReference("StoptimesInPattern");

    public GraphQLObjectType queryType;

    public GraphQLSchema indexSchema;

    private Relay relay = new Relay();

    private GraphQLInterfaceType nodeInterface = relay.nodeInterface(e ->  {
        Object o = e.getObject();
        if (o instanceof Stop){
            return (GraphQLObjectType) stopType;
        }
        if (o instanceof Trip){
            return (GraphQLObjectType) tripType;
        }
        if (o instanceof Route){
            return (GraphQLObjectType) routeType;
        }
        if (o instanceof TripPattern){
            return (GraphQLObjectType) patternType;
        }
        if (o instanceof Agency){
            return (GraphQLObjectType) agencyType;
        }
        if (o instanceof Operator) {
            return (GraphQLObjectType) operatorType;
        }
        if (o instanceof Notice) {
            return (GraphQLObjectType) noticeType;
        }
        return null;
    });

    public IndexGraphQLSchema(RoutingService routingService) {

        fuzzyTripMatcher = new GtfsRealtimeFuzzyTripMatcher(routingService);

        stopAtDistanceType = GraphQLObjectType.newObject()
            .name("stopAtDistance")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stop")
                .type(stopType)
                .dataFetcher(environment -> ((StopFinder.StopAndDistance) environment.getSource()).stop)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("distance")
                .type(Scalars.GraphQLInt)
                .dataFetcher(environment -> ((StopFinder.StopAndDistance) environment.getSource()).distance)
                .build())
            .build();

        stoptimesInPatternType = GraphQLObjectType.newObject()
            .name("StoptimesInPattern")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("pattern")
                .type(patternType)
                .dataFetcher(environment -> routingService.getTripPatternForId(
                    ((StopTimesInPattern) environment.getSource()).pattern.id)
                )
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stoptimes")
                .type(new GraphQLList(stoptimeType))
                .dataFetcher(environment -> ((StopTimesInPattern) environment.getSource()).times)
                .build())
            .build();

        stopType = GraphQLObjectType.newObject()
            .name("Stop")
            .withInterface(nodeInterface)
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("id")
                .type(new GraphQLNonNull(Scalars.GraphQLID))
                .dataFetcher(environment -> relay.toGlobalId(
                    stopType.getName(),
                    GtfsLibrary.convertIdToString(((Stop) environment.getSource()).getId())))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("gtfsId")
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
                .dataFetcher(environment -> (float) (((Stop) environment.getSource()).getLat()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("lon")
                .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                .dataFetcher(environment -> (float) (((Stop) environment.getSource()).getLon()))
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
                .type(locationTypeEnum)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("parentStation")
                .type(stopType)
                .dataFetcher(environment -> routingService.getStopForId().get(new FeedScopedId(
                    ((Stop) environment.getSource()).getId().getFeedId(),
                    ((Stop) environment.getSource()).getParentStation().getId().getId())))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("wheelchairBoarding")
                .type(wheelchairBoardingEnum)
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
                .type(Scalars.GraphQLInt)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("platformCode")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("routes")
                .type(new GraphQLList(new GraphQLNonNull(routeType)))
                .dataFetcher(environment -> routingService.getPatternsForStop()
                    .get((Stop) environment.getSource())
                    .stream()
                    .map(pattern -> pattern.route)
                    .distinct()
                    .collect(Collectors.toList()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("patterns")
                .type(new GraphQLList(patternType))
                .dataFetcher(environment -> routingService.getPatternsForStop()
                    .get((Stop) environment.getSource()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("transfers")               //TODO: add max distance as parameter?
                .type(new GraphQLList(stopAtDistanceType))
                .dataFetcher(environment -> routingService.getStopVertexForStop()
                    .get(environment.getSource())
                    .getOutgoing()
                    .stream()
                    .filter(edge -> edge instanceof SimpleTransfer)
                    .map(edge -> new ImmutableMap.Builder<String, Object>()
                        .put("stop", ((TransitStopVertex) edge.getToVertex()).getStop())
                        .put("distance", edge.getDistanceMeters())
                        .build())
                    .collect(Collectors.toList()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stoptimesForServiceDate")
                .type(new GraphQLList(stoptimesInPatternType))
                .argument(GraphQLArgument.newArgument()
                    .name("date")
                    .type(Scalars.GraphQLString)
                    .build())
                .dataFetcher(environment -> {
                    try {  // TODO: Add our own scalar types for at least serviceDate and FeedId
                        return routingService.getStopTimesForStop(
                            (Stop) environment.getSource(),
                            ServiceDate.parseString(environment.getArgument("date")),
                            environment.getArgument("omitNonPickups"));
                    } catch (ParseException e) {
                        return null;
                    }
                })
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stoptimesForPatterns")
                .type(new GraphQLList(stoptimesInPatternType))
                .argument(GraphQLArgument.newArgument()
                    .name("startTime")
                    .type(Scalars.GraphQLString) // No long exists in GraphQL
                    .defaultValue("0") // Default value is current time
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("timeRange")
                    .type(Scalars.GraphQLInt)
                    .defaultValue(24 * 60 * 60)
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("numberOfDepartures")
                    .type(Scalars.GraphQLInt)
                    .defaultValue(5)
                    .build())
                .argument(GraphQLArgument.newArgument()
            		.name("omitNonPickups")
            		.type(Scalars.GraphQLBoolean)
            		.defaultValue(false)
            		.build())
                .dataFetcher(environment ->
                    routingService.stopTimesForStop((Stop) environment.getSource(),
                        Long.parseLong(environment.getArgument("startTime")),
                        (int) environment.getArgument("timeRange"),
                        (int) environment.getArgument("numberOfDepartures"),
                        (boolean) environment.getArgument("omitNonPickups")))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stoptimesWithoutPatterns")
                .type(new GraphQLList(stoptimeType))
                .argument(GraphQLArgument.newArgument()
                    .name("startTime")
                    .type(Scalars.GraphQLString) // No long type exists in GraphQL specification
                    .defaultValue("0") // Default value is current time
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("timeRange")
                    .type(Scalars.GraphQLInt)
                    .defaultValue(24 * 60 * 60)
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("numberOfDepartures")
                    .type(Scalars.GraphQLInt)
                    .defaultValue(5)
                    .build())
                .argument(GraphQLArgument.newArgument()
            		.name("omitNonPickups")
            		.type(Scalars.GraphQLBoolean)
            		.defaultValue(false)
            		.build())
                .dataFetcher(environment ->
                    routingService.stopTimesForStop(
                        (Stop) environment.getSource(),
                        Long.parseLong(environment.getArgument("startTime")),
                        (int) environment.getArgument("timeRange"),
                        (int) environment.getArgument("numberOfDepartures"),
                        (boolean) environment.getArgument("omitNonPickups"))
                    .stream()
                    .flatMap(stoptimesWithPattern -> stoptimesWithPattern.times.stream())
                    .sorted(Comparator.comparing(t -> t.serviceDay + t.realtimeDeparture))
                    .limit((long) (int) environment.getArgument("numberOfDepartures"))
                    .collect(Collectors.toList()))
                .build())
            .build();

        stoptimeType = GraphQLObjectType.newObject()
            .name("Stoptime")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stop")
                .type(stopType)
                .dataFetcher(environment -> routingService.getStopForId()
                    .get(((TripTimeShort) environment.getSource()).stopId))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("scheduledArrival")
                .type(Scalars.GraphQLInt)
                .dataFetcher(
                    environment -> ((TripTimeShort) environment.getSource()).scheduledArrival)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("realtimeArrival")
                .type(Scalars.GraphQLInt)
                .dataFetcher(
                    environment -> ((TripTimeShort) environment.getSource()).realtimeArrival)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("arrivalDelay")
                .type(Scalars.GraphQLInt)
                .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).arrivalDelay)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("scheduledDeparture")
                .type(Scalars.GraphQLInt)
                .dataFetcher(
                    environment -> ((TripTimeShort) environment.getSource()).scheduledDeparture)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("realtimeDeparture")
                .type(Scalars.GraphQLInt)
                .dataFetcher(
                    environment -> ((TripTimeShort) environment.getSource()).realtimeArrival)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("departureDelay")
                .type(Scalars.GraphQLInt)
                .dataFetcher(
                    environment -> ((TripTimeShort) environment.getSource()).departureDelay)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("timepoint")
                .type(Scalars.GraphQLBoolean)
                .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).timepoint)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("realtime")
                .type(Scalars.GraphQLBoolean)
                .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).realtime)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("realtimeState")
                .type(realtimeStateEnum)
                .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).realtimeState)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("serviceDay")
                .type(Scalars.GraphQLLong)
                .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).serviceDay)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("trip")
                .type(tripType)
                .dataFetcher(environment -> routingService.getTripForId()
                    .get(((TripTimeShort) environment.getSource()).tripId))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
               	.name("headsign")
              	.type(Scalars.GraphQLString)
              	.dataFetcher(environment -> ((TripTimeShort) environment.getSource()).headsign)
              	.build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("notices")
                    .type(new GraphQLList(noticeType))
                    .argument(GraphQLArgument.newArgument()
                            .name("gtfsId")
                            .type(Scalars.GraphQLString)
                            .build())
                    .dataFetcher(environment -> {
                        TripTimeShort tts = (TripTimeShort) environment.getSource();
                        return routingService.getNoticesByEntity(new StopTimeKey(tts.tripId, tts.stopIndex));
                    })
                    .build())
            .build();

        noticeType = GraphQLObjectType.newObject()
                .name("Notice")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("Id")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(
                                environment -> ((Notice) environment.getSource()).getId())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("Text")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(
                                environment -> ((Notice) environment.getSource()).getText())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("PublicCode")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(
                                environment -> ((Notice) environment.getSource()).getPublicCode())
                        .build())
                .build();

        tripType = GraphQLObjectType.newObject()
            .name("Trip")
            .withInterface(nodeInterface)
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("id")
                .type(new GraphQLNonNull(Scalars.GraphQLID))
                .dataFetcher(environment -> relay.toGlobalId(
                    tripType.getName(),
                    GtfsLibrary.convertIdToString(((Trip) environment.getSource()).getId())))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("gtfsId")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .dataFetcher(environment -> GtfsLibrary
                    .convertIdToString(((Trip) environment.getSource()).getId()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("route")
                .type(new GraphQLNonNull(routeType))
                .dataFetcher(environment -> ((Trip) environment.getSource()).getRoute())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("operator")
                .type(operatorType)
                .dataFetcher(environment -> ((Trip) environment.getSource()).getOperator())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("serviceId")
                .type(Scalars.GraphQLString) //TODO:Should be serviceType
                .dataFetcher(environment -> GtfsLibrary
                    .convertIdToString(((Trip) environment.getSource()).getServiceId()))
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
                .type(Scalars.GraphQLString)
                .dataFetcher(environment -> GtfsLibrary
                    .convertIdToString(((Trip) environment.getSource()).getShapeId()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("wheelchairAccessible")
                .type(wheelchairBoardingEnum)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("bikesAllowed")
                .type(bikesAllowedEnum)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("pattern")
                .type(patternType)
                .dataFetcher(
                    environment -> routingService.getPatternForTrip().get((Trip) environment.getSource()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stops")
                .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(stopType))))
                .dataFetcher(environment -> routingService.getPatternForTrip()
                    .get((Trip) environment.getSource()).getStops())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("semanticHash")
                .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(stopType))))
                .dataFetcher(environment -> routingService.getPatternForTrip().get((Trip) environment.getSource())
                    .semanticHashString((Trip) environment.getSource()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stoptimes")
                .type(new GraphQLList(stoptimeType))
                .dataFetcher(environment -> TripTimeShort.fromTripTimes(
                    routingService.getPatternForTrip().get((Trip) environment.getSource()).scheduledTimetable,
                    (Trip) environment.getSource()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stoptimesForDate")
                .type(new GraphQLList(stoptimeType))
                .argument(GraphQLArgument.newArgument()
                    .name("serviceDay")
                    .type(Scalars.GraphQLString)
                    .build())
                .dataFetcher(environment -> {
                    try {
                        Trip trip = (Trip) environment.getSource();
                        ServiceDate serviceDay = ServiceDate.parseString(environment.getArgument("serviceDay"));
                        return routingService.getStopTimesForTripAndDate(trip, serviceDay);
                    } catch (ParseException e) {
                         return null; // Invalid date format
                    }
                })
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("notices")
                    .type(new GraphQLList(noticeType))
                    .argument(GraphQLArgument.newArgument()
                            .name("gtfsId")
                            .type(Scalars.GraphQLString)
                            .build())
                    .dataFetcher(environment -> {
                        return routingService.getNoticesByEntity((Trip) environment.getSource());
                    })
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("geometry")
                .type(Scalars.GraphQLString) //TODO: Should be geometry
                .dataFetcher(environment -> routingService.getPatternForTrip()
                    .get((Trip) environment.getSource()).getGeometry())
                .build())
            .build();

        coordinateType = GraphQLObjectType.newObject()
            .name("Coordinates")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("lat")
                .type(Scalars.GraphQLFloat)
                .dataFetcher(
                    environment -> (float) ((Coordinate) environment.getSource()).y)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("lon")
                .type(Scalars.GraphQLFloat)
                .dataFetcher(
                    environment -> (float) ((Coordinate) environment.getSource()).x)
                .build())
            .build();

        patternType = GraphQLObjectType.newObject()
            .name("Pattern")
            .withInterface(nodeInterface)
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("id")
                .type(new GraphQLNonNull(Scalars.GraphQLID))
                .dataFetcher(environment -> relay.toGlobalId(
                    patternType.getName(), ((TripPattern) environment.getSource()).getCode()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("route")
                .type(new GraphQLNonNull(routeType))
                .dataFetcher(environment -> ((TripPattern) environment.getSource()).route)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("directionId")
                .type(Scalars.GraphQLInt)
                .dataFetcher(environment -> ((TripPattern) environment.getSource()).directionId)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("name")
                .type(Scalars.GraphQLString)
                .dataFetcher(environment -> ((TripPattern) environment.getSource()).name)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("code")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .dataFetcher(environment -> ((TripPattern) environment.getSource()).getCode())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("headsign")
                .type(Scalars.GraphQLString)
                .dataFetcher(environment -> ((TripPattern) environment.getSource()).getDirection())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("trips")
                .type(new GraphQLList(new GraphQLNonNull(tripType)))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stops")
                .type(new GraphQLList(new GraphQLNonNull(stopType)))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("geometry")
                .type(new GraphQLList(coordinateType))
                .dataFetcher(environment -> {
                    LineString geometry = ((TripPattern) environment.getSource()).getGeometry();
                    if (geometry == null) {
                        return null;
                    } else {
                        return Arrays.asList(geometry.getCoordinates());
                    }
                })
                .build())
                // TODO: add stoptimes
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("semanticHash")
                .type(Scalars.GraphQLString)
                .dataFetcher(environment ->
                    ((TripPattern) environment.getSource()).semanticHashString(null))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("notices")
                    .type(new GraphQLList(noticeType))
                    .argument(GraphQLArgument.newArgument()
                            .name("gtfsId")
                            .type(Scalars.GraphQLString)
                            .build())
                    .dataFetcher(environment -> {
                        return routingService.getNoticesByEntity((TripPattern) environment.getSource());
                    })
                    .build())
            .build();


        routeType = GraphQLObjectType.newObject()
            .name("Route")
            .withInterface(nodeInterface)
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("id")
                .type(new GraphQLNonNull(Scalars.GraphQLID))
                .dataFetcher(environment -> relay.toGlobalId(
                    routeType.getName(),
                    GtfsLibrary.convertIdToString(((Route) environment.getSource()).getId())))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("gtfsId")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .dataFetcher(environment ->
                    GtfsLibrary.convertIdToString(((Route) environment.getSource()).getId()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("agency")
                .type(agencyType)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("operator")
                .type(operatorType)
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
                .name("mode")
                .type(Scalars.GraphQLString)
                .dataFetcher(environment -> GtfsLibrary.getTraverseMode(
                    (Route) environment.getSource()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("type")
                .type(Scalars.GraphQLString)
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
                .type(bikesAllowedEnum)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("patterns")
                .type(new GraphQLList(patternType))
                .dataFetcher(environment -> routingService.getPatternsForRoute()
                    .get((Route) environment.getSource()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stops")
                .type(new GraphQLList(stopType))
                .dataFetcher(environment -> routingService.getPatternsForRoute()
                    .get((Route) environment.getSource())
                    .stream()
                    .map(TripPattern::getStops)
                    .flatMap(Collection::stream)
                    .distinct()
                    .collect(Collectors.toList()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("trips")
                .type(new GraphQLList(tripType))
                .dataFetcher(environment -> routingService.getPatternsForRoute()
                    .get((Route) environment.getSource())
                    .stream()
                    .map(TripPattern::getTrips)
                    .flatMap(Collection::stream)
                    .distinct()
                    .collect(Collectors.toList()))
                .build())
            .build();

        agencyType = GraphQLObjectType.newObject()
            .name("Agency")
            .description("GTFS Agency or NeTEx Authority")
            .withInterface(nodeInterface)
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("id")
                .type(new GraphQLNonNull(Scalars.GraphQLID))
                .dataFetcher(environment -> relay
                    .toGlobalId(agencyType.getName(), ((Agency) environment.getSource()).getId()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("gtfsId")
                .description("Agency id")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .dataFetcher(environment -> ((Agency) environment.getSource()).getId())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("name")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("url")
                .type(Scalars.GraphQLString)
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
                .dataFetcher(environment -> routingService.getRouteForId().values()
                    .stream()
                    .filter(route -> route.getAgency() == environment.getSource())
                    .collect(Collectors.toList()))
                .build())
            .build();
            operatorType = GraphQLObjectType.newObject()
                    .name("Operator")
                    .description("NeTEx Operator, not available for data imported by GTFS")
                    .withInterface(nodeInterface)
                    .field(GraphQLFieldDefinition.newFieldDefinition()
                            .name("id")
                            .type(new GraphQLNonNull(Scalars.GraphQLID))
                            .dataFetcher(environment -> relay.toGlobalId(
                                    operatorType.getName(),
                                    GtfsLibrary.convertIdToString(((Operator) environment.getSource()).getId())))
                            .build())
                    .field(GraphQLFieldDefinition.newFieldDefinition()
                            .name("gtfsId")
                            .description("Operator id")
                            .type(new GraphQLNonNull(Scalars.GraphQLString))
                            .dataFetcher(environment ->
                                    GtfsLibrary.convertIdToString(((Operator) environment.getSource()).getId()))
                            .build())
                    .field(GraphQLFieldDefinition.newFieldDefinition()
                            .name("name")
                            .type(new GraphQLNonNull(Scalars.GraphQLString))
                            .build())
                    .field(GraphQLFieldDefinition.newFieldDefinition()
                            .name("url")
                            .type(Scalars.GraphQLString)
                            .build())
                    .field(GraphQLFieldDefinition.newFieldDefinition()
                            .name("phone")
                            .type(Scalars.GraphQLString)
                            .build())
                    .field(GraphQLFieldDefinition.newFieldDefinition()
                            .name("routes")
                            .type(new GraphQLList(routeType))
                            .dataFetcher(environment -> routingService.getRouteForId().values()
                                    .stream()
                                    .filter(route -> route.getOperator() == environment.getSource())
                                    .collect(Collectors.toList()))
                            .build())
                    .field(GraphQLFieldDefinition.newFieldDefinition()
                            .name("trips")
                            .type(new GraphQLList(tripType))
                            .dataFetcher(environment -> routingService.getTripForId().values()
                                    .stream()
                                    .filter(trip -> trip.getOperator() == environment.getSource())
                                    .collect(Collectors.toList()))
                            .build())
                    .build();

        queryType = GraphQLObjectType.newObject()
            .name("QueryType")
            .field(relay.nodeField(nodeInterface, environment -> {
                Relay.ResolvedGlobalId id = relay.fromGlobalId(environment.getArgument("id"));
                if (id.getType().equals(stopType.getName())) {
                    return routingService.getStopForId().get(GtfsLibrary.convertIdFromString(id.getId()));
                }
                if (id.getType().equals(tripType.getName())) {
                    return routingService.getTripForId().get(GtfsLibrary.convertIdFromString(id.getId()));
                }
                if (id.getType().equals(routeType.getName())) {
                    return routingService.getRouteForId().get(GtfsLibrary.convertIdFromString(id.getId()));
                }
                if (id.getType().equals(patternType.getName())) {
                    return routingService.getTripPatternForId(id.getId());
                }
                if (id.getType().equals(agencyType.getName())) {
                    return routingService.getAgencyWithoutFeedId(id.getId());
                }
                if (id.getType().equals(operatorType.getName())) {
                    return routingService.getOperatorForId().get(GtfsLibrary.convertIdFromString(id.getId()));
                }
                return null;
            }))
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("notices")
                    .type(new GraphQLList(noticeType))
                    .dataFetcher(environment -> routingService.getNotices())
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("agencies")
                .description("Get all agencies for the specified graph")
                .type(new GraphQLList(agencyType))
                .dataFetcher(environment -> routingService.getAllAgencies())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("agency")
                .description("Get a single agency based on agency ID")
                .type(agencyType)
                .argument(GraphQLArgument.newArgument()
                    .name("id")
                    .type(new GraphQLNonNull(Scalars.GraphQLString))
                    .build())
                .dataFetcher(environment ->
                    routingService.getAgencyWithoutFeedId(environment.getArgument("id")))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("operators")
                .description("Get all operators for the specified graph")
                .type(new GraphQLList(operatorType))
                .dataFetcher(environment -> new ArrayList<>(routingService.getAllOperators()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("operator")
                    .description("Get a single operator based on operator ID")
                    .type(operatorType)
                    .argument(GraphQLArgument.newArgument()
                            .name("id")
                            .type(new GraphQLNonNull(Scalars.GraphQLString))
                            .build())
                    .dataFetcher(environment -> routingService.getOperatorForId().get(
                            GtfsLibrary.convertIdFromString((String)environment.getArgument("id"))))
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stops")
                .description("Get all stops for the specified graph")
                .type(new GraphQLList(stopType))
                .argument(GraphQLArgument.newArgument()
                    .name("ids")
                    .type(new GraphQLList(Scalars.GraphQLString))
                    .build())
                .dataFetcher(environment -> {
                    if (!(environment.getArgument("ids") instanceof List)) {
                        return new ArrayList<>(routingService.getStopForId().values());
                    } else {
                        return ((List<String>) environment.getArgument("ids"))
                            .stream()
                            .map(id -> routingService.getStopForId().get(GtfsLibrary.convertIdFromString(id)))
                            .collect(Collectors.toList());
                    }
                })
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stopsByBbox")
                .description("Get all stops within the specified bounding box")
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
                .argument(GraphQLArgument.newArgument()
                    .name("agency")
                    .type(Scalars.GraphQLString)
                    .build())
                .dataFetcher(environment -> routingService.getStopsByBoundingBox(new Envelope(
                        new Coordinate(environment.getArgument("minLon"), environment.getArgument("minLat")),
                        new Coordinate(environment.getArgument("maxLon"), environment.getArgument("maxLat"))))
                    .stream()
                    .filter(stop -> environment.getArgument("agency") == null || stop.getId()
                        .getFeedId().equalsIgnoreCase(environment.getArgument("agency")))
                    .collect(Collectors.toList()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stopsByRadius")
                .description(
                    "Get all stops within the specified radius from a location. The returned type has two fields stop and distance")
                .type(relay.connectionType("stopAtDistance",
                    relay.edgeType("stopAtDistance", stopAtDistanceType, null, new ArrayList<>()),
                    new ArrayList<>()))
                .argument(GraphQLArgument.newArgument()
                    .name("lat")
                    .description("Latitude of the location")
                    .type(Scalars.GraphQLFloat)
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("lon")
                    .description("Longitude of the location")
                    .type(Scalars.GraphQLFloat)
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("radius")
                    .description("Radius (in meters) to search for from the specidied location")
                    .type(Scalars.GraphQLInt)
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("agency")
                    .type(Scalars.GraphQLString)
                    .build())
                .argument(relay.getConnectionFieldArguments())
                .dataFetcher(environment ->
                    new SimpleListConnection(routingService.findClosestStopsByWalking(
                        environment.getArgument("lat"), environment.getArgument("lon"),
                        environment.getArgument("radius")
                    )
                        .stream()
                        .filter(stopAndDistance -> environment.getArgument("agency") == null ||
                            stopAndDistance.stop.getId().getFeedId()
                                .equalsIgnoreCase(environment.getArgument("agency")))
                        .sorted(Comparator.comparing(s -> (float) s.distance))
                        .collect(Collectors.toList()))
                        .get(environment))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stop")
                .description("Get a single stop based on its id (format is Agency:StopId)")
                .type(stopType)
                .argument(GraphQLArgument.newArgument()
                    .name("id")
                    .type(new GraphQLNonNull(Scalars.GraphQLString))
                    .build())
                .dataFetcher(environment -> routingService.getStopForId()
                    .get(GtfsLibrary.convertIdFromString(environment.getArgument("id"))))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("routes")
                .description("Get all routes for the specified graph")
                .type(new GraphQLList(routeType))
                .argument(GraphQLArgument.newArgument()
                    .name("ids")
                    .type(new GraphQLList(Scalars.GraphQLString))
                    .build())
                .dataFetcher(environment -> {
                    if (!(environment.getArgument("ids") instanceof List)) {
                        return new ArrayList<>(routingService.getRouteForId().values());
                    } else {
                        return ((List<String>) environment.getArgument("ids"))
                            .stream()
                            .map(id -> routingService.getRouteForId().get(GtfsLibrary.convertIdFromString(id)))
                            .collect(Collectors.toList());
                    }
                })
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("route")
                .description("Get a single route based on its id (format is Agency:RouteId)")
                .type(routeType)
                .argument(GraphQLArgument.newArgument()
                    .name("id")
                    .type(new GraphQLNonNull(Scalars.GraphQLString))
                    .build())
                .dataFetcher(environment -> routingService.getRouteForId()
                    .get(GtfsLibrary.convertIdFromString(environment.getArgument("id"))))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("trips")
                .description("Get all trips for the specified graph")
                .type(new GraphQLList(tripType))
                .dataFetcher(environment -> new ArrayList<>(routingService.getTripForId().values()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("trip")
                .description("Get a single trip based on its id (format is Agency:TripId)")
                .type(tripType)
                .argument(GraphQLArgument.newArgument()
                    .name("id")
                    .type(new GraphQLNonNull(Scalars.GraphQLString))
                    .build())
                .dataFetcher(environment -> routingService.getTripForId()
                    .get(GtfsLibrary.convertIdFromString(environment.getArgument("id"))))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("fuzzyTrip")
                .type(tripType)
                .argument(GraphQLArgument.newArgument()
                    .name("route")
                    .type(Scalars.GraphQLString)
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("direction")
                    .type(Scalars.GraphQLInt)
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("date")
                    .type(Scalars.GraphQLString)
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("time")
                    .type(Scalars.GraphQLInt)
                    .build())
                .dataFetcher(environment -> {
                    try {
                        return fuzzyTripMatcher.getTrip(
                            routingService.getRouteForId().get(
                                GtfsLibrary.convertIdFromString(environment.getArgument("route"))),
                            environment.getArgument("direction"),
                            environment.getArgument("time"),
                            ServiceDate.parseString(environment.getArgument("date"))
                        );
                    } catch (ParseException e) {
                        return null; // Invalid date format
                    }
                })
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("patterns")
                .description("Get all patterns for the specified graph")
                .type(new GraphQLList(patternType))
                .dataFetcher(environment -> new ArrayList<>(routingService.getTripPatterns()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("pattern")
                .description("Get a single pattern based on its id")
                .type(patternType)
                .argument(GraphQLArgument.newArgument()
                    .name("id")
                    .type(new GraphQLNonNull(Scalars.GraphQLString))
                    .build())
                .dataFetcher(environment -> routingService.getTripPatternForId(environment.getArgument("id")))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("viewer")
                .description(
                    "Needed until https://github.com/facebook/relay/issues/112 is resolved")
                .type(new GraphQLTypeReference("QueryType"))
                .dataFetcher(DataFetchingEnvironment::getParentType)
                .build())
            .build();

        indexSchema = GraphQLSchema.newSchema()
            .query(queryType)
            .build();
    }
}
