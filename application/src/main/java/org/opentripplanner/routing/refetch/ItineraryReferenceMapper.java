package org.opentripplanner.routing.refetch;

import java.util.EnumSet;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.itineraryreference.BikeRentalPreferences;
import org.opentripplanner.model.plan.itineraryreference.CarRentalPreferences;
import org.opentripplanner.model.plan.itineraryreference.ItineraryReference;
import org.opentripplanner.model.plan.itineraryreference.ScooterRentalPreferences;
import org.opentripplanner.model.plan.leg.FrequencyTransitLeg;
import org.opentripplanner.model.plan.legreference.LegReference;
import org.opentripplanner.model.plan.legreference.ScheduledTransitLegReference;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.preference.RoutingPreferencesBuilder;
import org.opentripplanner.routing.api.request.request.JourneyRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.street.model.StreetMode;

/**
 * Creates an {@link ItineraryReference} from an {@link Itinerary} and the {@link RouteRequest} that
 * planned it, and turns a reference back into a request for {@link ItineraryRefetchService}.
 * <p>
 * Only the transit legs are stored, as {@link LegReference}s. Street legs are not stored, because
 * the refetch creates them again.
 * <p>
 * Access and egress may walk or use a rental vehicle. For a rental mode, the preferences that a
 * client can set for it are stored too, so the refetch routes the vehicle the same way. Transfers
 * may only walk.
 */
public final class ItineraryReferenceMapper {

  private static final Set<StreetMode> SUPPORTED_ACCESS_EGRESS_MODES = EnumSet.of(
    StreetMode.WALK,
    StreetMode.BIKE_RENTAL,
    StreetMode.CAR_RENTAL,
    StreetMode.SCOOTER_RENTAL
  );

  private ItineraryReferenceMapper() {}

  /**
   * Creates a reference for {@code itinerary}, planned with {@code routeRequest}.
   *
   * @throws UnsupportedItineraryReferenceException if the itinerary cannot be referenced
   */
  public static ItineraryReference toItineraryReference(
    Itinerary itinerary,
    RouteRequest routeRequest
  ) {
    var legReferences = itinerary
      .legs()
      .stream()
      .filter(leg -> !leg.isStreetLeg())
      .map(ItineraryReferenceMapper::requireLegReference)
      .map(ItineraryReferenceMapper::requireSupportedLegReference)
      .toList();

    if (legReferences.isEmpty()) {
      throw new UnsupportedItineraryReferenceException(
        "Itinerary has no supported transit leg references"
      );
    }
    if (legReferences.size() > ItineraryReference.MAX_LEG_REFERENCES) {
      throw new UnsupportedItineraryReferenceException(
        "Itinerary has more than " + ItineraryReference.MAX_LEG_REFERENCES + " transit legs"
      );
    }

    requireSupportedLocation(routeRequest.from());
    requireSupportedLocation(routeRequest.to());
    var journey = routeRequest.journey();
    requireSupportedAccessEgressMode(journey.access().mode(), "access");
    requireSupportedAccessEgressMode(journey.egress().mode(), "egress");
    requireWalkMode(journey.transfer().mode(), "transfer");
    requireNoViaLocations(routeRequest);

    var preferences = routeRequest.preferences();
    return new ItineraryReference(
      legReferences,
      routeRequest.from(),
      routeRequest.to(),
      journey.access().mode(),
      journey.egress().mode(),
      journey.transfer().mode(),
      preferences.transit().boardSlack().defaultValue(),
      preferences.transit().alightSlack().defaultValue(),
      preferences.walk().speed(),
      preferences.walk().reluctance(),
      preferences.walk().safetyFactor(),
      journey.wheelchair(),
      bikeRental(journey, preferences),
      carRental(journey, preferences),
      scooterRental(journey, preferences)
    );
  }

  /**
   * Builds the request for a refetch: the values stored in {@code ref} on top of
   * {@code defaultRouteRequest}. Values that are not stored, like
   * {@code maxAccessEgressDuration} or the rental pickup time, come from
   * {@code defaultRouteRequest}.
   * <p>
   * A reference decoded from a client's id may come from another OTP version, so its modes are
   * checked like when the id was created.
   *
   * @throws ItineraryRefetchException if the request cannot be built
   */
  public static RouteRequest toRouteRequest(
    ItineraryReference ref,
    RouteRequest defaultRouteRequest
  ) {
    requireSupportedAccessEgressMode(ref.accessMode(), "access");
    requireSupportedAccessEgressMode(ref.egressMode(), "egress");
    requireWalkMode(ref.transferMode(), "transfer");
    try {
      return defaultRouteRequest
        .copyOf()
        .withFrom(ref.from() != null ? ref.from() : boardStopLocation(ref))
        .withTo(ref.to() != null ? ref.to() : alightStopLocation(ref))
        .withJourney(journey ->
          journey
            .withAccess(new StreetRequest(ref.accessMode()))
            .withEgress(new StreetRequest(ref.egressMode()))
            .withTransfer(new StreetRequest(ref.transferMode()))
            .withWheelchair(ref.wheelchair())
        )
        .withPreferences(preferences -> setStoredPreferences(preferences, ref))
        .buildRequest();
    } catch (RoutingValidationException | IllegalArgumentException e) {
      throw new ItineraryRefetchException("Could not rebuild route request: " + e.getMessage());
    }
  }

