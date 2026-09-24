package org.opentripplanner.model.plan.itineraryreference;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.legreference.LegReference;
import org.opentripplanner.routing.api.request.framework.DurationForEnum;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.transit.model.basic.TransitMode;

/**
 * A stable reference containing the information required to refetch an itinerary using
 * {@link org.opentripplanner.routing.refetch.RefetchItineraryService}.
 * <p>
 * The reference is serialized as a versioned opaque token and is independent of the API exposing
 * it.
 * <p>
 * Custom {@code WheelchairPreferences} are currently not encoded in the reference, nor is
 * {@code transferSlack} (the existing refetch implementation does not use it).
 * {@code from}/{@code to} on-board {@link org.opentripplanner.routing.api.request.TripLocation}s
 * are not currently supported.
 */
public record ItineraryReference(
  List<LegReference> legReferences,
  @Nullable GenericLocation from,
  @Nullable GenericLocation to,
  StreetMode accessMode,
  StreetMode egressMode,
  StreetMode transferMode,
  DurationForEnum<TransitMode> boardSlack,
  DurationForEnum<TransitMode> alightSlack,
  double walkSpeed,
  double walkReluctance,
  DurationForEnum<StreetMode> maxAccessEgressDuration,
  boolean wheelchair
) {
  public ItineraryReference {
    Objects.requireNonNull(legReferences);
    Objects.requireNonNull(accessMode);
    Objects.requireNonNull(egressMode);
    Objects.requireNonNull(transferMode);
    Objects.requireNonNull(boardSlack);
    Objects.requireNonNull(alightSlack);
    Objects.requireNonNull(maxAccessEgressDuration);
    // List.copyOf() also rejects null elements and guards against a caller mutating the list
    // after construction.
    legReferences = List.copyOf(legReferences);
    if (legReferences.isEmpty()) {
      throw new IllegalArgumentException("legReferences must not be empty");
    }
  }
}
