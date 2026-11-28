package org.opentripplanner.netex.loader.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.xml.bind.JAXBElement;
import java.util.List;
import org.entur.netex2.model.AddressablePlaceRefStructure;
import org.entur.netex2.model.CarPoolingService;
import org.entur.netex2.model.MobilityService_VersionFrameStructure;
import org.entur.netex2.model.MobilityServices_RelStructure;
import org.entur.netex2.model.ObjectFactory;
import org.entur.netex2.model.ParkingRefStructure;
import org.entur.netex2.model.QuayRefStructure;
import org.entur.netex2.model.TaxiService;
import org.entur.netex2.model.VehicleMeetingPointAssignment;
import org.entur.netex2.model.VehicleMeetingPointAssignmentsInFrame_RelStructure;
import org.entur.netex2.model.VehicleMeetingPointRefStructure;
import org.junit.jupiter.api.Test;
import org.opentripplanner.netex.index.NetexEntityIndex;

class MobilityServiceFrameParserTest {

  private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

  private static final String MEETING_POINT_AT_QUAY = "RID:VehicleMeetingPoint:1";
  private static final String MEETING_POINT_AT_PARKING = "RID:VehicleMeetingPoint:2";
  private static final String QUAY_ID = "NSR:Quay:1";

  private final MobilityService_VersionFrameStructure frame =
    new MobilityService_VersionFrameStructure();
  private final NetexEntityIndex index = new NetexEntityIndex();

  @Test
  void onlyCarPoolingServicesAreIndexed() {
    var carPoolingService = new CarPoolingService().withId("RID:CarPoolingService:1");
    var taxiService = new TaxiService().withId("RID:TaxiService:1");
    frame.setMobilityServices(
      new MobilityServices_RelStructure().withMobilityService(
        OBJECT_FACTORY.createCarPoolingService(carPoolingService),
        OBJECT_FACTORY.createTaxiService(taxiService)
      )
    );

    parse();

    assertEquals(
      List.of(carPoolingService),
      List.copyOf(index.carPoolingServiceById.localValues())
    );
  }

  @Test
  void onlyAssignmentsToQuaysAreIndexed() {
    var quayRef = OBJECT_FACTORY.createQuayRef(new QuayRefStructure().withRef(QUAY_ID));
    var parkingRef = OBJECT_FACTORY.createParkingRef(
      new ParkingRefStructure().withRef("NSR:Parking:1")
    );
    frame.setVehicleMeetingPointAssignments(
      new VehicleMeetingPointAssignmentsInFrame_RelStructure().withVehicleMeetingPointAssignment(
        assignment(MEETING_POINT_AT_QUAY, quayRef),
        assignment(MEETING_POINT_AT_PARKING, parkingRef)
      )
    );

    parse();

    assertEquals(QUAY_ID, index.quayIdByVehicleMeetingPointRef.lookup(MEETING_POINT_AT_QUAY));
    assertNull(index.quayIdByVehicleMeetingPointRef.lookup(MEETING_POINT_AT_PARKING));
  }

  private void parse() {
    var parser = new MobilityServiceFrameParser();
    parser.parse(frame);
    parser.setResultOnIndex(index);
  }

  private static VehicleMeetingPointAssignment assignment(
    String meetingPointId,
    JAXBElement<? extends AddressablePlaceRefStructure> placeRef
  ) {
    return new VehicleMeetingPointAssignment()
      .withVehicleMeetingPointRef(new VehicleMeetingPointRefStructure().withRef(meetingPointId))
      .withAddressablePlaceRef(placeRef);
  }
}
