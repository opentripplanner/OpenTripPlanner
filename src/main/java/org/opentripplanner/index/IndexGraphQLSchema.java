package org.opentripplanner.index;

import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.LineString;
import graphql.Scalars;
import graphql.relay.Relay;
import graphql.relay.SimpleListConnection;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.PropertyDataFetcher;
import graphql.schema.TypeResolver;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.api.common.Message;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.api.model.Place;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.model.VertexType;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.index.model.StopTimesInPattern;
import org.opentripplanner.index.model.TripTimeShort;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.profile.StopCluster;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.bike_park.BikePark;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.bike_rental.BikeRentalStationService;
import org.opentripplanner.routing.car_park.CarPark;
import org.opentripplanner.routing.car_park.CarParkService;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.FareComponent;
import org.opentripplanner.routing.core.Money;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.SimpleTransfer;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.edgetype.TimetableSnapshot;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.graph.GraphIndex.PlaceAndDistance;
import org.opentripplanner.routing.trippattern.RealTimeState;
import org.opentripplanner.routing.vertextype.TransitVertex;
import org.opentripplanner.updater.GtfsRealtimeFuzzyTripMatcher;
import org.opentripplanner.updater.stoptime.TimetableSnapshotSource;
import org.opentripplanner.util.ResourceBundleSingleton;
import org.opentripplanner.util.TranslatedString;
import org.opentripplanner.util.model.EncodedPolylineBean;