  private static void setStoredPreferences(
    RoutingPreferencesBuilder preferences,
    ItineraryReference ref
  ) {
    preferences
      .withTransit(transit ->
        transit
          .withBoardSlack(b -> b.withDefault(ref.boardSlack()))
          .withAlightSlack(b -> b.withDefault(ref.alightSlack()))
      )
      .withWalk(walk ->
        walk
          .withSpeed(ref.walkSpeed())
          .withReluctance(ref.walkReluctance())
          .withSafetyFactor(ref.walkSafetyFactor())
      );
    var bikeRental = ref.bikeRental();
    if (bikeRental != null) {
      preferences.withBike(bike -> RentalPreferencesMapper.setBikeRental(bike, bikeRental));
    }
    var carRental = ref.carRental();
    if (carRental != null) {
      preferences.withCar(car -> RentalPreferencesMapper.setCarRental(car, carRental));
    }
    var scooterRental = ref.scooterRental();
    if (scooterRental != null) {
      preferences.withScooter(scooter ->
        RentalPreferencesMapper.setScooterRental(scooter, scooterRental)
      );
    }
  }

  @Nullable
  private static BikeRentalPreferences bikeRental(
    JourneyRequest journey,
    RoutingPreferences preferences
  ) {
    return usesAccessOrEgressMode(journey, StreetMode.BIKE_RENTAL)
      ? RentalPreferencesMapper.toBikeRental(preferences.bike())
      : null;
  }

  @Nullable
  private static CarRentalPreferences carRental(
    JourneyRequest journey,
    RoutingPreferences preferences
  ) {
    return usesAccessOrEgressMode(journey, StreetMode.CAR_RENTAL)
      ? RentalPreferencesMapper.toCarRental(preferences.car())
      : null;
  }

  @Nullable
  private static ScooterRentalPreferences scooterRental(
    JourneyRequest journey,
    RoutingPreferences preferences
  ) {
    return usesAccessOrEgressMode(journey, StreetMode.SCOOTER_RENTAL)
      ? RentalPreferencesMapper.toScooterRental(preferences.scooter())
      : null;
  }

  private static boolean usesAccessOrEgressMode(JourneyRequest journey, StreetMode mode) {
    return journey.access().mode() == mode || journey.egress().mode() == mode;
  }

  /** The stop the first encoded transit leg boards at. */
  private static GenericLocation boardStopLocation(ItineraryReference ref) {
    return GenericLocation.fromStopId(
      requireScheduledTransitLegReference(ref.legReferences().getFirst()).fromStopId()
    );
  }

  /** The stop the last encoded transit leg alights at. */
  private static GenericLocation alightStopLocation(ItineraryReference ref) {
    return GenericLocation.fromStopId(
      requireScheduledTransitLegReference(ref.legReferences().getLast()).toStopId()
    );
  }

  /**
   * A decoded token may in the future hold other leg reference types. Report them as unsupported
   * instead of failing with a {@link ClassCastException}.
   */
  private static ScheduledTransitLegReference requireScheduledTransitLegReference(
    LegReference legReference
  ) {
    if (legReference instanceof ScheduledTransitLegReference scheduledTransitLegReference) {
      return scheduledTransitLegReference;
    }
    throw new UnsupportedItineraryReferenceException(
      "Unsupported leg reference type: " + legReference.getClass().getSimpleName()
    );
  }

  /**
   * A transit leg without a reference (for example flex) cannot be stored. Skipping it would give an
   * id for a different trip, so the whole itinerary is unsupported.
   */
  private static LegReference requireLegReference(Leg leg) {
    if (leg instanceof FrequencyTransitLeg) {
      throw new UnsupportedItineraryReferenceException(
        "Frequency-based transit legs cannot be refetched: " + leg.getClass().getSimpleName()
      );
    }
    var legReference = leg.legReference();
    if (legReference == null) {
      throw new UnsupportedItineraryReferenceException(
        "Leg without a LegReference is not supported: " + leg.getClass().getSimpleName()
      );
    }
    return legReference;
  }

  private static LegReference requireSupportedLegReference(LegReference legReference) {
    if (legReference instanceof ScheduledTransitLegReference) {
      return legReference;
    }
    throw new UnsupportedItineraryReferenceException(
      "Unsupported leg reference type: " + legReference.getClass().getSimpleName()
    );
  }

  private static void requireSupportedLocation(@Nullable GenericLocation location) {
    if (location != null && location.isOnBoard()) {
      throw new UnsupportedItineraryReferenceException(
        "On-board TripLocation is not supported in ItineraryReference: " + location
      );
    }
  }

  /** Via locations are not stored, so an itinerary planned with them is not supported yet. */
  private static void requireNoViaLocations(RouteRequest routeRequest) {
    if (routeRequest.isViaSearch()) {
      throw new UnsupportedItineraryReferenceException(
        "Via locations are not supported in ItineraryReference"
      );
    }
  }

  /**
   * Only walk and rental preferences are stored. An access or egress with an own bike or car, with
   * parking, or with flex would be refetched with the server's preferences and could change, so it
   * is not supported yet.
   */
  private static void requireSupportedAccessEgressMode(StreetMode mode, String subject) {
    if (!SUPPORTED_ACCESS_EGRESS_MODES.contains(mode)) {
      throw new UnsupportedItineraryReferenceException(
        "Only walk and rental " + subject + " are supported in ItineraryReference: " + mode
      );
    }
  }

  /**
   * Transfers are read from the regular transfers of the request. Only walking transfers are
   * supported yet.
   */
  private static void requireWalkMode(StreetMode mode, String subject) {
    if (mode != StreetMode.WALK) {
      throw new UnsupportedItineraryReferenceException(
        "Only walk " + subject + " is supported in ItineraryReference: " + mode
      );
    }
  }
}
