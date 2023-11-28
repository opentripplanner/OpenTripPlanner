package org.opentripplanner.standalone.config.routerequest;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_0;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_1;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_2;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_3;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_4;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_5;
import static org.opentripplanner.standalone.config.routerequest.ItineraryFiltersConfig.mapItineraryFilterParams;
import static org.opentripplanner.standalone.config.routerequest.TransferConfig.mapTransferPreferences;
import static org.opentripplanner.standalone.config.routerequest.VehicleRentalConfig.setVehicleRental;
import static org.opentripplanner.standalone.config.routerequest.WheelchairConfig.mapWheelchairPreferences;

import java.time.Duration;
import java.util.List;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;
import org.opentripplanner.routing.api.request.preference.BikePreferences;
import org.opentripplanner.routing.api.request.preference.CarPreferences;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.preference.StreetPreferences;
import org.opentripplanner.routing.api.request.preference.SystemPreferences;
import org.opentripplanner.routing.api.request.preference.TransitPreferences;
import org.opentripplanner.routing.api.request.preference.WalkPreferences;
import org.opentripplanner.routing.api.request.request.VehicleParkingRequest;
import org.opentripplanner.routing.api.request.request.filter.VehicleParkingFilter.TagsFilter;
import org.opentripplanner.routing.api.request.request.filter.VehicleParkingFilterRequest;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.sandbox.DataOverlayParametersMapper;
import org.opentripplanner.transit.model.basic.TransitMode;

public class RouteRequestConfig {

  private static final String WHEELCHAIR_ACCESSIBILITY = "wheelchairAccessibility";

  public static RouteRequest mapDefaultRouteRequest(String parameterName, NodeAdapter root) {
    var c = root
      .of(parameterName)
      .since(V2_0)
      .summary("The default parameters for the routing query.")
      .description("Most of these are overridable through the various API endpoints.")
      .asObject();
    return mapRouteRequest(c);
  }

  public static RouteRequest mapRouteRequest(NodeAdapter c) {
    return mapRouteRequest(c, new RouteRequest());
  }

