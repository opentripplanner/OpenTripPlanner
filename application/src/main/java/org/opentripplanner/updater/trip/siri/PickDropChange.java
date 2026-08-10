package org.opentripplanner.updater.trip.siri;

import static java.lang.Boolean.TRUE;
import static org.opentripplanner.model.PickDrop.CANCELLED;
import static org.opentripplanner.model.PickDrop.NONE;
import static org.opentripplanner.model.PickDrop.SCHEDULED;

import java.util.Optional;
import org.opentripplanner.model.PickDrop;
import uk.org.siri.siri21.ArrivalBoardingActivityEnumeration;
import uk.org.siri.siri21.CallStatusEnumeration;
import uk.org.siri.siri21.DepartureBoardingActivityEnumeration;

/**
 * The pick/drop intent of a single call end (arrival or departure), captured from the SIRI data but
 * resolved to an OTP {@link PickDrop} only later, once the scheduled value is known.
 * <p>
 * This decouples the {@link CallWrapper} from the point where the scheduled pick/drop is available:
 * a caller can hold a {@code PickDropChange} and invoke {@link #applyTo(PickDrop)} whenever it has
 * the scheduled value, without keeping the wrapper (or any SIRI/JAXB type) in scope. The SIRI
 * enumerations are normalized away at construction, so this object carries no JAXB.
 */
public final class PickDropChange {

  /** The normalized boarding activity of the call end. */
  private enum Activity {
    /** Boarding/alighting is allowed (SIRI {@code BOARDING}/{@code ALIGHTING}). */
    ALLOWED,
    /** Boarding/alighting is not allowed (SIRI {@code NO_BOARDING}/{@code NO_ALIGHTING}). */
    NOT_ALLOWED,
    /** The vehicle passes through without stopping (SIRI {@code PASS_THRU}). */
    PASS_THRU,
    /** No boarding activity was reported. */
    UNSPECIFIED,
  }

  private final boolean cancelled;
  private final Activity activity;

  private PickDropChange(boolean cancelled, Activity activity) {
    this.cancelled = cancelled;
    this.activity = activity;
  }

  /**
   * Apply this change to the scheduled pick/drop value and return the effective value.
   * <p>
   * SIRI carries less information than the OTP pick/drop type, so the value is only changed when
   * routability changes; an empty result means "leave the scheduled value unchanged".
   *
   * @param scheduled the current pick drop value on a stopTime
   * @return the mapped {@link PickDrop} type, empty if routability is not changed
   */
  public Optional<PickDrop> applyTo(PickDrop scheduled) {
    if (cancelled) {
      return scheduled.isNotRoutable() ? Optional.empty() : Optional.of(CANCELLED);
    }
    return switch (activity) {
      case UNSPECIFIED -> Optional.empty();
      case ALLOWED -> scheduled.isNotRoutable() ? Optional.of(SCHEDULED) : Optional.empty();
      case NOT_ALLOWED -> Optional.of(NONE);
      case PASS_THRU -> Optional.of(CANCELLED);
    };
  }

  /**
   * The drop-off (arrival) change of a call.
   */
  static PickDropChange ofArrival(
    Boolean cancellation,
    CallStatusEnumeration arrivalStatus,
    ArrivalBoardingActivityEnumeration arrivalBoardingActivity
  ) {
    return new PickDropChange(
      isCancelled(cancellation, arrivalStatus),
      activityOf(arrivalBoardingActivity)
    );
  }

  /**
   * The pick-up (departure) change of a call.
   */
  static PickDropChange ofDeparture(
    Boolean cancellation,
    CallStatusEnumeration departureStatus,
    DepartureBoardingActivityEnumeration departureBoardingActivity
  ) {
    return new PickDropChange(
      isCancelled(cancellation, departureStatus),
      activityOf(departureBoardingActivity)
    );
  }

  private static boolean isCancelled(Boolean cancellation, CallStatusEnumeration status) {
    return TRUE.equals(cancellation) || status == CallStatusEnumeration.CANCELLED;
  }

  private static Activity activityOf(ArrivalBoardingActivityEnumeration activity) {
    if (activity == null) {
      return Activity.UNSPECIFIED;
    }
    return switch (activity) {
      case ALIGHTING -> Activity.ALLOWED;
      case NO_ALIGHTING -> Activity.NOT_ALLOWED;
      case PASS_THRU -> Activity.PASS_THRU;
    };
  }

  private static Activity activityOf(DepartureBoardingActivityEnumeration activity) {
    if (activity == null) {
      return Activity.UNSPECIFIED;
    }
    return switch (activity) {
      case BOARDING -> Activity.ALLOWED;
      case NO_BOARDING -> Activity.NOT_ALLOWED;
      case PASS_THRU -> Activity.PASS_THRU;
    };
  }
}
