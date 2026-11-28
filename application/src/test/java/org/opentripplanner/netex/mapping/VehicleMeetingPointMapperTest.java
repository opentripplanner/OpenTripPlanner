package org.opentripplanner.netex.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.opentripplanner.netex.NetexTestDataSupport.createVehicleMeetingPoint;
import static org.opentripplanner.netex.mapping.MappingSupport.ID_FACTORY;

import org.entur.netex2.model.VehicleMeetingPoint;
import org.junit.jupiter.api.Test;
import org.opentripplanner.netex.index.NetexEntityIndex;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.SiteRepositoryBuilder;

class VehicleMeetingPointMapperTest {

  private static final String MEETING_POINT_ID = "RID:VehicleMeetingPoint:1";
  private static final String QUAY_ID = "NSR:Quay:1";

  private final NetexEntityIndex index = new NetexEntityIndex();
  private final SiteRepositoryBuilder siteRepositoryBuilder = SiteRepository.of();
  private final VehicleMeetingPointMapper mapper = new VehicleMeetingPointMapper(
    ID_FACTORY,
    siteRepositoryBuilder,
    index.vehicleMeetingPointById,
    index.quayIdByVehicleMeetingPointRef
  );

  @Test
  void meetingPointBecomesStopAtItsLocation() {
    index.vehicleMeetingPointById.add(
      createVehicleMeetingPoint(MEETING_POINT_ID, "Hill road 12", 59.9, 10.7)
    );

    RegularStop stop = mapper.mapToStop(MEETING_POINT_ID);

    assertEquals(ID_FACTORY.createId(MEETING_POINT_ID), stop.getId());
    assertEquals("Hill road 12", stop.getName().toString());
    assertEquals(new WgsCoordinate(59.9, 10.7), stop.getCoordinate());
    assertSame(stop, siteRepositoryBuilder.regularStopsById().get(stop.getId()));
    assertSame(stop, mapper.mapToStop(MEETING_POINT_ID));
  }

  @Test
  void meetingPointWithoutNameGetsPlaceholderName() {
    var meetingPoint = createVehicleMeetingPoint(MEETING_POINT_ID, "Hill road 12", 59.9, 10.7);
    meetingPoint.setName(null);
    index.vehicleMeetingPointById.add(meetingPoint);

    assertEquals("N/A", mapper.mapToStop(MEETING_POINT_ID).getName().toString());
  }

  @Test
  void meetingPointAssignedToQuayUsesStopOfQuay() {
    RegularStop quay = siteRepositoryBuilder
      .regularStop(ID_FACTORY.createId(QUAY_ID))
      .withCoordinate(60.0, 10.0)
      .build();
    siteRepositoryBuilder.withRegularStop(quay);
    index.vehicleMeetingPointById.add(new VehicleMeetingPoint().withId(MEETING_POINT_ID));
    index.quayIdByVehicleMeetingPointRef.add(MEETING_POINT_ID, QUAY_ID);

    assertSame(quay, mapper.mapToStop(MEETING_POINT_ID));
  }

  @Test
  void noStopForMeetingPointAssignedToUnknownQuay() {
    index.vehicleMeetingPointById.add(
      createVehicleMeetingPoint(MEETING_POINT_ID, "Hill road 12", 59.9, 10.7)
    );
    index.quayIdByVehicleMeetingPointRef.add(MEETING_POINT_ID, QUAY_ID);

    assertNull(mapper.mapToStop(MEETING_POINT_ID));
  }

  @Test
  void noStopForMeetingPointWithoutLocation() {
    index.vehicleMeetingPointById.add(new VehicleMeetingPoint().withId(MEETING_POINT_ID));

    assertNull(mapper.mapToStop(MEETING_POINT_ID));
  }

  @Test
  void noStopForMeetingPointWithInvalidLocation() {
    index.vehicleMeetingPointById.add(
      createVehicleMeetingPoint(MEETING_POINT_ID, "Hill road 12", 599.0, 10.7)
    );

    assertNull(mapper.mapToStop(MEETING_POINT_ID));
  }

  @Test
  void noStopForUnknownMeetingPoint() {
    assertNull(mapper.mapToStop(MEETING_POINT_ID));
  }
}