  public static RouteRequest mapRouteRequest(NodeAdapter c, RouteRequest dft) {
    if (c.isEmpty()) {
      return dft;
    }

    RouteRequest request = dft.clone();
    VehicleParkingRequest vehicleParking = request.journey().parking();

    // Keep this alphabetically sorted so it is easy to check if a parameter is missing from the
    // mapping or duplicate exist.

    request.setArriveBy(
      c
        .of("arriveBy")
        .since(V2_0)
        .summary("Whether the trip should depart or arrive at the specified date and time.")
        .asBoolean(dft.arriveBy())
    );

    request.setLocale(c.of("locale").since(V2_0).summary("TODO").asLocale(dft.locale()));

    request
      .journey()
      .setModes(
        c
          .of("modes")
          .since(V2_0)
          .summary("The set of access/egress/direct/transit modes to be used for the route search.")
          .asCustomStringType(
            RequestModes.defaultRequestModes(),
            "TRANSIT,WALK",
            s -> new QualifiedModeSet(s).getRequestModes()
          )
      );

    request.setNumItineraries(
      c
        .of("numItineraries")
        .since(V2_0)
        .summary("The maximum number of itineraries to return.")
        .asInt(dft.numItineraries())
    );
    request.setSearchWindow(
      c
        .of("searchWindow")
        .since(V2_0)
        .summary("The duration of the search-window.")
        .description(
          """
  This is the time/duration in seconds from the earliest-departure-time(EDT) to the
  latest-departure-time(LDT). In case of a reverse search it will be the time from earliest to
  latest arrival time (LAT - EAT).

  All optimal travels that depart within the search window is guaranteed to be found.

  This is sometimes referred to as the Range Raptor Search Window - but could be used in a none
  Transit search as well; Hence this is named search-window and not raptor-search-window.

  This is normally dynamically calculated by the server. Use `null` to unset, and *zero* to do one
  Raptor iteration. The value is dynamically  assigned a suitable value, if not set. In a small to
  medium size operation you may use a fixed value, like 60 minutes. If you have a mixture of high
  frequency cities routes and  infrequent long distant journeys, the best option is normally to use
  the dynamic auto assignment. If not provided the value is resolved depending on the other input
  parameters, available transit options and realtime changes.

  There is no need to set this when going to the next/previous page. The OTP Server will
  increase/decrease the search-window when paging to match the requested number of itineraries.
  """
        )
        .asDuration(dft.searchWindow())
    );

    vehicleParking.setUnpreferredCost(
      c
        .of("unpreferredVehicleParkingTagCost")
        .since(V2_3)
        .summary("What cost to add if a parking facility doesn't contain a preferred tag.")
        .description("See `preferredVehicleParkingTags`.")
        .asInt(vehicleParking.unpreferredCost())
    );

    var bannedTags = c
      .of("bannedVehicleParkingTags")
      .since(V2_1)
      .summary("Tags with which a vehicle parking will not be used. If empty, no tags are banned.")
      .asStringSet(List.of());

    var requiredTags = c
      .of("requiredVehicleParkingTags")
      .since(V2_1)
      .summary(
        "Tags without which a vehicle parking will not be used. If empty, no tags are required."
      )
      .asStringSet(List.of());
    vehicleParking.setFilter(
      new VehicleParkingFilterRequest(new TagsFilter(bannedTags), new TagsFilter(requiredTags))
    );

    var preferredTags = c
      .of("preferredVehicleParkingTags")
      .since(V2_3)
      .summary(
        "Vehicle parking facilities that don't have one of these tags will receive an extra cost and will therefore be penalised."
      )
      .asStringSet(List.of());

    vehicleParking.setPreferred(
      new VehicleParkingFilterRequest(List.of(), List.of(new TagsFilter(preferredTags)))
    );

    request.setWheelchair(WheelchairConfig.wheelchairEnabled(c, WHEELCHAIR_ACCESSIBILITY));

    NodeAdapter unpreferred = c
      .of("unpreferred")
      .since(V2_2)
      .summary(
        "Parameters listing authorities or lines that preferably should not be used in trip patters."
      )
      .description(
        """
A cost is applied to boarding nonpreferred authorities or routes.

The routing engine will add extra penalty - on the *unpreferred* routes and/or agencies using a
cost function. The cost function (`unpreferredCost`) is defined as a linear function of the form
`A + B x`, where `A` is a fixed cost (in seconds) and `B` is reluctance multiplier for transit leg
travel time `x` (in seconds).
"""
      )
      .asObject();
    request
      .journey()
      .transit()
      .setUnpreferredRoutes(
        unpreferred
          .of("routes")
          .since(V2_2)
          .summary(
            "The ids of the routes that incur an extra cost when being used. Format: `FeedId:RouteId`"
          )
          .description("How much cost is added is configured in `unpreferredCost`.")
          .asFeedScopedIds(request.journey().transit().unpreferredRoutes())
      );

    request
      .journey()
      .transit()
      .setUnpreferredAgencies(
        unpreferred
          .of("agencies")
          .since(V2_2)
          .summary(
            "The ids of the agencies that incur an extra cost when being used. Format: `FeedId:AgencyId`"
          )
          .description("How much cost is added is configured in `unpreferredCost`.")
          .asFeedScopedIds(request.journey().transit().unpreferredAgencies())
      );

    TransitPriorityGroupConfig.mapTransitRequest(c, request.journey().transit());

    // Map preferences
    request.withPreferences(preferences -> mapPreferences(c, request, preferences));

    return request;
  }

  private static void mapPreferences(
    NodeAdapter c,
    RouteRequest request,
    RoutingPreferences.Builder preferences
  ) {
    preferences.withTransit(it -> mapTransitPreferences(c, it));
    preferences.withBike(it -> mapBikePreferences(c, it));
    preferences.withStreet(it -> mapStreetPreferences(c, it));
    preferences.withCar(it -> mapCarPreferences(c, it));
    preferences.withSystem(it -> mapSystemPreferences(c, it));
    preferences.withRental(it -> setVehicleRental(c, request, it));
    preferences.withTransfer(it -> mapTransferPreferences(c, it));
    preferences.withWalk(it -> mapWalkPreferences(c, it));
    preferences.withWheelchair(it -> mapWheelchairPreferences(c, it, WHEELCHAIR_ACCESSIBILITY));
    preferences.withItineraryFilter(it -> mapItineraryFilterParams("itineraryFilters", c, it));
  }

