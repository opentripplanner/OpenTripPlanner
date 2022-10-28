package org.opentripplanner.standalone.config.routingrequest;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_0;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_1;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_2;
import static org.opentripplanner.standalone.config.routingrequest.ItineraryFiltersMapper.mapItineraryFilterParams;
import static org.opentripplanner.standalone.config.routingrequest.WheelchairAccessibilityRequestMapper.mapAccessibilityRequest;

import java.time.Duration;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.BikePreferences;
import org.opentripplanner.routing.api.request.preference.CarPreferences;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.preference.StreetPreferences;
import org.opentripplanner.routing.api.request.preference.SystemPreferences;
import org.opentripplanner.routing.api.request.preference.TransferOptimizationPreferences;
import org.opentripplanner.routing.api.request.preference.TransferPreferences;
import org.opentripplanner.routing.api.request.preference.TransitPreferences;
import org.opentripplanner.routing.api.request.preference.VehicleParkingPreferences;
import org.opentripplanner.routing.api.request.preference.VehicleRentalPreferences;
import org.opentripplanner.routing.api.request.preference.WalkPreferences;
import org.opentripplanner.routing.api.request.request.VehicleParkingRequest;
import org.opentripplanner.routing.api.request.request.VehicleRentalRequest;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.sandbox.DataOverlayParametersMapper;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.util.OTPFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoutingRequestMapper {

  private static final Logger LOG = LoggerFactory.getLogger(RoutingRequestMapper.class);

  public static RouteRequest mapRouteRequest(NodeAdapter c) {
    RouteRequest dft = new RouteRequest();

    if (c.isEmpty()) {
      return dft;
    }

    LOG.debug("Loading default routing parameters from JSON.");
    RouteRequest request = new RouteRequest();
    VehicleRentalRequest vehicleRental = request.journey().rental();
    VehicleParkingRequest vehicleParking = request.journey().parking();

    // Keep this alphabetically sorted so it is easy to check if a parameter is missing from the
    // mapping or duplicate exist.

    vehicleRental.setAllowedNetworks(
      c
        .of("allowedVehicleRentalNetworks")
        .since(NA)
        .summary(
          "The vehicle rental networks which may be used. If empty all networks may be used."
        )
        .asStringSet(vehicleRental.allowedNetworks())
    );
    request.setArriveBy(
      c
        .of("arriveBy")
        .since(NA)
        .summary("Whether the trip should depart or arrive at the specified date and time.")
        .asBoolean(dft.arriveBy())
    );
    vehicleParking.setBannedTags(
      c
        .of("bannedVehicleParkingTags")
        .since(NA)
        .summary("Tags with which a vehicle parking will not be used. If empty, no tags are banned")
        .asStringSet(vehicleParking.bannedTags())
    );
    vehicleRental.setBannedNetworks(
      c
        .of("bannedVehicleRentalNetworks")
        .since(NA)
        .summary(
          "he vehicle rental networks which may not be used. If empty, no networks are banned."
        )
        .asStringSet(vehicleRental.bannedNetworks())
    );

    request
      .journey()
      .rental()
      .setAllowArrivingInRentedVehicleAtDestination(
        c
          .of("allowKeepingRentedBicycleAtDestination")
          .since(NA)
          .summary(
            "If a vehicle should be allowed to be kept at the end of a station-based rental."
          )
          .asBoolean(request.journey().rental().allowArrivingInRentedVehicleAtDestination())
      );

    request.setLocale(c.of("locale").since(NA).summary("TODO").asLocale(dft.locale()));

    request
      .journey()
      .setModes(
        c
          .of("modes")
          .since(NA)
          .summary("TODO")
          .asCustomStringType(
            RequestModes.defaultRequestModes(),
            s -> new QualifiedModeSet(s).getRequestModes()
          )
      );

    request.setNumItineraries(
      c
        .of("numItineraries")
        .since(NA)
        .summary("The maximum number of itineraries to return.")
        .asInt(dft.numItineraries())
    );
    request.setSearchWindow(
      c
        .of("searchWindow")
        .since(NA)
        .summary(
          "The length of the search-window in minutes." +
          " This is normally dynamically calculated by the server, but you may override this by setting it." +
          " The search-window used in a request is returned in the response metadata." +
          " To get the \"next page\" of trips use the metadata(searchWindowUsed and nextWindowDateTime) to create a new request." +
          " If not provided the value is resolved depending on the other input parameters, available transit options and realtime changes."
        )
        .asDuration(dft.searchWindow())
    );
    vehicleParking.setRequiredTags(
      c
        .of("requiredVehicleParkingTags")
        .since(NA)
        .summary(
          "Tags which are required to use a vehicle parking. If empty, no tags are required."
        )
        .asStringSet(vehicleParking.requiredTags())
    );

    request.setWheelchair(
      c
        .of("wheelchairAccessibility")
        .since(NA)
        .summary("TODO")
        .description(/*TODO DOC*/"TODO")
        .asObject()
        .of("enabled")
        .since(NA)
        .summary("TODO")
        .asBoolean(false)
    );

    NodeAdapter unpreferred = c
      .of("unpreferred")
      .since(NA)
      .summary(
        "Parameters for indicating authorities or lines that preferably should not be used in trip patters." +
        "A cost is applied to boarding nonpreferred authorities or lines (otherThanPreferredRoutesPenalty)."
      )
      .description(/*TODO DOC*/"TODO")
      .asObject();
    request
      .journey()
      .transit()
      .setUnpreferredRoutes(
        unpreferred
          .of("routes")
          .since(NA)
          .summary("TODO")
          .asFeedScopedIds(request.journey().transit().unpreferredRoutes())
      );

    request
      .journey()
      .transit()
      .setUnpreferredAgencies(
        unpreferred
          .of("routes")
          .since(NA)
          .summary("TODO")
          .asFeedScopedIds(request.journey().transit().unpreferredRoutes())
      );

    // Map preferences
    request.withPreferences(preferences -> mapPreferences(c, preferences));

    return request;
  }

  private static void mapPreferences(NodeAdapter c, RoutingPreferences.Builder preferences) {
    preferences.withTransit(it -> mapTransitPreferences(c, it));
    preferences.withBike(it -> mapBikePreferences(c, it));
    preferences.withRental(it -> mapRentalPreferences(c, it));
    preferences.withStreet(it -> mapStreetPreferences(c, it));
    preferences.withCar(it -> mapCarPreferences(c, it));
    preferences.withSystem(it -> mapSystemPreferences(c, it));
    preferences.withTransfer(it -> mapTransferPreferences(c, it));
    preferences.withParking(mapParkingPreferences(c, preferences));
    preferences.withWalk(it -> mapWalkPreferences(c, it));
    preferences.withWheelchair(
      mapAccessibilityRequest(
        c
          .of("wheelchairAccessibility")
          .since(NA)
          .summary("TODO")
          .description(/*TODO DOC*/"TODO")
          .asObject()
      )
    );
    preferences.withItineraryFilter(it -> {
      mapItineraryFilterParams("itineraryFilters", c, it);
    });
  }

  private static void mapTransitPreferences(NodeAdapter c, TransitPreferences.Builder builder) {
    var dft = builder.original();
    builder
      .withAlightSlack(it ->
        it
          .withDefault(
            c
              .of("alightSlack")
              .since(NA)
              .summary("The minimum extra time after exiting a public transport vehicle.")
              .asDuration2(dft.alightSlack().defaultValue(), SECONDS)
          )
          .withValues(
            c
              .of("alightSlackForMode")
              .since(V2_0)
              .summary("How much time alighting a vehicle takes for each given mode.")
              .asEnumMap(TransitMode.class, Duration.class)
          )
      )
      .withBoardSlack(it ->
        it
          .withDefault(
            c
              .of("boardSlack")
              .since(NA)
              .summary(
                "The boardSlack is the minimum extra time to board a public transport vehicle." +
                " This is the same as the 'minimumTransferTime', except that this also apply to to the first transit leg in the trip." +
                " This is the default value used, if not overridden by the 'boardSlackList'."
              )
              .asDuration2(dft.boardSlack().defaultValue(), SECONDS)
          )
          .withValues(
            c
              .of("boardSlackForMode")
              .since(V2_0)
              .summary("How much time ride a vehicle takes for each given mode.")
              .asEnumMap(TransitMode.class, Duration.class)
          )
      )
      .setIgnoreRealtimeUpdates(
        c
          .of("ignoreRealtimeUpdates")
          .since(NA)
          .summary("When true, realtime updates are ignored during this search.")
          .asBoolean(dft.ignoreRealtimeUpdates())
      )
      .setOtherThanPreferredRoutesPenalty(
        c
          .of("otherThanPreferredRoutesPenalty")
          .since(NA)
          .summary(
            "Penalty added for using every route that is not preferred if user set any route as preferred." +
            " We return number of seconds that we are willing to wait for preferred route."
          )
          .asInt(dft.otherThanPreferredRoutesPenalty())
      )
      .setReluctanceForMode(
        c
          .of("transitReluctanceForMode")
          .since(NA)
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
            or for an unpreferred agency's departure. For example, 600 + 2.0 x
            """
          )
          .asLinearFunction(dft.unpreferredCost())
      );
  }

  private static void mapBikePreferences(NodeAdapter c, BikePreferences.Builder builder) {
    var dft = builder.original();
    builder
      .withSpeed(
        c
          .of("bikeSpeed")
          .since(NA)
          .summary("Max bike speed along streets, in meters per second")
          .asDouble(dft.speed())
      )
      .withReluctance(
        c
          .of("bikeReluctance")
          .since(NA)
          .summary(
            "A multiplier for how bad biking is, compared to being in transit for equal lengths of time."
          )
          .asDouble(dft.reluctance())
      )
      .withBoardCost(
        c
          .of("bikeBoardCost")
          .since(NA)
          .summary(
            "Prevents unnecessary transfers by adding a cost for boarding a vehicle." +
            " This is the cost that is used when boarding while cycling. This is usually higher that walkBoardCost."
          )
          .asInt(dft.boardCost())
      )
      .withParkTime(
        c.of("bikeParkTime").since(NA).summary("Time to park a bike.").asInt(dft.parkTime())
      )
      .withParkCost(
        c.of("bikeParkCost").since(NA).summary("Cost to park a bike.").asInt(dft.parkCost())
      )
      .withWalkingSpeed(
        c
          .of("bikeWalkingSpeed")
          .since(NA)
          .summary(
            "The user's bike walking speed in meters/second. Defaults to approximately 3 MPH."
          )
          .asDouble(dft.walkingSpeed())
      )
      .withWalkingReluctance(
        c
          .of("bikeWalkingReluctance")
          .since(NA)
          .summary(
            "A multiplier for how bad walking with a bike is, compared to being in transit for equal lengths of time."
          )
          .asDouble(dft.walkingReluctance())
      )
      .withSwitchTime(
        c
          .of("bikeSwitchTime")
          .since(NA)
          .summary("The time it takes the user to fetch their bike and park it again in seconds.")
          .asInt(dft.switchTime())
      )
      .withSwitchCost(
        c
          .of("bikeSwitchCost")
          .since(NA)
          .summary("The cost of the user fetching their bike and parking it again.")
          .asInt(dft.switchCost())
      )
      .withOptimizeType(
        c
          .of("optimize")
          .since(NA)
          .summary("The set of characteristics that the user wants to optimize for.")
          .asEnum(dft.optimizeType())
      )
      .withOptimizeTriangle(it ->
        it
          .withTime(
            c
              .of("bikeTriangleTimeFactor")
              .since(NA)
              .summary("For bike triangle routing, how much time matters (range 0-1).")
              .asDouble(it.time())
          )
          .withSlope(
            c
              .of("bikeTriangleSlopeFactor")
              .since(NA)
              .summary("For bike triangle routing, how much slope matters (range 0-1).")
              .asDouble(it.slope())
          )
          .withSafety(
            c
              .of("bikeTriangleSafetyFactor")
              .since(NA)
              .summary("For bike triangle routing, how much safety matters (range 0-1).")
              .asDouble(it.safety())
          )
      );
  }

  private static void mapRentalPreferences(
    NodeAdapter c,
    VehicleRentalPreferences.Builder builder
  ) {
    var dft = builder.original();
    builder
      .withDropoffCost(
        c
          .of("bikeRentalDropoffCost")
          .since(NA)
          .summary("Cost to drop-off a rented bike.")
          .asInt(dft.dropoffCost())
      )
      .withDropoffTime(
        c
          .of("bikeRentalDropoffTime")
          .since(NA)
          .summary("Time to drop-off a rented bike.")
          .asInt(dft.dropoffTime())
      )
      .withPickupCost(
        c
          .of("bikeRentalPickupCost")
          .since(NA)
          .summary("Cost to rent a bike.")
          .asInt(dft.pickupCost())
      )
      .withPickupTime(
        c
          .of("bikeRentalPickupTime")
          .since(NA)
          .summary("Time to rent a bike.")
          .asInt(dft.pickupTime())
      )
      .withUseAvailabilityInformation(
        c
          .of("useBikeRentalAvailabilityInformation")
          .since(NA)
          .summary(
            "Whether or not bike rental availability information will be used to plan bike rental trips."
          )
          .asBoolean(dft.useAvailabilityInformation())
      )
      .withArrivingInRentalVehicleAtDestinationCost(
        c
          .of("keepingRentedBicycleAtDestinationCost")
          .since(NA)
          .summary(
            "The cost of arriving at the destination with the rented bicycle, to discourage doing so."
          )
          .asDouble(dft.arrivingInRentalVehicleAtDestinationCost())
      );
  }

  private static void mapStreetPreferences(NodeAdapter c, StreetPreferences.Builder builder) {
    var dft = builder.original();
    builder
      .withTurnReluctance(
        c
          .of("turnReluctance")
          .since(NA)
          .summary("Multiplicative factor on expected turning time.")
          .asDouble(dft.turnReluctance())
      )
      .withDrivingDirection(
        c
          .of("drivingDirection")
          .since(NA)
          .summary("The driving direction to use in the intersection traversal calculation")
          .asEnum(dft.drivingDirection())
      )
      .withElevator(elevator -> {
        var dftElevator = dft.elevator();
        elevator
          .withBoardCost(
            c
              .of("elevatorBoardCost")
              .since(NA)
              .summary("What is the cost of boarding a elevator?")
              .asInt(dftElevator.boardCost())
          )
          .withBoardTime(
            c
              .of("elevatorBoardTime")
              .since(NA)
              .summary("How long does it take to get on an elevator, on average.")
              .asInt(dftElevator.boardTime())
          )
          .withHopCost(
            c
              .of("elevatorHopCost")
              .since(NA)
              .summary("What is the cost of travelling one floor on an elevator?")
              .asInt(dftElevator.hopCost())
          )
          .withHopTime(
            c
              .of("elevatorHopTime")
              .since(NA)
              .summary("How long does it take to advance one floor on an elevator?")
              .asInt(dftElevator.hopTime())
          );
      })
      .withMaxAccessEgressDuration(
        c
          .of("maxAccessEgressDuration")
          .since(V2_2)
          .summary(
            "This is the maximum duration for access/egress per street mode for street searches." +
            " This is a performance limit and should therefore be set high. Results close to the limit are not guaranteed to be optimal." +
            " Use itinerary-filters to limit what is presented to the client." +
            " The duration can be set per mode, because some street modes searches are much more resource intensive than others." +
            " A default value is applied if the mode specific value do not exist."
          )
          .asDuration(dft.maxAccessEgressDuration().defaultValue()),
        c
          .of("maxAccessEgressDurationForMode")
          .since(NA)
          .summary("Limit access/egress per street mode.")
          .asEnumMap(StreetMode.class, Duration.class)
      )
      .withMaxDirectDuration(
        c
          .of("maxDirectStreetDuration")
          .since(NA)
          .summary(
            "This is the maximum duration for a direct street search for each mode." +
            " This is a performance limit and should therefore be set high." +
            " Results close to the limit are not guaranteed to be optimal." +
            " Use itinerary-filters to limit what is presented to the client." +
            " The duration can be set per mode, because some street modes searches are much more resource intensive than others." +
            " A default value is applied if the mode specific value do not exist."
          )
          .asDuration(dft.maxDirectDuration().defaultValue()),
        c
          .of("maxDirectStreetDurationForMode")
          .since(V2_2)
          .summary("Limit direct route duration per street mode.")
          .asEnumMap(StreetMode.class, Duration.class)
      )
      .withIntersectionTraversalModel(
        c
          .of("intersectionTraversalModel")
          .since(NA)
          .summary("The model that computes the costs of turns.")
          .asEnum(dft.intersectionTraversalModel())
      );
  }

  private static void mapCarPreferences(NodeAdapter c, CarPreferences.Builder builder) {
    var dft = builder.original();
    builder
      .withSpeed(
        c
          .of("carSpeed")
          .since(NA)
          .summary("Max car speed along streets, in meters per second")
          .asDouble(dft.speed())
      )
      .withReluctance(
        c
          .of("carReluctance")
          .since(NA)
          .summary(
            "A multiplier for how bad driving is, compared to being in transit for equal lengths of time."
          )
          .asDouble(dft.reluctance())
      )
      .withDropoffTime(
        c
          .of("carDropoffTime")
          .since(NA)
          .summary(
            "Time to park a car in a park and ride, w/o taking into account driving and walking cost."
          )
          .asInt(dft.dropoffTime())
      )
      .withParkCost(
        c.of("carParkCost").since(NA).summary("Cost of parking a car.").asInt(dft.parkCost())
      )
      .withParkTime(
        c.of("carParkTime").since(NA).summary("Time to park a car").asInt(dft.parkTime())
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
          .since(NA)
          .summary("The acceleration speed of an automobile, in meters per second per second.")
          .asDouble(dft.accelerationSpeed())
      )
      .withDecelerationSpeed(
        c
          .of("carDecelerationSpeed")
          .since(NA)
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
          .since(NA)
          .summary(
            "If true, the Graph's ellipsoidToGeoidDifference is applied to all elevations returned by this query."
          )
          .asBoolean(dft.geoidElevation())
      )
      .withMaxJourneyDuration(
        c
          .of("maxJourneyDuration")
          .since(NA)
          .summary(
            "The expected maximum time a journey can last across all possible journeys for the current deployment." +
            " Normally you would just do an estimate and add enough slack, so you are sure that there is no journeys that falls outside this window." +
            " The parameter is used find all possible dates for the journey and then search only the services which run on those dates." +
            " The duration must include access, egress, wait-time and transit time for the whole journey. It should also take low frequency days/periods like holidays into account." +
            " In other words, pick the two points within your area that has the worst connection and then try to travel on the worst possible day, and find the maximum journey duration." +
            " Using a value that is too high has the effect of including more patterns in the search, hence, making it a bit slower." +
            " Recommended values would be from 12 hours(small town/city), 1 day (region) to 2 days (country like Norway)."
          )
          .asDuration(dft.maxJourneyDuration())
      );
    if (OTPFeature.DataOverlay.isOn()) {
      builder.withDataOverlay(
        DataOverlayParametersMapper.map(
          c
            .of("dataOverlay")
            .since(NA)
            .summary("The filled request parameters for penalties and thresholds values")
            .description(/*TODO DOC*/"TODO")
            .asObject()
        )
      );
    }
  }

  private static void mapTransferPreferences(NodeAdapter c, TransferPreferences.Builder tx) {
    var dft = tx.original();
    tx
      .withNonpreferredCost(
        c.of("nonpreferredTransferPenalty").since(NA).summary("TODO").asInt(dft.nonpreferredCost())
      )
      .withCost(
        c
          .of("transferPenalty")
          .since(NA)
          .summary(
            "An additional penalty added to boardings after the first." +
            " The value is in OTP's internal weight units, which are roughly equivalent to seconds." +
            " Set this to a high value to discourage transfers." +
            " Of course, transfers that save significant time or walking will still be taken."
          )
          .asInt(dft.cost())
      )
      .withSlack(
        c
          .of("transferSlack")
          .since(NA)
          .summary(
            "An expected transfer time (in seconds) that specifies the amount of time that must pass between exiting one public transport vehicle and boarding another." +
            " This time is in addition to time it might take to walk between stops."
          )
          .asInt(dft.slack())
      )
      .withWaitReluctance(
        c
          .of("waitReluctance")
          .since(NA)
          .summary(
            "How much worse is waiting for a transit vehicle than being on a transit vehicle, as a multiplier."
          )
          .asDouble(dft.waitReluctance())
      )
      .withOptimization(
        mapTransferOptimization(
          c
            .of("transferOptimization")
            .since(NA)
            .summary(
              "Optimize where a transfer between to trip happens. This is a separate step *after* the routing is done. "
            )
            .description(
              """
The main purpose of transfer optimization is to handle cases where it is possible to transfer
between two routes at more than one point (pair of stops). The transfer optimization ensures that
transfers occur at the best possible location. By post-processing all paths returned by the router,
OTP can apply sophisticated calculations that are too slow or not algorithmically valid within 
Raptor. Transfers are optimized before the paths are passed to the itinerary-filter-chain.

To toggle transfer optimization on or off use the OTPFeature `OptimizeTransfers` (default is on). 
You should leave this on unless there is a critical issue with it. The OTPFeature 
`GuaranteedTransfers` will toggle on and off the priority optimization 
(part of OptimizeTransfers).
              
The optimized transfer service will try to, in order:
              
1. Use transfer priority. This includes stay-seated and guaranteed transfers.
2. Use the transfers with the best distribution of the wait-time, and avoid very short transfers.
3. Avoid back-travel
4. Boost stop-priority to select preferred and recommended stops.
              
If two paths have the same transfer priority level, then we break the tie by looking at waiting 
times. The goal is to maximize the wait-time for each stop, avoiding situations where there is 
little time available to make the transfer. This is balanced with the generalized-cost. The cost
is adjusted with a new cost for wait-time (optimized-wait-time-cost).
              
The defaults should work fine, but if you have results with short wait-times dominating a better 
option or "back-travel", then try to increase the `minSafeWaitTimeFactor`, 
`backTravelWaitTimeFactor` and/or `extraStopBoardAlightCostsFactor`.
"""
            )
            .asObject()
        )
      );
  }

  private static VehicleParkingPreferences mapParkingPreferences(
    NodeAdapter c,
    RoutingPreferences.Builder preferences
  ) {
    return VehicleParkingPreferences.of(
      c
        .of("useVehicleParkingAvailabilityInformation")
        .asBoolean(preferences.parking().useAvailabilityInformation())
    );
  }

  private static void mapWalkPreferences(NodeAdapter c, WalkPreferences.Builder walk) {
    var dft = walk.original();
    walk
      .withSpeed(
        c
          .of("walkSpeed")
          .since(NA)
          .summary("The user's walking speed in meters/second.")
          .asDouble(dft.speed())
      )
      .withReluctance(
        c
          .of("walkReluctance")
          .since(NA)
          .summary(
            """
A multiplier for how bad walking is, compared to being in transit for equal lengths of time.
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
          .since(NA)
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
          .since(NA)
          .summary("Used instead of walkReluctance for stairs.")
          .asDouble(dft.stairsReluctance())
      )
      .withStairsTimeFactor(
        c
          .of("stairsTimeFactor")
          .since(NA)
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
          .since(NA)
          .summary(
            "Factor for how much the walk safety is considered in routing. Value should be between 0 and 1." +
            " If the value is set to be 0, safety is ignored"
          )
          .asDouble(dft.safetyFactor())
      );
  }

  private static TransferOptimizationPreferences mapTransferOptimization(NodeAdapter c) {
    var dft = TransferOptimizationPreferences.DEFAULT;
    return TransferOptimizationPreferences
      .of()
      .withOptimizeTransferWaitTime(
        c
          .of("optimizeTransferWaitTime")
          .since(NA)
          .summary(
            "This enables the transfer wait time optimization." +
            " If not enabled generalizedCost function is used to pick the optimal transfer point"
          )
          .asBoolean(dft.optimizeTransferWaitTime())
      )
      .withMinSafeWaitTimeFactor(
        c
          .of("minSafeWaitTimeFactor")
          .since(NA)
          .summary(
            "This defines the maximum cost for the logarithmic function relative to the " +
            "min-safe-transfer-time (t0) when wait time goes towards zero(0). f(0) = n * t0"
          )
          .asDouble(dft.minSafeWaitTimeFactor())
      )
      .withBackTravelWaitTimeFactor(
        c
          .of("backTravelWaitTimeFactor")
          .since(NA)
          .summary(
            "The wait time is used to prevent *back-travel*, the backTravelWaitTimeFactor is " +
            "multiplied with the wait-time and subtracted from the optimized-transfer-cost."
          )
          .asDouble(dft.backTravelWaitTimeFactor())
      )
      .withExtraStopBoardAlightCostsFactor(
        c
          .of("extraStopBoardAlightCostsFactor")
          .since(NA)
          .summary("Add an extra board- and alight-cost for prioritized stops.")
          .description(
            """
            A stopBoardAlightCosts is added to the generalized-cost during routing. But this cost
            cannot be too high, because that would add extra cost to the transfer, and favor other 
            alternative paths. But, when optimizing transfers, we do not have to take other paths 
            into consideration and can *boost* the stop-priority-cost to allow transfers to 
            take place at a preferred stop. The cost added during routing is already added to the 
            generalized-cost used as a base in the optimized transfer calculation. By setting this 
            parameter to 0, no extra cost is added, by setting it to {@code 1.0} the stop-cost is
            doubled. Stop priority is only supported by the NeTEx import, not GTFS.
            """
          )
          .asDouble(dft.extraStopBoardAlightCostsFactor())
      )
      .build();
  }
}
