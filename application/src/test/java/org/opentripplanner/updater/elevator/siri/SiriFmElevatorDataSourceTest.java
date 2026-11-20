package org.opentripplanner.updater.elevator.siri;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.service.elevatorstatus.model.ElevatorStatus.inService;
import static org.opentripplanner.service.elevatorstatus.model.ElevatorStatus.outOfService;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.io.HttpHeaders;
import org.opentripplanner.test.support.ResourceLoader;

class SiriFmElevatorDataSourceTest {

  private static final ResourceLoader RESOURCES = ResourceLoader.of(
    SiriFmElevatorDataSourceTest.class
  );

  @Test
  void update() {
    var source = new SiriFmElevatorDataSource(
      new SiriFmElevatorUpdaterParameters(
        "siri-fm-elevator",
        RESOURCES.uri("siri-fm-elevators.xml"),
        Duration.ofMinutes(1),
        HttpHeaders.empty(),
        UnknownElevatorStatus.OUT_OF_SERVICE
      )
    );

    assertTrue(source.update());
    assertEquals(
      List.of(
        inService("Lift Maker:1001"),
        outOfService("Lift Maker:1002"),
        outOfService("Lift Maker:1003")
      ),
      source.getUpdates()
    );
  }
}