  private static void mapTransitPreferences(NodeAdapter c, TransitPreferences.Builder builder) {
    var dft = builder.original();
    builder
      .withAlightSlack(it ->
        it
          .withDefault(
            c
              .of("alightSlack")
              .since(V2_0)
              .summary("The minimum extra time after exiting a public transport vehicle.")
              .description(
                "The slack is added to the time when going from the transit vehicle to the stop."
              )
              .asDuration(dft.alightSlack().defaultValue())
          )
          .withValues(
            c
              .of("alightSlackForMode")
              .since(V2_0)
              .summary(
                "How much extra time should be given when alighting a vehicle for each given mode."
              )
              .description(
                "Sometimes there is a need to configure a longer alighting times for specific " +
                "modes, such as airplanes or ferries."
              )
              .asEnumMap(TransitMode.class, Duration.class)
          )
      )
      .withBoardSlack(it ->
        it
          .withDefault(
            c
              .of("boardSlack")
              .since(V2_0)
              .summary(
                "The boardSlack is the minimum extra time to board a public transport vehicle."
              )
              .description(
                """
The board time is added to the time when going from the stop (offboard) to onboard a transit
vehicle.

This is the same as the `transferSlack`, except that this also apply to to the first
transit leg in the trip. This is the default value used, if not overridden by the `boardSlackList`.
"""
              )
              .asDuration(dft.boardSlack().defaultValue())
          )
          .withValues(
            c
              .of("boardSlackForMode")
              .since(V2_0)
              .summary(
                "How much extra time should be given when boarding a vehicle for each given mode."
              )
              .description(
                """
Sometimes there is a need to configure a board times for specific modes, such as airplanes or
ferries, where the check-in process needs to be done in good time before ride.
"""
              )
              .asEnumMap(TransitMode.class, Duration.class)
          )
      )
      .setIgnoreRealtimeUpdates(
        c
          .of("ignoreRealtimeUpdates")
          .since(V2_0)
          .summary("When true, realtime updates are ignored during this search.")
          .asBoolean(dft.ignoreRealtimeUpdates())
      )
      .setOtherThanPreferredRoutesPenalty(
        c
          .of("otherThanPreferredRoutesPenalty")
          .since(V2_0)
          .summary(
            "Penalty added for using every route that is not preferred if user set any route as preferred."
          )
          .description(
            "We return number of seconds that we are willing to wait for preferred route."
          )
          .asInt(dft.otherThanPreferredRoutesPenalty())
      )
      .setReluctanceForMode(
        c
          .of("transitReluctanceForMode")
          .since(V2_1)
          .summary("Transit reluctance for a given transport mode")
          .asEnumMap(TransitMode.class, Double.class)
      )
      .setUnpreferredCost(
        c
          .of("unpreferredCost")
          .since(V2_2)
          .summary("A cost function used to calculate penalty for an unpreferred route.")
          .description(
            """
            Function should return number of seconds that we are willing to wait for preferred route
            or for an unpreferred agency's departure. For example: `5m + 2.0 t`
            """
          )
          .asCostLinearFunction(dft.unpreferredCost())
      );

    String relaxTransitPriorityGroupValue = c
      .of("relaxTransitPriorityGroup")
      .since(V2_5)
      .summary("The relax function for transit-priority-groups")
      .description(
        """
        A path is considered optimal if the generalized-cost is less than the 
        generalized-cost of another path. If this parameter is set, the comparison is relaxed
        further if they belong to different transit-priority-groups.
        """
      )
      .asString(dft.relaxTransitPriorityGroup().toString());

    if (relaxTransitPriorityGroupValue != null) {
      builder.withTransitGroupPriorityGeneralizedCostSlack(
        CostLinearFunction.of(relaxTransitPriorityGroupValue)
      );
    }

    builder.withRaptor(it ->
      c
        .of("relaxTransitSearchGeneralizedCostAtDestination")
        .since(V2_3)
        .summary("Whether non-optimal transit paths at the destination should be returned")
        .description(
          """
                Let c be the existing minimum pareto optimal generalized cost to beat. Then a trip
                with cost c' is accepted if the following is true:
                `c' < Math.round(c * relaxRaptorCostCriteria)`.

                The parameter is optional. If not set a normal comparison is performed.

                Values equals or less than zero is not allowed. Values greater than 2.0 are not
                supported, due to performance reasons.
                """
        )
        .asDoubleOptional()
        .ifPresent(it::withRelaxGeneralizedCostAtDestination)
    );
  }

