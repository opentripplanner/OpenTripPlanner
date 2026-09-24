package org.opentripplanner.routing.refetch;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.itineraryreference.ItineraryReference;
import org.opentripplanner.model.plan.legreference.LegReference;
import org.opentripplanner.model.plan.legreference.ScheduledTransitLegReference;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.framework.DurationForEnum;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.transit.model.basic.TransitMode;

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

  public static ItineraryReference toItineraryReference(
    Itinerary itinerary,
    RouteRequest routeRequest
  ) {
    var legReferences = itinerary
      .legs()
      .stream()
      .map(Leg::legReference)
      .filter(Objects::nonNull)
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
      preferences.transit().boardSlack(),
      preferences.transit().alightSlack(),
      preferences.walk().speed(),
      preferences.walk().reluctance(),
      preferences.street().accessEgress().maxDuration(),
      routeRequest.journey().wheelchair()
    );
  }

  public static Itinerary refetch(
    ItineraryReference ref,
    RouteRequest defaultRouteRequest,
    RefetchItineraryService refetchItineraryService
  ) {
    var routeRequest = defaultRouteRequest
      .copyOf()
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
              .withBoardSlack(b ->
                b
                  .withDefault(ref.boardSlack().defaultValue())
                  .withValues(fullValueMap(ref.boardSlack(), TransitMode.class))
              )
              .withAlightSlack(b ->
                b
                  .withDefault(ref.alightSlack().defaultValue())
                  .withValues(fullValueMap(ref.alightSlack(), TransitMode.class))
              )
          )
          .withWalk(walk -> walk.withSpeed(ref.walkSpeed()).withReluctance(ref.walkReluctance()))
          .withStreet(street ->
            street.withAccessEgress(accessEgress ->
              accessEgress.withMaxDuration(
                ref.maxAccessEgressDuration().defaultValue(),
                fullValueMap(ref.maxAccessEgressDuration(), StreetMode.class)
              )
            )
          )
      )
      .buildRequest();

    return refetchItineraryService.refetchItinerary(
      ref.from(),
      ref.to(),
      ref.legReferences(),
      routeRequest
    );
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

  /**
   * Resolves every enum constant to its effective value ({@code isSet} override or default).
   * Used so overriding a {@link DurationForEnum} on a {@link RouteRequest} builder fully replaces
   * it rather than merging on top of whatever {@code defaultRouteRequest} already had - the
   * builder's convenience overloads only add/replace the given entries, they don't clear
   * pre-existing ones for modes absent from the map.
   */
  private static <E extends Enum<E>> Map<E, Duration> fullValueMap(
    DurationForEnum<E> value,
    Class<E> type
  ) {
    return Arrays.stream(type.getEnumConstants()).collect(Collectors.toMap(e -> e, value::valueOf));
  }
}
