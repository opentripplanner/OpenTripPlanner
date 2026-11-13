package org.opentripplanner.apis.gtfs.datafetchers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import graphql.schema.DataFetchingEnvironment;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLPickupDropoffType;
import org.opentripplanner.apis.gtfs.model.CallSchedule;
import org.opentripplanner.apis.gtfs.model.CallScheduledTime.TimeWindow;
import org.opentripplanner.apis.support.graphql.DataFetchingSupport;
import org.opentripplanner.model.PickDrop;

class CallScheduleImplTest {

  private static final OffsetDateTime NOON = OffsetDateTime.parse("2026-11-09T12:00+02:00");
  private static final TimeWindow TIME = new TimeWindow(NOON, NOON.plusMinutes(30));

  private final CallScheduleImpl subject = new CallScheduleImpl();

  @Test
  void time() throws Exception {
    assertEquals(TIME, subject.time().get(env(PickDrop.SCHEDULED, PickDrop.SCHEDULED)));
  }

  @Test
  void pickupAndDropOffAreMappedIndependently() throws Exception {
    var env = env(PickDrop.CALL_AGENCY, PickDrop.COORDINATE_WITH_DRIVER);

    assertEquals(GraphQLPickupDropoffType.CALL_AGENCY, subject.pickupType().get(env));
    assertEquals(GraphQLPickupDropoffType.COORDINATE_WITH_DRIVER, subject.dropOffType().get(env));
  }

  @Test
  void cancelledHasNoPickupDropoffType() throws Exception {
    var env = env(PickDrop.CANCELLED, PickDrop.CANCELLED);

    assertNull(subject.pickupType().get(env));
    assertNull(subject.dropOffType().get(env));
  }

  @Test
  void missingPickupDropoffType() throws Exception {
    var env = env(null, null);

    assertNull(subject.pickupType().get(env));
    assertNull(subject.dropOffType().get(env));
  }

  private static DataFetchingEnvironment env(
    @Nullable PickDrop pickup,
    @Nullable PickDrop dropOff
  ) {
    return DataFetchingSupport.dataFetchingEnvironment(new CallSchedule(TIME, pickup, dropOff));
  }
}
