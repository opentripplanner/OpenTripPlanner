package org.opentripplanner.index;
import static java.util.Collections.emptyList;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import graphql.language.StringValue;
import graphql.schema.*;
import org.opentripplanner.model.*;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.api.common.Message;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.api.model.Place;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.model.VertexType;
import org.opentripplanner.api.model.WalkStep;
import org.opentripplanner.api.parameter.QualifiedMode;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.common.model.P2;
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
import org.opentripplanner.routing.core.TicketType;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.SimpleTransfer;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.edgetype.TimetableSnapshot;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.graph.GraphIndex.PlaceAndDistance;
import org.opentripplanner.routing.trippattern.RealTimeState;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.TransitVertex;
import org.opentripplanner.updater.GtfsRealtimeFuzzyTripMatcher;
import org.opentripplanner.updater.stoptime.TimetableSnapshotSource;
import org.opentripplanner.util.PolylineEncoder;
import org.opentripplanner.util.ResourceBundleSingleton;
import org.opentripplanner.util.TranslatedString;
import org.opentripplanner.util.model.EncodedPolylineBean;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.LineString;

import graphql.Scalars;
import graphql.relay.Relay;
import graphql.relay.SimpleListConnection;

public class IndexGraphQLSchema {

    public static String experimental(String message) {
        return String.format("**This API is experimental and might change without further notice**  \n %s", message);
    }

    public static GraphQLScalarType polylineScalar = new GraphQLScalarType("Polyline", "List of coordinates in an encoded polyline format (see https://developers.google.com/maps/documentation/utilities/polylinealgorithm). The value appears in JSON as a string.", new Coercing<String, String>() {
        @Override
        public String serialize(Object input) {
            return input == null ? null : input.toString();
        }

        @Override
        public String parseValue(Object input) {
            return serialize(input);
        }

        @Override
        public String parseLiteral(Object input) {
            if (!(input instanceof StringValue)) return null;
            return ((StringValue) input).getValue();
        }
    });

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
        .value("LEG_SWITCH", TraverseMode.LEG_SWITCH, "Only used internally. No use for API users.")
        .value("RAIL", TraverseMode.RAIL, "RAIL")
        .value("SUBWAY", TraverseMode.SUBWAY, "SUBWAY")
        .value("TRAM", TraverseMode.TRAM, "TRAM")
        .value("TRANSIT", TraverseMode.TRANSIT, "A special transport mode, which includes all public transport.")
        .value("WALK", TraverseMode.WALK, "WALK")
        .build();

    public static GraphQLEnumType qualifierEnum = GraphQLEnumType.newEnum()
        .name("Qualifier")
        .description("Additional qualifier for a transport mode.  \n Note that qualifiers can only be used with certain transport modes.")
        .value("RENT", QualifiedMode.Qualifier.RENT, "The vehicle used for transport can be rented")
        .value("HAVE", QualifiedMode.Qualifier.HAVE, "~~HAVE~~  \n **Currently not used**")
        .value("PARK", QualifiedMode.Qualifier.PARK, "The vehicle used must be left to a parking area before continuing the journey. This qualifier is usable with transport modes `CAR` and `BICYCLE`.  \n Note that the vehicle is only parked if the journey is continued with public transportation (e.g. if only `CAR` and `WALK` transport modes are allowed to be used, the car will not be parked as it is used for the whole journey).")
        .value("KEEP", QualifiedMode.Qualifier.KEEP, "~~KEEP~~  \n **Currently not used**")
        .value("PICKUP", QualifiedMode.Qualifier.PICKUP, "The user can be picked up by someone else riding a vehicle")
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
        .description("Optimization type for bicycling legs")
        .value("QUICK", OptimizeType.QUICK, "Prefer faster routes")
        .value("SAFE", OptimizeType.SAFE, "Prefer safer routes, i.e. avoid crossing streets and use bike paths when possible")
        .value("FLAT", OptimizeType.FLAT, "Prefer flat terrain")
        .value("GREENWAYS", OptimizeType.GREENWAYS, "GREENWAYS")
        .value("TRIANGLE", OptimizeType.TRIANGLE, "**TRIANGLE** optimization type can be used to set relative preferences of optimization factors. See argument `triangle`.")
        .value("TRANSFERS", OptimizeType.TRANSFERS, "Deprecated, use argument `transferPenalty` to optimize for less transfers.")
        .build();

    private final GtfsRealtimeFuzzyTripMatcher fuzzyTripMatcher;

    public GraphQLOutputType feedType = new GraphQLTypeReference("Feed");

    public GraphQLOutputType agencyType = new GraphQLTypeReference("Agency");

    public GraphQLOutputType ticketType = new GraphQLTypeReference("TicketType");

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
        .description("Interface for places, e.g. stops, stations, parking areas..")
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("id")
            .type(new GraphQLNonNull(Scalars.GraphQLID))
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("lat")
            .description("Latitude of the place (WGS 84)")
            .type(Scalars.GraphQLFloat)
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("lon")
            .description("Longitude of the place (WGS 84)")
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


