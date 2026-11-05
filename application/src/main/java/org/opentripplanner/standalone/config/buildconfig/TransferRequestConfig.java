package org.opentripplanner.standalone.config.buildconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_10;

import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.preference.EscalatorPreferences;
import org.opentripplanner.routing.api.request.preference.VehicleWalkingPreferences;
import org.opentripplanner.routing.api.request.preference.WalkPreferences;
import org.opentripplanner.routing.api.request.preference.WheelchairPreferences;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.routerequest.WheelchairConfig;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileType;

/**
 * Maps the street search parameters of a transfer profile type, which are set in the block of the
 * type, for example {@code transfers.walk}, to the request the street search of all the profiles of
 * the type is based on.
 * <p>
 * Only the parameters that are not user preferences are set here: speed, reluctance and the other
 * user preferences are set by the {@code userPreferences} of the profiles. Parameters that the
 * street search does not use for the type are not accepted. The request is {@code RouteRequest}'s
 * default for everything else.
 */
final class TransferRequestConfig {

  private TransferRequestConfig() {}

  /** The request of the profiles of {@code type}, read from the block of the type. */
  static RouteRequest mapRequest(NodeAdapter block, TransferProfileType type) {
    var builder = RouteRequest.defaultValue().copyOf();
    switch (type) {
      case WALK -> builder.withPreferences(p -> p.withWalk(walk -> mapWalk(block, walk)));
      case WHEELCHAIR -> builder.withPreferences(p ->
        p.withWheelchair(wheelchair -> mapWheelchair(block, wheelchair))
      );
      case BICYCLE -> builder.withPreferences(p ->
        p.withBike(bike -> bike.withWalking(walking -> mapBicycleWalking(block, walking)))
      );
      case CAR -> {
        // The street search needs no parameters other than the car reluctance, a user preference
      }
    }
    return builder.buildDefault();
  }

  private static void mapWalk(NodeAdapter c, WalkPreferences.Builder walk) {
    var dft = walk.original();
    walk
      .withStairsReluctance(
        c
          .of("stairsReluctance")
          .since(V2_10)
          .summary(
            "A multiplier to specify how bad walking on stairs is, on top of the reluctance of the user preferences."
          )
          .asDouble(dft.stairsReluctance())
      )
      .withStairsTimeFactor(
        c
          .of("stairsTimeFactor")
          .since(V2_10)
          .summary(
            "How much more time does it take to walk a flight of stairs compared to walking a similar horizontal length."
          )
          .asDouble(dft.stairsTimeFactor())
      )
      .withEscalator(escalator -> mapEscalator(c, escalator));
  }

  private static void mapEscalator(NodeAdapter root, EscalatorPreferences.Builder escalator) {
    var dft = escalator.original();
    var c = root.of("escalator").since(V2_10).summary("Escalator preferences.").asObject();
    escalator
      .withReluctance(
        c
          .of("reluctance")
          .since(V2_10)
          .summary(
            "A multiplier for how bad being in an escalator is compared to being in transit for equal lengths of time"
          )
          .asDouble(dft.reluctance())
      )
      .withSpeed(
        c
          .of("speed")
          .since(V2_10)
          .summary("How fast does an escalator move horizontally, in m/s?")
          .asDouble(dft.speed())
      );
  }

  /**
   * The wheelchair preferences of the request, without the {@code trip} accessibility: the trips
   * are not used by the transfer search.
   */
  private static void mapWheelchair(NodeAdapter c, WheelchairPreferences.Builder builder) {
    var dft = builder.original();
    builder
      .withStop(it ->
        WheelchairConfig.mapAccessibilityPreferences(
          c
            .of("stop")
            .since(V2_10)
            .summary("Configuration for when to use inaccessible stops.")
            .description(
              """
              The default is to only use the stops that are marked as wheelchair accessible. Many
              deployments have little accessibility information, and get almost no transfers with
              this default. Set `unknownCost` and `inaccessibleCost` to use the other stops with a
              cost.
              """
            )
            .asObject(),
          it
        )
      )
      .withElevator(it ->
        WheelchairConfig.mapAccessibilityPreferences(
          c
            .of("elevator")
            .since(V2_10)
            .summary("Configuration for when to use inaccessible elevators.")
            .asObject(),
          it
        )
      )
      .withInaccessibleStreetReluctance(
        c
          .of("inaccessibleStreetReluctance")
          .since(V2_10)
          .summary(
            "The factor to multiply the cost of traversing a street edge that is not wheelchair-accessible."
          )
          .asDouble(dft.inaccessibleStreetReluctance())
      )
      .withMaxSlope(
        c
          .of("maxSlope")
          .since(V2_10)
          .summary("The maximum slope as a fraction of 1.")
          .description("9 percent would be `0.09`")
          .asDouble(dft.maxSlope())
      )
      .withSlopeExceededReluctance(
        c
          .of("slopeExceededReluctance")
          .since(V2_10)
          .summary("How much streets with high slope should be avoided.")
          .asDouble(dft.slopeExceededReluctance())
      )
      .withStairsReluctance(
        c
          .of("stairsReluctance")
          .since(V2_10)
          .summary("How much stairs should be avoided.")
          .asDouble(dft.stairsReluctance())
      );
  }

  private static void mapBicycleWalking(
    NodeAdapter root,
    VehicleWalkingPreferences.Builder walking
  ) {
    var dft = walking.original();
    var c = root
      .of("walking")
      .since(V2_10)
      .summary("Parameters for walking with the bicycle, on top of the `walk` user preferences.")
      .asObject();
    walking
      .withStairsReluctance(
        c
          .of("stairsReluctance")
          .since(V2_10)
          .summary(
            "How bad is it to walk the bicycle up/down a flight of stairs, on top of the reluctance."
          )
          .asDouble(dft.stairsReluctance())
      )
      .withMountDismountTime(
        c
          .of("mountDismountTime")
          .since(V2_10)
          .summary("The time it takes the user to hop on or off the bicycle.")
          .asDuration(dft.mountDismountTime())
      )
      .withMountDismountCost(
        c
          .of("mountDismountCost")
          .since(V2_10)
          .summary("The cost of hopping on or off the bicycle.")
          .asInt(dft.mountDismountCost().toSeconds())
      );
  }
}