  private static void mapBikePreferences(NodeAdapter c, BikePreferences.Builder builder) {
    var dft = builder.original();
    builder
      .withSpeed(
        c
          .of("bikeSpeed")
          .since(V2_0)
          .summary("Max bike speed along streets, in meters per second")
          .asDouble(dft.speed())
      )
      .withReluctance(
        c
          .of("bikeReluctance")
          .since(V2_0)
          .summary(
            "A multiplier for how bad biking is, compared to being in transit for equal lengths of time."
          )
          .asDouble(dft.reluctance())
      )
      .withBoardCost(
        c
          .of("bikeBoardCost")
          .since(V2_0)
          .summary("Prevents unnecessary transfers by adding a cost for boarding a vehicle.")
          .description(
            "This is the cost that is used when boarding while cycling." +
            "This is usually higher that walkBoardCost."
          )
          .asInt(dft.boardCost())
      )
      .withParkTime(
        c.of("bikeParkTime").since(V2_0).summary("Time to park a bike.").asInt(dft.parkTime())
      )
      .withParkCost(
        c.of("bikeParkCost").since(V2_0).summary("Cost to park a bike.").asInt(dft.parkCost())
      )
      .withWalkingSpeed(
        c
          .of("bikeWalkingSpeed")
          .since(V2_1)
          .summary(
            "The user's bike walking speed in meters/second. Defaults to approximately 3 MPH."
          )
          .asDouble(dft.walkingSpeed())
      )
      .withWalkingReluctance(
        c
          .of("bikeWalkingReluctance")
          .since(V2_1)
          .summary(
            "A multiplier for how bad walking with a bike is, compared to being in transit for equal lengths of time."
          )
          .asDouble(dft.walkingReluctance())
      )
      .withSwitchTime(
        c
          .of("bikeSwitchTime")
          .since(V2_0)
          .summary("The time it takes the user to fetch their bike and park it again in seconds.")
          .asInt(dft.switchTime())
      )
      .withSwitchCost(
        c
          .of("bikeSwitchCost")
          .since(V2_0)
          .summary("The cost of the user fetching their bike and parking it again.")
          .asInt(dft.switchCost())
      )
      .withOptimizeType(
        c
          .of("optimize")
          .since(V2_0)
          .summary("The set of characteristics that the user wants to optimize for.")
          .asEnum(dft.optimizeType())
      )
      .withOptimizeTriangle(it ->
        it
          .withTime(
            c
              .of("bikeTriangleTimeFactor")
              .since(V2_0)
              .summary("For bike triangle routing, how much time matters (range 0-1).")
              .asDouble(it.time())
          )
          .withSlope(
            c
              .of("bikeTriangleSlopeFactor")
              .since(V2_0)
              .summary("For bike triangle routing, how much slope matters (range 0-1).")
              .asDouble(it.slope())
          )
          .withSafety(
            c
              .of("bikeTriangleSafetyFactor")
              .since(V2_0)
              .summary("For bike triangle routing, how much safety matters (range 0-1).")
              .asDouble(it.safety())
          )
      )
      .withStairsReluctance(
        c
          .of("bikeStairsReluctance")
          .since(V2_3)
          .summary(
            "How bad is it to walk the bicycle up/down a flight of stairs compared to taking a detour."
          )
          .asDouble(dft.stairsReluctance())
      );
  }

