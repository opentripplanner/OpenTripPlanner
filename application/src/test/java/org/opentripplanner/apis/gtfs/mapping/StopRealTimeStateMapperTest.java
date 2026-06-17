package org.opentripplanner.apis.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLStopRealTimeState.CANCELLED;
import static org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLStopRealTimeState.DEFAULT;
import static org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLStopRealTimeState.INACCURATE_PREDICTIONS;
import static org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLStopRealTimeState.NO_DATA;
import static org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLStopRealTimeState.RECORDED;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.timetable.StopRealTimeState;

class StopRealTimeStateMapperTest {

  @Test
  void defaultMapsToDefault() {
    assertEquals(DEFAULT, StopRealTimeStateMapper.map(StopRealTimeState.DEFAULT));
  }

  @Test
  void noDataMapsToNoData() {
    assertEquals(NO_DATA, StopRealTimeStateMapper.map(StopRealTimeState.NO_DATA));
  }

  @Test
  void cancelledMapsToCANCELLED() {
    assertEquals(CANCELLED, StopRealTimeStateMapper.map(StopRealTimeState.CANCELLED));
  }

  @Test
  void inaccuratePredictionsMapsToInaccuratePredictions() {
    assertEquals(
      INACCURATE_PREDICTIONS,
      StopRealTimeStateMapper.map(StopRealTimeState.INACCURATE_PREDICTIONS)
    );
  }

  @Test
  void recordedMapsToRecorded() {
    assertEquals(RECORDED, StopRealTimeStateMapper.map(StopRealTimeState.RECORDED));
  }
}
