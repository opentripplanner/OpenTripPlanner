package org.opentripplanner.routing.refetch;

import javax.annotation.Nullable;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.itineraryreference.ItineraryReference;
import org.opentripplanner.model.plan.leg.FrequencyTransitLeg;
import org.opentripplanner.model.plan.legreference.LegReference;
import org.opentripplanner.model.plan.legreference.ScheduledTransitLegReference;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.error.RoutingValidationException;

/**
 * Maps an {@link Itinerary} and its original {@link RouteRequest} to a stable
 * {@link ItineraryReference}, and reconstructs the inputs required by
 * {@link RefetchItineraryService}.
 * <p>
 * Street legs do not form part of the serialized transit spine. Supported transit
 * {@link LegReference}s are extracted from the itinerary while generated access, transfer and
 * egress street legs are ignored.
 */
public final class ItineraryReferenceMapper {

  private ItineraryReferenceMapper() {}

  /**
   * Builds a stable {@link ItineraryReference} for {@code itinerary}, given the
   * {@link RouteRequest} that produced it.
   *
   * @throws UnsupportedItineraryReferenceException if {@code itinerary} has no supported transit
   * leg reference, if any non-street leg has no {@link LegReference} at all, or if
   * {@code routeRequest}'s {@code from}/{@code to} is an on-board {@code TripLocation}.
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

    requireSupportedLocation(routeRequest.from());
    requireSupportedLocation(routeRequest.to());

    var preferences = routeRequest.preferences();
    return new ItineraryReference(
      legReferences,
      routeRequest.from(),
      routeRequest.to(),
      routeRequest.journey().access().mode(),
      routeRequest.journey().egress().mode(),
      routeRequest.journey().transfer().mode(),
      preferences.transit().boardSlack().defaultValue(),
      preferences.transit().alightSlack().defaultValue(),
      preferences.walk().speed(),
      preferences.walk().reluctance(),
      routeRequest.journey().wheelchair()
    );
  }

  /**
   * Reconstructs the {@link RouteRequest} that was in effect when {@code ref}'s itinerary was
   * originally planned, by overlaying {@code ref}'s fields onto {@code defaultRouteRequest}.
   * Callers pass the result, together with {@link ItineraryReference#legReferences()}, to
   * {@link RefetchItineraryService#refetchItinerary}.
   * <p>
   * {@code defaultRouteRequest}'s {@code maxAccessEgressDuration} and per-mode board/alight slack
   * overrides are left untouched, so they always reflect the current server configuration rather
   * than whatever they were when the itinerary was originally planned - see
   * {@link ItineraryReference}'s class doc.
   * <p>
   * Throws {@link RefetchItineraryException}, not the underlying {@code RoutingValidationException}
   * or {@code IllegalArgumentException}, so that callers producing a client-facing result need to
   * catch only one exception type for every way a reference can turn out not to be refetchable.
   */
  public static RouteRequest toRouteRequest(
    ItineraryReference ref,
    RouteRequest defaultRouteRequest
  ) {
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
        .withPreferences(preferences ->
          preferences
            .withTransit(transit ->
              transit
                .withBoardSlack(b -> b.withDefault(ref.boardSlack()))
                .withAlightSlack(b -> b.withDefault(ref.alightSlack()))
            )
            .withWalk(walk -> walk.withSpeed(ref.walkSpeed()).withReluctance(ref.walkReluctance()))
        )
        .buildRequest();
    } catch (RoutingValidationException | IllegalArgumentException e) {
      throw new RefetchItineraryException("Could not rebuild route request: " + e.getMessage());
    }
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
   * {@code decode()} does not itself guarantee every leg reference is a
   * {@link ScheduledTransitLegReference} - the only type {@code LegReferenceSerializer} produces
   * today, but not something this class should assume holds for every future type. A mismatch here
   * means the reference, though well-formed, cannot be used, so it is reported the same way an
   * unsupported leg reference is reported at encode time, rather than failing with a
   * {@link ClassCastException} the resolver would not otherwise catch.
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
   * Street legs are regenerated by the refetch itself and carry no {@link LegReference}, so they
   * are filtered out before this is reached. Any remaining leg without a reference (flex, carpool)
   * cannot be encoded, and dropping it silently would produce an id that refetches a different
   * trip - so the whole itinerary is reported as unsupported instead.
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
}
