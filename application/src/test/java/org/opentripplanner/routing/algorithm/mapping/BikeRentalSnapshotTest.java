package org.opentripplanner.routing.algorithm.mapping;

import au.com.origin.snapshots.junit5.SnapshotExtension;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.modes.ExcludeAllTransitFilter;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.filter.AllowAllTransitFilter;
import org.opentripplanner.routing.error.RoutingValidationException;

@ExtendWith(SnapshotExtension.class)
@ResourceLock(Resources.LOCALE)
public class BikeRentalSnapshotTest extends SnapshotTestBase {

  private static final Locale DEFAULT_LOCALE = Locale.getDefault();

  static GenericLocation p1 = new GenericLocation(
    "SW Johnson St. & NW 24th Ave. (P1)",
    null,
    45.52832,
    -122.70059
  );

  static GenericLocation p2 = new GenericLocation(
    "NW Hoyt St. & NW 20th Ave. (P2)",
    null,
    45.52704,
    -122.69240
  );

  static GenericLocation p3 = new GenericLocation(
    "NW Everett St. & NW 5th Ave. (P3)",
    null,
    45.52523,
    -122.67525
  );

  @BeforeAll
  public static void beforeClass() {
    Locale.setDefault(Locale.US);
    loadGraphBeforeClass(false);
  }

  @AfterAll
  public static void afterClass() {
    Locale.setDefault(DEFAULT_LOCALE);
  }

  @DisplayName("Direct BIKE_RENTAL")
  @Test
  public void directBikeRental() {
    RouteRequest request = createTestRequest(2009, 10, 21, 16, 10, 0);

    request.journey().setModes(RequestModes.of().withDirectMode(StreetMode.BIKE_RENTAL).build());

    request.journey().transit().setFilters(List.of(ExcludeAllTransitFilter.of()));

    request.setFrom(p1);
    request.setTo(p2);

    expectArriveByToMatchDepartAtAndSnapshot(request);
  }

  /**
   * The next to two tests are an example where departAt and arriveBy searches return different (but
   * still correct) results.
   * <p>
   * It's probably down to the intersection traversal because when you use a constant cost they
   * become the same route again.
   * <p>
   * More discussion: https://github.com/opentripplanner/OpenTripPlanner/pull/3574
   */
  @DisplayName("Direct BIKE_RENTAL while keeping the bicycle at the destination with departAt")
  @Test
  public void directBikeRentalArrivingAtDestinationWithDepartAt() {
    RouteRequest request = createTestRequest(2009, 10, 21, 16, 10, 0);

    request.journey().setModes(RequestModes.of().withDirectMode(StreetMode.BIKE_RENTAL).build());
    request.journey().transit().setFilters(List.of(ExcludeAllTransitFilter.of()));

    allowArrivalWithRentalVehicle(request);
    request.setFrom(p1);
    request.setTo(p2);

    expectRequestResponseToMatchSnapshot(request);
  }

  @DisplayName("Direct BIKE_RENTAL while keeping the bicycle at the destination with arriveBy")
  @Test
  public void directBikeRentalArrivingAtDestinationWithArriveBy() {
    RouteRequest request = createTestRequest(2009, 10, 21, 16, 10, 0);

    request.journey().setModes(RequestModes.of().withDirectMode(StreetMode.BIKE_RENTAL).build());
    request.journey().transit().setFilters(List.of(ExcludeAllTransitFilter.of()));

    allowArrivalWithRentalVehicle(request);
    request.setFrom(p1);
    request.setTo(p2);
    request.setArriveBy(true);

    expectRequestResponseToMatchSnapshot(request);
  }

  @DisplayName("Access BIKE_RENTAL")
  @Test
  public void accessBikeRental() {
    RouteRequest request = createTestRequest(2009, 10, 21, 16, 14, 0);

    request
      .journey()
      .setModes(
        RequestModes.of()
          .withAccessMode(StreetMode.BIKE_RENTAL)
          .withEgressMode(StreetMode.WALK)
          .withDirectMode(StreetMode.NOT_SET)
          .withTransferMode(StreetMode.WALK)
          .build()
      );
    request.journey().transit().setFilters(List.of(AllowAllTransitFilter.of()));
    request.setFrom(p1);
    request.setTo(p3);

    try {
      expectArriveByToMatchDepartAtAndSnapshot(request);
    } catch (CompletionException e) {
      RoutingValidationException.unwrapAndRethrowCompletionException(e);
    }
  }

  @DisplayName("Egress BIKE_RENTAL")
  @Test
  public void egressBikeRental() {
    RouteRequest request = createTestRequest(2009, 10, 21, 16, 10, 0);

    request
      .journey()
      .setModes(
        RequestModes.of()
          .withAccessMode(StreetMode.WALK)
          .withEgressMode(StreetMode.BIKE_RENTAL)
          .withTransferMode(StreetMode.WALK)
          .withDirectMode(StreetMode.NOT_SET)
          .build()
      );
    request.journey().transit().setFilters(List.of(AllowAllTransitFilter.of()));
    request.setFrom(p3);
    request.setTo(p1);

    expectArriveByToMatchDepartAtAndSnapshot(request);
  }

  private void allowArrivalWithRentalVehicle(RouteRequest request) {
    request.withPreferences(preferences ->
      preferences.withBike(bike ->
        bike.withRental(rental -> rental.withAllowArrivingInRentedVehicleAtDestination(true))
      )
    );
  }
}
