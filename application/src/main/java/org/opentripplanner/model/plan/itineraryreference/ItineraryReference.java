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
 * Everything needed to refetch an itinerary later with
 * {@link org.opentripplanner.routing.refetch.RefetchItineraryService}. It is sent to clients as an
 * opaque, versioned token that does not depend on any particular API.
 * <p>
 * Not every itinerary can be referenced.
 * <p>
 * A refetch updates the same itinerary with current realtime data; it does not plan it again. So
 * the stored values are the ones used when the itinerary was planned.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public final class ItineraryReference {

  /**
   * The maximum number of transit legs in a reference. A plan never has more than 30 transfers, so
   * it never has more than 31 transit legs. The limit also caps the work a single refetch can do.
   */
  public static final int MAX_LEG_REFERENCES = 31;

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

  /** The street mode used to get from {@link #from()} to the first boarding stop. */
  public StreetMode accessMode() {
    return accessMode;
  }

  /** The street mode used to get from the last alighting stop to {@link #to()}. */
  public StreetMode egressMode() {
    return egressMode;
  }

  /** The street mode used for transfers between two transit legs at different stops. */
  public StreetMode transferMode() {
    return transferMode;
  }

  /** The default board slack. */
  public Duration boardSlack() {
    return boardSlack;
  }

  /** The default alight slack. */
  public Duration alightSlack() {
    return alightSlack;
  }

  /** The walk speed in meters per second. */
  public double walkSpeed() {
    return walkSpeed;
  }

  /** How much walking is avoided: the cost factor for time spent walking. */
  public double walkReluctance() {
    return walkReluctance;
  }

  /** Whether the itinerary was planned as wheelchair accessible. */
  public boolean wheelchair() {
    return wheelchair;
  }

  /** Returns this reference as an opaque token. */
  public String encode() {
    return ItineraryReferenceSerializer.encode(this);
  }

  /** Reads a token created by {@link #encode()}. Returns {@code null} if it is not valid. */
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