import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;

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
    
    public static GraphQLEnumType pickupDropoffTypeEnum = GraphQLEnumType.newEnum()
        .name("PickupDropoffType")
        .value("SCHEDULED", StopPattern.PICKDROP_SCHEDULED, "Regularly scheduled pickup / drop off.")
        .value("NONE", StopPattern.PICKDROP_NONE, "No pickup / drop off available.")
        .value("CALL_AGENCY", StopPattern.PICKDROP_CALL_AGENCY, "Must phone agency to arrange pickup / drop off.")
        .value("COORDINATE_WITH_DRIVER", StopPattern.PICKDROP_COORDINATE_WITH_DRIVER, "Must coordinate with driver to arrange pickup / drop off.")
        .build();

    public static GraphQLEnumType vertexTypeEnum = GraphQLEnumType.newEnum()
        .name("VertexType")
        .value("NORMAL", VertexType.NORMAL, "NORMAL")
        .value("TRANSIT", VertexType.TRANSIT, "TRANSIT")
        .value("BIKEPARK", VertexType.BIKEPARK, "BIKEPARK")
        .value("BIKESHARE", VertexType.BIKESHARE, "BIKESHARE")
        .value("PARKANDRIDE", VertexType.PARKANDRIDE, "PARKANDRIDE")
        .build();

    public static GraphQLEnumType modeEnum = GraphQLEnumType.newEnum()
        .name("Mode")
        .value("AIRPLANE", TraverseMode.AIRPLANE, "AIRPLANE")
        .value("BICYCLE", TraverseMode.BICYCLE, "BICYCLE")
        .value("BUS", TraverseMode.BUS, "BUS")
        .value("CABLE_CAR", TraverseMode.CABLE_CAR, "CABLE_CAR")
        .value("CAR", TraverseMode.CAR, "CAR")
        .value("FERRY", TraverseMode.FERRY, "FERRY")
        .value("FUNICULAR", TraverseMode.FUNICULAR, "FUNICULAR")
        .value("GONDOLA", TraverseMode.GONDOLA, "GONDOLA")
        .value("LEG_SWITCH", TraverseMode.LEG_SWITCH, "LEG_SWITCH")
        .value("RAIL", TraverseMode.RAIL, "RAIL")
        .value("SUBWAY", TraverseMode.SUBWAY, "SUBWAY")
        .value("TRAM", TraverseMode.TRAM, "TRAM")
        .value("TRANSIT", TraverseMode.TRANSIT, "TRANSIT")
        .value("WALK", TraverseMode.WALK, "WALK")
        .build();

    public static GraphQLEnumType filterPlaceTypeEnum = GraphQLEnumType.newEnum()
        .name("FilterPlaceType")
        .value("STOP", GraphIndex.PlaceType.STOP, "Stops")
        .value("DEPARTURE_ROW", GraphIndex.PlaceType.DEPARTURE_ROW, "Departure rows")
        .value("BICYCLE_RENT", GraphIndex.PlaceType.BICYCLE_RENT, "Bicycle rent stations")
        .value("BIKE_PARK", GraphIndex.PlaceType.BIKE_PARK, "Bike parks")
        .value("CAR_PARK", GraphIndex.PlaceType.CAR_PARK, "Car parks")
        .build();

    public static GraphQLEnumType optimizeTypeEnum = GraphQLEnumType.newEnum()
        .name("OptimizeType")
        .value("QUICK", OptimizeType.QUICK, "QUICK")
        .value("SAFE", OptimizeType.SAFE, "SAFE")
        .value("FLAT", OptimizeType.FLAT, "FLAT")
        .value("GREENWAYS", OptimizeType.GREENWAYS, "GREENWAYS")
        .value("TRIANGLE", OptimizeType.TRIANGLE, "TRIANGLE")
        .value("TRANSFERS", OptimizeType.TRANSFERS, "TRANSFERS")
        .build();

    private final GtfsRealtimeFuzzyTripMatcher fuzzyTripMatcher;

    public GraphQLOutputType agencyType = new GraphQLTypeReference("Agency");

    public GraphQLOutputType alertType = new GraphQLTypeReference("Alert");

    public GraphQLOutputType serviceTimeRangeType = new GraphQLTypeReference("ServiceTimeRange");

    public GraphQLOutputType bikeRentalStationType = new GraphQLTypeReference("BikeRentalStation");

    public GraphQLOutputType bikeParkType = new GraphQLTypeReference("BikePark");

    public GraphQLOutputType carParkType = new GraphQLTypeReference("CarPark");

    public GraphQLOutputType coordinateType = new GraphQLTypeReference("Coordinates");

    public GraphQLOutputType clusterType = new GraphQLTypeReference("Cluster");

    public GraphQLOutputType patternType = new GraphQLTypeReference("Pattern");

    public GraphQLOutputType routeType = new GraphQLTypeReference("Route");

    public GraphQLOutputType stoptimeType = new GraphQLTypeReference("Stoptime");

    public GraphQLOutputType stopType = new GraphQLTypeReference("Stop");

    public GraphQLOutputType tripType = new GraphQLTypeReference("Trip");

    public GraphQLOutputType stopAtDistanceType = new GraphQLTypeReference("StopAtDistance");

    public GraphQLOutputType stoptimesInPatternType = new GraphQLTypeReference("StoptimesInPattern");

    public GraphQLOutputType translatedStringType = new GraphQLTypeReference("TranslatedString");

    public GraphQLOutputType departureRowType = new GraphQLTypeReference("DepartureRow");

    public GraphQLOutputType placeAtDistanceType = new GraphQLTypeReference("PlaceAtDistance");

    public GraphQLObjectType queryType;

    public GraphQLOutputType planType = new GraphQLTypeReference("Plan");

    public GraphQLSchema indexSchema;

    private Relay relay = new Relay();

    private GraphQLInterfaceType nodeInterface = relay.nodeInterface(new TypeResolver() {
        @Override
        public GraphQLObjectType getType(Object o) {
            if (o instanceof StopCluster) {
                return (GraphQLObjectType) clusterType;
            }
            if (o instanceof GraphIndex.StopAndDistance) {
                return (GraphQLObjectType) stopAtDistanceType;
            }
            if (o instanceof Stop) {
                return (GraphQLObjectType) stopType;
            }
            if (o instanceof Trip) {
                return (GraphQLObjectType) tripType;
            }
            if (o instanceof Route) {
                return (GraphQLObjectType) routeType;
            }
            if (o instanceof TripPattern) {
                return (GraphQLObjectType) patternType;
            }
            if (o instanceof Agency) {
                return (GraphQLObjectType) agencyType;
            }
            if (o instanceof AlertPatch) {
                return (GraphQLObjectType) alertType;
            }
            if (o instanceof BikeRentalStation) {
                return (GraphQLObjectType) bikeRentalStationType;
            }
            if (o instanceof BikePark) {
                return (GraphQLObjectType) bikeParkType;
            }
            if (o instanceof CarPark) {
                return (GraphQLObjectType) carParkType;
            }
            if (o instanceof GraphIndex.DepartureRow) {
                return (GraphQLObjectType) departureRowType;
            }
            if (o instanceof PlaceAndDistance) {
                return (GraphQLObjectType) placeAtDistanceType;
            }
            return null;
        }
    });

    private GraphQLInterfaceType placeInterface = GraphQLInterfaceType.newInterface()
        .name("PlaceInterface")
        .description("Interface for places, i.e. stops, stations, parks")
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("id")
            .type(new GraphQLNonNull(Scalars.GraphQLID))
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("lat")
            .type(Scalars.GraphQLFloat)
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("lon")
            .type(Scalars.GraphQLFloat)
            .build())
        .typeResolver(new TypeResolver() {
            @Override
            public GraphQLObjectType getType(Object o) {
                if (o instanceof Stop) {
                    return (GraphQLObjectType) stopType;
                }
                if (o instanceof GraphIndex.DepartureRow) {
                    return (GraphQLObjectType) departureRowType;
                }
                if (o instanceof BikeRentalStation) {
                    return (GraphQLObjectType) bikeRentalStationType;
                }
                if (o instanceof BikePark) {
                    return (GraphQLObjectType) bikeParkType;
                }
                if (o instanceof CarPark) {
                    return (GraphQLObjectType) carParkType;
                }
                return null;
            }
        }).build();

    private Agency getAgency(GraphIndex index, String agencyId) {
        //xxx what if there are duplciate agency ids?
        //now we return the first
        for (Map<String, Agency> feedAgencies : index.agenciesForFeedId.values()) {
            if (feedAgencies.get(agencyId) != null) {
                return feedAgencies.get(agencyId);
            }
        }
        return null;
    }

    private List<Agency> getAllAgencies(GraphIndex index) {
        //xxx what if there are duplciate agency ids?
        //now we return the first
        ArrayList<Agency> agencies = new ArrayList<Agency>();
        for (Map<String, Agency> feedAgencies : index.agenciesForFeedId.values()) {
            agencies.addAll(feedAgencies.values());
        }
        return agencies;
    }

    @SuppressWarnings("unchecked")
    public IndexGraphQLSchema(GraphIndex index) {
        createPlanType(index);

        GraphQLInputObjectType coordinateInputType = GraphQLInputObjectType.newInputObject()
            .name("InputCoordinates")
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("lat")
                .description("The latitude of the place.")
                .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("lon")
                .description("The longitude of the place.")
                .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("address")
                .description("The name of the place.")
                .type(Scalars.GraphQLString)
                .build())
            .build();

        GraphQLInputObjectType preferredInputType = GraphQLInputObjectType.newInputObject()
            .name("InputPreferred")
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("routes")
                .description("Set of preferred agencies by user.")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("agencies")
                .description("Set of preferred agencies by user.")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("otherThanPreferredRoutesPenalty")
                .description("Penalty added for using every route that is not preferred if user set any route as preferred. We return number of seconds that we are willing to wait for preferred route.")
                .type(Scalars.GraphQLInt)
                .build())
            .build();

        GraphQLInputObjectType unpreferredInputType = GraphQLInputObjectType.newInputObject()
            .name("InputUnpreferred")
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("routes")
                .description("Set of unpreferred routes for given user.")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("agencies")
                .description("Set of unpreferred agencies for given user.")
                .type(Scalars.GraphQLString)
                .build())
            .build();

        GraphQLInputObjectType bannedInputType = GraphQLInputObjectType.newInputObject()
            .name("InputBanned")
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("routes")
                .description("Do not use certain named routes")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("agencies")
                .description("Do not use certain named agencies")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("trips")
                .description("Do not use certain named trips")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("stops")
                .description("Do not use certain stops. See for more information the bannedStops property in the RoutingResource class.")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("stopsHard")
                .description("Do not use certain stops. See for more information the bannedStopsHard property in the RoutingResource class.")
                .type(Scalars.GraphQLString)
                .build())
            .build();

        GraphQLInputObjectType triangleInputType = GraphQLInputObjectType.newInputObject()
            .name("InputTriangle")
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("safetyFactor")
                .description("For the bike triangle, how important safety is")
                .type(Scalars.GraphQLFloat)
                .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("slopeFactor")
                .description("For the bike triangle, how important slope is")
                .type(Scalars.GraphQLFloat)
                .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("timeFactor")
                .description("For the bike triangle, how important time is")
                .type(Scalars.GraphQLFloat)
                .build())
            .build();

        GraphQLFieldDefinition planFieldType = GraphQLFieldDefinition.newFieldDefinition()
            .name("plan")
            .description("Gets plan of a route")
            .type(planType)
            .argument(GraphQLArgument.newArgument()
                .name("date")
                .type(Scalars.GraphQLString)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("time")
                .type(Scalars.GraphQLString)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("from")
                .description("The start location")
                .type(coordinateInputType)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("to")
                .description("The end location")
                .type(coordinateInputType)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("fromPlace")
                .type(Scalars.GraphQLString)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("toPlace")
                .type(Scalars.GraphQLString)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("wheelchair")
                .description("Whether the trip must be wheelchair accessible.")
                .type(Scalars.GraphQLBoolean)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("numItineraries")
                .description("The maximum number of itineraries to return.")
                .defaultValue(3)
                .type(Scalars.GraphQLInt)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("maxWalkDistance")
                .description("The maximum distance (in meters) the user is willing to walk. Defaults to unlimited.")
                .type(Scalars.GraphQLFloat)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("maxPreTransitTime")
                .description("The maximum time (in seconds) of pre-transit travel when using drive-to-transit (park and ride or kiss and ride). Defaults to unlimited.")
                .type(Scalars.GraphQLInt)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("walkReluctance")
                .description("A multiplier for how bad walking is, compared to being in transit for equal lengths of time. Defaults to 2. Empirically, values between 10 and 20 seem to correspond well to the concept of not wanting to walk too much without asking for totally ridiculous itineraries, but this observation should in no way be taken as scientific or definitive. Your mileage may vary.")
                .type(Scalars.GraphQLFloat)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("waitReluctance")
                .description("How much worse is waiting for a transit vehicle than being on a transit vehicle, as a multiplier. The default value treats wait and on-vehicle time as the same. It may be tempting to set this higher than walkReluctance (as studies often find this kind of preferences among riders) but the planner will take this literally and walk down a transit line to avoid waiting at a stop. This used to be set less than 1 (0.95) which would make waiting offboard preferable to waiting onboard in an interlined trip. That is also undesirable. If we only tried the shortest possible transfer at each stop to neighboring stop patterns, this problem could disappear.")
                .type(Scalars.GraphQLFloat)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("waitAtBeginningFactor")
                .description("How much less bad is waiting at the beginning of the trip (replaces waitReluctance on the first boarding)")
                .type(Scalars.GraphQLFloat)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("walkSpeed")
                .description("max walk speed along streets, in meters per second")
                .type(Scalars.GraphQLFloat)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("bikeSpeed")
                .description("max bike speed along streets, in meters per second")
                .type(Scalars.GraphQLFloat)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("bikeSwitchTime")
                .description("Time to get on and off your own bike")
                .type(Scalars.GraphQLInt)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("bikeSwitchCost")
                .description("Cost of getting on and off your own bike")
                .type(Scalars.GraphQLInt)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("optimize")
                .description("The set of characteristics that the user wants to optimize for -- defaults to QUICK, or optimize for transit time.")
                .type(optimizeTypeEnum)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("triangle")
                .description("Triangle optimization parameters. triangleTimeFactor+triangleSlopeFactor+triangleSafetyFactor == 1")
                .type(triangleInputType)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("arriveBy")
                .description("Whether the trip should depart at dateTime (false, the default), or arrive at dateTime.")
                .type(Scalars.GraphQLBoolean)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("intermediatePlaces")
                .description("An ordered list of intermediate locations to be visited.")
                .type(new GraphQLList(coordinateInputType))
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("preferred")
                .description("Preferred")
                .type(preferredInputType)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("unpreferred")
                .description("Unpreferred")
                .type(unpreferredInputType)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("walkBoardCost")
                .description("This prevents unnecessary transfers by adding a cost for boarding a vehicle.")
                .type(Scalars.GraphQLInt)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("bikeBoardCost")
                .description("Separate cost for boarding a vehicle with a bicycle, which is more difficult than on foot.")
                .type(Scalars.GraphQLInt)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("banned")
                .description("Banned")
                .type(bannedInputType)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("transferPenalty")
                .description("An extra penalty added on transfers (i.e. all boardings except the first one). Not to be confused with bikeBoardCost and walkBoardCost, which are the cost of boarding a vehicle with and without a bicycle. The boardCosts are used to model the 'usual' perceived cost of using a transit vehicle, and the transferPenalty is used when a user requests even less transfers. In the latter case, we don't actually optimize for fewest transfers, as this can lead to absurd results. Consider a trip in New York from Grand Army Plaza (the one in Brooklyn) to Kalustyan's at noon. The true lowest transfers route is to wait until midnight, when the 4 train runs local the whole way. The actual fastest route is the 2/3 to the 4/5 at Nevins to the 6 at Union Square, which takes half an hour. Even someone optimizing for fewest transfers doesn't want to wait until midnight. Maybe they would be willing to walk to 7th Ave and take the Q to Union Square, then transfer to the 6. If this takes less than optimize_transfer_penalty seconds, then that's what we'll return.")
                .type(Scalars.GraphQLInt)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("batch")
                .description("when true, do not use goal direction or stop at the target, build a full SPT")
                .type(Scalars.GraphQLBoolean)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("modes")
                .description("The set of TraverseModes that a user is willing to use. Defaults to WALK | TRANSIT.")
                .type(Scalars.GraphQLString)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("allowBikeRental")
                .description("Is bike rental allowed?")
                .type(Scalars.GraphQLBoolean)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("boardSlack")
                .description("Invariant: boardSlack + alightSlack <= transferSlack.")
                .type(Scalars.GraphQLInt)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("alightSlack")
                .description("Invariant: boardSlack + alightSlack <= transferSlack.")
                .type(Scalars.GraphQLInt)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("minTransferTime")
                .description("A global minimum transfer time (in seconds) that specifies the minimum amount of time that must pass between exiting one transit vehicle and boarding another. This time is in addition to time it might take to walk between transit stops. This time should also be overridden by specific transfer timing information in transfers.txt")
                .type(Scalars.GraphQLInt)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("nonpreferredTransferPenalty")
                .description("Penalty for using a non-preferred transfer")
                .type(Scalars.GraphQLInt)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("maxTransfers")
                .description("Maximum number of transfers")
                .type(Scalars.GraphQLInt)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("startTransitStopId")
                .description("A transit stop that this trip must start from")
                .type(Scalars.GraphQLString)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("startTransitTripId")
                .description("A trip where this trip must start from (depart-onboard routing)")
                .type(Scalars.GraphQLString)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("claimInitialWait")
                .description("The maximum wait time in seconds the user is willing to delay trip start. Only effective in Analyst.")
                .type(Scalars.GraphQLLong)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("reverseOptimizeOnTheFly")
                .description("When true, reverse optimize this search on the fly whenever needed, rather than reverse-optimizing the entire path when it's done.")
                .type(Scalars.GraphQLBoolean)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("ignoreRealtimeUpdates")
                .description("When true, realtime updates are ignored during this search.")
                .type(Scalars.GraphQLBoolean)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("disableRemainingWeightHeuristic")
                .description("If true, the remaining weight heuristic is disabled. Currently only implemented for the long distance path service.")
                .type(Scalars.GraphQLBoolean)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("locale")
                .description("Locale for returned text")
                .type(Scalars.GraphQLString)
                .build())
            .dataFetcher(environment -> new GraphQlPlanner(index).plan(environment))
            .build();

        fuzzyTripMatcher = new GtfsRealtimeFuzzyTripMatcher(index);
        index.clusterStopsAsNeeded();

        translatedStringType = GraphQLObjectType.newObject()
            .name("TranslatedString")
            .description("Text with language")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("text")
                .type(Scalars.GraphQLString)
                .dataFetcher(environment -> ((Map.Entry<String, String>) environment.getSource()).getValue())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("language")
                .type(Scalars.GraphQLString)
                .dataFetcher(environment -> ((Map.Entry<String, String>) environment.getSource()).getKey())
                .build())
            .build();

        alertType = GraphQLObjectType.newObject()
            .name("Alert")
            .withInterface(nodeInterface)
            .description("Simple alert")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("id")
                .type(new GraphQLNonNull(Scalars.GraphQLID))
                .dataFetcher(environment -> relay.toGlobalId(
                    alertType.getName(), ((AlertPatch) environment.getSource()).getId()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("agency")
                .type(agencyType)
                .dataFetcher(environment -> getAgency(index, ((AlertPatch) environment.getSource()).getAgency()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("route")
                .type(routeType)
                .dataFetcher(environment -> index.routeForId.get(((AlertPatch) environment.getSource()).getRoute()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("trip")
                .type(tripType)
                .dataFetcher(environment -> index.tripForId.get(((AlertPatch) environment.getSource()).getTrip()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stop")
                .type(stopType)
                .dataFetcher(environment -> index.stopForId.get(((AlertPatch) environment.getSource()).getStop()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("patterns")
                .description("Get all patterns for this alert")
                .type(new GraphQLList(patternType))
                .dataFetcher(environment -> ((AlertPatch) environment.getSource()).getTripPatterns())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("alertHeaderText")
                .type(Scalars.GraphQLString)
                .description("Header of alert if it exists")
                .dataFetcher(environment -> ((AlertPatch) environment.getSource()).getAlert().alertHeaderText)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("alertHeaderTextTranslations")
                .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(translatedStringType))))
                .description("Headers of alert in all different translations available notnull")
                .dataFetcher(environment -> {
                    AlertPatch alertPatch = (AlertPatch) environment.getSource();
                    Alert alert = alertPatch.getAlert();
                    if (alert.alertHeaderText instanceof TranslatedString) {
                        return ((TranslatedString) alert.alertHeaderText).getTranslations();
                    } else {
                        return emptyList();
                    }
                })
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("alertDescriptionText")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .description("Long description of alert notnull")
                .dataFetcher(environment -> ((AlertPatch) environment.getSource()).getAlert().alertDescriptionText)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("alertDescriptionTextTranslations")
                .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(translatedStringType))))
                .description("Long descriptions of alert in all different translations available notnull")
                .dataFetcher(environment -> {
                    AlertPatch alertPatch = (AlertPatch) environment.getSource();
                    Alert alert = alertPatch.getAlert();
                    if (alert.alertDescriptionText instanceof TranslatedString) {
                        return ((TranslatedString) alert.alertDescriptionText).getTranslations();
                    } else {
                        return emptyList();
                    }
                })
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("alertUrl")
                .type(Scalars.GraphQLString)
                .description("Url with more information")
                .dataFetcher(environment -> ((AlertPatch) environment.getSource()).getAlert().alertUrl)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("effectiveStartDate")
                .type(Scalars.GraphQLLong)
                .description("When this alert comes into effect")
                .dataFetcher(environment -> {
                    Alert alert = ((AlertPatch) environment.getSource()).getAlert();
                    return alert.effectiveStartDate != null ? alert.effectiveStartDate.getTime() / 1000 : null;
                })
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("effectiveEndDate")
                .type(Scalars.GraphQLLong)
                .description("When this alert is not in effect anymore")
                .dataFetcher(environment -> {
                    Alert alert = ((AlertPatch) environment.getSource()).getAlert();
                    return alert.effectiveEndDate != null ? alert.effectiveEndDate.getTime() / 1000 : null;
                })
                .build())
            .build();


        serviceTimeRangeType = GraphQLObjectType.newObject()
            .name("serviceTimeRange")
            .description("Time range covered by the routing graph")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("start")
                .type(Scalars.GraphQLLong)
                .description("Beginning of service time range")
                .dataFetcher(environment -> index.graph.getTransitServiceStarts())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("end")
                .type(Scalars.GraphQLLong)
                .description("End of service time range")
                .dataFetcher(environment -> index.graph.getTransitServiceEnds())
                .build())
            .build();


        stopAtDistanceType = GraphQLObjectType.newObject()
            .name("stopAtDistance")
            .withInterface(nodeInterface)
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("id")
                .type(new GraphQLNonNull(Scalars.GraphQLID))
                .dataFetcher(environment -> relay.toGlobalId(stopAtDistanceType.getName(),
                    Integer.toString(((GraphIndex.StopAndDistance) environment.getSource()).distance) + ";" +
                    GtfsLibrary.convertIdToString(((GraphIndex.StopAndDistance) environment.getSource()).stop.getId())))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stop")
                .type(stopType)
                .dataFetcher(environment -> ((GraphIndex.StopAndDistance) environment.getSource()).stop)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("distance")
                .type(Scalars.GraphQLInt)
                .dataFetcher(environment -> ((GraphIndex.StopAndDistance) environment.getSource()).distance)
                .build())
            .build();

        departureRowType = GraphQLObjectType.newObject()
            .name("DepartureRow")
            .withInterface(nodeInterface)
            .withInterface(placeInterface)
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("id")
                .type(new GraphQLNonNull(Scalars.GraphQLID))
                .dataFetcher(environment -> relay.toGlobalId(departureRowType.getName(), ((GraphIndex.DepartureRow)environment.getSource()).id))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stop")
                .type(stopType)
                .dataFetcher(environment -> ((GraphIndex.DepartureRow)environment.getSource()).stop)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("lat")
                .type(Scalars.GraphQLFloat)
                .dataFetcher(environment -> ((GraphIndex.DepartureRow)environment.getSource()).stop.getLat())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("lon")
                .type(Scalars.GraphQLFloat)
                .dataFetcher(environment -> ((GraphIndex.DepartureRow)environment.getSource()).stop.getLon())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("pattern")
                .type(patternType)
                .dataFetcher(environment -> ((GraphIndex.DepartureRow)environment.getSource()).pattern)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stoptimes")
                .type(new GraphQLList(stoptimeType))
                .argument(GraphQLArgument.newArgument()
                    .name("startTime")
                    .description("What is the start time for the times. Default is to use current time. (0)")
                    .type(Scalars.GraphQLLong)
                    .defaultValue(0l) // Default value is current time
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("timeRange")
                    .description("How many seconds ahead to search for departures. Default is one day.")
                    .type(Scalars.GraphQLInt)
                    .defaultValue(24 * 60 * 60)
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("numberOfDepartures")
                    .description("Maximum number of departures to return.")
                    .type(Scalars.GraphQLInt)
                    .defaultValue(1)
                    .build())
                .dataFetcher(environment -> {
                    GraphIndex.DepartureRow departureRow = (GraphIndex.DepartureRow)environment.getSource();
                    long startTime = environment.getArgument("startTime");
                    int timeRange = environment.getArgument("timeRange");
                    int maxDepartures = environment.getArgument("numberOfDepartures");
                    return departureRow.getStoptimes(index, startTime, timeRange, maxDepartures);
                })
                .build())
            .build();

        placeAtDistanceType = GraphQLObjectType.newObject()
            .name("placeAtDistance")
            .withInterface(nodeInterface)
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("id")
                .type(new GraphQLNonNull(Scalars.GraphQLID))
                .dataFetcher(environment -> {
                    Object place = ((PlaceAndDistance) environment.getSource()).place;
                    return relay.toGlobalId(placeAtDistanceType.getName(),
                        Integer.toString(((PlaceAndDistance) environment.getSource()).distance) + ";" +
                            placeInterface.getTypeResolver()
                                .getType(place)
                                .getFieldDefinition("id")
                                .getDataFetcher()
                                .get(new DataFetchingEnvironment(place, null, null,
                                    null, null, placeAtDistanceType, null))

                    );
                })
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("place")
                .type(placeInterface)
                .dataFetcher(environment -> ((GraphIndex.PlaceAndDistance) environment.getSource()).place)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("distance")
                .type(Scalars.GraphQLInt)
                .dataFetcher(environment -> ((GraphIndex.PlaceAndDistance) environment.getSource()).distance)
                .build())
            .build();

        stoptimesInPatternType = GraphQLObjectType.newObject()
            .name("StoptimesInPattern")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("pattern")
                .type(patternType)
                .dataFetcher(environment -> index.patternForId
                    .get(((StopTimesInPattern) environment.getSource()).pattern.id))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stoptimes")
                .type(new GraphQLList(stoptimeType))
                .dataFetcher(environment -> ((StopTimesInPattern) environment.getSource()).times)
                .build())
            .build();

        clusterType = GraphQLObjectType.newObject()
            .name("Cluster")
            .withInterface(nodeInterface)
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("id")
                .type(new GraphQLNonNull(Scalars.GraphQLID))
                .dataFetcher(environment -> relay
                    .toGlobalId(clusterType.getName(), ((StopCluster) environment.getSource()).id))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("gtfsId")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .dataFetcher(environment -> ((StopCluster) environment.getSource()).id)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("name")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .dataFetcher(environment -> ((StopCluster) environment.getSource()).name)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("lat")
                .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                .dataFetcher(environment -> (((StopCluster) environment.getSource()).lat))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("lon")
                .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                .dataFetcher(environment -> (((StopCluster) environment.getSource()).lon))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stops")
                .type(new GraphQLList(new GraphQLNonNull(stopType)))
                .dataFetcher(environment -> ((StopCluster) environment.getSource()).children)
                .build())
            .build();

        stopType = GraphQLObjectType.newObject()
            .name("Stop")
            .withInterface(nodeInterface)
            .withInterface(placeInterface)
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("id")
                .type(new GraphQLNonNull(Scalars.GraphQLID))
                .dataFetcher(environment -> relay.toGlobalId(
                    stopType.getName(),
                    GtfsLibrary.convertIdToString(((Stop) environment.getSource()).getId())))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stopTimesForPattern")
                .type(new GraphQLList(stoptimeType))
                .argument(GraphQLArgument.newArgument()
                    .name("id")
                    .type(Scalars.GraphQLString)
                    .defaultValue(null)
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("startTime")
                    .type(Scalars.GraphQLLong)
                    .defaultValue(0l) // Default value is current time
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("timeRange")
                    .type(Scalars.GraphQLInt)
                    .defaultValue(24 * 60 * 60)
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("numberOfDepartures")
                    .type(Scalars.GraphQLInt)
                    .defaultValue(2)
                    .build())
                .dataFetcher(environment ->
                    index.stopTimesForPattern((Stop) environment.getSource(),
                        index.patternForId.get(environment.getArgument("id")),
                        environment.getArgument("startTime"),
                        environment.getArgument("timeRange"),
                        environment.getArgument("numberOfDepartures")))
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
                .type(Scalars.GraphQLFloat)
                .dataFetcher(environment -> (((Stop) environment.getSource()).getLat()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("lon")
                .type(Scalars.GraphQLFloat)
                .dataFetcher(environment -> (((Stop) environment.getSource()).getLon()))
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
                .dataFetcher(environment -> ((Stop) environment.getSource()).getParentStation() != null ?
                    index.stationForId.get(new AgencyAndId(
                        ((Stop) environment.getSource()).getId().getAgencyId(),
                        ((Stop) environment.getSource()).getParentStation())) : null)
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
                .name("cluster")
                .type(clusterType)
                .dataFetcher(environment -> index.stopClusterForStop
                    .get((Stop) environment.getSource()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stops")
                .description("Returns all stops that are childen of this station (Only applicable for locationType = 1)")
                .type(new GraphQLList(stopType))
                .dataFetcher(environment -> index.stopsForParentStation.get(((Stop) environment.getSource()).getId()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("routes")
                .type(new GraphQLList(new GraphQLNonNull(routeType)))
                .dataFetcher(environment -> index.patternsForStop
                    .get((Stop) environment.getSource())
                    .stream()
                    .map(pattern -> pattern.route)
                    .distinct()
                    .collect(Collectors.toList()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("patterns")
                .type(new GraphQLList(patternType))
                .dataFetcher(environment -> index.patternsForStop
                    .get((Stop) environment.getSource()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("transfers")               //TODO: add max distance as parameter?
                .type(new GraphQLList(stopAtDistanceType))
                .dataFetcher(environment -> index.stopVertexForStop
                    .get(environment.getSource())
                    .getOutgoing()
                    .stream()
                    .filter(edge -> edge instanceof SimpleTransfer)
                    .map(edge -> new ImmutableMap.Builder<String, Object>()
                        .put("stop", ((TransitVertex) edge.getToVertex()).getStop())
                        .put("distance", edge.getDistance())
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
                    ServiceDate date;
                    try {  // TODO: Add our own scalar types for at least serviceDate and AgencyAndId
                        date = ServiceDate.parseString(environment.getArgument("date"));
                    } catch (ParseException e) {
                        return null;
                    }
                    Stop stop = (Stop) environment.getSource();
                    if (stop.getLocationType() == 1) {
                        // Merge all stops if this is a station
                        return index.stopsForParentStation
                            .get(stop.getId())
                            .stream()
                            .flatMap(singleStop -> index.getStopTimesForStop(singleStop, date).stream())
                            .collect(Collectors.toList());
                    }
                    return index.getStopTimesForStop(stop, date);
                })
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stoptimesForPatterns")
                .type(new GraphQLList(stoptimesInPatternType))
                .argument(GraphQLArgument.newArgument()
                    .name("startTime")
                    .type(Scalars.GraphQLLong)
                    .defaultValue(0L) // Default value is current time
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
                .dataFetcher(environment -> {
                    Stop stop = (Stop) environment.getSource();
                    if (stop.getLocationType() == 1) {
                        // Merge all stops if this is a station
                        return index.stopsForParentStation
                            .get(stop.getId())
                            .stream()
                            .flatMap(singleStop ->
                                index.stopTimesForStop(singleStop,
                                    environment.getArgument("startTime"),
                                    environment.getArgument("timeRange"),
                                    environment.getArgument("numberOfDepartures"))
                                .stream()
                            )
                            .collect(Collectors.toList());
                    }
                    return index.stopTimesForStop(stop,
                        environment.getArgument("startTime"),
                        environment.getArgument("timeRange"),
                        environment.getArgument("numberOfDepartures"));

                })
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stoptimesWithoutPatterns")
                .type(new GraphQLList(stoptimeType))
                .argument(GraphQLArgument.newArgument()
                    .name("startTime")
                    .type(Scalars.GraphQLLong)
                    .defaultValue(0L) // Default value is current time
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
                .dataFetcher(environment -> {
                    Stop stop = (Stop) environment.getSource();
                    Stream<StopTimesInPattern> stream;
                    if (stop.getLocationType() == 1) {
                        stream = index.stopsForParentStation
                            .get(stop.getId())
                            .stream()
                            .flatMap(singleStop ->
                                index.stopTimesForStop(singleStop,
                                    environment.getArgument("startTime"),
                                    environment.getArgument("timeRange"),
                                    environment.getArgument("numberOfDepartures"))
                                    .stream()
                            );
                    }
                    else {
                        stream = index.stopTimesForStop(
                            (Stop) environment.getSource(),
                            environment.getArgument("startTime"),
                            environment.getArgument("timeRange"),
                            environment.getArgument("numberOfDepartures")
                        ).stream();
                    }
                    return stream.flatMap(stoptimesWithPattern -> stoptimesWithPattern.times.stream())
                    .sorted(Comparator.comparing(t -> t.serviceDay + t.realtimeDeparture))
                    .limit((long) (int) environment.getArgument("numberOfDepartures"))
                    .collect(Collectors.toList());
                })
                .build())
            .build();

        stoptimeType = GraphQLObjectType.newObject()
            .name("Stoptime")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stop")
                .type(stopType)
                .dataFetcher(environment -> index.stopForId
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
                    environment -> ((TripTimeShort) environment.getSource()).realtimeDeparture)
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
                .name("pickupType")
                .type(pickupDropoffTypeEnum)
                .dataFetcher(environment -> index.patternForTrip
                    .get(index.tripForId.get(((TripTimeShort) environment.getSource()).tripId))
                    .getBoardType(((TripTimeShort) environment.getSource()).stopIndex))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("dropoffType")
                .type(pickupDropoffTypeEnum)
                .dataFetcher(environment -> index.patternForTrip
                    .get(index.tripForId.get(((TripTimeShort) environment.getSource()).tripId))
                    .getAlightType(((TripTimeShort) environment.getSource()).stopIndex))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("serviceDay")
                .type(Scalars.GraphQLLong)
                .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).serviceDay)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("trip")
                .type(tripType)
                .dataFetcher(environment -> index.tripForId
                    .get(((TripTimeShort) environment.getSource()).tripId))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stopHeadsign")
                .type(Scalars.GraphQLString)
                .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).headsign)
                .deprecate("Use headsign instead, will be removed in the future")
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("headsign")
              	.type(Scalars.GraphQLString)
              	.dataFetcher(environment -> ((TripTimeShort) environment.getSource()).headsign)
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
                .name("serviceId")
                .type(Scalars.GraphQLString) //TODO:Should be serviceType
                .dataFetcher(environment -> GtfsLibrary
                    .convertIdToString(((Trip) environment.getSource()).getServiceId()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("activeDates")
                .type(new GraphQLList(Scalars.GraphQLString))
                .dataFetcher(environment -> index.graph.getCalendarService()
                    .getServiceDatesForServiceId((((Trip) environment.getSource()).getServiceId()))
                    .stream()
                    .map(ServiceDate::getAsString)
                    .collect(Collectors.toList())
                )
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
                    environment -> index.patternForTrip.get((Trip) environment.getSource()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stops")
                .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(stopType))))
                .dataFetcher(environment -> index.patternForTrip
                    .get((Trip) environment.getSource()).getStops())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("semanticHash")
                .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(stopType))))
                .dataFetcher(environment -> index.patternForTrip.get((Trip) environment.getSource())
                    .semanticHashString((Trip) environment.getSource()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stoptimes")
                .type(new GraphQLList(stoptimeType))
                .dataFetcher(environment -> TripTimeShort.fromTripTimes(
                    index.patternForTrip.get((Trip) environment.getSource()).scheduledTimetable,
                    (Trip) environment.getSource()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stoptimesForDate")
                .type(new GraphQLList(stoptimeType))
                .argument(GraphQLArgument.newArgument()
                    .name("serviceDay")
                    .type(Scalars.GraphQLString)
                    .defaultValue(null)
                    .build())
                .dataFetcher(environment -> {
                    try {
                        final Trip trip = (Trip) environment.getSource();
                        final String argServiceDay = environment.getArgument("serviceDay");
                        final ServiceDate serviceDate = argServiceDay != null
                            ? ServiceDate.parseString(argServiceDay) : new ServiceDate();
                        final ServiceDay serviceDay = new ServiceDay(index.graph, serviceDate,
                            index.graph.getCalendarService(), trip.getRoute().getAgency().getId());
                        TimetableSnapshotSource timetableSnapshotSource = index.graph.timetableSnapshotSource;
                        Timetable timetable = null;
                        if (timetableSnapshotSource != null) {
                            TimetableSnapshot timetableSnapshot = timetableSnapshotSource.getTimetableSnapshot();
                            if (timetableSnapshot != null) {
                                timetable = timetableSnapshot.resolve(index.patternForTrip.get(trip), serviceDate);
                            }
                        }
                        if (timetable == null) {
                            timetable = index.patternForTrip.get(trip).scheduledTimetable;
                        }
                        return TripTimeShort.fromTripTimes(timetable, trip, serviceDay);
                    } catch (ParseException e) {
                        return null; // Invalid date format
                    }
                })
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("geometry")
                .type(new GraphQLList(new GraphQLList(Scalars.GraphQLFloat))) //TODO: Should be geometry
                .dataFetcher(environment -> {
                    LineString geometry = index.patternForTrip
                            .get((Trip) environment.getSource())
                            .geometry;
                    if (geometry == null) {return null;}
                    return Arrays.stream(geometry.getCoordinateSequence().toCoordinateArray())
                        .map(coordinate -> Arrays.asList(coordinate.x, coordinate.y))
                        .collect(Collectors.toList());
                    }
                )
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("alerts")
                .description("Get all alerts active for the trip")
                .type(new GraphQLList(alertType))
                .dataFetcher(dataFetchingEnvironment -> index.getAlertsForTrip((Trip) dataFetchingEnvironment.getSource()))
                .build())
            .build();

        coordinateType = GraphQLObjectType.newObject()
            .name("Coordinates")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("lat")
                .type(Scalars.GraphQLFloat)
                .dataFetcher(
                    environment -> ((Coordinate) environment.getSource()).y)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("lon")
                .type(Scalars.GraphQLFloat)
                .dataFetcher(
                    environment -> ((Coordinate) environment.getSource()).x)
                .build())
            .build();

        patternType = GraphQLObjectType.newObject()
            .name("Pattern")
            .withInterface(nodeInterface)
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("id")
                .type(new GraphQLNonNull(Scalars.GraphQLID))
                .dataFetcher(environment -> relay.toGlobalId(
                    patternType.getName(), ((TripPattern) environment.getSource()).code))
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
                .dataFetcher(environment -> ((TripPattern) environment.getSource()).code)
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
                .name("tripsForDate")
                .argument(GraphQLArgument.newArgument()
                    .name("serviceDay")
                    .type(Scalars.GraphQLString)
                    .build())
                .type(new GraphQLList(new GraphQLNonNull(tripType)))
                .dataFetcher(environment -> {
                    try {
                        BitSet services = index.servicesRunning(
                            ServiceDate.parseString(environment.getArgument("serviceDay"))
                        );
                        return ((TripPattern) environment.getSource()).scheduledTimetable.tripTimes
                            .stream()
                            .filter(times -> services.get(times.serviceCode))
                            .map(times -> times.trip)
                            .collect(Collectors.toList());
                    } catch (ParseException e) {
                        return null; // Invalid date format
                    }
                })
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stops")
                .type(new GraphQLList(new GraphQLNonNull(stopType)))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("geometry")
                .type(new GraphQLList(coordinateType))
                .dataFetcher(environment -> {
                    LineString geometry = ((TripPattern) environment.getSource()).geometry;
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
                .name("alerts")
                .description("Get all alerts active for the pattern")
                .type(new GraphQLList(alertType))
                .dataFetcher(dataFetchingEnvironment -> index.getAlertsForPattern((TripPattern) dataFetchingEnvironment.getSource()))
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
                .dataFetcher(environment -> GtfsLibrary.getTraverseMode(
                    (Route) environment.getSource())) //TODO: Remove the data fetcher to export proper type when upgrading to v2 of the HSL scheme
                .deprecate("The meaning of the type field will be changed to v2. Please use the mode-field instead. Type will export the raw type integer form the GTFS source.")
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
                .dataFetcher(environment -> index.patternsForRoute
                    .get((Route) environment.getSource()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stops")
                .type(new GraphQLList(stopType))
                .dataFetcher(environment -> index.patternsForRoute
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
                .dataFetcher(environment -> index.patternsForRoute
                    .get((Route) environment.getSource())
                    .stream()
                    .map(TripPattern::getTrips)
                    .flatMap(Collection::stream)
                    .distinct()
                    .collect(Collectors.toList()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("alerts")
                .description("Get all alerts active for the route")
                .type(new GraphQLList(alertType))
                .dataFetcher(dataFetchingEnvironment -> index.getAlertsForRoute((Route) dataFetchingEnvironment.getSource()))
                .build())
            .build();

        agencyType = GraphQLObjectType.newObject()
            .name("Agency")
            .description("Agency in the graph")
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
                .dataFetcher(environment -> index.routeForId.values()
                    .stream()
                    .filter(route -> route.getAgency() == environment.getSource())
                    .collect(Collectors.toList()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("alerts")
                .description("Get all alerts active for the agency")
                .type(new GraphQLList(alertType))
                .dataFetcher(dataFetchingEnvironment -> index.getAlertsForAgency((Agency) dataFetchingEnvironment.getSource()))
                .build())
            .build();

        bikeRentalStationType = GraphQLObjectType.newObject()
            .name("BikeRentalStation")
            .withInterface(nodeInterface)
            .withInterface(placeInterface)
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("id")
                .type(new GraphQLNonNull(Scalars.GraphQLID))
                .dataFetcher(environment -> relay
                    .toGlobalId(bikeRentalStationType.getName(), ((BikeRentalStation) environment.getSource()).id))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stationId")
                .type(Scalars.GraphQLString)
                .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).id)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("name")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).getName())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("bikesAvailable")
                .type(Scalars.GraphQLInt)
                .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).bikesAvailable)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("spacesAvailable")
                .type(Scalars.GraphQLInt)
                .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).spacesAvailable)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("realtime")
                .type(Scalars.GraphQLBoolean)
                .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).realTimeData)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("allowDropoff")
                .type(Scalars.GraphQLBoolean)
                .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).allowDropoff)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("networks")
                .type(new GraphQLList(Scalars.GraphQLString))
                .dataFetcher(environment -> new ArrayList<>(((BikeRentalStation) environment.getSource()).networks))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("lon")
                .type(Scalars.GraphQLFloat)
                .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).x)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("lat")
                .type(Scalars.GraphQLFloat)
                .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).y)
                .build())
            .build();

        bikeParkType = GraphQLObjectType.newObject()
            .name("BikePark")
            .withInterface(nodeInterface)
            .withInterface(placeInterface)
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("id")
                .type(new GraphQLNonNull(Scalars.GraphQLID))
                .dataFetcher(environment -> relay
                    .toGlobalId(bikeParkType.getName(), ((BikePark) environment.getSource()).id))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("bikeParkId")
                .type(Scalars.GraphQLString)
                .dataFetcher(environment -> ((BikePark) environment.getSource()).id)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("name")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .dataFetcher(environment -> ((BikePark) environment.getSource()).name)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("spacesAvailable")
                .type(Scalars.GraphQLInt)
                .dataFetcher(environment -> ((BikePark) environment.getSource()).spacesAvailable)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("realtime")
                .type(Scalars.GraphQLBoolean)
                .dataFetcher(environment -> ((BikePark) environment.getSource()).realTimeData)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("lon")
                .type(Scalars.GraphQLFloat)
                .dataFetcher(environment -> ((BikePark) environment.getSource()).x)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("lat")
                .type(Scalars.GraphQLFloat)
                .dataFetcher(environment -> ((BikePark) environment.getSource()).y)
                .build())
            .build();

        carParkType = GraphQLObjectType.newObject()
            .name("CarPark")
            .withInterface(nodeInterface)
            .withInterface(placeInterface)
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("id")
                .type(new GraphQLNonNull(Scalars.GraphQLID))
                .dataFetcher(environment -> relay
                    .toGlobalId(carParkType.getName(), ((CarPark) environment.getSource()).id))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("carParkId")
                .type(Scalars.GraphQLString)
                .dataFetcher(environment -> ((CarPark) environment.getSource()).id)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("name")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .dataFetcher(environment -> ((CarPark) environment.getSource()).name)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("maxCapacity")
                .type(Scalars.GraphQLInt)
                .dataFetcher(environment -> ((CarPark) environment.getSource()).maxCapacity)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("spacesAvailable")
                .type(Scalars.GraphQLInt)
                .dataFetcher(environment -> ((CarPark) environment.getSource()).spacesAvailable)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("realtime")
                .type(Scalars.GraphQLBoolean)
                .dataFetcher(environment -> ((CarPark) environment.getSource()).realTimeData)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("lon")
                .type(Scalars.GraphQLFloat)
                .dataFetcher(environment -> ((CarPark) environment.getSource()).x)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("lat")
                .type(Scalars.GraphQLFloat)
                .dataFetcher(environment -> ((CarPark) environment.getSource()).y)
                .build())
            .build();

        GraphQLInputObjectType filterInputType = GraphQLInputObjectType.newInputObject()
            .name("InputFilters")
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("stops")
                .description("Stops to include by GTFS id.")
                .type(new GraphQLList(Scalars.GraphQLString))
                .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("routes")
                .description("Routes to include by GTFS id.")
                .type(new GraphQLList(Scalars.GraphQLString))
                .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("bikeRentalStations")
                .description("Bike rentals to include by id.")
                .type(new GraphQLList(Scalars.GraphQLString))
                .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("bikeParks")
                .description("Bike parks to include by id.")
                .type(new GraphQLList(Scalars.GraphQLString))
                .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("carParks")
                .description("Car parks to include by id.")
                .type(new GraphQLList(Scalars.GraphQLString))
                .build())
            .build();

        DataFetcher nodeDataFetcher = new DataFetcher() {
            @Override public Object get(DataFetchingEnvironment environment) {
                return getObject(environment.getArgument("id"));
            }

            private Object getObject(String idString) {
                Relay.ResolvedGlobalId id = relay.fromGlobalId(idString);
                if (id.type.equals(clusterType.getName())) {
                    return index.stopClusterForId.get(id.id);
                }
                if (id.type.equals(stopAtDistanceType.getName())) {
                    String[] parts = id.id.split(";", 2);
                    return new GraphIndex.StopAndDistance(
                        index.stopForId.get(GtfsLibrary.convertIdFromString(parts[1])),
                        Integer.parseInt(parts[0], 10));
                }
                if (id.type.equals(stopType.getName())) {
                    return index.stopForId.get(GtfsLibrary.convertIdFromString(id.id));
                }
                if (id.type.equals(tripType.getName())) {
                    return index.tripForId.get(GtfsLibrary.convertIdFromString(id.id));
                }
                if (id.type.equals(routeType.getName())) {
                    return index.routeForId.get(GtfsLibrary.convertIdFromString(id.id));
                }
                if (id.type.equals(patternType.getName())) {
                    return index.patternForId.get(id.id);
                }
                if (id.type.equals(agencyType.getName())) {
                    return index.getAgencyWithoutFeedId(id.id);
                }
                if (id.type.equals(alertType.getName())) {
                    return index.getAlertForId(id.id);
                }
                if (id.type.equals(departureRowType.getName())) {
                    return GraphIndex.DepartureRow.fromId(index, id.id);
                }
                if (id.type.equals(bikeRentalStationType.getName())) {
                    // No index exists for bikeshare station ids
                    return index.graph.getService(BikeRentalStationService.class)
                        .getBikeRentalStations()
                        .stream()
                        .filter(bikeRentalStation -> bikeRentalStation.id.equals(id.id))
                        .findFirst()
                        .orElse(null);
                }
                if (id.type.equals(bikeParkType.getName())) {
                    // No index exists for bike parking ids
                    return index.graph.getService(BikeRentalStationService.class)
                        .getBikeParks()
                        .stream()
                        .filter(bikePark -> bikePark.id.equals(id.id))
                        .findFirst()
                        .orElse(null);
                }
                if (id.type.equals(carParkType.getName())) {
                    // No index exists for car parking ids
                    return index.graph.getService(CarParkService.class)
                        .getCarParks()
                        .stream()
                        .filter(carPark -> carPark.id.equals(id.id))
                        .findFirst()
                        .orElse(null);
                }
                if (id.type.equals(placeAtDistanceType.getName())) {
                    String[] parts = id.id.split(";", 2);
                    return new PlaceAndDistance(getObject(parts[1]), Integer.parseInt(parts[0], 10));
                }
                return null;
            }
        };

        queryType = GraphQLObjectType.newObject()
            .name("QueryType")
            .field(relay.nodeField(nodeInterface, nodeDataFetcher))
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("agencies")
                .description("Get all agencies for the specified graph")
                .type(new GraphQLList(agencyType))
                .dataFetcher(environment -> new ArrayList<>(index.getAllAgencies()))
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
                    index.getAgencyWithoutFeedId(environment.getArgument("id")))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stops")
                .description("Get all stops for the specified graph")
                .type(new GraphQLList(stopType))
                .argument(GraphQLArgument.newArgument()
                    .name("ids")
                    .type(new GraphQLList(Scalars.GraphQLString))
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("name")
                    .type(Scalars.GraphQLString)
                    .build())
                .dataFetcher(environment -> {
                    if ((environment.getArgument("ids") instanceof List)) {
                        if (environment.getArguments().entrySet()
                            .stream()
                            .filter(stringObjectEntry -> stringObjectEntry.getValue() != null)
                            .collect(Collectors.toList())
                            .size() != 1) {
                            throw new IllegalArgumentException("Unable to combine other filters with ids");
                        }
                        return ((List<String>) environment.getArgument("ids"))
                            .stream()
                            .map(id -> index.stopForId.get(GtfsLibrary.convertIdFromString(id)))
                            .collect(Collectors.toList());
                    }
                    Stream<Stop> stream;
                    if (environment.getArgument("name") == null) {
                        stream = index.stopForId.values().stream();
                    }
                    else {
                        stream = index.getLuceneIndex().query(environment.getArgument("name"), true, true, false, false)
                            .stream()
                            .map(result -> index.stopForId.get(GtfsLibrary.convertIdFromString(result.id)));
                    }
                    return stream.collect(Collectors.toList());
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
                .dataFetcher(environment -> index.graph.streetIndex
                    .getTransitStopForEnvelope(new Envelope(
                        new Coordinate(environment.getArgument("minLon"),
                            environment.getArgument("minLat")),
                        new Coordinate(environment.getArgument("maxLon"),
                            environment.getArgument("maxLat"))))
                    .stream()
                    .map(TransitVertex::getStop)
                    .filter(stop -> environment.getArgument("agency") == null || stop.getId()
                        .getAgencyId().equalsIgnoreCase(environment.getArgument("agency")))
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
                    .description("Radius (in meters) to search for from the specified location")
                    .type(Scalars.GraphQLInt)
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("agency")
                    .type(Scalars.GraphQLString)
                    .build())
                .argument(relay.getConnectionFieldArguments())
                .dataFetcher(environment -> {
                    List<GraphIndex.StopAndDistance> stops;
                    try {
                        stops = index.findClosestStopsByWalking(
                            environment.getArgument("lat"),
                            environment.getArgument("lon"),
                            environment.getArgument("radius"))
                            .stream()
                            .filter(stopAndDistance -> environment.getArgument("agency") == null ||
                                stopAndDistance.stop.getId().getAgencyId()
                                    .equalsIgnoreCase(environment.getArgument("agency")))
                            .sorted(Comparator.comparing(s -> s.distance))
                            .collect(Collectors.toList());
                    } catch (VertexNotFoundException e) {
                        stops = Collections.emptyList();
                    }

                    return new SimpleListConnection(stops).get(environment);
                })
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("nearest")
                .description(
                    "Get all places (stops, stations, etc. with coordinates) within the specified radius from a location. The returned type has two fields place and distance. The search is done by walking so the distance is according to the network of walkables.")
                .type(relay.connectionType("placeAtDistance",
                    relay.edgeType("placeAtDistance", placeAtDistanceType, null, new ArrayList<>()),
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
                    .name("maxDistance")
                    .description("Maximum distance (in meters) to search for from the specified location. Default is 2000m.")
                    .defaultValue(2000)
                    .type(Scalars.GraphQLInt)
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("maxResults")
                    .description("Maximum number of results. Search is stopped when this limit is reached. Default is 20.")
                    .defaultValue(20)
                    .type(Scalars.GraphQLInt)
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("filterByPlaceTypes")
                    .description("Only include places that imply this type. i.e. mode for stops, station etc. Also BICYCLE_RENT for bike rental stations.")
                    .type(new GraphQLList(filterPlaceTypeEnum))
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("filterByModes")
                    .description("Only include places that include this mode. Only checked for places with mode i.e. stops, departure rows.")
                    .type(new GraphQLList(modeEnum))
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("filterByIds")
                    .description("Only include places that match one of the given GTFS ids.")
                    .type(filterInputType)
                    .build())
                .argument(relay.getConnectionFieldArguments())
                .dataFetcher(environment -> {
                    List<AgencyAndId> filterByStops = null;
                    List<AgencyAndId> filterByRoutes = null;
                    List<String> filterByBikeRentalStations = null;
                    List<String> filterByBikeParks = null;
                    List<String> filterByCarParks = null;
                    @SuppressWarnings("rawtypes")
                    Map filterByIds = (Map)environment.getArgument("filterByIds");
                    if (filterByIds != null) {
                        filterByStops = toIdList(((List<String>) filterByIds.get("stops")));
                        filterByRoutes = toIdList(((List<String>) filterByIds.get("routes")));
                        filterByBikeRentalStations = filterByIds.get("bikeRentalStations") != null ? (List<String>) filterByIds.get("bikeRentalStations") : Collections.emptyList();
                        filterByBikeParks = filterByIds.get("bikeParks") != null ? (List<String>) filterByIds.get("bikeParks") : Collections.emptyList();
                        filterByCarParks = filterByIds.get("carParks") != null ? (List<String>) filterByIds.get("carParks") : Collections.emptyList();
                    }

                    List<TraverseMode> filterByModes = environment.getArgument("filterByModes");
                    List<GraphIndex.PlaceType> filterByPlaceTypes = environment.getArgument("filterByPlaceTypes");

                    List<GraphIndex.PlaceAndDistance> places;
                    try {
                        places = index.findClosestPlacesByWalking(
                            environment.getArgument("lat"),
                            environment.getArgument("lon"),
                            environment.getArgument("maxDistance"),
                            environment.getArgument("maxResults"),
                            filterByModes,
                            filterByPlaceTypes,
                            filterByStops,
                            filterByRoutes,
                            filterByBikeRentalStations,
                            filterByBikeParks,
                            filterByCarParks
                            )
                            .stream()
                            .collect(Collectors.toList());
                    } catch (VertexNotFoundException e) {
                        places = Collections.emptyList();
                    }

                    return new SimpleListConnection(places).get(environment);
                })
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("departureRow")
                .description("Get a single departure row based on its id (format is Agency:StopId:PatternId)")
                .type(departureRowType)
                .argument(GraphQLArgument.newArgument()
                    .name("id")
                    .type(new GraphQLNonNull(Scalars.GraphQLString))
                    .build())
                .dataFetcher(environment -> GraphIndex.DepartureRow.fromId(index, environment.getArgument("id")))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stop")
                .description("Get a single stop based on its id (format is Agency:StopId)")
                .type(stopType)
                .argument(GraphQLArgument.newArgument()
                    .name("id")
                    .type(new GraphQLNonNull(Scalars.GraphQLString))
                    .build())
                .dataFetcher(environment -> index.stopForId
                    .get(GtfsLibrary.convertIdFromString(environment.getArgument("id"))))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("station")
                .description("Get a single station (stop with location_type = 1) based on its id (format is Agency:StopId)")
                .type(stopType)
                .argument(GraphQLArgument.newArgument()
                    .name("id")
                    .type(new GraphQLNonNull(Scalars.GraphQLString))
                    .build())
                .dataFetcher(environment -> index.stationForId
                    .get(GtfsLibrary.convertIdFromString(environment.getArgument("id"))))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stations")
                .description("Get all stations (stop with location_type = 1)")
                .type(new GraphQLList(stopType))
                .dataFetcher(environment -> new ArrayList<>(index.stationForId.values()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("routes")
                .description("Get all routes for the specified graph")
                .type(new GraphQLList(routeType))
                .argument(GraphQLArgument.newArgument()
                    .name("ids")
                    .type(new GraphQLList(Scalars.GraphQLString))
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("name")
                    .type(Scalars.GraphQLString)
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("modes")
                    .type(Scalars.GraphQLString)
                    .build())
                .dataFetcher(environment -> {
                    if ((environment.getArgument("ids") instanceof List)) {
                        if (environment.getArguments().entrySet()
                            .stream()
                            .filter(stringObjectEntry -> stringObjectEntry.getValue() != null)
                            .collect(Collectors.toList())
                            .size() != 1) {
                            throw new IllegalArgumentException("Unable to combine other filters with ids");
                        }
                        return ((List<String>) environment.getArgument("ids"))
                            .stream()
                            .map(id -> index.routeForId.get(GtfsLibrary.convertIdFromString(id)))
                            .collect(Collectors.toList());
                    }
                    Stream<Route> stream = index.routeForId.values().stream();
                    if (environment.getArgument("name") != null) {
                        stream = stream
                            .filter(route -> route.getShortName() != null)
                            .filter(route -> route.getShortName().toLowerCase().startsWith(
                                    ((String) environment.getArgument("name")).toLowerCase())
                            );
                    }
                    if (environment.getArgument("modes") != null) {
                        Set<TraverseMode> modes = new QualifiedModeSet(
                            environment.getArgument("modes")).qModes
                            .stream()
                            .map(qualifiedMode -> qualifiedMode.mode)
                            .filter(TraverseMode::isTransit)
                            .collect(Collectors.toSet());
                        stream = stream
                            .filter(route ->
                                modes.contains(GtfsLibrary.getTraverseMode(route)));
                    }
                    return stream.collect(Collectors.toList());
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
                .dataFetcher(environment -> index.routeForId
                    .get(GtfsLibrary.convertIdFromString(environment.getArgument("id"))))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("trips")
                .description("Get all trips for the specified graph")
                .type(new GraphQLList(tripType))
                .dataFetcher(environment -> new ArrayList<>(index.tripForId.values()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("trip")
                .description("Get a single trip based on its id (format is Agency:TripId)")
                .type(tripType)
                .argument(GraphQLArgument.newArgument()
                    .name("id")
                    .type(new GraphQLNonNull(Scalars.GraphQLString))
                    .build())
                .dataFetcher(environment -> index.tripForId
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
                            index.routeForId.get(
                                GtfsLibrary.convertIdFromString(environment.getArgument("route"))),
                            environment.getArgument("direction"),
                            environment.getArgument("time"),
                            ServiceDate.parseString(((String) environment.getArgument("date")).replace("-", ""))
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
                .dataFetcher(environment -> new ArrayList<>(index.patternForId.values()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("pattern")
                .description("Get a single pattern based on its id")
                .type(patternType)
                .argument(GraphQLArgument.newArgument()
                    .name("id")
                    .type(new GraphQLNonNull(Scalars.GraphQLString))
                    .build())
                .dataFetcher(environment -> index.patternForId.get(environment.getArgument("id")))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("clusters")
                .description("Get all clusters for the specified graph")
                .type(new GraphQLList(clusterType))
                .dataFetcher(environment -> new ArrayList<>(index.stopClusterForId.values()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("cluster")
                .description("Get a single cluster based on its id")
                .type(clusterType)
                .argument(GraphQLArgument.newArgument()
                    .name("id")
                    .type(new GraphQLNonNull(Scalars.GraphQLString))
                    .build())
                .dataFetcher(
                    environment -> index.stopClusterForId.get(environment.getArgument("id")))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("alerts")
                .description("Get all alerts active in the graph")
                .type(new GraphQLList(alertType))
                .dataFetcher(dataFetchingEnvironment -> index.getAlerts())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("serviceTimeRange")
                .description("Get start and end time for publict transit services present in the graph")
                .type(serviceTimeRangeType)
                .dataFetcher(environment -> index.graph)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("bikeRentalStations")
                .type(new GraphQLList(bikeRentalStationType))
                .dataFetcher(dataFetchingEnvironment -> new ArrayList<>(index.graph.getService(BikeRentalStationService.class).getBikeRentalStations()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("bikeRentalStation")
                .type(bikeRentalStationType)
                .argument(GraphQLArgument.newArgument()
                    .name("id")
                    .type(new GraphQLNonNull(Scalars.GraphQLString))
                    .build())
                .dataFetcher(environment -> index.graph.getService(BikeRentalStationService.class)
                    .getBikeRentalStations()
                    .stream()
                    .filter(bikeRentalStation -> bikeRentalStation.id.equals(environment.getArgument("id")))
                    .findFirst()
                    .orElse(null))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("bikeParks")
                .type(new GraphQLList(bikeParkType))
                .dataFetcher(dataFetchingEnvironment -> new ArrayList<>(index.graph.getService(BikeRentalStationService.class).getBikeParks()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("bikePark")
                .type(bikeParkType)
                .argument(GraphQLArgument.newArgument()
                    .name("id")
                    .type(new GraphQLNonNull(Scalars.GraphQLString))
                    .build())
                .dataFetcher(environment -> index.graph.getService(BikeRentalStationService.class)
                    .getBikeParks()
                    .stream()
                    .filter(bikePark -> bikePark.id.equals(environment.getArgument("id")))
                    .findFirst()
                    .orElse(null))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("carParks")
                .type(new GraphQLList(carParkType))
                .argument(GraphQLArgument.newArgument()
                    .name("ids")
                    .type(new GraphQLList(Scalars.GraphQLString))
                    .build())
                .dataFetcher(environment -> {
                    if ((environment.getArgument("ids") instanceof List)) {
                        Map<String, CarPark> carParks = index.graph.getService(CarParkService.class).getCarParkById();
                        return ((List<String>) environment.getArgument("ids"))
                            .stream()
                            .map(carParks::get)
                            .collect(Collectors.toList());
                    }
                    return new ArrayList<>(index.graph.getService(CarParkService.class).getCarParks());
                })
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("carPark")
                .type(carParkType)
                .argument(GraphQLArgument.newArgument()
                    .name("id")
                    .type(new GraphQLNonNull(Scalars.GraphQLString))
                    .build())
                .dataFetcher(environment -> index.graph.getService(CarParkService.class)
                    .getCarParks()
                    .stream()
                    .filter(carPark -> carPark.id.equals(environment.getArgument("id")))
                    .findFirst()
                    .orElse(null))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("viewer")
                .description(
                    "Needed until https://github.com/facebook/relay/issues/112 is resolved")
                .type(new GraphQLTypeReference("QueryType"))
                .dataFetcher(DataFetchingEnvironment::getParentType)
                .build())
            .field(planFieldType)
            .build();

        Set<GraphQLType> dictionary = new HashSet<GraphQLType>();
        dictionary.add(placeInterface);

        indexSchema = GraphQLSchema.newSchema()
            .query(queryType)
            .build(dictionary);
    }

    private List<AgencyAndId> toIdList(List<String> ids) {
        if (ids == null) return Collections.emptyList();
        return ids.stream().map(GtfsLibrary::convertIdFromString).collect(Collectors.toList());
    }

    private void createPlanType(GraphIndex index) {
        final GraphQLObjectType legGeometryType = GraphQLObjectType.newObject()
            .name("LegGeometry")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("length")
                .description("The number of points in the string")
                .type(Scalars.GraphQLInt)
                .dataFetcher(environment -> ((EncodedPolylineBean)environment.getSource()).getLength())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("points")
                .description("The encoded points of the polyline.")
                .type(Scalars.GraphQLString)
                .dataFetcher(environment -> ((EncodedPolylineBean)environment.getSource()).getPoints())
                .build())
            .build();

        final GraphQLObjectType placeType = GraphQLObjectType.newObject()
            .name("Place")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("name")
                .description("For transit stops, the name of the stop. For points of interest, the name of the POI.")
                .type(Scalars.GraphQLString)
                .dataFetcher(environment -> ((Place)environment.getSource()).name)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("vertexType")
                .description("Type of vertex. (Normal, Bike sharing station, Bike P+R, Transit stop) Mostly used for better localization of bike sharing and P+R station names")
                .type(vertexTypeEnum)
                .dataFetcher(environment -> ((Place)environment.getSource()).vertexType)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("lat")
                .description("The latitude of the place.")
                .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                .dataFetcher(environment -> ((Place)environment.getSource()).lat)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("lon")
                .description("The longitude of the place.")
                .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                .dataFetcher(environment -> ((Place)environment.getSource()).lon)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stop")
                .description("The stop related to the place.")
                .type(stopType)
                .dataFetcher(environment -> ((Place) environment.getSource()).vertexType.equals(VertexType.TRANSIT) ? index.stopForId.get(((Place) environment.getSource()).stopId) : null)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("bikeRentalStation")
                .type(bikeRentalStationType)
                .description("The bike rental station related to the place")
                .dataFetcher(environment -> ((Place) environment.getSource()).vertexType.equals(VertexType.BIKESHARE) ?
                    index.graph.getService(BikeRentalStationService.class)
                        .getBikeRentalStations()
                        .stream()
                        .filter(bikeRentalStation -> bikeRentalStation.id.equals(((Place) environment.getSource()).bikeShareId))
                        .findFirst()
                        .orElse(null)
                : null)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("bikePark")
                .type(bikeParkType)
                .description("The bike parking related to the place")
                .dataFetcher(environment -> ((Place) environment.getSource()).vertexType.equals(VertexType.BIKEPARK) ?
                    index.graph.getService(BikeRentalStationService.class)
                        .getBikeParks()
                        .stream()
                        .filter(bikePark -> bikePark.id.equals(((Place) environment.getSource()).bikeParkId))
                        .findFirst()
                        .orElse(null)
                    : null)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("carPark")
                .type(carParkType)
                .description("The car parking related to the place")
                .dataFetcher(environment -> ((Place) environment.getSource()).vertexType.equals(VertexType.PARKANDRIDE) ?
                    index.graph.getService(CarParkService.class)
                        .getCarParks()
                        .stream()
                        .filter(carPark -> carPark.id.equals(((Place) environment.getSource()).carParkId))
                        .findFirst()
                        .orElse(null)
                    : null)
                .build())
            .build();

        final GraphQLObjectType legType = GraphQLObjectType.newObject()
            .name("Leg")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("startTime")
                .description("The date and time this leg begins.")
                .type(Scalars.GraphQLLong)
                .dataFetcher(environment -> ((Leg)environment.getSource()).startTime.getTime().getTime())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("endTime")
                .description("The date and time this leg ends.")
                .type(Scalars.GraphQLLong)
                .dataFetcher(environment -> ((Leg)environment.getSource()).endTime.getTime().getTime())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("mode")
                .description("The mode (e.g., Walk) used when traversing this leg.")
                .type(modeEnum)
                .dataFetcher(environment -> Enum.valueOf(TraverseMode.class, ((Leg)environment.getSource()).mode))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("duration")
                .description("The leg's duration in seconds")
                .type(Scalars.GraphQLFloat)
                .dataFetcher(environment -> ((Leg)environment.getSource()).getDuration())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("legGeometry")
                .description("The leg's geometry.")
                .type(legGeometryType)
                .dataFetcher(environment -> ((Leg)environment.getSource()).legGeometry)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("agency")
                .description("For transit legs, the transit agency that operates the service used for this leg. For non-transit legs, null.")
                .type(agencyType)
                .dataFetcher(environment -> getAgency(index, ((Leg)environment.getSource()).agencyId))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("realTime")
                .description("Whether there is real-time data about this Leg")
                .type(Scalars.GraphQLBoolean)
                .dataFetcher(environment -> ((Leg)environment.getSource()).realTime)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("distance")
                .description("The distance traveled while traversing the leg in meters.")
                .type(Scalars.GraphQLFloat)
                .dataFetcher(environment -> ((Leg)environment.getSource()).distance)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("transitLeg")
                .description("Whether this leg is a transit leg or not.")
                .type(Scalars.GraphQLBoolean)
                .dataFetcher(environment -> ((Leg)environment.getSource()).isTransitLeg())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("rentedBike")
                .description("Whether this leg is with a rented bike.")
                .type(Scalars.GraphQLBoolean)
                .dataFetcher(environment -> ((Leg)environment.getSource()).rentedBike)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("from")
                .description("The Place where the leg originates.")
                .type(new GraphQLNonNull(placeType))
                .dataFetcher(environment -> ((Leg)environment.getSource()).from)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("to")
                .description("The Place where the leg ends.")
                .type(new GraphQLNonNull(placeType))
                .dataFetcher(environment -> ((Leg)environment.getSource()).to)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("route")
                .description("For transit legs, the route. For non-transit legs, null.")
                .type(routeType)
                .dataFetcher(environment -> index.routeForId.get(((Leg)environment.getSource()).routeId))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("trip")
                .description("For transit legs, the trip. For non-transit legs, null.")
                .type(tripType)
                .dataFetcher(environment -> index.tripForId.get(((Leg)environment.getSource()).tripId))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("intermediateStops")
                .description("For transit legs, intermediate stops between the Place where the leg originates and the Place where the leg ends. For non-transit legs, null.")
                .type(new GraphQLList(stopType))
                .dataFetcher(environment -> {
                    return ((Leg)environment.getSource()).stop.stream()
                        .filter(place -> place.stopId != null)
                        .map(placeWithStop -> index.stopForId.get(placeWithStop.stopId))
                        .filter(stop -> stop != null)
                        .collect(Collectors.toList());
                })
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("intermediatePlace")
                .description("Do we continue from a specified intermediate place")
                .type(Scalars.GraphQLBoolean)
                .dataFetcher(environment -> ((Leg) environment.getSource()).intermediatePlace)
                .build())
            .build();

        GraphQLObjectType fareType = GraphQLObjectType.newObject()
            .name("fare")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("type")
                .type(Scalars.GraphQLString)
                .dataFetcher(new PropertyDataFetcher("name"))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("currency")
                .type(Scalars.GraphQLString)
                .dataFetcher(environment -> ((Money)((Map<String, Object>) environment.getSource()).get("fare")).getCurrency().getCurrencyCode())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("cents")
                .type(Scalars.GraphQLInt)
                .dataFetcher(environment -> ((Money)((Map<String, Object>) environment.getSource()).get("fare")).getCents())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("components")
                .type(new GraphQLList(GraphQLObjectType.newObject()
                    .name("fareComponent")
                    .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("fareId")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(environment -> GtfsLibrary
                            .convertIdToString(((FareComponent) environment.getSource()).fareId))
                        .build())
                    .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("currency")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(environment -> ((FareComponent) environment.getSource()).price.getCurrency().getCurrencyCode())
                        .build())
                    .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("cents")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(environment -> ((FareComponent) environment.getSource()).price.getCents())
                        .build())
                    .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("routes")
                        .type(new GraphQLList(routeType))
                        .dataFetcher(environment -> ((FareComponent) environment.getSource())
                            .routes
                            .stream()
                            .map(index.routeForId::get)
                            .collect(Collectors.toList()))
                        .build())
                    .build()))
                .dataFetcher(new PropertyDataFetcher("details"))
                .build())
            .build();

        final GraphQLObjectType itineraryType = GraphQLObjectType.newObject()
            .name("Itinerary")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("startTime")
                .description("Time that the trip departs.")
                .type(Scalars.GraphQLLong)
                .dataFetcher(environment -> ((Itinerary)environment.getSource()).startTime.getTime().getTime())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("endTime")
                .description("Time that the trip arrives.")
                .type(Scalars.GraphQLLong)
                .dataFetcher(environment -> ((Itinerary)environment.getSource()).endTime.getTime().getTime())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("duration")
                .description("Duration of the trip on this itinerary, in seconds.")
                .type(Scalars.GraphQLLong)
                .dataFetcher(environment -> ((Itinerary)environment.getSource()).duration)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("waitingTime")
                .description("How much time is spent waiting for transit to arrive, in seconds.")
                .type(Scalars.GraphQLLong)
                .dataFetcher(environment -> ((Itinerary)environment.getSource()).waitingTime)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("walkTime")
                .description("How much time is spent walking, in seconds.")
                .type(Scalars.GraphQLLong)
                .dataFetcher(environment -> ((Itinerary)environment.getSource()).walkTime)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("walkDistance")
                .description("How far the user has to walk, in meters.")
                .type(Scalars.GraphQLFloat)
                .dataFetcher(environment -> ((Itinerary)environment.getSource()).walkDistance)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("legs")
                .description("A list of Legs. Each Leg is either a walking (cycling, car) portion of the trip, or a transit trip on a particular vehicle. So a trip where the use walks to the Q train, transfers to the 6, then walks to their destination, has four legs.")
                .type(new GraphQLNonNull(new GraphQLList(legType)))
                .dataFetcher(environment -> ((Itinerary)environment.getSource()).legs)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("fares")
                .description("Information about the fares for this itinerary")
                .type(new GraphQLList(fareType))
                .dataFetcher(environment -> {
                    Fare fare = ((Itinerary)environment.getSource()).fare;
                    if (fare == null) {
                        return null;
                    }
                    List<Map<String, Object>> results = fare.fare.keySet().stream().map(fareKey -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("name", fareKey);
                        result.put("fare", fare.getFare(fareKey));
                        result.put("details", fare.getDetails(fareKey));
                        return result;
                    }).collect(Collectors.toList());
                    return results;
                })
                .build())
            .build();

        planType = GraphQLObjectType.newObject()
            .name("Plan")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("date")
                .description("The time and date of travel")
                .type(Scalars.GraphQLLong)
                .dataFetcher(environment -> ((TripPlan) ((Map)environment.getSource()).get("plan")).date.getTime())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("from")
                .description("The origin")
                .type(new GraphQLNonNull(placeType))
                .dataFetcher(environment -> ((TripPlan) ((Map)environment.getSource()).get("plan")).from)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("to")
                .description("The destination")
                .type(new GraphQLNonNull(placeType))
                .dataFetcher(environment -> ((TripPlan) ((Map)environment.getSource()).get("plan")).to)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("itineraries")
                .description("A list of possible itineraries")
                .type(new GraphQLNonNull(new GraphQLList(itineraryType)))
                .dataFetcher(environment -> ((TripPlan) ((Map)environment.getSource()).get("plan")).itinerary)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("messageEnums")
                .description("A list of possible error messages as enum")
                .type(new GraphQLNonNull(new GraphQLList(Scalars.GraphQLString)))
                .dataFetcher(environment -> ((List<Message>)((Map)environment.getSource()).get("messages"))
                    .stream().map(message -> message.name()).collect(Collectors.toList()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("messageStrings")
                .description("A list of possible error messages in cleartext")
                .type(new GraphQLNonNull(new GraphQLList(Scalars.GraphQLString)))
                .dataFetcher(environment -> ((List<Message>)((Map)environment.getSource()).get("messages"))
                    .stream()
                    .map(message -> message.get(ResourceBundleSingleton.INSTANCE.getLocale(
                        environment.getArgument("locale"))))
                    .collect(Collectors.toList())
                )
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("debugOutput")
                .description("Information about the timings for the plan generation")
                .type(new GraphQLNonNull(GraphQLObjectType.newObject()
                    .name("debugOutput")
                    .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("totalTime")
                        .type(Scalars.GraphQLLong)
                        .build())
                    .build()))
                .dataFetcher(environment -> (((Map)environment.getSource()).get("debugOutput")))
                .build())
            .build();
    }
}
