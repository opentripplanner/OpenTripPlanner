package org.opentripplanner.model.plan.itineraryreference;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.legreference.LegReference;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.utils.tostring.ToStringBuilder;

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
 * <p>
 * Only walk preferences are encoded, so only itineraries with walk access, egress and transfers
 * get a reference. Refetching a bike, car or scooter leg would otherwise silently use the server's
 * default preferences for that mode and could return a different itinerary. Encoding the
 * preferences of the remaining modes requires covering their nested parking, rental and
 * optimisation preferences too, and is left to a follow-up.
 * <p>
 * A refetch is meant to update the same itinerary with current realtime data, not to route it
 * again. The encoded values (walk speed and reluctance, default board/alight slack, wheelchair) are
 * therefore the values that were in effect when the itinerary was planned, whether the client set
 * them or they came from the server defaults; later changes to those defaults do not affect an
 * existing reference. Values no supported API lets a client set - per-mode board/alight slack
 * overrides and {@code maxAccessEgressDuration} - are not encoded and are read from the current
 * {@code defaultRouteRequest} at refetch time, so a client-controlled id cannot inject them.
 * {@code boardSlack}/{@code alightSlack} are validated at decode time
 * ({@link ItineraryReferenceSerializer#decode}) against the same bound the GTFS API enforces on
 * client input.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public final class ItineraryReference {

  private final List<LegReference> legReferences;

  @Nullable
  private final GenericLocation from;

  @Nullable
  private final GenericLocation to;

  private final StreetMode accessMode;
  private final StreetMode egressMode;
  private final StreetMode transferMode;
  private final Duration boardSlack;
  private final Duration alightSlack;
  private final double walkSpeed;
  private final double walkReluctance;
  private final boolean wheelchair;

  public ItineraryReference(
    List<LegReference> legReferences,
    @Nullable GenericLocation from,
    @Nullable GenericLocation to,
    StreetMode accessMode,
    StreetMode egressMode,
    StreetMode transferMode,
    Duration boardSlack,
    Duration alightSlack,
    double walkSpeed,
    double walkReluctance,
    boolean wheelchair
  ) {
    // List.copyOf() also rejects null elements and guards against a caller mutating the list
    // after construction.
    this.legReferences = List.copyOf(Objects.requireNonNull(legReferences));
    if (this.legReferences.isEmpty()) {
      throw new IllegalArgumentException("legReferences must not be empty");
    }
    this.from = from;
    this.to = to;
    this.accessMode = Objects.requireNonNull(accessMode);
    this.egressMode = Objects.requireNonNull(egressMode);
    this.transferMode = Objects.requireNonNull(transferMode);
    this.boardSlack = Objects.requireNonNull(boardSlack);
    this.alightSlack = Objects.requireNonNull(alightSlack);
    this.walkSpeed = walkSpeed;
    this.walkReluctance = walkReluctance;
    this.wheelchair = wheelchair;
  }

  /** The transit legs of the itinerary, in travel order. Never empty. */
  public List<LegReference> legReferences() {
    return legReferences;
  }

  /** The origin of the original search, or {@code null} to start at the first boarding stop. */
  @Nullable
  public GenericLocation from() {
    return from;
  }

  /** The destination of the original search, or {@code null} to end at the last alighting stop. */
  @Nullable
  public GenericLocation to() {
    return to;
  }

  public StreetMode accessMode() {
    return accessMode;
  }

  public StreetMode egressMode() {
    return egressMode;
  }

  public StreetMode transferMode() {
    return transferMode;
  }

  /** The default board slack; per-mode overrides are read from the current server config. */
  public Duration boardSlack() {
    return boardSlack;
  }

  /** The default alight slack; per-mode overrides are read from the current server config. */
  public Duration alightSlack() {
    return alightSlack;
  }

  public double walkSpeed() {
    return walkSpeed;
  }

  public double walkReluctance() {
    return walkReluctance;
  }

  public boolean wheelchair() {
    return wheelchair;
  }

  /**
   * The opaque, versioned token for this reference. Delegates to
   * {@link ItineraryReferenceSerializer#encode}.
   */
  public String encode() {
    return ItineraryReferenceSerializer.encode(this);
  }

  /** Delegates to {@link ItineraryReferenceSerializer#decode}. */
  @Nullable
  public static ItineraryReference decode(@Nullable String itineraryReference) {
    return ItineraryReferenceSerializer.decode(itineraryReference);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ItineraryReference that = (ItineraryReference) o;
    return (
      Double.compare(walkSpeed, that.walkSpeed) == 0 &&
      Double.compare(walkReluctance, that.walkReluctance) == 0 &&
      wheelchair == that.wheelchair &&
      legReferences.equals(that.legReferences) &&
      Objects.equals(from, that.from) &&
      Objects.equals(to, that.to) &&
      accessMode == that.accessMode &&
      egressMode == that.egressMode &&
      transferMode == that.transferMode &&
      boardSlack.equals(that.boardSlack) &&
      alightSlack.equals(that.alightSlack)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      legReferences,
      from,
      to,
      accessMode,
      egressMode,
      transferMode,
      boardSlack,
      alightSlack,
      walkSpeed,
      walkReluctance,
      wheelchair
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(ItineraryReference.class)
      .addCol("legReferences", legReferences)
      .addObj("from", from)
      .addObj("to", to)
      .addEnum("accessMode", accessMode)
      .addEnum("egressMode", egressMode)
      .addEnum("transferMode", transferMode)
      .addDuration("boardSlack", boardSlack)
      .addDuration("alightSlack", alightSlack)
      .addNum("walkSpeed", walkSpeed)
      .addNum("walkReluctance", walkReluctance)
      .addBool("wheelchair", wheelchair)
      .toString();
  }
}
