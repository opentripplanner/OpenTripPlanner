package org.opentripplanner.transfer.configure;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.preference.RoutingPreferencesBuilder;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferParameters;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfile;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileType;

class TransferRequestDifferencesTest {

  private static final RouteRequest DEFAULT = RouteRequest.defaultValue();

  @Test
  void equalRequestsHaveNoDifferences() {
    for (var type : TransferProfileType.values()) {
      assertThat(TransferRequestDifferences.of(type, DEFAULT, DEFAULT)).isEmpty();
    }
  }

  @Test
  void aWalkDifferenceIsReportedWithTheValueOfBoth() {
    var transfers = request(p -> p.withWalk(w -> w.withStairsReluctance(1.5)));
    var routingDefaults = request(p -> p.withWalk(w -> w.withStairsReluctance(3.0)));

    assertThat(
      TransferRequestDifferences.of(TransferProfileType.WALK, transfers, routingDefaults)
    ).containsExactly("walk.stairsReluctance: 1.5 (transfers), 3.0 (routingDefaults)");
  }

  @Test
  void theEscalatorOfTheWalkIsCompared() {
    var transfers = request(p -> p.withWalk(w -> w.withEscalator(e -> e.withSpeed(0.5))));

    assertThat(TransferRequestDifferences.of(TransferProfileType.WALK, transfers, DEFAULT)).hasSize(
      1
    );
  }

  @Test
  void theUserPreferencesAreNotCompared() {
    var transfers = request(p ->
      p.withWalk(w -> w.withSpeed(1.0).withReluctance(5.0).withSafetyFactor(0.5))
    );

    assertThat(
      TransferRequestDifferences.of(TransferProfileType.WALK, transfers, DEFAULT)
    ).isEmpty();
  }

  @Test
  void onlyTheParametersOfTheTypeAreCompared() {
    var transfers = request(p -> p.withWalk(w -> w.withStairsReluctance(1.5)));

    assertThat(
      TransferRequestDifferences.of(TransferProfileType.CAR, transfers, DEFAULT)
    ).isEmpty();
    assertThat(
      TransferRequestDifferences.of(TransferProfileType.WHEELCHAIR, transfers, DEFAULT)
    ).isEmpty();
  }

  @Test
  void theWheelchairAccessibilityOfStopsIsCompared() {
    var transfers = request(p -> p.withWheelchair(w -> w.withStopCost(100, 1000)));
    var routingDefaults = request(p -> p.withWheelchair(w -> w.withStopCost(600, 3600)));

    var result = TransferRequestDifferences.of(
      TransferProfileType.WHEELCHAIR,
      transfers,
      routingDefaults
    );

    assertThat(result).containsExactly(
      "wheelchair.stop.unknownCost: 100 (transfers), 600 (routingDefaults)",
      "wheelchair.stop.inaccessibleCost: 1000 (transfers), 3600 (routingDefaults)"
    );
  }

  @Test
  void theCostsOfAStopAreNotComparedIfOneRequestOnlyConsidersAccessibleStops() {
    var lenient = request(p -> p.withWheelchair(w -> w.withStopCost(600, 3600)));

    var result = TransferRequestDifferences.of(TransferProfileType.WHEELCHAIR, DEFAULT, lenient);

    assertThat(result).containsExactly(
      "wheelchair.stop.onlyConsiderAccessible: true (transfers), false (routingDefaults)"
    );
  }

  @Test
  void theTripAccessibilityOfTheWheelchairIsNotCompared() {
    var transfers = request(p -> p.withWheelchair(w -> w.withTripCost(100, 1000)));

    assertThat(
      TransferRequestDifferences.of(TransferProfileType.WHEELCHAIR, transfers, DEFAULT)
    ).isEmpty();
  }

  @Test
  void theWalkingWithTheBicycleIsCompared() {
    var transfers = request(p ->
      p.withBike(b -> b.withWalking(w -> w.withStairsReluctance(4.0).withMountDismountCost(5)))
    );

    assertThat(
      TransferRequestDifferences.of(TransferProfileType.BICYCLE, transfers, DEFAULT)
    ).hasSize(2);
  }

  @Test
  void oneWarningIsCreatedForEachTypeThatDiffers() {
    var walk = request(p -> p.withWalk(w -> w.withStairsReluctance(1.5)));
    var parameters = new TransferParameters<>(
      List.of(
        new TransferProfile<>("walk", TransferProfileType.WALK, null, walk),
        new TransferProfile<>("car", TransferProfileType.CAR, null, DEFAULT)
      )
    );

    var warnings = TransferRequestDifferences.warnings(parameters, DEFAULT);

    assertThat(warnings).hasSize(1);
    assertThat(warnings.getFirst()).contains("'walk'");
    assertThat(warnings.getFirst()).contains("walk.stairsReluctance");
  }

  private static RouteRequest request(Consumer<RoutingPreferencesBuilder> body) {
    return DEFAULT.copyOf().withPreferences(body).buildDefault();
  }
}
