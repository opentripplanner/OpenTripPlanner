package org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitTuningParameters;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.site.StopTransferPriority;
import org.opentripplanner.transit.service.SiteRepositoryMock;

class RaptorTransitDataMapperTest {

  private final TimetableRepositoryForTest testModel = TimetableRepositoryForTest.of();

  private final Station STATION_A = testModel
    .station("A")
    .withPriority(StopTransferPriority.DISCOURAGED)
    .build();
  private final Station STATION_B = testModel
    .station("B")
    .withPriority(StopTransferPriority.ALLOWED)
    .build();
  private final Station STATION_C = testModel
    .station("C")
    .withPriority(StopTransferPriority.RECOMMENDED)
    .build();
  private final Station STATION_D = testModel
    .station("D")
    .withPriority(StopTransferPriority.PREFERRED)
    .build();

  private final RegularStop STOP_0 = testModel.stop("ID-" + 1).build();
  private final RegularStop STOP_1 = testModel.stop("ID-" + 2).withParentStation(STATION_A).build();
  private final RegularStop STOP_2 = testModel.stop("ID-" + 3).withParentStation(STATION_B).build();
  private final RegularStop STOP_3 = testModel.stop("ID-" + 4).withParentStation(STATION_C).build();
  private final RegularStop STOP_4 = testModel.stop("ID-" + 5).withParentStation(STATION_D).build();
  private final List<StopLocation> STOPS = Arrays.asList(STOP_0, STOP_1, STOP_2, STOP_3, STOP_4);

  @Test
  public void createStopBoardAlightTransferCosts() {
    int[] result = RaptorTransitDataMapper.createStopBoardAlightTransferCosts(
      new SiteRepositoryMock(STOPS),
      TransitTuningParameters.FOR_TEST
    );

    assertEquals("[6000, 360000, 6000, 2000, 0]", Arrays.toString(result));
  }
}