  private static void mapStreetPreferences(NodeAdapter c, StreetPreferences.Builder builder) {
    var dft = builder.original();
    NodeAdapter cae = c
      .of("accessEgress")
      .since(V2_4)
      .summary("Parameters for access and egress routing.")
      .asObject();

    builder
      .withTurnReluctance(
        c
          .of("turnReluctance")
          .since(V2_0)
          .summary("Multiplicative factor on expected turning time.")
          .asDouble(dft.turnReluctance())
      )
      .withDrivingDirection(
        c
          .of("drivingDirection")
          .since(V2_2)
          .summary("The driving direction to use in the intersection traversal calculation")
          .asEnum(dft.drivingDirection())
      )
      .withElevator(elevator -> {
        var dftElevator = dft.elevator();
        elevator
          .withBoardCost(
            c
              .of("elevatorBoardCost")
              .since(V2_0)
              .summary("What is the cost of boarding a elevator?")
              .asInt(dftElevator.boardCost())
          )
          .withBoardTime(
            c
              .of("elevatorBoardTime")
              .since(V2_0)
              .summary("How long does it take to get on an elevator, on average.")
              .asInt(dftElevator.boardTime())
          )
          .withHopCost(
            c
              .of("elevatorHopCost")
              .since(V2_0)
              .summary("What is the cost of travelling one floor on an elevator?")
              .asInt(dftElevator.hopCost())
          )
          .withHopTime(
            c
              .of("elevatorHopTime")
              .since(V2_0)
              .summary("How long does it take to advance one floor on an elevator?")
              .asInt(dftElevator.hopTime())
          );
      })
      .withAccessEgress(accessEgress -> {
        var dftAccessEgress = dft.accessEgress();
        accessEgress
          .withPenalty(
            // The default value is NO-PENALTY and is not configurable
            cae
              .of("penalty")
              .since(V2_4)
              .summary("Penalty for access/egress by street mode.")
              .description(
                """
            Use this to add a time and cost penalty to an access/egress legs for a given street
            mode. This will favour other street-modes and transit. This has a performance penalty,
            since the search-window is increased with the same amount as the maximum penalty for
            the access legs used. In other cases where the access(CAR) is faster than transit the
            performance will be better.

            The default is no penalty, if not configured.

            Example: `"car-to-park" : { "timePenalty": "10m + 1.5t", "costFactor": 2.5 }`

            **Time penalty**

            The `timePenalty` is used to add a penalty to the access/egress duration/time. The
            time including the penalty is used in the algorithm when comparing paths, but the
            actual duration is used when presented to the end user.

            **Cost factor**

            The `costFactor` is used to add an additional cost to the leg´s  generalized-cost. The
            time-penalty is multiplied with the cost-factor. A cost-factor of zero, gives no
            extra cost, while 1.0 will add the same amount to both time and cost.
            """
              )
              .asEnumMap(StreetMode.class, TimeAndCostPenaltyMapper::map)
          )
          .withMaxDuration(
            cae
              .of("maxDuration")
              .since(V2_1)
              .summary("This is the maximum duration for access/egress for street searches.")
              .description(
                """
This is a performance limit and should therefore be set high. Results close to the limit are not
guaranteed to be optimal. Use itinerary-filters to limit what is presented to the client. The
duration can be set per mode(`maxDurationForMode`), because some street modes searches
are much more resource intensive than others. A default value is applied if the mode specific value
do not exist.
"""
              )
              .asDuration(dftAccessEgress.maxDuration().defaultValue()),
            cae
              .of("maxDurationForMode")
              .since(V2_1)
              .summary("Limit access/egress per street mode.")
              .description(
                """
              Override the settings in `maxDuration` for specific street modes. This is
              done because some street modes searches are much more resource intensive than others.
              """
              )
              .asEnumMap(StreetMode.class, Duration.class)
          )
          .withMaxStopCount(
            cae
              .of("maxStopCount")
              .since(V2_4)
              .summary("Maximal number of stops collected in access/egress routing")
              .description(
                """
              Safety limit to prevent access to and egress from too many stops.
              """
              )
              .asInt(dftAccessEgress.maxStopCount())
          );
      })
      .withMaxDirectDuration(
        c
          .of("maxDirectStreetDuration")
          .since(V2_1)
          .summary("This is the maximum duration for a direct street search for each mode.")
          .description(
            """
This is a performance limit and should therefore be set high. Results close to the limit are not
guaranteed to be optimal. Use itinerary-filters to limit what is presented to the client. The
duration can be set per mode(`maxDirectStreetDurationForMode`), because some street modes searches
are much more resource intensive than others. A default value is applied if the mode specific value
do not exist."
"""
          )
          .asDuration(dft.maxDirectDuration().defaultValue()),
        c
          .of("maxDirectStreetDurationForMode")
          .since(V2_2)
          .summary("Limit direct route duration per street mode.")
          .description(
            """
            Override the settings in `maxDirectStreetDuration` for specific street modes. This is
            done because some street modes searches are much more resource intensive than others.
            """
          )
          .asEnumMap(StreetMode.class, Duration.class)
      )
      .withIntersectionTraversalModel(
        c
          .of("intersectionTraversalModel")
          .since(V2_2)
          .summary("The model that computes the costs of turns.")
          .asEnum(dft.intersectionTraversalModel())
      )
      .withRoutingTimeout(
        c
          .of("streetRoutingTimeout")
          .since(V2_2)
          .summary(
            "The maximum time a street routing request is allowed to take before returning the " +
            "results."
          )
          .description(
            """
The street search(AStar) aborts after this duration and any paths found are returned to the client.
The street part of the routing may take a long time if searching very long distances. You can set
the street routing timeout to avoid tying up server resources on pointless searches and ensure that
your users receive a timely response. You can also limit the max duration. There are is also a
'apiProcessingTimeout'. Make sure the street timeout is less than the 'apiProcessingTimeout'.
            """
          )
          .asDuration(dft.routingTimeout())
      );
  }

