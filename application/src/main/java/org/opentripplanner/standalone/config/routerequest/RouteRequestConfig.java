package org.opentripplanner.standalone.config.routerequest;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_0;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_1;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_2;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_3;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_4;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_5;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_7;
import static org.opentripplanner.standalone.config.routerequest.ItineraryFiltersConfig.mapItineraryFilterParams;
import static org.opentripplanner.standalone.config.routerequest.TransferConfig.mapTransferPreferences;
import static org.opentripplanner.standalone.config.routerequest.TriangleOptimizationConfig.mapOptimizationTriangle;
import static org.opentripplanner.standalone.config.routerequest.VehicleParkingConfig.mapParking;
import static org.opentripplanner.standalone.config.routerequest.VehicleRentalConfig.mapRental;
import static org.opentripplanner.standalone.config.routerequest.VehicleWalkingConfig.mapVehicleWalking;
import static org.opentripplanner.standalone.config.routerequest.WheelchairConfig.mapWheelchairPreferences;

import java.time.Duration;
import java.util.stream.Collectors;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;
import org.opentripplanner.routing.api.request.preference.AccessEgressPreferences;
import org.opentripplanner.routing.api.request.preference.BikePreferences;
import org.opentripplanner.routing.api.request.preference.CarPreferences;
import org.opentripplanner.routing.api.request.preference.EscalatorPreferences;
import org.opentripplanner.routing.api.request.preference.RoutingPreferencesBuilder;
import org.opentripplanner.routing.api.request.preference.ScooterPreferences;
import org.opentripplanner.routing.api.request.preference.StreetPreferences;
import org.opentripplanner.routing.api.request.preference.SystemPreferences;
import org.opentripplanner.routing.api.request.preference.TransitPreferences;
import org.opentripplanner.routing.api.request.preference.WalkPreferences;
import org.opentripplanner.routing.api.request.request.TransitRequest;
import org.opentripplanner.routing.api.request.request.TransitRequestBuilder;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.sandbox.DataOverlayParametersMapper;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.utils.lang.StringUtils;

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
    return mapRouteRequest(c, RouteRequest.defaultValue());
  }

  public static RouteRequest mapRouteRequest(NodeAdapter c, RouteRequest dft) {
    if (c.isEmpty()) {
      return dft;
    }

    var requestBuilder = dft.copyOf();

    // Keep this alphabetically sorted so it is easy to check if a parameter is missing from the
    // mapping or duplicate exist.

    requestBuilder.withArriveBy(
      c
        .of("arriveBy")
        .since(V2_0)
        .summary("Whether the trip should depart or arrive at the specified date and time.")
        .asBoolean(dft.arriveBy())
    );

    requestBuilder.withJourney(b ->
      b.setModes(
        c
          .of("modes")
          .since(V2_0)
          .summary(
            "The set of access/egress/direct/transfer modes (separated by a comma) to be used for the route search."
          )
          .asCustomStringType(RequestModes.defaultRequestModes(), "WALK", s ->
            new QualifiedModeSet(s).getRequestModes()
          )
      )
    );

    requestBuilder.withNumItineraries(
      c
        .of("numItineraries")
        .since(V2_0)
        .summary("The maximum number of itineraries to return.")
        .asInt(dft.numItineraries())
    );
    requestBuilder.withSearchWindow(
      c
        .of("searchWindow")
        .since(V2_0)
        .summary("The duration of the search-window.")
        .description(
          """
          This is the time/duration in seconds from the earliest-departure-time (EDT) to the
          latest-departure-time (LDT). In case of a reverse search it will be the time from earliest to
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
    requestBuilder.withJourney(jb -> {
      jb.withWheelchair(WheelchairConfig.wheelchairEnabled(c, WHEELCHAIR_ACCESSIBILITY));

      jb.withTransit(b -> {
        mapTransit(unpreferred, b, dft.journey().transit());
        TransitGroupPriorityConfig.mapTransitRequest(c, b);
      });
    });

    // Map preferences
    requestBuilder.withPreferences(preferences -> mapPreferences(c, preferences));

    return requestBuilder.buildDefault();
  }

  private static void mapTransit(
    NodeAdapter c,
    TransitRequestBuilder builder,
    TransitRequest defaultValues
  ) {
    builder.withUnpreferredRoutes(
      c
        .of("routes")
        .since(V2_2)
        .summary(
          "The ids of the routes that incur an extra cost when being used. Format: `FeedId:RouteId`"
        )
        .description("How much cost is added is configured in `unpreferredCost`.")
        .asFeedScopedIds(defaultValues.unpreferredRoutes())
    );

    builder.withUnpreferredAgencies(
      c
        .of("agencies")
        .since(V2_2)
        .summary(
          "The ids of the agencies that incur an extra cost when being used. Format: `FeedId:AgencyId`"
        )
        .description("How much cost is added is configured in `unpreferredCost`.")
        .asFeedScopedIds(defaultValues.unpreferredAgencies())
    );
  }

  private static void mapPreferences(NodeAdapter c, RoutingPreferencesBuilder preferences) {
    preferences.withTransit(it -> mapTransitPreferences(c, it));
    preferences.withBike(it -> mapBikePreferences(c, it));
    preferences.withStreet(it -> mapStreetPreferences(c, it));
    preferences.withCar(it -> mapCarPreferences(c, it));
    preferences.withScooter(it -> mapScooterPreferences(c, it));
    preferences.withSystem(it -> mapSystemPreferences(c, it));
    preferences.withTransfer(it -> mapTransferPreferences(c, it));
    preferences.withWalk(it -> mapWalkPreferences(c, it));
    preferences.withWheelchair(it -> mapWheelchairPreferences(c, it, WHEELCHAIR_ACCESSIBILITY));
    preferences.withItineraryFilter(it -> mapItineraryFilterParams("itineraryFilters", c, it));

    var dft = RouteRequest.defaultValue().preferences();
    preferences.withLocale(c.of("locale").since(V2_0).summary("TODO").asLocale(dft.locale()));
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
              .summary("The time safety margin when alighting from a vehicle.")
              .description(
                """
                This time slack is added to arrival time of the vehicle before any transfer or onward travel.

                This time slack helps model potential delays or procedures a passenger experiences during the process of passing through the alighting location. This
                parameter is intended to be set by agencies not individual users. For specific modes, like airplane and
                subway, that need more time than others, this is also configurable per mode with `alightSlackForMode`.
                A related parameter (transferSlack) exists to help avoid missed connections when there are minor schedule variations.
                """
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
              .summary("The time safety margin when boarding a vehicle.")
              .description(
                """
                The board slack is added to the passenger's arrival time at a stop, before evaluating which
                vehicles can be boarded.

                This time slack helps model potential delays or procedures a passenger experiences during the process
                of passing through the boarding location, as well as some minor schedule variation. This parameter is
                intended to be set by agencies not individual users.

                Agencies can use this parameter to ensure that the trip planner does not instruct passengers to arrive
                at the last second. This slack is added at every boarding including the first vehicle and transfers
                except for in-seat transfers and guaranteed transfers.

                For specific modes, like airplane and subway, that need more time than others, this is also
                configurable per mode with `boardSlackForMode`.

                A related parameter (transferSlack) also helps avoid missed connections when there are minor schedule
                variations.
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
          .summary("When true, real-time updates are ignored during this search.")
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

    String relaxTransitGroupPriorityValue = c
      .of("relaxTransitGroupPriority")
      .since(V2_5)
      .summary("The relax function for transit-group-priority")
      .description(
        """
        A path is considered optimal if the generalized-cost is less than the generalized-cost of
        another path. If this parameter is set, the comparison is relaxed further if they belong
        to different transit groups.
        """
      )
      .asString(dft.relaxTransitGroupPriority().toString());

    if (relaxTransitGroupPriorityValue != null) {
      builder.withRelaxTransitGroupPriority(CostLinearFunction.of(relaxTransitGroupPriorityValue));
    }

    // TODO REMOVE THIS
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

  private static void mapBikePreferences(NodeAdapter root, BikePreferences.Builder builder) {
    var dft = builder.original();
    NodeAdapter c = root.of("bicycle").since(V2_5).summary("Bicycle preferences.").asObject();
    builder
      .withSpeed(
        c
          .of("speed")
          .since(V2_0)
          .summary("Max bicycle speed along streets, in meters per second")
          .asDouble(dft.speed())
      )
      .withReluctance(
        c
          .of("reluctance")
          .since(V2_0)
          .summary(
            "A multiplier for how bad cycling is, compared to being in transit for equal lengths of time."
          )
          .asDouble(dft.reluctance())
      )
      .withBoardCost(
        c
          .of("boardCost")
          .since(V2_0)
          .summary(
            "Prevents unnecessary transfers by adding a cost for boarding a transit vehicle."
          )
          .description(
            "This is the cost that is used when boarding while cycling. " +
            "This is usually higher that walkBoardCost."
          )
          .asInt(dft.boardCost())
      )
      .withOptimizeType(
        c
          .of("optimization")
          .since(V2_0)
          .summary("The set of characteristics that the user wants to optimize for.")
          .description(
            "If the triangle optimization is used, it's enough to just define the triangle parameters"
          )
          .asEnum(dft.optimizeType())
      )
      // triangle overrides the optimization type if defined
      .withForcedOptimizeTriangle(it -> mapOptimizationTriangle(c, it))
      .withWalking(it -> mapVehicleWalking(c, it))
      .withParking(it -> mapParking(c, it))
      .withRental(it -> mapRental(c, it));
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
                the access legs used. In other cases where the access (CAR) is faster than transit the
                performance will be better.

                The default values are

                %s

                Example: `"car-to-park" : { "timePenalty": "10m + 1.5t", "costFactor": 2.5 }`

                **Time penalty**

                The `timePenalty` is used to add a penalty to the access/egress duration/time. The
                time including the penalty is used in the algorithm when comparing paths, but the
                actual duration is used when presented to the end user.

                **Cost factor**

                The `costFactor` is used to add an additional cost to the leg´s  generalized-cost. The
                time-penalty is multiplied with the cost-factor. A cost-factor of zero, gives no
                extra cost, while 1.0 will add the same amount to both time and cost.
                """.formatted(formatPenaltyDefaultValues(dftAccessEgress))
              )
              .asEnumMap(
                StreetMode.class,
                TimeAndCostPenaltyMapper::map,
                dftAccessEgress.penalty().asEnumMap()
              )
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
                duration can be set per mode (`maxDurationForMode`), because some street modes searches
                are much more resource intensive than others. A default value is applied if the mode specific value
                does not exist.
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
              .asInt(dftAccessEgress.maxStopCountLimit().defaultLimit()),
            cae
              .of("maxStopCountForMode")
              .since(V2_7)
              .summary(
                "Maximal number of stops collected in access/egress routing for the given mode"
              )
              .description(
                """
                Safety limit to prevent access to and egress from too many stops.
                Mode-specific version of `maxStopCount`.
                """
              )
              .asEnumMap(StreetMode.class, Integer.class)
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
            duration can be set per mode (`maxDirectStreetDurationForMode`), because some street modes searches
            are much more resource intensive than others. A default value is applied if the mode specific value
            does not exist."
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
            The street search (AStar) aborts after this duration and any paths found are returned to the client.
            The street part of the routing may take a long time if searching very long distances. You can set
            the street routing timeout to avoid tying up server resources on pointless searches and ensure that
            your users receive a timely response. You can also limit the max duration. There are is also a
            'apiProcessingTimeout'. Make sure the street timeout is less than the 'apiProcessingTimeout'.
                        """
          )
          .asDuration(dft.routingTimeout())
      );
  }

  private static String formatPenaltyDefaultValues(AccessEgressPreferences dftAccessEgress) {
    return dftAccessEgress
      .penalty()
      .asEnumMap()
      .entrySet()
      .stream()
      .map(s -> "- `%s` = %s".formatted(StringUtils.kebabCase(s.getKey().toString()), s.getValue()))
      .collect(Collectors.joining("\n"));
  }

  private static void mapCarPreferences(NodeAdapter root, CarPreferences.Builder builder) {
    var dft = builder.original();
    NodeAdapter c = root.of("car").since(V2_5).summary("Car preferences.").asObject();
    builder
      .withReluctance(
        c
          .of("reluctance")
          .since(V2_0)
          .summary(
            "A multiplier for how bad driving is, compared to being in transit for equal lengths of time."
          )
          .asDouble(dft.reluctance())
      )
      .withBoardCost(
        c
          .of("boardCost")
          .since(V2_7)
          .summary(
            "Prevents unnecessary transfers by adding a cost for boarding a transit vehicle."
          )
          .description(
            "This is the cost that is used when boarding while driving. " +
            "This can be different compared to the boardCost while walking or cycling."
          )
          .asInt(dft.boardCost())
      )
      .withPickupCost(
        c
          .of("pickupCost")
          .since(V2_1)
          .summary("Add a cost for car pickup changes when a pickup or drop off takes place")
          .asInt(dft.pickupCost().toSeconds())
      )
      .withPickupTime(
        c
          .of("pickupTime")
          .since(V2_1)
          .summary("Add a time for car pickup changes when a pickup or drop off takes place")
          .asDuration(dft.pickupTime())
      )
      .withAccelerationSpeed(
        c
          .of("accelerationSpeed")
          .since(V2_0)
          .summary("The acceleration speed of an automobile, in meters per second per second.")
          .asDouble(dft.accelerationSpeed())
      )
      .withDecelerationSpeed(
        c
          .of("decelerationSpeed")
          .since(V2_0)
          .summary("The deceleration speed of an automobile, in meters per second per second.")
          .asDouble(dft.decelerationSpeed())
      )
      .withParking(it -> mapParking(c, it))
      .withRental(it -> mapRental(c, it));
  }

  private static void mapScooterPreferences(NodeAdapter root, ScooterPreferences.Builder builder) {
    var dft = builder.original();
    NodeAdapter c = root.of("scooter").since(V2_5).summary("Scooter preferences.").asObject();
    builder
      .withSpeed(
        c
          .of("speed")
          .since(V2_0)
          .summary("Max scooter speed along streets, in meters per second")
          .asDouble(dft.speed())
      )
      .withReluctance(
        c
          .of("reluctance")
          .since(V2_0)
          .summary(
            "A multiplier for how bad scooter travel is, compared to being in transit for equal lengths of time."
          )
          .asDouble(dft.reluctance())
      )
      .withOptimizeType(
        c
          .of("optimization")
          .since(V2_0)
          .summary("The set of characteristics that the user wants to optimize for.")
          .description(
            "If the triangle optimization is used, it's enough to just define the triangle parameters"
          )
          .asEnum(dft.optimizeType())
      )
      // triangle overrides the optimization type if defined
      .withForcedOptimizeTriangle(it -> mapOptimizationTriangle(c, it))
      .withRental(it -> mapRental(c, it));
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
            search, hence, making it a bit slower. Recommended values would be from 12 hours (small town/city),
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

  private static void mapEscalatorPreferences(
    NodeAdapter root,
    EscalatorPreferences.Builder escalator
  ) {
    var dft = escalator.original();
    NodeAdapter c = root.of("escalator").since(V2_7).summary("Escalator preferences.").asObject();
    escalator
      .withReluctance(
        c
          .of("reluctance")
          .since(V2_4)
          .summary(
            "A multiplier for how bad being in an escalator is compared to being in transit for equal lengths of time"
          )
          .asDouble(dft.reluctance())
      )
      .withSpeed(
        c
          .of("speed")
          .since(V2_7)
          .summary("How fast does an escalator move horizontally?")
          .description("Horizontal speed of escalator in m/s.")
          .asDouble(dft.speed())
      );
  }

  private static void mapWalkPreferences(NodeAdapter root, WalkPreferences.Builder walk) {
    var dft = walk.original();
    NodeAdapter c = root.of("walk").since(V2_5).summary("Walking preferences.").asObject();
    walk
      .withSpeed(
        c
          .of("speed")
          .since(V2_0)
          .summary("The user's walking speed in meters/second.")
          .asDouble(dft.speed())
      )
      .withReluctance(
        c
          .of("reluctance")
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
          .of("boardCost")
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
          .of("safetyFactor")
          .since(V2_2)
          .summary("Factor for how much the walk safety is considered in routing.")
          .description(
            "Value should be between 0 and 1." + " If the value is set to be 0, safety is ignored."
          )
          .asDouble(dft.safetyFactor())
      )
      .withEscalator(escalator -> mapEscalatorPreferences(c, escalator));
  }
}
