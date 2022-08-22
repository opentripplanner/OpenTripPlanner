package org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitTuningParameters;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.site.StopTransferPriority;
import org.opentripplanner.transit.service.StopModelMock;

class TransitLayerMapperTest {

  private final Station STATION_A = TransitModelForTest
    .station("A")
    .withPriority(StopTransferPriority.DISCOURAGED)
    .build();
  private final Station STATION_B = TransitModelForTest
    .station("B")
    .withPriority(StopTransferPriority.ALLOWED)
    .build();
  private final Station STATION_C = TransitModelForTest
    .station("C")
    .withPriority(StopTransferPriority.RECOMMENDED)
    .build();
  private final Station STATION_D = TransitModelForTest
    .station("D")
    .withPriority(StopTransferPriority.PREFERRED)
    .build();

  private final RegularStop STOP_0 = TransitModelForTest.stop("ID-" + 1).build();
  private final RegularStop STOP_1 = TransitModelForTest
    .stop("ID-" + 2)
    .withParentStation(STATION_A)
    .build();
  private final RegularStop STOP_2 = TransitModelForTest
    .stop("ID-" + 3)
    .withParentStation(STATION_B)
    .build();
  private final RegularStop STOP_3 = TransitModelForTest
    .stop("ID-" + 4)
    .withParentStation(STATION_C)
    .build();
  private final RegularStop STOP_4 = TransitModelForTest
    .stop("ID-" + 5)
    .withParentStation(STATION_D)
    .build();
  private final List<StopLocation> STOPS = Arrays.asList(STOP_0, STOP_1, STOP_2, STOP_3, STOP_4);

  @Test
  public void createStopTransferCosts() {
    int[] result = TransitLayerMapper.createStopTransferCosts(
      new StopModelMock(STOPS),
      TransitTuningParameters.FOR_TEST
    );

    assertEquals("[6000, 360000, 6000, 2000, 0]", Arrays.toString(result));
  }
}