  private static void mapCarPreferences(NodeAdapter c, CarPreferences.Builder builder) {
    var dft = builder.original();
    builder
      .withSpeed(
        c
          .of("carSpeed")
          .since(V2_0)
          .summary("Max car speed along streets, in meters per second")
          .asDouble(dft.speed())
      )
      .withReluctance(
        c
          .of("carReluctance")
          .since(V2_0)
          .summary(
            "A multiplier for how bad driving is, compared to being in transit for equal lengths of time."
          )
          .asDouble(dft.reluctance())
      )
      .withDropoffTime(
        c
          .of("carDropoffTime")
          .since(V2_0)
          .summary(
            "Time to park a car in a park and ride, w/o taking into account driving and walking cost."
          )
          .asInt(dft.dropoffTime())
      )
      .withParkCost(
        c.of("carParkCost").since(V2_1).summary("Cost of parking a car.").asInt(dft.parkCost())
      )
      .withParkTime(
        c.of("carParkTime").since(V2_1).summary("Time to park a car").asInt(dft.parkTime())
      )
      .withPickupCost(
        c
          .of("carPickupCost")
          .since(V2_1)
          .summary("Add a cost for car pickup changes when a pickup or drop off takes place")
          .asInt(dft.pickupCost())
      )
      .withPickupTime(
        c
          .of("carPickupTime")
          .since(V2_1)
          .summary("Add a time for car pickup changes when a pickup or drop off takes place")
          .asInt(dft.pickupTime())
      )
      .withAccelerationSpeed(
        c
          .of("carAccelerationSpeed")
          .since(V2_0)
          .summary("The acceleration speed of an automobile, in meters per second per second.")
          .asDouble(dft.accelerationSpeed())
      )
      .withDecelerationSpeed(
        c
          .of("carDecelerationSpeed")
          .since(V2_0)
          .summary("The deceleration speed of an automobile, in meters per second per second.")
          .asDouble(dft.decelerationSpeed())
      );
  }

