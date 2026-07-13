package org.opentripplanner.ext.carpooling;

import static org.opentripplanner._support.time.ZoneIds.UTC;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_00;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_55;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZonedDateTime;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.transit.model.TransitTestEnvironment;

/**
 * Standard itinerary data for carpooling filter tests.
 * <p>
 * The base itinerary departs at 11:00 UTC and arrives at 11:55 UTC on {@link #SERVICE_DAY}.
 */
public class CarpoolItineraryTestData {

  public static final LocalDate SERVICE_DAY = LocalDate.of(2020, Month.FEBRUARY, 2);

  private static final Place ORIGIN;
  private static final Place DESTINATION;

  static {
    var builder = TransitTestEnvironment.of(SERVICE_DAY);
    ORIGIN = Place.forStop(builder.stop("A", c -> c.withCoordinate(5.0, 8.0)));
    DESTINATION = Place.forStop(builder.stop("B", c -> c.withCoordinate(6.0, 8.5)));
    builder.build();
  }

  /** Standard carpool itinerary: departs 11:00 UTC, arrives 11:55 UTC on {@link #SERVICE_DAY}. */
  public static Itinerary driveItinerary() {
    return newItinerary(ORIGIN).drive(T11_00, T11_55, DESTINATION).build();
  }

  /** UTC instant at {@link #SERVICE_DAY} {@code hour:minute}. */
  public static Instant at(int hour, int minute) {
    return ZonedDateTime.of(SERVICE_DAY, LocalTime.of(hour, minute), UTC).toInstant();
  }

  private CarpoolItineraryTestData() {}
}