    private final GraphQLObjectType geometryType = GraphQLObjectType.newObject()
            .name("Geometry")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("length")
                    .description("The number of points in the string")
                    .type(Scalars.GraphQLInt)
                    .dataFetcher(environment -> ((EncodedPolylineBean)environment.getSource()).getLength())
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("points")
                    .description("List of coordinates of in a Google encoded polyline format (see https://developers.google.com/maps/documentation/utilities/polylinealgorithm)")
                    .type(polylineScalar)
                    .dataFetcher(environment -> ((EncodedPolylineBean)environment.getSource()).getPoints())
                    .build())
            .build();


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

    @SuppressWarnings("unchecked")
    public IndexGraphQLSchema(GraphIndex index) {
        createPlanType(index);

        GraphQLInputObjectType coordinateInputType = GraphQLInputObjectType.newInputObject()
            .name("InputCoordinates")
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("lat")
                .description("Latitude of the place (WGS 84)")
                .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("lon")
                .description("Longitude of the place (WGS 84)")
                .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("address")
                .description("The name of the place. If specified, the place name in results uses this value instead of `\"Origin\"` or `\"Destination\"`")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("locationSlack")
                .description("The amount of time, in seconds, to spend at this location before venturing forth.")
                .type(Scalars.GraphQLInt)
                .build())
            .build();

        GraphQLInputObjectType preferredInputType = GraphQLInputObjectType.newInputObject()
            .name("InputPreferred")
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("routes")
                .description("A comma-separated list of ids of the routes preferred by the user.")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("agencies")
                .description("A comma-separated list of ids of the agencies preferred by the user.")
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
                .description("A comma-separated list of ids of the routes unpreferred by the user.")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("agencies")
                .description("A comma-separated list of ids of the agencies unpreferred by the user.")
                .type(Scalars.GraphQLString)
                .build())
            .build();

        GraphQLInputObjectType bannedInputType = GraphQLInputObjectType.newInputObject()
            .name("InputBanned")
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("routes")
                .description("A comma-separated list of banned route ids")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("agencies")
                .description("A comma-separated list of banned agency ids")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("trips")
                .description("A comma-separated list of banned trip ids")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("stops")
                .description("A comma-separated list of banned stop ids. Note that these stops are only banned for boarding and disembarking vehicles — it is possible to get an itinerary where a vehicle stops at one of these stops")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("stopsHard")
                .description("A comma-separated list of banned stop ids. Only itineraries where these stops are not travelled through are returned, e.g. if a bus route stops at one of these stops, that route will not be used in the itinerary, even if the stop is not used for boarding or disembarking the vehicle. ")
                .type(Scalars.GraphQLString)
                .build())
            .build();

        GraphQLInputObjectType triangleInputType = GraphQLInputObjectType.newInputObject()
            .name("InputTriangle")
            .description("Relative importances of optimization factors. Only effective for bicycling legs.  \n Invariant: `timeFactor + slopeFactor + safetyFactor == 1`")
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("safetyFactor")
                .description("Relative importance of safety")
                .type(Scalars.GraphQLFloat)
                .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("slopeFactor")
                .description("Relative importance of flat terrain")
                .type(Scalars.GraphQLFloat)
                .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("timeFactor")
                .description("Relative importance of duration")
                .type(Scalars.GraphQLFloat)
                .build())
            .build();

        GraphQLInputObjectType modeWeightInputType = GraphQLInputObjectType.newInputObject()
            .name("InputModeWeight")
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("TRAM")
                .description("The weight of TRAM traverse mode. Values over 1 add cost to tram travel and values under 1 decrease cost")
                .type(Scalars.GraphQLFloat)
                .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("SUBWAY")
                .description("The weight of SUBWAY traverse mode. Values over 1 add cost to subway travel and values under 1 decrease cost")
                .type(Scalars.GraphQLFloat)
                .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("RAIL")
                .description("The weight of RAIL traverse mode. Values over 1 add cost to rail travel and values under 1 decrease cost")
                .type(Scalars.GraphQLFloat)
                .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("BUS")
                .description("The weight of BUS traverse mode. Values over 1 add cost to bus travel and values under 1 decrease cost")
                .type(Scalars.GraphQLFloat)
                .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("FERRY")
                .description("The weight of FERRY traverse mode. Values over 1 add cost to ferry travel and values under 1 decrease cost")
                .type(Scalars.GraphQLFloat)
                .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("CABLE_CAR")
                .description("The weight of CABLE_CAR traverse mode. Values over 1 add cost to cable car travel and values under 1 decrease cost")
                .type(Scalars.GraphQLFloat)
                .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("GONDOLA")
                .description("The weight of GONDOLA traverse mode. Values over 1 add cost to gondola travel and values under 1 decrease cost")
                .type(Scalars.GraphQLFloat)
                .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("FUNICULAR")
                .description("The weight of FUNICULAR traverse mode. Values over 1 add cost to funicular travel and values under 1 decrease cost")
                .type(Scalars.GraphQLFloat)
                .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("AIRPLANE")
                .description("The weight of AIRPLANE traverse mode. Values over 1 add cost to airplane travel and values under 1 decrease cost")
                .type(Scalars.GraphQLFloat)
                .build())
            .build();

        GraphQLInputObjectType transportModeInputType = GraphQLInputObjectType.newInputObject()
            .name("TransportMode")
            .description("Transportation mode which can be used in the itinerary")
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("mode")
                .type(new GraphQLNonNull(modeEnum))
                .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("qualifier")
                .description("Optional additional qualifier for transport mode, e.g. `RENT`")
                .type(qualifierEnum)
                .build())
            .build();

        GraphQLFieldDefinition planFieldType = GraphQLFieldDefinition.newFieldDefinition()
            .name("plan")
            .description("Plans an itinerary from point A to point B based on the given arguments")
            .type(planType)
            .argument(GraphQLArgument.newArgument()
                .name("date")
                .description("Date of departure or arrival in format YYYY-MM-DD. Default value: current date")
                .type(Scalars.GraphQLString)
		.build())
            .argument(GraphQLArgument.newArgument()
                .name("time")
		.description("Time of departure or arrival in format hh:mm:ss. Default value: current time")
                .type(Scalars.GraphQLString)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("from")
                .description("The geographical location where the itinerary begins.  \n Use either this argument or `fromPlace`, but not both.")
                .type(coordinateInputType)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("to")
                .description("The geographical location where the itinerary ends.  \n Use either this argument or `toPlace`, but not both.")
                .type(coordinateInputType)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("fromPlace")
                .description("The place where the itinerary begins in format `name::place`, where `place` is either a lat,lng pair (e.g. `Pasila::60.199041,24.932928`) or a stop id (e.g. `Pasila::HSL:1000202`).  \n Use either this argument or `from`, but not both.")
                .type(Scalars.GraphQLString)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("toPlace")
                .description("The place where the itinerary ends in format `name::place`, where `place` is either a lat,lng pair (e.g. `Pasila::60.199041,24.932928`) or a stop id (e.g. `Pasila::HSL:1000202`).  \n Use either this argument or `to`, but not both.")
                .type(Scalars.GraphQLString)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("wheelchair")
                .description("Whether the itinerary must be wheelchair accessible. Default value: false")
                .type(Scalars.GraphQLBoolean)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("numItineraries")
                .description("The maximum number of itineraries to return. Default value: 3.")
                .defaultValue(3)
                .type(Scalars.GraphQLInt)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("maxWalkDistance")
                .description("The maximum distance (in meters) the user is willing to walk per walking section. If the only transport mode allowed is `WALK`, then the value of this argument is ignored.  \n Default: 2000m  \n Maximum value: 15000m  \n **Note:** If this argument has a relatively small value and only some transport modes are allowed (e.g. `WALK` and `RAIL`), it is possible to get an itinerary which has (useless) back and forth public transport legs to avoid walking too long distances.")
                .type(Scalars.GraphQLFloat)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("maxPreTransitTime")
                .description("The maximum time (in seconds) of pre-transit travel when using drive-to-transit (park and ride or kiss and ride). Default value: 1800.")
                .type(Scalars.GraphQLInt)
                .build())
            .argument(GraphQLArgument.newArgument()
                    .name("carParkCarLegWeight")
                    .description("How expensive it is to drive a car when car&parking, increase this value to make car driving legs shorter. Default value: 1.")
                    .type(Scalars.GraphQLFloat)
                    .build())
            .argument(GraphQLArgument.newArgument()
                    .name("itineraryFiltering")
                    .description("How easily bad itineraries are filtered from results. Value 0 (default) disables filtering. Itineraries are filtered if they are worse than another one in some respect (e.g. more walking) by more than the percentage of filtering level, which is calculated by dividing 100% by the value of this argument (e.g. `itineraryFiltering = 0.5` → 200% worse itineraries are filtered).")
                    .type(Scalars.GraphQLFloat)
                    .build())
            .argument(GraphQLArgument.newArgument()
                .name("walkReluctance")
                .description("A multiplier for how bad walking is, compared to being in transit for equal lengths of time.Empirically, values between 10 and 20 seem to correspond well to the concept of not wanting to walk too much without asking for totally ridiculous itineraries, but this observation should in no way be taken as scientific or definitive. Your mileage may vary. Default value: 2.0 ")
                .type(Scalars.GraphQLFloat)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("walkOnStreetReluctance")
                .description("How much more reluctant is the user to walk on streets with car traffic allowed. Default value: 1.0")
                .type(Scalars.GraphQLFloat)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("waitReluctance")
                .description("How much worse is waiting for a transit vehicle than being on a transit vehicle, as a multiplier. The default value treats wait and on-vehicle time as the same. It may be tempting to set this higher than walkReluctance (as studies often find this kind of preferences among riders) but the planner will take this literally and walk down a transit line to avoid waiting at a stop. This used to be set less than 1 (0.95) which would make waiting offboard preferable to waiting onboard in an interlined trip. That is also undesirable. If we only tried the shortest possible transfer at each stop to neighboring stop patterns, this problem could disappear. Default value: 1.0.")
                .type(Scalars.GraphQLFloat)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("waitAtBeginningFactor")
                .description("How much less bad is waiting at the beginning of the trip (replaces `waitReluctance` on the first boarding). Default value: 0.4")
                .type(Scalars.GraphQLFloat)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("walkSpeed")
                .description("Max walk speed along streets, in meters per second. Default value: 1.33")
                .type(Scalars.GraphQLFloat)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("bikeSpeed")
                .description("Max bike speed along streets, in meters per second. Default value: 5.0")
                .type(Scalars.GraphQLFloat)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("bikeSwitchTime")
                .description("Time to get on and off your own bike, in seconds. Default value: 0")
                .type(Scalars.GraphQLInt)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("bikeSwitchCost")
                .description("Cost of getting on and off your own bike. Unit: seconds. Default value: 0")
                .type(Scalars.GraphQLInt)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("optimize")
                .description("Optimization type for bicycling legs, e.g. prefer flat terrain. Default value: `QUICK`")
                .type(optimizeTypeEnum)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("triangle")
                .description("Triangle optimization parameters for bicycling legs. Only effective when `optimize` is set to **TRIANGLE**.")
                .type(triangleInputType)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("arriveBy")
                .description("Whether the itinerary should depart at the specified time (false), or arrive to the destination at the specified time (true). Default value: false.")
                .type(Scalars.GraphQLBoolean)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("intermediatePlaces")
                .description("An ordered list of intermediate locations to be visited.")
                .type(new GraphQLList(coordinateInputType))
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("preferred")
                .description("List of routes and agencies which are given higher preference when planning the itinerary")
                .type(preferredInputType)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("unpreferred")
                .description("List of routes and agencies which are given lower preference when planning the itinerary")
                .type(unpreferredInputType)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("walkBoardCost")
                .description("This prevents unnecessary transfers by adding a cost for boarding a vehicle. Unit: seconds. Default value: 600")
                .type(Scalars.GraphQLInt)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("bikeBoardCost")
                .description("Separate cost for boarding a vehicle with a bicycle, which is more difficult than on foot. Unit: seconds. Default value: 600")
                .type(Scalars.GraphQLInt)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("banned")
                .description("List of routes, trips, agencies and stops which are not used in the itinerary")
                .type(bannedInputType)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("transferPenalty")
                .description("An extra penalty added on transfers (i.e. all boardings except the first one). Not to be confused with bikeBoardCost and walkBoardCost, which are the cost of boarding a vehicle with and without a bicycle. The boardCosts are used to model the 'usual' perceived cost of using a transit vehicle, and the transferPenalty is used when a user requests even less transfers. In the latter case, we don't actually optimize for fewest transfers, as this can lead to absurd results. Consider a trip in New York from Grand Army Plaza (the one in Brooklyn) to Kalustyan's at noon. The true lowest transfers route is to wait until midnight, when the 4 train runs local the whole way. The actual fastest route is the 2/3 to the 4/5 at Nevins to the 6 at Union Square, which takes half an hour. Even someone optimizing for fewest transfers doesn't want to wait until midnight. Maybe they would be willing to walk to 7th Ave and take the Q to Union Square, then transfer to the 6. If this takes less than optimize_transfer_penalty seconds, then that's what we'll return. Default value: 0.")
                .type(Scalars.GraphQLInt)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("batch")
                .description("This argument has no use for itinerary planning and will be removed later.  \n ~~When true, do not use goal direction or stop at the target, build a full SPT. Default value: false.~~")
                .type(Scalars.GraphQLBoolean)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("modes")
                .description("Deprecated, use `transportModes` instead.  \n ~~The set of TraverseModes that a user is willing to use. Default value: WALK | TRANSIT.~~")
                .type(Scalars.GraphQLString)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("transportModes")
                .description("List of transportation modes that the user is willing to use. Default: `[\"WALK\",\"TRANSIT\"]`")
                .type(new GraphQLList(transportModeInputType))
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("modeWeight")
                .description("The weight multipliers for transit modes. WALK, BICYCLE, CAR, TRANSIT and LEG_SWITCH are not included.")
                .type(modeWeightInputType)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("allowBikeRental")
                .description("Is bike rental allowed? Default value: false")
                .type(Scalars.GraphQLBoolean)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("boardSlack")
                .description("Invariant: `boardSlack + alightSlack <= transferSlack`. Default value: 0")
                .type(Scalars.GraphQLInt)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("alightSlack")
                .description("Invariant: `boardSlack + alightSlack <= transferSlack`. Default value: 0")
                .type(Scalars.GraphQLInt)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("minTransferTime")
                .description("A global minimum transfer time (in seconds) that specifies the minimum amount of time that must pass between exiting one transit vehicle and boarding another. This time is in addition to time it might take to walk between transit stops. Default value: 0")
                .type(Scalars.GraphQLInt)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("nonpreferredTransferPenalty")
                .description("Penalty (in seconds) for using a non-preferred transfer. Default value: 180")
                .type(Scalars.GraphQLInt)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("maxTransfers")
                .description("Maximum number of transfers. Default value: 2")
                .type(Scalars.GraphQLInt)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("startTransitStopId")
                .description("This argument has currently no effect on which itineraries are returned. Use argument `fromPlace` to start the itinerary from a specific stop.  \n ~~A transit stop that this trip must start from~~")
                .type(Scalars.GraphQLString)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("startTransitTripId")
                .description("ID of the trip on which the itinerary starts. This argument can be used to plan itineraries when the user is already onboard a vehicle. When using this argument, arguments `time` and `from` should be set based on a vehicle position message received from the vehicle running the specified trip.  \n **Note:** this argument only takes into account the route and estimated travel time of the trip (and therefore arguments `time` and `from` must be used correctly to get meaningful itineraries).")
                .type(Scalars.GraphQLString)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("claimInitialWait")
                .description("No effect on itinerary planning, adjust argument `time` instead to get later departures.  \n ~~The maximum wait time in seconds the user is willing to delay trip start. Only effective in Analyst.~~")
                .type(Scalars.GraphQLLong)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("reverseOptimizeOnTheFly")
                .description("**Consider this argument experimental** – setting this argument to true causes timeouts and unoptimal routes in many cases.  \n When true, reverse optimize (find alternative transportation mode, which still arrives to the destination in time) this search on the fly after processing each transit leg, rather than reverse-optimizing the entire path when it's done. Default value: false.")
                .type(Scalars.GraphQLBoolean)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("omitCanceled")
                .description("When false, return itineraries using canceled trips. Default value: true.")
                .defaultValue(true)
                .type(Scalars.GraphQLBoolean)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("ignoreRealtimeUpdates")
                .description("When true, realtime updates are ignored during this search. Default value: false")
                .type(Scalars.GraphQLBoolean)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("disableRemainingWeightHeuristic")
                .description("Only useful for testing and troubleshooting.  \n ~~If true, the remaining weight heuristic is disabled. Currently only implemented for the long distance path service. Default value: false.~~")
                .type(Scalars.GraphQLBoolean)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("locale")
                .description("Two-letter language code (ISO 639-1) used for returned text.  \n **Note:** only part of the data has translations available and names of stops and POIs are returned in their default language. Due to missing translations, it is sometimes possible that returned text uses a mixture of two languages.")
                .type(Scalars.GraphQLString)
                .build())
            .argument(GraphQLArgument.newArgument()
                 .name("ticketTypes")
                 .description("A comma-separated list of allowed ticket types.")
                 .type(Scalars.GraphQLString)
                 .build())
            .argument(GraphQLArgument.newArgument()
                .name("heuristicStepsPerMainStep")
                .description("Tuning parameter for the search algorithm, mainly useful for testing.")
                .type(Scalars.GraphQLInt)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("compactLegsByReversedSearch")
                .description("Whether legs should be compacted by performing a reversed search.  \n **Experimental argument, will be removed!**")
                .type(Scalars.GraphQLBoolean)
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
                .description("Two-letter language code (ISO 639-1)")
                .type(Scalars.GraphQLString)
                .dataFetcher(environment -> ((Map.Entry<String, String>) environment.getSource()).getKey())
                .build())
            .build();

        alertType = GraphQLObjectType.newObject()
            .name("Alert")
            .withInterface(nodeInterface)
            .description("Alert of a current or upcoming disruption in public transportation")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("id")
                .description("Global object ID provided by Relay. This value can be used to refetch this object using **node** query.")
                .type(new GraphQLNonNull(Scalars.GraphQLID))
                .dataFetcher(environment -> relay.toGlobalId(
                    alertType.getName(), ((AlertPatch) environment.getSource()).getId()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("feed")
		.description("The feed in which this alert was published")
                .type(Scalars.GraphQLString)
                .dataFetcher(environment -> ((AlertPatch) environment.getSource()).getFeedId())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("agency")
                .description("Agency affected by the disruption. Note that this value is present only if the disruption has an effect on all operations of the agency (e.g. in case of a strike).")
                .type(agencyType)
                .dataFetcher(environment -> getAgency(index, ((AlertPatch) environment.getSource()).getAgency()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("route")
		.description("Route affected by the disruption")
                .type(routeType)
                .dataFetcher(environment -> index.routeForId.get(((AlertPatch) environment.getSource()).getRoute()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("trip")
		.description("Trip affected by the disruption")
                .type(tripType)
                .dataFetcher(environment -> index.tripForId.get(((AlertPatch) environment.getSource()).getTrip()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stop")
		.description("Stop affected by the disruption")
                .type(stopType)
                .dataFetcher(environment -> index.stopForId.get(((AlertPatch) environment.getSource()).getStop()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("patterns")
                .description("Patterns affected by the disruption")
                .type(new GraphQLList(patternType))
                .dataFetcher(environment -> ((AlertPatch) environment.getSource()).getTripPatterns())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("alertHeaderText")
                .type(Scalars.GraphQLString)
                .description("Header of the alert, if available")
                .dataFetcher(environment -> ((AlertPatch) environment.getSource()).getAlert().alertHeaderText)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("alertHeaderTextTranslations")
                .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(translatedStringType))))
                .description("Header of the alert in all different available languages")
                .dataFetcher(environment -> {
                    AlertPatch alertPatch = environment.getSource();
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
                .description("Long description of the alert")
                .dataFetcher(environment -> ((AlertPatch) environment.getSource()).getAlert().alertDescriptionText)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("alertDescriptionTextTranslations")
                .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(translatedStringType))))
                .description("Long descriptions of the alert in all different available languages")
                .dataFetcher(environment -> {
                    AlertPatch alertPatch = environment.getSource();
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
                .description("Time when this alert comes into effect. Format: Unix timestamp in seconds")
                .dataFetcher(environment -> {
                    Alert alert = ((AlertPatch) environment.getSource()).getAlert();
                    return alert.effectiveStartDate != null ? alert.effectiveStartDate.getTime() / 1000 : null;
                })
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("effectiveEndDate")
                .type(Scalars.GraphQLLong)
                .description("Time when this alert is not in effect anymore. Format: Unix timestamp in seconds")
                .dataFetcher(environment -> {
                    Alert alert = ((AlertPatch) environment.getSource()).getAlert();
                    return alert.effectiveEndDate != null ? alert.effectiveEndDate.getTime() / 1000 : null;
                })
                .build())
            .build();


        serviceTimeRangeType = GraphQLObjectType.newObject()
            .name("serviceTimeRange")
            .description("Time range for which the API has data available")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("start")
                .type(Scalars.GraphQLLong)
                .description("Time from which the API has data available. Format: Unix timestamp in seconds")
                .dataFetcher(environment -> index.graph.getTransitServiceStarts())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("end")
                .type(Scalars.GraphQLLong)
                .description("Time until which the API has data available. Format: Unix timestamp in seconds")
                .dataFetcher(environment -> index.graph.getTransitServiceEnds())
                .build())
            .build();


        stopAtDistanceType = GraphQLObjectType.newObject()
            .name("stopAtDistance")
            .withInterface(nodeInterface)
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("id")
                .description("Global object ID provided by Relay. This value can be used to refetch this object using **node** query.")
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
		.description("Walking distance to the stop along streets and paths")
                .type(Scalars.GraphQLInt)
                .dataFetcher(environment -> ((GraphIndex.StopAndDistance) environment.getSource()).distance)
                .build())
            .build();

        departureRowType = GraphQLObjectType.newObject()
            .name("DepartureRow")
	    .description("Departure row is a location, which lists departures of a certain pattern from a stop. Departure rows are identified with the pattern, so querying departure rows will return only departures from one stop per pattern")
            .withInterface(nodeInterface)
            .withInterface(placeInterface)
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("id")
                .description("Global object ID provided by Relay. This value can be used to refetch this object using **node** query.")
                .type(new GraphQLNonNull(Scalars.GraphQLID))
                .dataFetcher(environment -> relay.toGlobalId(departureRowType.getName(), ((GraphIndex.DepartureRow)environment.getSource()).id))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stop")
		.description("Stop from which the departures leave")
                .type(stopType)
                .dataFetcher(environment -> ((GraphIndex.DepartureRow)environment.getSource()).stop)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("lat")
                .description("Latitude of the stop (WGS 84)")
                .type(Scalars.GraphQLFloat)
                .dataFetcher(environment -> ((GraphIndex.DepartureRow)environment.getSource()).stop.getLat())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("lon")
                .description("Longitude of the stop (WGS 84)")
                .type(Scalars.GraphQLFloat)
                .dataFetcher(environment -> ((GraphIndex.DepartureRow)environment.getSource()).stop.getLon())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("pattern")
		.description("Pattern of the departure row")
                .type(patternType)
                .dataFetcher(environment -> ((GraphIndex.DepartureRow)environment.getSource()).pattern)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stoptimes")
		        .description("Departures of the pattern from the stop")
                .type(new GraphQLList(stoptimeType))
                .argument(GraphQLArgument.newArgument()
                    .name("startTime")
                    .description("Return rows departing after this time. Time format: Unix timestamp in seconds. Default: current time.")
                    .type(Scalars.GraphQLLong)
                    .defaultValue(0L) // Default value is current time
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
                .argument(GraphQLArgument.newArgument()
                    .name("omitNonPickups")
		            .description("If true, only those departures which allow boarding are returned")
                    .type(Scalars.GraphQLBoolean)
                    .defaultValue(false)
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("omitCanceled")
                    .description("If false, returns also canceled trips")
                    .type(Scalars.GraphQLBoolean)
                    .defaultValue(true)
                    .build())
                .dataFetcher(environment -> {
                    GraphIndex.DepartureRow departureRow = environment.getSource();
                    long startTime = environment.getArgument("startTime");
                    int timeRange = environment.getArgument("timeRange");
                    int maxDepartures = environment.getArgument("numberOfDepartures");
                    boolean omitNonPickups = environment.getArgument("omitNonPickups");
                    boolean omitCanceled = environment.getArgument("omitCanceled");
                    return departureRow.getStoptimes(index, startTime, timeRange, maxDepartures, omitNonPickups, omitCanceled);
                })
                .build())
            .build();

        placeAtDistanceType = GraphQLObjectType.newObject()
            .name("placeAtDistance")
            .withInterface(nodeInterface)
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("id")
                .description("Global object ID provided by Relay. This value can be used to refetch this object using **node** query.")
                .type(new GraphQLNonNull(Scalars.GraphQLID))
                .dataFetcher(environment -> {
                    Object place = ((PlaceAndDistance) environment.getSource()).place;
                    return relay.toGlobalId(placeAtDistanceType.getName(),
                        Integer.toString(((PlaceAndDistance) environment.getSource()).distance) + ";" +
                            placeInterface.getTypeResolver()
                                .getType(place)
                                .getFieldDefinition("id")
                                .getDataFetcher()
                                .get(new DataFetchingEnvironmentImpl(place, null, null,
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
		.description("Walking distance to the place along streets and paths")
                .type(Scalars.GraphQLInt)
                .dataFetcher(environment -> ((GraphIndex.PlaceAndDistance) environment.getSource()).distance)
                .build())
            .build();

        stoptimesInPatternType = GraphQLObjectType.newObject()
            .name("StoptimesInPattern")
	    .description("Stoptimes grouped by pattern")
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
	    .description("Cluster is a list of stops grouped by name and proximity")
            .withInterface(nodeInterface)
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("id")
                .description("Global object ID provided by Relay. This value can be used to refetch this object using **node** query.")
                .type(new GraphQLNonNull(Scalars.GraphQLID))
                .dataFetcher(environment -> relay
                    .toGlobalId(clusterType.getName(), ((StopCluster) environment.getSource()).id))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("gtfsId")
                .description("ID of the cluster")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .dataFetcher(environment -> ((StopCluster) environment.getSource()).id)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("name")
                .description("Name of the cluster")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .dataFetcher(environment -> ((StopCluster) environment.getSource()).name)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("lat")
		        .description("Latitude of the center of this cluster (i.e. average latitude of stops in this cluster)")
                .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                .dataFetcher(environment -> (((StopCluster) environment.getSource()).lat))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("lon")
		        .description("Longitude of the center of this cluster (i.e. average longitude of stops in this cluster)")
                .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                .dataFetcher(environment -> (((StopCluster) environment.getSource()).lon))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stops")
		        .description("List of stops in the cluster")
                .type(new GraphQLList(new GraphQLNonNull(stopType)))
                .dataFetcher(environment -> ((StopCluster) environment.getSource()).children)
                .build())
            .build();

        stopType = GraphQLObjectType.newObject()
            .name("Stop")
            .description("Stop can represent either a single public transport stop, where passengers can board and/or disembark vehicles, or a station, which contains multiple stops. See field `locationType`.")
            .withInterface(nodeInterface)
            .withInterface(placeInterface)
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("id")
                .description("Global object ID provided by Relay. This value can be used to refetch this object using **node** query.")
                .type(new GraphQLNonNull(Scalars.GraphQLID))
                .dataFetcher(environment -> relay.toGlobalId(
                    stopType.getName(),
                    GtfsLibrary.convertIdToString(((Stop) environment.getSource()).getId())))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stopTimesForPattern")
		.description("Returns timetable of the specified pattern at this stop")
                .type(new GraphQLList(stoptimeType))
                .argument(GraphQLArgument.newArgument()
                    .name("id")
		    .description("Id of the pattern")
                    .type(new GraphQLNonNull(Scalars.GraphQLString))
                    .defaultValue(null)
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("startTime")
		    .description("Return  departures after this time. Format: Unix timestamp in seconds. Default value: current time")
                    .type(Scalars.GraphQLLong)
                    .defaultValue(0l) // Default value is current time
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("timeRange")
                    .description("Return stoptimes within this time range, starting from `startTime`. Unit: Seconds")
                    .type(Scalars.GraphQLInt)
                    .defaultValue(24 * 60 * 60)
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("numberOfDepartures")
                    .type(Scalars.GraphQLInt)
                    .defaultValue(2)
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("omitNonPickups")
		            .description("If true, only those departures which allow boarding are returned")
                    .type(Scalars.GraphQLBoolean)
                    .defaultValue(false)
                    .build())
                .argument(GraphQLArgument.newArgument()
                        .name("omitCanceled")
                        .description("If false, returns also canceled trips")
                        .type(Scalars.GraphQLBoolean)
                        .defaultValue(true)
                        .build())
                .dataFetcher(environment ->
                    index.stopTimesForPattern(environment.getSource(),
                        index.patternForId.get(environment.getArgument("id")),
                        environment.getArgument("startTime"),
                        environment.getArgument("timeRange"),
                        environment.getArgument("numberOfDepartures"),
                        environment.getArgument("omitNonPickups"),
                        environment.getArgument("omitCanceled")))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("gtfsId")
		        .description("ÌD of the stop in format `FeedId:StopId`")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .dataFetcher(environment ->
                    ((Stop) environment.getSource()).getId().toString())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("name")
		.description("Name of the stop, e.g. Pasilan asema")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("lat")
                .description("Latitude of the stop (WGS 84)")
                .type(Scalars.GraphQLFloat)
                .dataFetcher(environment -> (((Stop) environment.getSource()).getLat()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("lon")
                .description("Longitude of the stop (WGS 84)")
                .type(Scalars.GraphQLFloat)
                .dataFetcher(environment -> (((Stop) environment.getSource()).getLon()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("code")
		.description("Stop code which is visible at the stop")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("desc")
		.description("Description of the stop, usually a street name")
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
                .description("Identifies whether this stop represents a stop or station.")
                .type(locationTypeEnum)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("parentStation")
		.description("The station which this stop is part of (or null if this stop is not part of a station)")
                .type(stopType)
                .dataFetcher(environment -> ((Stop) environment.getSource()).getParentStation() != null ?
                    index.stationForId.get(new FeedScopedId(
                        ((Stop) environment.getSource()).getId().getAgencyId(),
                        ((Stop) environment.getSource()).getParentStation())) : null)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("wheelchairBoarding")
		.description("Whether wheelchair boarding is possible for at least some of vehicles on this stop")
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
		        .description("The raw GTFS route type used by routes which pass through this stop. For the list of possible values, see: https://developers.google.com/transit/gtfs/reference/#routestxt and https://developers.google.com/transit/gtfs/reference/extended-route-types")
                .type(Scalars.GraphQLInt)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("vehicleMode")
                .description("Transport mode (e.g. `BUS`) used by routes which pass through this stop or `null` if mode cannot be determined, e.g. in case no routes pass through the stop.  \n Note that also other types of vehicles may use the stop, e.g. tram replacement buses might use stops which have `TRAM` as their mode.")
                .type(modeEnum)
                .dataFetcher(environment -> {
                    try {
                        return GtfsLibrary.getTraverseMode(((Stop)environment.getSource()).getVehicleType());
                    } catch (IllegalArgumentException iae) {
                        //If 'vehicleType' is not specified, guess vehicle mode from list of patterns
                        return index.patternsForStop.get(environment.getSource())
                                .stream()
                                .map(pattern -> pattern.mode)
                                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                                .entrySet()
                                .stream()
                                .max(Comparator.comparing(Map.Entry::getValue))
                                .map(e -> e.getKey())
                                .orElse(null);
                    }
                })
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("platformCode")
		.description("Identifier of the platform, usually a number. This value is only present for stops that are part of a station")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("cluster")
		.description("The cluster which this stop is part of")
                .type(clusterType)
                .dataFetcher(environment -> index.stopClusterForStop.get(environment.getSource()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stops")
                .description("Returns all stops that are children of this station (Only applicable for stations)")
                .type(new GraphQLList(stopType))
                .dataFetcher(environment -> index.stopsForParentStation.get(((Stop) environment.getSource()).getId()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("routes")
		.description("Routes which pass through this stop")
                .type(new GraphQLList(new GraphQLNonNull(routeType)))
                .dataFetcher(environment -> index.patternsForStop
                    .get(environment.getSource())
                    .stream()
                    .map(pattern -> pattern.route)
                    .distinct()
                    .collect(Collectors.toList()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("patterns")
		.description("Patterns which pass through this stop")
                .type(new GraphQLList(patternType))
                .dataFetcher(environment -> index.patternsForStop.get(environment.getSource()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("transfers")
		        .description("List of nearby stops which can be used for transfers")
                .type(new GraphQLList(stopAtDistanceType))
                .argument(GraphQLArgument.newArgument()
                        .name("maxDistance")
                        .description("Maximum distance to the transfer stop. Defaults to unlimited.  \n **Note:** only stops that are linked as a transfer stops to this stop are returned, i.e. this does not do a query to search for *all* stops within radius of `maxDistance`.")
                        .type(Scalars.GraphQLInt)
                        .build())
                .dataFetcher(environment -> index.stopVertexForStop
                    .get(environment.getSource())
                    .getOutgoing()
                    .stream()
                    .filter(edge -> edge instanceof SimpleTransfer)
                    .filter(edge -> environment.getArgument("maxDistance") == null || edge.getDistance() <= (Integer)environment.getArgument("maxDistance"))
                    .map(edge -> new GraphIndex.StopAndDistance(((TransitVertex)edge.getToVertex()).getStop(), (int)Math.round(edge.getDistance())))
                    .collect(Collectors.toList()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stoptimesForServiceDate")
		.description("Returns list of stoptimes for the specified date")
                .type(new GraphQLList(stoptimesInPatternType))
                .argument(GraphQLArgument.newArgument()
                    .name("date")
		    .description("Date in format YYYYMMDD")
                    .type(Scalars.GraphQLString)
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("omitNonPickups")
		    .description("If true, only those departures which allow boarding are returned")
                    .type(Scalars.GraphQLBoolean)
                    .defaultValue(false)
                    .build())
            .argument(GraphQLArgument.newArgument()
                    .name("omitCanceled")
                    .description("If false, returns also canceled trips")
                    .type(Scalars.GraphQLBoolean)
                    .defaultValue(true)
                    .build())
                .dataFetcher(environment -> {
                    ServiceDate date;
                    try {  // TODO: Add our own scalar types for at least serviceDate and AgencyAndId
                        date = ServiceDate.parseString(environment.getArgument("date"));
                    } catch (ParseException e) {
                        return null;
                    }
                    Stop stop = environment.getSource();
                    boolean omitNonPickups = environment.getArgument("omitNonPickups");
                    if (stop.getLocationType() == 1) {
                        // Merge all stops if this is a station
                        return index.stopsForParentStation
                                .get(stop.getId())
                                .stream()
                                .flatMap(singleStop -> index.getStopTimesForStop(singleStop, date,omitNonPickups).stream())
                                .collect(Collectors.toList());
                    }
                    return index.getStopTimesForStop(stop, date, omitNonPickups);
                })
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stoptimesForPatterns")
		.description("Returns list of stoptimes (arrivals and departures) at this stop, grouped by patterns")
                .type(new GraphQLList(stoptimesInPatternType))
                .argument(GraphQLArgument.newArgument()
                    .name("startTime")
		    .description("Return departures after this time. Format: Unix timestamp in seconds. Default value: current time")
                    .type(Scalars.GraphQLLong)
                    .defaultValue(0L) // Default value is current time
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("timeRange")
                    .description("Return stoptimes within this time range, starting from `startTime`. Unit: Seconds")
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
		            .description("If true, only those departures which allow boarding are returned")
                    .type(Scalars.GraphQLBoolean)
                    .defaultValue(false)
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("omitCanceled")
                    .description("If false, returns also canceled trips")
                    .type(Scalars.GraphQLBoolean)
                    .defaultValue(true)
                    .build())
                .dataFetcher(environment -> {
                    Stop stop = environment.getSource();
                    if (stop.getLocationType() == 1) {
                        // Merge all stops if this is a station
                        return index.stopsForParentStation
                            .get(stop.getId())
                            .stream()
                            .flatMap(singleStop ->
                                index.stopTimesForStop(singleStop,
                                    environment.getArgument("startTime"),
                                    environment.getArgument("timeRange"),
                                    environment.getArgument("numberOfDepartures"),
                                    environment.getArgument("omitNonPickups"),
                                    environment.getArgument("omitCanceled"))
                                .stream()
                            )
                            .collect(Collectors.toList());
                    }
                    return index.stopTimesForStop(stop,
                        environment.getArgument("startTime"),
                        environment.getArgument("timeRange"),
                        environment.getArgument("numberOfDepartures"),
                        environment.getArgument("omitNonPickups"),
                        environment.getArgument("omitCanceled"));

                })
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stoptimesWithoutPatterns")
		.description("Returns list of stoptimes (arrivals and departures) at this stop")
                .type(new GraphQLList(stoptimeType))
                .argument(GraphQLArgument.newArgument()
                    .name("startTime")
		    .description("Return departures after this time. Format: Unix timestamp in seconds. Default value: current time")
                    .type(Scalars.GraphQLLong)
                    .defaultValue(0L) // Default value is current time
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("timeRange")
                    .description("Return stoptimes within this time range, starting from `startTime`. Unit: Seconds")
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
		            .description("If true, only those departures which allow boarding are returned")
                    .type(Scalars.GraphQLBoolean)
                    .defaultValue(false)
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("omitCanceled")
                    .description("If false, returns also canceled trips")
                    .type(Scalars.GraphQLBoolean)
                    .defaultValue(true)
                    .build())
                .dataFetcher(environment -> {
                    Stop stop = environment.getSource();
                    Stream<StopTimesInPattern> stream;
                    if (stop.getLocationType() == 1) {
                        stream = index.stopsForParentStation
                            .get(stop.getId())
                            .stream()
                            .flatMap(singleStop ->
                                index.stopTimesForStop(singleStop,
                                    environment.getArgument("startTime"),
                                    environment.getArgument("timeRange"),
                                    environment.getArgument("numberOfDepartures"),
                                    environment.getArgument("omitNonPickups"),
                                    environment.getArgument("omitCanceled"))
                                    .stream()
                            );
                    }
                    else {
                        stream = index.stopTimesForStop(
                            environment.getSource(),
                            environment.getArgument("startTime"),
                            environment.getArgument("timeRange"),
                            environment.getArgument("numberOfDepartures"),
                            environment.getArgument("omitNonPickups"),
                            environment.getArgument("omitCanceled")
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
	        .description("Stoptime represents the time when a specific trip arrives to or departs from a specific stop.")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stop")
                .description("The stop where this arrival/departure happens")
                .type(stopType)
                .dataFetcher(environment -> index.stopForId
                    .get(((TripTimeShort) environment.getSource()).stopId))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("scheduledArrival")
		.description("Scheduled arrival time. Format: seconds since midnight of the departure date")
                .type(Scalars.GraphQLInt)
                .dataFetcher(
                    environment -> ((TripTimeShort) environment.getSource()).scheduledArrival)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("realtimeArrival")
		.description("Realtime prediction of arrival time. Format: seconds since midnight of the departure date")
                .type(Scalars.GraphQLInt)
                .dataFetcher(
                    environment -> ((TripTimeShort) environment.getSource()).realtimeArrival)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("arrivalDelay")
		.description("The offset from the scheduled arrival time in seconds. Negative values indicate that the trip is running ahead of schedule.")
                .type(Scalars.GraphQLInt)
                .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).arrivalDelay)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("scheduledDeparture")
		.description("Scheduled departure time. Format: seconds since midnight of the departure date")
                .type(Scalars.GraphQLInt)
                .dataFetcher(
                    environment -> ((TripTimeShort) environment.getSource()).scheduledDeparture)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("realtimeDeparture")
		.description("Realtime prediction of departure time. Format: seconds since midnight of the departure date")
                .type(Scalars.GraphQLInt)
                .dataFetcher(
                    environment -> ((TripTimeShort) environment.getSource()).realtimeDeparture)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("departureDelay")
		.description("The offset from the scheduled departure time in seconds. Negative values indicate that the trip is running ahead of schedule")
                .type(Scalars.GraphQLInt)
                .dataFetcher(
                    environment -> ((TripTimeShort) environment.getSource()).departureDelay)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("timepoint")
		.description("true, if this stop is used as a time equalization stop. false otherwise.")
                .type(Scalars.GraphQLBoolean)
                .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).timepoint)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("realtime")
		.description("true, if this stoptime has real-time data available")
                .type(Scalars.GraphQLBoolean)
                .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).realtime)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("realtimeState")
		.description("State of real-time data")
                .type(realtimeStateEnum)
                .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).realtimeState)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("pickupType")
		.description("Whether the vehicle can be boarded at this stop. This field can also be used to indicate if boarding is possible only with special arrangements.")
                .type(pickupDropoffTypeEnum)
                .dataFetcher(environment -> index.patternForTrip
                    .get(index.tripForId.get(((TripTimeShort) environment.getSource()).tripId))
                    .getBoardType(((TripTimeShort) environment.getSource()).stopIndex))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("dropoffType")
		.description("Whether the vehicle can be disembarked at this stop. This field can also be used to indicate if disembarkation is possible only with special arrangements.")
                .type(pickupDropoffTypeEnum)
                .dataFetcher(environment -> index.patternForTrip
                    .get(index.tripForId.get(((TripTimeShort) environment.getSource()).tripId))
                    .getAlightType(((TripTimeShort) environment.getSource()).stopIndex))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("serviceDay")
		.description("Departure date of the trip. Format: Unix timestamp (local time) in seconds.")
                .type(Scalars.GraphQLLong)
                .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).serviceDay)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("trip")
		.description("Trip which this stoptime is for")
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
		        .description("Vehicle headsign of the trip on this stop. Trip headsigns can change during the trip (e.g. on routes which run on loops), so this value should be used instead of `tripHeadsign` to display the headsign relevant to the user. ")
              	.type(Scalars.GraphQLString)
              	.dataFetcher(environment -> ((TripTimeShort) environment.getSource()).headsign)
              	.build())
            .build();

        tripType = GraphQLObjectType.newObject()
            .name("Trip")
	    .description("Trip is a specific occurance of a pattern, usually identified by route, direction on the route and exact departure time.")
            .withInterface(nodeInterface)
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("id")
                .description("Global object ID provided by Relay. This value can be used to refetch this object using **node** query.")
                .type(new GraphQLNonNull(Scalars.GraphQLID))
                .dataFetcher(environment -> relay.toGlobalId(
                    tripType.getName(),
                    GtfsLibrary.convertIdToString(((Trip) environment.getSource()).getId())))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("gtfsId")
                .description("ID of the trip in format `FeedId:TripId`")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .dataFetcher(environment -> GtfsLibrary
                    .convertIdToString(((Trip) environment.getSource()).getId()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("route")
		.description("The route the trip is running on")
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
		.description("List of dates when this trip is in service. Format: YYYYMMDD")
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
		.description("Headsign of the vehicle when running on this trip")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("routeShortName")
                .description("Short name of the route this trip is running. See field `shortName` of Route.")
                .type(Scalars.GraphQLString)
                .dataFetcher(environment -> {
                    Trip trip = (Trip)environment.getSource();

                    return trip.getRouteShortName() != null ? trip.getRouteShortName() : trip.getRoute().getShortName();
                })
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("directionId")
                .description("Direction code of the trip, i.e. is this the outbound or inbound trip of a pattern. Possible values: 0, 1 or `null` if the direction is irrelevant, i.e. the pattern has trips only in one direction.")
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
		.description("Whether the vehicle running this trip can be boarded by a wheelchair")
                .type(wheelchairBoardingEnum)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("bikesAllowed")
		.description("Whether bikes are allowed on board the vehicle running this trip")
                .type(bikesAllowedEnum)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("pattern")
		.description("The pattern the trip is running on")
                .type(patternType)
                .dataFetcher(
                    environment -> index.patternForTrip.get(environment.getSource()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stops")
                .description("List of stops this trip passes through")
                .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(stopType))))
                .dataFetcher(environment -> index.patternForTrip
                    .get(environment.getSource()).getStops())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("semanticHash")
		.description("Hash code of the trip. This value is stable and not dependent on the trip id.")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .dataFetcher(environment -> index.patternForTrip.get(environment.getSource())
                    .semanticHashString(environment.getSource()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stoptimes")
		.description("List of times when this trip arrives to or departs from a stop")
                .type(new GraphQLList(stoptimeType))
                .dataFetcher(environment -> TripTimeShort.fromTripTimes(
                    index.patternForTrip.get((Trip) environment.getSource()).scheduledTimetable,
                    environment.getSource()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("departureStoptime")
                .description("Departure time from the first stop")
                .type(stoptimeType)
                .argument(GraphQLArgument.newArgument()
                    .name("serviceDate")
                    .description("Date for which the departure time is returned. Format: YYYYMMDD. If this argument is not used, field `serviceDay` in the stoptime will have a value of 0.")
                    .type(Scalars.GraphQLString)
                    .build())
                .dataFetcher(environment -> {
                    try {
                        Timetable timetable = index.patternForTrip.get((Trip) environment.getSource()).scheduledTimetable;
                        TripTimes triptimes = timetable.getTripTimes(environment.getSource());
                        return new TripTimeShort(triptimes,
                                0,
                                timetable.pattern.getStop(0),
                                environment.getArgument("serviceDate") == null
                                        ? null : new ServiceDay(index.graph, ServiceDate.parseString(environment.getArgument("serviceDate")), index.graph.getCalendarService(), ((Trip)environment.getSource()).getRoute().getAgency().getId()));
                    } catch (ParseException e) {
                        //Invalid date format
                        return null;
                    }
                })
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("arrivalStoptime")
                .description("Arrival time to the final stop")
                .type(stoptimeType)
                .argument(GraphQLArgument.newArgument()
                        .name("serviceDate")
                        .description("Date for which the arrival time is returned. Format: YYYYMMDD. If this argument is not used, field `serviceDay` in the stoptime will have a value of 0.")
                        .type(Scalars.GraphQLString)
                        .build())
                .dataFetcher(environment -> {
                    try {
                        Timetable timetable = index.patternForTrip.get((Trip) environment.getSource()).scheduledTimetable;
                        TripTimes triptimes = timetable.getTripTimes(environment.getSource());
                        return new TripTimeShort(triptimes,
                                triptimes.getNumStops() - 1,
                                timetable.pattern.getStop(triptimes.getNumStops() - 1),
                                environment.getArgument("serviceDate") == null
                                        ? null : new ServiceDay(index.graph, ServiceDate.parseString(environment.getArgument("serviceDate")), index.graph.getCalendarService(), ((Trip)environment.getSource()).getRoute().getAgency().getId()));
                    } catch (ParseException e) {
                        //Invalid date format
                        return null;
                    }
                })
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stoptimesForDate")
                .type(new GraphQLList(stoptimeType))
                .argument(GraphQLArgument.newArgument()
                    .name("serviceDay")
                    .type(Scalars.GraphQLString)
                    .defaultValue(null)
                    .description("Deprecated, please switch to serviceDate instead")
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("serviceDate")
		    .description("Date for which stoptimes are returned. Format: YYYYMMDD")
                    .type(Scalars.GraphQLString)
                    .build())
                .dataFetcher(environment -> {
                    try {
                        final Trip trip = environment.getSource();
                        final String argServiceDate =
                            environment.containsArgument("serviceDate")
                                && environment.getArgument("serviceDate") != null
                                ? environment.getArgument("serviceDate")
                                : environment.getArgument("serviceDay");
                        final ServiceDate serviceDate = argServiceDate != null
                            ? ServiceDate.parseString(argServiceDate) : new ServiceDate();
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
		        .description("List of coordinates of this trip's route")
                .type(new GraphQLList(new GraphQLList(Scalars.GraphQLFloat))) //TODO: Should be geometry
                .dataFetcher(environment -> {
                    LineString geometry = index.patternForTrip
                            .get(environment.getSource())
                            .geometry;
                    if (geometry == null) {return null;}
                    return Arrays.stream(geometry.getCoordinateSequence().toCoordinateArray())
                        .map(coordinate -> Arrays.asList(coordinate.x, coordinate.y))
                        .collect(Collectors.toList());
                    }
                )
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("tripGeometry")
                .description("Coordinates of the route of this trip in Google polyline encoded format")
                .type(geometryType)
                .dataFetcher(environment -> {
                    LineString geometry = index.patternForTrip
                        .get(environment.getSource())
                        .geometry;
                    if (geometry == null) {
                        return null;
                    }

                    return PolylineEncoder.createEncodings(Arrays.asList(geometry.getCoordinates()));
                })
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("alerts")
                .description("List of alerts which have an effect on this trip")
                .type(new GraphQLList(alertType))
                .dataFetcher(dataFetchingEnvironment -> index.getAlertsForTrip(
                    dataFetchingEnvironment.getSource()))
                .build())
            .build();

        coordinateType = GraphQLObjectType.newObject()
            .name("Coordinates")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("lat")
                .description("Latitude (WGS 84)")
                .type(Scalars.GraphQLFloat)
                .dataFetcher(
                    environment -> ((Coordinate) environment.getSource()).y)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("lon")
                .description("Longitude (WGS 84)")
                .type(Scalars.GraphQLFloat)
                .dataFetcher(
                    environment -> ((Coordinate) environment.getSource()).x)
                .build())
            .build();

        patternType = GraphQLObjectType.newObject()
            .name("Pattern")
	          .description("Pattern is sequence of stops used by trips on a specific direction and variant of a route. Most routes have only two patterns: one for outbound trips and one for inbound trips")
            .withInterface(nodeInterface)
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("id")
                .description("Global object ID provided by Relay. This value can be used to refetch this object using **node** query.")
                .type(new GraphQLNonNull(Scalars.GraphQLID))
                .dataFetcher(environment -> relay.toGlobalId(
                    patternType.getName(), ((TripPattern) environment.getSource()).code))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("route")
                .description("The route this pattern runs on")
                .type(new GraphQLNonNull(routeType))
                .dataFetcher(environment -> ((TripPattern) environment.getSource()).route)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("directionId")
		        .description("Direction of the pattern. Possible values: 0, 1 or -1.  \n -1 indicates that the direction is irrelevant, i.e. the route has patterns only in one direction.")
                .type(Scalars.GraphQLInt)
                .dataFetcher(environment -> ((TripPattern) environment.getSource()).directionId)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("name")
                .description("Name of the pattern. Pattern name can be just the name of the route or it can include details of destination and origin stops.")
                .type(Scalars.GraphQLString)
                .dataFetcher(environment -> ((TripPattern) environment.getSource()).name)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("code")
		        .description("ID of the pattern")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .dataFetcher(environment -> ((TripPattern) environment.getSource()).code)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("headsign")
		        .description("Vehicle headsign used by trips of this pattern")
                .type(Scalars.GraphQLString)
                .dataFetcher(environment -> ((TripPattern) environment.getSource()).getDirection())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("trips")
		        .description("Trips which run on this pattern")
                .type(new GraphQLList(new GraphQLNonNull(tripType)))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("tripsForDate")
		        .description("Trips which run on this pattern on the specified date")
                .argument(GraphQLArgument.newArgument()
                    .name("serviceDay")
                    .type(Scalars.GraphQLString)
                    .description("Deprecated, please switch to serviceDate instead")
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("serviceDate")
		            .description("Return trips of the pattern active on this date. Format: YYYYMMDD")
                    .type(Scalars.GraphQLString)
                    .build())
                .type(new GraphQLList(new GraphQLNonNull(tripType)))
                .dataFetcher(environment -> {
                    try {
                        BitSet services = index.servicesRunning(
                            ServiceDate.parseString(
                                environment.containsArgument("serviceDate")
                                    && environment.getArgument("serviceDate") != null
                                    ? environment.getArgument("serviceDate")
                                    : environment.getArgument("serviceDay")
                            )
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
		        .description("List of stops served by this pattern")
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
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("patternGeometry")
                .description("Coordinates of the route of this pattern in Google polyline encoded format")
                .type(geometryType)
                .dataFetcher(environment -> {
                    LineString geometry = ((TripPattern) environment.getSource()).geometry;
                    if (geometry == null) {
                        return null;
                    }

                    return PolylineEncoder.createEncodings(Arrays.asList(geometry.getCoordinates()));
                })
                .build())
                // TODO: add stoptimes
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("semanticHash")
                .description("Hash code of the pattern. This value is stable and not dependent on the pattern id, i.e. this value can be used to check whether two patterns are the same, even if their ids have changed.")
                .type(Scalars.GraphQLString)
                .dataFetcher(environment ->
                    ((TripPattern) environment.getSource()).semanticHashString(null))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("alerts")
                .description("List of alerts which have an effect on trips of the pattern")
                .type(new GraphQLList(alertType))
                .dataFetcher(dataFetchingEnvironment -> index.getAlertsForPattern(
                    dataFetchingEnvironment.getSource()))
                .build())
            .build();


        routeType = GraphQLObjectType.newObject()
            .name("Route")
            .description("Route represents a public transportation service, usually from point A to point B and *back*, shown to customers under a single name, e.g. bus 550. Routes contain patterns (see field `patterns`), which describe different variants of the route, e.g. outbound pattern from point A to point B and inbound pattern from point B to point A.")
	    .withInterface(nodeInterface)
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("id")
                .description("Global object ID provided by Relay. This value can be used to refetch this object using **node** query.")
                .type(new GraphQLNonNull(Scalars.GraphQLID))
                .dataFetcher(environment -> relay.toGlobalId(
                    routeType.getName(),
                    GtfsLibrary.convertIdToString(((Route) environment.getSource()).getId())))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("gtfsId")
                .description("ID of the route in format `FeedId:RouteId`")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .dataFetcher(environment ->
                    GtfsLibrary.convertIdToString(((Route) environment.getSource()).getId()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("agency")
                .description("Agency operating the route")
                .type(agencyType)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("shortName")
		.description("Short name of the route, usually a line number, e.g. 550")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("longName")
		.description("Long name of the route, e.g. Helsinki-Leppävaara")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("mode")
                .description("Transport mode of this route, e.g. `BUS`")
                .type(modeEnum)
                .dataFetcher(environment -> GtfsLibrary.getTraverseMode(
                    environment.getSource()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("type")
                .type(Scalars.GraphQLInt)
                .dataFetcher(environment -> ((Route) environment.getSource()).getType())
                .description("The raw GTFS route type as a integer. For the list of possible values, see: https://developers.google.com/transit/gtfs/reference/#routestxt and https://developers.google.com/transit/gtfs/reference/extended-route-types")
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
                .description("The color (in hexadecimal format) the agency operating this route would prefer to use on UI elements (e.g. polylines on a map) related to this route. This value is not available for most routes.")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("textColor")
                .description("The color (in hexadecimal format) the agency operating this route would prefer to use when displaying text related to this route. This value is not available for most routes.")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("bikesAllowed")
                .type(bikesAllowedEnum)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("patterns")
		.description("List of patterns which operate on this route")
                .type(new GraphQLList(patternType))
                .dataFetcher(environment -> index.patternsForRoute
                    .get(environment.getSource()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stops")
		.description("List of stops on this route")
                .type(new GraphQLList(stopType))
                .dataFetcher(environment -> index.patternsForRoute
                    .get(environment.getSource())
                    .stream()
                    .map(TripPattern::getStops)
                    .flatMap(Collection::stream)
                    .distinct()
                    .collect(Collectors.toList()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("trips")
		.description("List of trips which operate on this route")
                .type(new GraphQLList(tripType))
                .dataFetcher(environment -> index.patternsForRoute
                    .get(environment.getSource())
                    .stream()
                    .map(TripPattern::getTrips)
                    .flatMap(Collection::stream)
                    .distinct()
                    .collect(Collectors.toList()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("alerts")
                .description("List of alerts which have an effect on the route")
                .type(new GraphQLList(alertType))
                .dataFetcher(dataFetchingEnvironment -> index.getAlertsForRoute(
                    dataFetchingEnvironment.getSource()))
                .build())
            .build();

        feedType = GraphQLObjectType.newObject()
            .name("Feed")
            .description("A feed provides routing data (stops, routes, timetables, etc.) from one or more public transport agencies.")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("feedId")
                    .description("ID of the feed")
                    .type(new GraphQLNonNull(Scalars.GraphQLString))
                    .dataFetcher(environment -> environment.getSource())
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("agencies")
                    .description("List of agencies which provide data to this feed")
                    .type(new GraphQLList(agencyType))
                    .dataFetcher(environment -> index.graph.getAgencies(environment.getSource()))
                    .build())
            .build();

        agencyType = GraphQLObjectType.newObject()
            .name("Agency")
            .description("A public transport agency")
            .withInterface(nodeInterface)
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("id")
                .description("Global object ID provided by Relay. This value can be used to refetch this object using **node** query.")
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
                .description("Name of the agency")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("url")
                .description("URL to the home page of the agency")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("timezone")
                .description("ID of the time zone which this agency operates on")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("lang")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("phone")
                .description("Phone number which customers can use to contact this agency")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("fareUrl")
                .description("URL to a web page which has information of fares used by this agency")
                .type(Scalars.GraphQLString)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("routes")
                .description("List of routes operated by this agency")
                .type(new GraphQLList(routeType))
                .dataFetcher(environment -> index.routeForId.values()
                    .stream()
                    .filter(route -> route.getAgency() == environment.getSource())
                    .collect(Collectors.toList()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("alerts")
                .description("List of alerts which have an effect on all operations of the agency (e.g. a strike)")
                .type(new GraphQLList(alertType))
                .dataFetcher(dataFetchingEnvironment -> index.getAlertsForAgency(
                    dataFetchingEnvironment.getSource()))
                .build())
            .build();

        bikeRentalStationType = GraphQLObjectType.newObject()
            .name("BikeRentalStation")
            .description("Bike rental station represents a location where users can rent bicycles for a fee.")
            .withInterface(nodeInterface)
            .withInterface(placeInterface)
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("id")
                .description("Global object ID provided by Relay. This value can be used to refetch this object using **node** query.")
                .type(new GraphQLNonNull(Scalars.GraphQLID))
                .dataFetcher(environment -> relay
                    .toGlobalId(bikeRentalStationType.getName(), ((BikeRentalStation) environment.getSource()).id))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stationId")
                .description("ID of the bike rental station")
                .type(Scalars.GraphQLString)
                .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).id)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("name")
                .description("Name of the bike rental station")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).getName())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("bikesAvailable")
		        .description("Number of bikes currently available on the rental station. The total capacity of this bike rental station is the sum of fields `bikesAvailable` and `spacesAvailable`.")
                .type(Scalars.GraphQLInt)
                .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).bikesAvailable)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("spacesAvailable")
		        .description("Number of free spaces currently available on the rental station. The total capacity of this bike rental station is the sum of fields `bikesAvailable` and `spacesAvailable`.  \n Note that this value being 0 does not necessarily indicate that bikes cannot be returned to this station, as it might be possible to leave the bike in the vicinity of the rental station, even if the bike racks don't have any spaces available (see field `allowDropoff`).")
                .type(Scalars.GraphQLInt)
                .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).spacesAvailable)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("state")
		        .description("A description of the current state of this bike rental station, e.g. \"Station on\"")
                .type(Scalars.GraphQLString)
                .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).state)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("realtime")
		        .description("If true, values of `bikesAvailable` and `spacesAvailable` are updated from a real-time source. If false, values of `bikesAvailable` and `spacesAvailable` are always the total capacity divided by two.")
                .type(Scalars.GraphQLBoolean)
                .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).realTimeData)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("allowDropoff")
                .description("If true, bikes can be returned to this station.")
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
                .description("Longitude of the bike rental station (WGS 84)")
                .type(Scalars.GraphQLFloat)
                .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).x)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("lat")
                .description("Latitude of the bike rental station (WGS 84)")
                .type(Scalars.GraphQLFloat)
                .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).y)
                .build())
            .build();

        bikeParkType = GraphQLObjectType.newObject()
            .name("BikePark")
            .description("Bike park represents a location where bicycles can be parked.")
            .withInterface(nodeInterface)
            .withInterface(placeInterface)
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("id")
                .description("Global object ID provided by Relay. This value can be used to refetch this object using **node** query.")
                .type(new GraphQLNonNull(Scalars.GraphQLID))
                .dataFetcher(environment -> relay
                    .toGlobalId(bikeParkType.getName(), ((BikePark) environment.getSource()).id))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("bikeParkId")
                .description("ID of the bike park")
                .type(Scalars.GraphQLString)
                .dataFetcher(environment -> ((BikePark) environment.getSource()).id)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("name")
                .description("Name of the bike park")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .dataFetcher(environment -> ((BikePark) environment.getSource()).name)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("spacesAvailable")
		        .description("Number of spaces available for bikes")
                .type(Scalars.GraphQLInt)
                .dataFetcher(environment -> ((BikePark) environment.getSource()).spacesAvailable)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("realtime")
		        .description("If true, value of `spacesAvailable` is updated from a real-time source.")
                .type(Scalars.GraphQLBoolean)
                .dataFetcher(environment -> ((BikePark) environment.getSource()).realTimeData)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("lon")
                .description("Longitude of the bike park (WGS 84)")
                .type(Scalars.GraphQLFloat)
                .dataFetcher(environment -> ((BikePark) environment.getSource()).x)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("lat")
                .description("Latitude of the bike park (WGS 84)")
                .type(Scalars.GraphQLFloat)
                .dataFetcher(environment -> ((BikePark) environment.getSource()).y)
                .build())
            .build();

        ticketType = GraphQLObjectType.newObject()
            .name("TicketType")
            .description(experimental("Describes ticket type"))
            .withInterface(nodeInterface)
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("id")
                .description("Global object ID provided by Relay. This value can be used to refetch this object using **node** query.")
                .type(new GraphQLNonNull(Scalars.GraphQLID))
                .dataFetcher(environment -> relay
                        .toGlobalId(ticketType.getName(), ((TicketType) environment.getSource()).getId()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("fareId")
                    .type(new GraphQLNonNull(Scalars.GraphQLID))
                    .dataFetcher(environment ->  ((TicketType) environment.getSource()).getId())
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("price")
                .type(Scalars.GraphQLFloat)
                .dataFetcher(environment -> ((TicketType) environment.getSource()).getPrice())
                .build()
            )
            .build();


        carParkType = GraphQLObjectType.newObject()
            .name("CarPark")
            .description("Car park represents a location where cars can be parked.")
            .withInterface(nodeInterface)
            .withInterface(placeInterface)
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("id")
                .description("Global object ID provided by Relay. This value can be used to refetch this object using **node** query.")
                .type(new GraphQLNonNull(Scalars.GraphQLID))
                .dataFetcher(environment -> relay
                    .toGlobalId(carParkType.getName(), ((CarPark) environment.getSource()).id))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("carParkId")
                .description("ID of the car park")
                .type(Scalars.GraphQLString)
                .dataFetcher(environment -> ((CarPark) environment.getSource()).id)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("name")
                .description("Name of the car park")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .dataFetcher(environment -> ((CarPark) environment.getSource()).name)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("maxCapacity")
		        .description("Number of parking spaces at the car park")
                .type(Scalars.GraphQLInt)
                .dataFetcher(environment -> ((CarPark) environment.getSource()).maxCapacity)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("spacesAvailable")
		        .description("Number of currently available parking spaces at the car park")
                .type(Scalars.GraphQLInt)
                .dataFetcher(environment -> ((CarPark) environment.getSource()).spacesAvailable)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("realtime")
		        .description("If true, value of `spacesAvailable` is updated from a real-time source.")
                .type(Scalars.GraphQLBoolean)
                .dataFetcher(environment -> ((CarPark) environment.getSource()).realTimeData)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("lon")
                .description("Longitude of the car park (WGS 84)")
                .type(Scalars.GraphQLFloat)
                .dataFetcher(environment -> ((CarPark) environment.getSource()).x)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("lat")
                .description("Latitude of the car park (WGS 84)")
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
                        index.stopForId.get(FeedScopedId.convertFromString(parts[1])),
                        Integer.parseInt(parts[0], 10));
                }
                if (id.type.equals(stopType.getName())) {
                    Stop matchedStop = index.stopForId.get(FeedScopedId.convertFromString(id.id));
                    if (matchedStop == null) {
                        matchedStop = index.stationForId.get(FeedScopedId.convertFromString(id.id));
                    }

                    return matchedStop;
                }
                if (id.type.equals(tripType.getName())) {
                    return index.tripForId.get(FeedScopedId.convertFromString(id.id));
                }
                if (id.type.equals(routeType.getName())) {
                    return index.routeForId.get(FeedScopedId.convertFromString(id.id));
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
                    BikeRentalStationService service = index.graph.getService(BikeRentalStationService.class);
                    if (service==null)
                        return null;

                    return service.getBikeRentalStations()
                        .stream()
                        .filter(bikeRentalStation -> bikeRentalStation.id.equals(id.id))
                        .findFirst()
                        .orElse(null);
                }
                if (id.type.equals(bikeParkType.getName())) {
                    // No index exists for bike parking ids
                    BikeRentalStationService service = index.graph.getService(BikeRentalStationService.class);
                    if (service==null)
                        return null;

                    return service.getBikeParks()
                        .stream()
                        .filter(bikePark -> bikePark.id.equals(id.id))
                        .findFirst()
                        .orElse(null);
                }
                if (id.type.equals(carParkType.getName())) {
                    // No index exists for car parking ids
                    CarParkService service = index.graph.getService(CarParkService.class);
                    if (service==null)
                        return null;

                    return service.getCarParks()
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
                .name("feeds")
                .description("Get all available feeds")
                .type(new GraphQLList(feedType))
                .dataFetcher(environment -> index.graph.getFeedIds())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("agencies")
                .description("Get all agencies")
                .type(new GraphQLList(agencyType))
                .dataFetcher(environment -> new ArrayList<>(index.getAllAgencies()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("ticketTypes")
                    .description(experimental("Return list of available ticket types."))
                    .type(new GraphQLList(ticketType))
                    .dataFetcher(environment -> new ArrayList<>(index.getAllTicketTypes()))
                    .build()
                    )
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
                .description("Get all stops")
                .type(new GraphQLList(stopType))
                .argument(GraphQLArgument.newArgument()
                    .name("ids")
                    .description("Return stops with these ids")
                    .type(new GraphQLList(Scalars.GraphQLString))
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("name")
                    .description("Query stops by this name")
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
                            .map(id -> index.stopForId.get(FeedScopedId.convertFromString(id)))
                            .collect(Collectors.toList());
                    }
                    Stream<Stop> stream;
                    if (environment.getArgument("name") == null) {
                        stream = index.stopForId.values().stream();
                    }
                    else {
                        stream = index.getLuceneIndex().query(environment.getArgument("name"), true, true, false, false)
                            .stream()
                            .map(result -> index.stopForId.get(FeedScopedId.convertFromString(result.id)));
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
		    .description("Southern bound of the bounding box")
                    .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("minLon")
	 	    .description("Western bound of the bounding box")
                    .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("maxLat")
		    .description("Northern bound of the bounding box")
                    .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("maxLon")
		    .description("Eastern bound of the bounding box")
                    .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("agency")
                    .description("Deprecated, use argument `feeds` instead")
                    .type(Scalars.GraphQLString)
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("feeds")
                    .description("List of feed ids from which stops are returned")
                    .type(new GraphQLList(new GraphQLNonNull(Scalars.GraphQLString)))
                    .build())
                .dataFetcher(environment -> index.graph.streetIndex
                    .getTransitStopForEnvelope(new Envelope(
                        new Coordinate(environment.getArgument("minLon"),
                            environment.getArgument("minLat")),
                        new Coordinate(environment.getArgument("maxLon"),
                            environment.getArgument("maxLat"))))
                    .stream()
                    .map(TransitVertex::getStop)
                    .filter(stop -> (environment.getArgument("agency") == null && environment.getArgument("feeds") == null) ||
                        stop.getId().getAgencyId().equalsIgnoreCase(environment.getArgument("agency")) || (environment.getArgument("feeds") instanceof List && ((List)environment.getArgument("feeds")).contains(stop.getId().getAgencyId()))
                    )
                    .collect(Collectors.toList()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stopsByRadius")
                .description("Get all stops within the specified radius from a location. The returned type is a Relay connection (see https://facebook.github.io/relay/graphql/connections.htm). The stopAtDistance type has two values: stop and distance.")
                .type(relay.connectionType("stopAtDistance",
                    relay.edgeType("stopAtDistance", stopAtDistanceType, null, new ArrayList<>()),
                    new ArrayList<>()))
                .argument(GraphQLArgument.newArgument()
                    .name("lat")
                    .description("Latitude of the location (WGS 84)")
                    .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("lon")
                    .description("Longitude of the location (WGS 84)")
                    .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("radius")
                    .description("Radius (in meters) to search for from the specified location. Note that this is walking distance along streets and paths rather than a geographic distance.")
                    .type(new GraphQLNonNull(Scalars.GraphQLInt))
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("agency")
                    .description("Deprecated, use argument `feeds` instead")
                    .type(Scalars.GraphQLString)
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("feeds")
                    .description("List of feed ids from which stops are returned")
                    .type(new GraphQLList(new GraphQLNonNull(Scalars.GraphQLString)))
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
                            .filter(stopAndDistance -> (environment.getArgument("agency") == null && environment.getArgument("feeds") == null) ||
                                stopAndDistance.stop.getId().getAgencyId().equalsIgnoreCase(environment.getArgument("agency")) || (environment.getArgument("feeds") instanceof List && ((List)environment.getArgument("feeds")).contains(stopAndDistance.stop.getId().getAgencyId()))
                            )
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
                    "Get all places (stops, stations, etc. with coordinates) within the specified radius from a location. The returned type is a Relay connection (see https://facebook.github.io/relay/graphql/connections.htm). The placeAtDistance type has two fields: place and distance. The search is done by walking so the distance is according to the network of walkable streets and paths.")
                .type(relay.connectionType("placeAtDistance",
                    relay.edgeType("placeAtDistance", placeAtDistanceType, null, new ArrayList<>()),
                    new ArrayList<>()))
                .argument(GraphQLArgument.newArgument()
                    .name("lat")
                    .description("Latitude of the location (WGS 84)")
                    .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("lon")
                    .description("Longitude of the location (WGS 84)")
                    .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("maxDistance")
                    .description("Maximum distance (in meters) to search for from the specified location. Note that this is walking distance along streets and paths rather than a geographic distance. Default is 2000m")
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
                    .description("Only return places that are one of these types, e.g. `STOP` or `BICYCLE_RENT`")
                    .type(new GraphQLList(filterPlaceTypeEnum))
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("filterByModes")
                    .description("Only return places that are related to one of these transport modes. This argument can be used to return e.g. only nearest railway stations or only nearest places related to bicycling.")
                    .type(new GraphQLList(modeEnum))
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("filterByIds")
                    .description("Only include places that match one of the given GTFS ids.")
                    .type(filterInputType)
                    .build())
                .argument(relay.getConnectionFieldArguments())
                .dataFetcher(environment -> {
                    List<FeedScopedId> filterByStops = null;
                    List<FeedScopedId> filterByRoutes = null;
                    List<String> filterByBikeRentalStations = null;
                    List<String> filterByBikeParks = null;
                    List<String> filterByCarParks = null;
                    @SuppressWarnings("rawtypes")
                    Map filterByIds = environment.getArgument("filterByIds");
                    if (filterByIds != null) {
                        filterByStops = toIdList(((List<String>) filterByIds.get("stops")));
                        filterByRoutes = toIdList(((List<String>) filterByIds.get("routes")));
                        filterByBikeRentalStations = filterByIds.get("bikeRentalStations") != null ? (List<String>) filterByIds.get("bikeRentalStations") : Collections.emptyList();
                        filterByBikeParks = filterByIds.get("bikeParks") != null ? (List<String>) filterByIds.get("bikeParks") : Collections.emptyList();
                        filterByCarParks = filterByIds.get("carParks") != null ? (List<String>) filterByIds.get("carParks") : Collections.emptyList();
                    }

                    List<TraverseMode> filterByModes = environment.getArgument("filterByModes");
                    List<GraphIndex.PlaceType> filterByPlaceTypes = environment.getArgument("filterByPlaceTypes");

                    List<PlaceAndDistance> places;
                    try {
                        places = new ArrayList<>(index.findClosestPlacesByWalking(
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
                        ));
                    } catch (VertexNotFoundException e) {
                        places = Collections.emptyList();
                    }

                    return new SimpleListConnection(places).get(environment);
                })
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("departureRow")
                .description("Get a single departure row based on its ID (ID format is `FeedId:StopId:PatternId`)")
                .type(departureRowType)
                .argument(GraphQLArgument.newArgument()
                    .name("id")
                    .type(new GraphQLNonNull(Scalars.GraphQLString))
                    .build())
                .dataFetcher(environment -> GraphIndex.DepartureRow.fromId(index, environment.getArgument("id")))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stop")
                .description("Get a single stop based on its ID, i.e. value of field `gtfsId` (ID format is `FeedId:StopId`)")
                .type(stopType)
                .argument(GraphQLArgument.newArgument()
                    .name("id")
                    .type(new GraphQLNonNull(Scalars.GraphQLString))
                    .build())
                .dataFetcher(environment -> index.stopForId
                    .get(FeedScopedId.convertFromString(environment.getArgument("id"))))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("station")
                .description("Get a single station based on its ID, i.e. value of field `gtfsId` (format is `FeedId:StopId`)")
                .type(stopType)
                .argument(GraphQLArgument.newArgument()
                    .name("id")
                    .type(new GraphQLNonNull(Scalars.GraphQLString))
                    .build())
                .dataFetcher(environment -> index.stationForId
                    .get(FeedScopedId.convertFromString(environment.getArgument("id"))))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stations")
                .description("Get all stations")
                .type(new GraphQLList(stopType))
                .argument(GraphQLArgument.newArgument()
                    .name("ids")
                    .description("Only return stations that match one of the ids in this list")
                    .type(new GraphQLList(Scalars.GraphQLString))
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("name")
                    .description("Query stations by name")
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
                                .map(id -> index.stationForId.get(FeedScopedId.convertFromString(id)))
                                .collect(Collectors.toList());
                    }

                    Stream<Stop> stream;
                    if (environment.getArgument("name") == null) {
                        stream = index.stationForId.values().stream();
                    } else {
                        stream = index.getLuceneIndex().query(environment.getArgument("name"), true, false, true, false, false)
                            .stream()
                            .map(result -> index.stationForId.get(FeedScopedId.convertFromString(result.id)));
                    }

                    return stream.collect(Collectors.toList());
                })
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("routes")
                .description("Get all routes")
                .type(new GraphQLList(routeType))
                .argument(GraphQLArgument.newArgument()
                    .name("ids")
                    .description("Only return routes with these ids")
                    .type(new GraphQLList(Scalars.GraphQLString))
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("name")
                    .description("Query routes by this name")
                    .type(Scalars.GraphQLString)
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("modes")
                    .description("Deprecated, use argument `transportModes` instead.")
                    .type(Scalars.GraphQLString)
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("transportModes")
                    .description("Only include routes, which use one of these modes")
                    .type(GraphQLList.list(modeEnum))
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
                            .map(id -> index.routeForId.get(FeedScopedId.convertFromString(id)))
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
                    if (environment.getArgument("modes") != null && !(environment.getArgument("transportModes") instanceof List)) {
                        Set<TraverseMode> modes = new QualifiedModeSet((String)
                            environment.getArgument("modes")).qModes
                            .stream()
                            .map(qualifiedMode -> qualifiedMode.mode)
                            .filter(TraverseMode::isTransit)
                            .collect(Collectors.toSet());
                        stream = stream
                            .filter(route ->
                                modes.contains(GtfsLibrary.getTraverseMode(route)));
                    }
                    if (environment.getArgument("transportModes") instanceof List) {
                        List<TraverseMode> modes = environment.getArgument("transportModes");
                        stream = stream.filter(route -> modes.contains(GtfsLibrary.getTraverseMode(route)));
                    }

                    return stream.collect(Collectors.toList());
                })
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("route")
                .description("Get a single route based on its ID, i.e. value of field `gtfsId` (format is `FeedId:RouteId`)")
                .type(routeType)
                .argument(GraphQLArgument.newArgument()
                    .name("id")
                    .type(new GraphQLNonNull(Scalars.GraphQLString))
                    .build())
                .dataFetcher(environment -> index.routeForId
                    .get(FeedScopedId.convertFromString(environment.getArgument("id"))))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("trips")
                .description("Get all trips")
                .type(new GraphQLList(tripType))
                .dataFetcher(environment -> new ArrayList<>(index.tripForId.values()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("trip")
                .description("Get a single trip based on its ID, i.e. value of field `gtfsId` (format is `FeedId:TripId`)")
                .type(tripType)
                .argument(GraphQLArgument.newArgument()
                    .name("id")
                    .type(new GraphQLNonNull(Scalars.GraphQLString))
                    .build())
                .dataFetcher(environment -> index.tripForId
                    .get(FeedScopedId.convertFromString(environment.getArgument("id"))))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("fuzzyTrip")
                .description("Finds a trip matching the given parameters. This query type is useful if the id of a trip is not known, but other details uniquely identifying the trip are available from some source (e.g. MQTT vehicle positions).")
                .type(tripType)
                .argument(GraphQLArgument.newArgument()
                    .name("route")
                    .description("id of the route")
                    .type(new GraphQLNonNull(Scalars.GraphQLString))
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("direction")
                    .description("Direction of the trip, possible values: 0, 1 or -1.  \n -1 indicates that the direction is irrelevant, i.e. in case the route has trips only in one direction. See field `directionId` of Pattern.")
                    .type(Scalars.GraphQLInt)
                    .defaultValue(-1)
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("date")
                    .description("Departure date of the trip, format: YYYY-MM-DD")
                    .type(new GraphQLNonNull(Scalars.GraphQLString))
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("time")
                    .description("Departure time of the trip, format: seconds since midnight of the departure date")
                    .type(new GraphQLNonNull(Scalars.GraphQLInt))
                    .build())
                .dataFetcher(environment -> {
                    try {
                        return fuzzyTripMatcher.getTrip(
                            index.routeForId.get(
                                FeedScopedId.convertFromString(environment.getArgument("route"))),
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
                .description("Get all patterns")
                .type(new GraphQLList(patternType))
                .dataFetcher(environment -> new ArrayList<>(index.patternForId.values()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("pattern")
                .description("Get a single pattern based on its ID, i.e. value of field `code` (format is `FeedId:RouteId:DirectionId:PatternVariantNumber`)")
                .type(patternType)
                .argument(GraphQLArgument.newArgument()
                    .name("id")
                    .type(new GraphQLNonNull(Scalars.GraphQLString))
                    .build())
                .dataFetcher(environment -> index.patternForId.get(environment.getArgument("id")))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("clusters")
                .description("Get all clusters")
                .type(new GraphQLList(clusterType))
                .dataFetcher(environment -> new ArrayList<>(index.stopClusterForId.values()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("cluster")
                .description("Get a single cluster based on its ID, i.e. value of field `gtfsId`")
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
                .description("Get all active alerts")
                .type(new GraphQLList(alertType))
                .argument(GraphQLArgument.newArgument()
                    .name("feeds")
		    .description("Only return alerts in these feeds")
                    .type(new GraphQLList(new GraphQLNonNull(Scalars.GraphQLString)))
                    .build())
                .dataFetcher(environment -> environment.getArgument("feeds") != null
                    ? index.getAlerts()
                        .stream()
                        .filter(alertPatch ->
                            ((List) environment.getArgument("feeds"))
                                .contains(alertPatch.getFeedId())
                        )
                        .collect(Collectors.toList())
                    : index.getAlerts()
                )
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("serviceTimeRange")
                .description("Get the time range for which the API has data available")
                .type(serviceTimeRangeType)
                .dataFetcher(environment -> index.graph)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("bikeRentalStations")
		.description("Get all bike rental stations")
                .type(new GraphQLList(bikeRentalStationType))
                .dataFetcher(dataFetchingEnvironment -> new ArrayList<>(
                        index.graph.getService(BikeRentalStationService.class) != null
                          ? index.graph.getService(BikeRentalStationService.class).getBikeRentalStations()
                          : Collections.EMPTY_LIST))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("bikeRentalStation")
		.description("Get a single bike rental station based on its ID, i.e. value of field `stationId`")
                .type(bikeRentalStationType)
                .argument(GraphQLArgument.newArgument()
                    .name("id")
                    .type(new GraphQLNonNull(Scalars.GraphQLString))
                    .build())
                .dataFetcher(environment -> new ArrayList<BikeRentalStation>(
                        index.graph.getService(BikeRentalStationService.class) != null
                          ? index.graph.getService(BikeRentalStationService.class).getBikeRentalStations()
                          : Collections.EMPTY_LIST)
                    .stream()
                    .filter(bikeRentalStation -> bikeRentalStation.id.equals(environment.getArgument("id")))
                    .findFirst()
                    .orElse(null))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("bikeParks")
		.description("Get all bike parks")
                .type(new GraphQLList(bikeParkType))
                .dataFetcher(dataFetchingEnvironment -> new ArrayList<>(
                        index.graph.getService(BikeRentalStationService.class) != null
                          ? index.graph.getService(BikeRentalStationService.class).getBikeParks()
                          : Collections.EMPTY_LIST))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("bikePark")
		.description("Get a single bike park based on its ID, i.e. value of field `bikeParkId`")
                .type(bikeParkType)
                .argument(GraphQLArgument.newArgument()
                    .name("id")
                    .type(new GraphQLNonNull(Scalars.GraphQLString))
                    .build())
                .dataFetcher(environment -> new ArrayList<BikePark>(
                        index.graph.getService(BikeRentalStationService.class) != null
                          ? index.graph.getService(BikeRentalStationService.class).getBikeParks()
                          : Collections.EMPTY_LIST)
                    .stream()
                    .filter(bikePark -> bikePark.id.equals(environment.getArgument("id")))
                    .findFirst()
                    .orElse(null))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("carParks")
		.description("Get all car parks")
                .type(new GraphQLList(carParkType))
                .argument(GraphQLArgument.newArgument()
                    .name("ids")
                    .description("Return car parks with these ids.  \n **Note:** if an id is invalid (or the car park service is unavailable) the returned list will contain `null` values.")
                    .type(new GraphQLList(Scalars.GraphQLString))
                    .build())
                .dataFetcher(environment -> {
                    if ((environment.getArgument("ids") instanceof List)) {
                        Map<String, CarPark> carParks = index.graph.getService(CarParkService.class) != null
                                ? index.graph.getService(CarParkService.class).getCarParkById()
                                : Collections.EMPTY_MAP;
                        return ((List<String>) environment.getArgument("ids"))
                            .stream()
                            .map(carParks::get)
                            .collect(Collectors.toList());
                    }
                    return new ArrayList<>(index.graph.getService(CarParkService.class) != null
                      ? index.graph.getService(CarParkService.class).getCarParks()
                      : Collections.EMPTY_LIST);
                })
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("carPark")
		.description("Get a single car park based on its ID, i.e. value of field `carParkId`")
                .type(carParkType)
                .argument(GraphQLArgument.newArgument()
                    .name("id")
                    .type(new GraphQLNonNull(Scalars.GraphQLString))
                    .build())
                .dataFetcher(environment -> new ArrayList<CarPark>(
                        index.graph.getService(CarParkService.class) != null
                          ? index.graph.getService(CarParkService.class).getCarParks()
                          : Collections.EMPTY_LIST)
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

        Set<GraphQLType> dictionary = new HashSet<>();
        dictionary.add(placeInterface);

        indexSchema = GraphQLSchema.newSchema()
            .query(queryType)
            .build(dictionary);
    }

    private List<FeedScopedId> toIdList(List<String> ids) {
        if (ids == null) return Collections.emptyList();
        return ids.stream().map(FeedScopedId::convertFromString).collect(Collectors.toList());
    }

    private void createPlanType(GraphIndex index) {
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
                .description("Latitude of the place (WGS 84)")
                .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                .dataFetcher(environment -> ((Place)environment.getSource()).lat)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("lon")
                .description("Longitude of the place (WGS 84)")
                .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                .dataFetcher(environment -> ((Place)environment.getSource()).lon)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("arrivalTime")
                .description("The time the rider will arrive at the place. Format: Unix timestamp in milliseconds.")
                .type(new GraphQLNonNull(Scalars.GraphQLLong))
                .dataFetcher(environment -> ((Place)environment.getSource()).arrival.getTime().getTime())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("departureTime")
                .description("The time the rider will depart the place. Format: Unix timestamp in milliseconds.")
                .type(new GraphQLNonNull(Scalars.GraphQLLong))
                .dataFetcher(environment -> ((Place)environment.getSource()).departure.getTime().getTime())
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
                    new ArrayList<>(
                            index.graph.getService(BikeRentalStationService.class) != null
                              ? index.graph.getService(BikeRentalStationService.class).getBikeRentalStations()
                              : Collections.emptyList())
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
                    new ArrayList<>(
                            index.graph.getService(BikeRentalStationService.class) != null
                              ? index.graph.getService(BikeRentalStationService.class).getBikeParks()
                              : Collections.emptyList())
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
                    new ArrayList<>(
                            index.graph.getService(CarParkService.class) != null
                              ? index.graph.getService(CarParkService.class).getCarParks()
                              : Collections.emptyList())
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
                .description("The date and time when this leg begins. Format: Unix timestamp in milliseconds.")
                .type(Scalars.GraphQLLong)
                .dataFetcher(environment -> ((Leg)environment.getSource()).startTime.getTime().getTime())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("endTime")
                .description("The date and time when this leg ends. Format: Unix timestamp in milliseconds.")
                .type(Scalars.GraphQLLong)
                .dataFetcher(environment -> ((Leg)environment.getSource()).endTime.getTime().getTime())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("departureDelay")
                .description("For transit leg, the offset from the scheduled departure time of the boarding stop in this leg, i.e. scheduled time of departure at boarding stop = `startTime - departureDelay`")
                .type(Scalars.GraphQLInt)
                .dataFetcher(environment -> ((Leg)environment.getSource()).departureDelay)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("arrivalDelay")
                .description("For transit leg, the offset from the scheduled arrival time of the alighting stop in this leg, i.e. scheduled time of arrival at alighting stop = `endTime - arrivalDelay`")
                .type(Scalars.GraphQLInt)
                .dataFetcher(environment -> ((Leg)environment.getSource()).arrivalDelay)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("mode")
                .description("The mode (e.g. `WALK`) used when traversing this leg.")
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
                .type(geometryType)
                .dataFetcher(environment -> ((Leg)environment.getSource()).legGeometry)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("agency")
                .description("For transit legs, the transit agency that operates the service used for this leg. For non-transit legs, `null`.")
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
                .name("realtimeState")
                .description("State of real-time data")
                .type(realtimeStateEnum)
                .dataFetcher(environment -> ((Leg)environment.getSource()).realTimeState)
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
                .description("Whether this leg is traversed with a rented bike.")
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
                .description("For transit legs, the route that is used for traversing the leg. For non-transit legs, `null`.")
                .type(routeType)
                .dataFetcher(environment -> index.routeForId.get(((Leg)environment.getSource()).routeId))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("trip")
                .description("For transit legs, the trip that is used for traversing the leg. For non-transit legs, `null`.")
                .type(tripType)
                .dataFetcher(environment -> index.tripForId.get(((Leg)environment.getSource()).tripId))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("serviceDate")
                .description("For transit legs, the service date of the trip. Format: YYYYMMDD. For non-transit legs, null.")
                .type(Scalars.GraphQLString)
                .dataFetcher(environment -> ((Leg)environment.getSource()).serviceDate)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("intermediateStops")
                .description("For transit legs, intermediate stops between the Place where the leg originates and the Place where the leg ends. For non-transit legs, null.")
                .type(new GraphQLList(stopType))
                .dataFetcher(environment -> ((Leg)environment.getSource()).stop.stream()
                    .filter(place -> place.stopId != null)
                    .map(placeWithStop -> index.stopForId.get(placeWithStop.stopId))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()))
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("intermediatePlaces")
                .description("For transit legs, intermediate stops between the Place where the leg originates and the Place where the leg ends. For non-transit legs, null. Returns Place type, which has fields for e.g. departure and arrival times")
                .type(new GraphQLList(placeType))
                .dataFetcher(environment -> ((Leg)environment.getSource()).stop)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("intermediatePlace")
                .description("Whether the destination of this leg (field `to`) is one of the intermediate places specified in the query.")
                .type(Scalars.GraphQLBoolean)
                .dataFetcher(environment -> ((Leg) environment.getSource()).intermediatePlace)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("steps")
                .type(new GraphQLList(GraphQLObjectType.newObject()
                    .name("step")
                    .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("distance")
                        .description("The distance in meters that this step takes.")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(env -> ((WalkStep)env.getSource()).distance)
                        .build())
                    .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("lon")
                        .description("The longitude of the start of the step.")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(env -> ((WalkStep)env.getSource()).lon)
                        .build())
                    .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("lat")
                        .description("The latitude of the start of the step.")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(env -> ((WalkStep)env.getSource()).lat)
                        .build())
                    .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("elevationProfile")
                        .description("The elevation profile as a list of { distance, elevation } values.")
                        .type(new GraphQLList(GraphQLObjectType.newObject()
                            .name("elevationProfileComponent")
                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                .name("distance")
                                .description("The distance from the start of the step, in meters.")
                                .type(Scalars.GraphQLFloat)
                                .dataFetcher(env -> ((P2<Double>)env.getSource()).first)
                                .build())
                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                .name("elevation")
                                .description("The elevation at this distance, in meters.")
                                .type(Scalars.GraphQLFloat)
                                .dataFetcher(env -> ((P2<Double>)env.getSource()).second)
                                .build())
                            .build()))
                        .dataFetcher(env -> ((WalkStep)env.getSource()).elevation)
                        .build())
                    .build()))
                .dataFetcher(new PropertyDataFetcher("walkSteps"))
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
		.description("ISO 4217 currency code")
                .type(Scalars.GraphQLString)
                .dataFetcher(environment -> ((Money)((Map<String, Object>) environment.getSource()).get("fare")).getCurrency().getCurrencyCode())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("cents")
		.description("Fare price in cents. **Note:** this value is dependent on the currency used, as one cent is not necessarily ¹/₁₀₀ of the basic monerary unit.")
                .type(Scalars.GraphQLInt)
                .dataFetcher(environment -> ((Money)((Map<String, Object>) environment.getSource()).get("fare")).getCents())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("components")
		.description("Components which this fare is composed of")
                .type(new GraphQLList(GraphQLObjectType.newObject()
                    .name("fareComponent")
		    .description("Component of the fare (i.e. ticket) for a part of the itinerary")
                    .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("fareId")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(environment -> GtfsLibrary
                            .convertIdToString(((FareComponent) environment.getSource()).fareId))
                        .build())
                    .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("currency")
			.description("ISO 4217 currency code")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(environment -> ((FareComponent) environment.getSource()).price.getCurrency().getCurrencyCode())
                        .build())
                    .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("cents")
			.description("Fare price in cents. **Note:** this value is dependent on the currency used, as one cent is not necessarily ¹/₁₀₀ of the basic monerary unit.")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(environment -> ((FareComponent) environment.getSource()).price.getCents())
                        .build())
                    .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("routes")
			.description("List of routes which use this fare component")
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
                .description("Time when the user leaves from the origin. Format: Unix timestamp in milliseconds.")
                .type(Scalars.GraphQLLong)
                .dataFetcher(environment -> ((Itinerary)environment.getSource()).startTime.getTime().getTime())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("endTime")
                .description("Time when the user arrives to the destination.. Format: Unix timestamp in milliseconds.")
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
                .description("A list of Legs. Each Leg is either a walking (cycling, car) portion of the itinerary, or a transit leg on a particular vehicle. So a itinerary where the user walks to the Q train, transfers to the 6, then walks to their destination, has four legs.")
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
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("elevationGained")
                .description("How much elevation is gained, in total, over the course of the itinerary, in meters.")
                .type(Scalars.GraphQLFloat)
                .dataFetcher(env -> ((Itinerary)env.getSource()).elevationGained)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("elevationLost")
                .description("How much elevation is lost, in total, over the course of the itinerary, in meters.")
                .type(Scalars.GraphQLFloat)
                .dataFetcher(env -> ((Itinerary)env.getSource()).elevationLost)
                .build())
            .build();

        planType = GraphQLObjectType.newObject()
            .name("Plan")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("date")
                .description("The time and date of travel. Format: Unix timestamp in milliseconds.")
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
                    .stream().map(Enum::name).collect(Collectors.toList()))
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
                    .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("pathCalculationTime")
                        .type(Scalars.GraphQLLong)
                        .build())
                    .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("precalculationTime")
                        .type(Scalars.GraphQLLong)
                        .build())
                    .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("renderingTime")
                        .type(Scalars.GraphQLLong)
                        .build())
                    .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("timedOut")
                        .type(Scalars.GraphQLBoolean)
                        .build())
                    .build()))
                .dataFetcher(environment -> (((Map)environment.getSource()).get("debugOutput")))
                .build())
            .build();
    }
}