  private static void mapSystemPreferences(NodeAdapter c, SystemPreferences.Builder builder) {
    var dft = builder.original();
    builder
      .withGeoidElevation(
        c
          .of("geoidElevation")
          .since(V2_0)
          .summary(
            "If true, the Graph's ellipsoidToGeoidDifference is applied to all elevations returned by this query."
          )
          .asBoolean(dft.geoidElevation())
      )
      .withMaxJourneyDuration(
        c
          .of("maxJourneyDuration")
          .since(V2_1)
          .summary(
            "The expected maximum time a journey can last across all possible journeys for the current deployment."
          )
          .description(
            """
Normally you would just do an estimate and add enough slack, so you are sure that there is no
journeys that falls outside this window. The parameter is used find all possible dates for the
journey and then search only the services which run on those dates. The duration must include
access, egress, wait-time and transit time for the whole journey. It should also take low frequency
days/periods like holidays into account. In other words, pick the two points within your area that
has the worst connection and then try to travel on the worst possible day, and find the maximum
journey duration. Using a value that is too high has the effect of including more patterns in the
search, hence, making it a bit slower. Recommended values would be from 12 hours(small town/city),
1 day (region) to 2 days (country like Norway)."
"""
          )
          .asDuration(dft.maxJourneyDuration())
      );
    if (OTPFeature.DataOverlay.isOn()) {
      builder.withDataOverlay(
        DataOverlayParametersMapper.map(
          c
            .of("dataOverlay")
            .since(V2_1)
            .summary("The filled request parameters for penalties and thresholds values")
            .description(/*TODO DOC*/"TODO")
            .asObject()
        )
      );
    }
  }

  private static void mapWalkPreferences(NodeAdapter c, WalkPreferences.Builder walk) {
    var dft = walk.original();
    walk
      .withSpeed(
        c
          .of("walkSpeed")
          .since(V2_0)
          .summary("The user's walking speed in meters/second.")
          .asDouble(dft.speed())
      )
      .withReluctance(
        c
          .of("walkReluctance")
          .since(V2_0)
          .summary(
            "A multiplier for how bad walking is, compared to being in transit for equal lengths of time."
          )
          .description(
            """
Empirically, values between 2 and 4 seem to correspond well to the concept of not wanting to walk
too much without asking for totally ridiculous itineraries, but this observation should in no way
be taken as scientific or definitive. Your mileage may vary.
See https://github.com/opentripplanner/OpenTripPlanner/issues/4090 for impact on performance with
high values.
"""
          )
          .asDouble(dft.reluctance())
      )
      .withBoardCost(
        c
          .of("walkBoardCost")
          .since(V2_0)
          .summary(
            """
            Prevents unnecessary transfers by adding a cost for boarding a vehicle. This is the
            cost that is used when boarding while walking.
            """
          )
          .asInt(dft.boardCost())
      )
      .withStairsReluctance(
        c
          .of("stairsReluctance")
          .since(V2_0)
          .summary("Used instead of walkReluctance for stairs.")
          .asDouble(dft.stairsReluctance())
      )
      .withStairsTimeFactor(
        c
          .of("stairsTimeFactor")
          .since(V2_1)
          .summary(
            "How much more time does it take to walk a flight of stairs compared to walking a similar horizontal length."
          )
          .description(
            """
            Default value is based on: Fujiyama, T., & Tyler, N. (2010). Predicting the walking
            speed of pedestrians on stairs. Transportation Planning and Technology, 33(2), 177–202.
            """
          )
          .asDouble(dft.stairsTimeFactor())
      )
      .withSafetyFactor(
        c
          .of("walkSafetyFactor")
          .since(V2_2)
          .summary("Factor for how much the walk safety is considered in routing.")
          .description(
            "Value should be between 0 and 1." + " If the value is set to be 0, safety is ignored."
          )
          .asDouble(dft.safetyFactor())
      )
      .withEscalatorReluctance(
        c
          .of("escalatorReluctance")
          .since(V2_4)
          .summary(
            "A multiplier for how bad being in an escalator is compared to being in transit for equal lengths of time"
          )
          .asDouble(dft.escalatorReluctance())
      );
  }
}
