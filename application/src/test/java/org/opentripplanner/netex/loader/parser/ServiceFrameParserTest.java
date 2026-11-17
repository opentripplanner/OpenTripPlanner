package org.opentripplanner.netex.loader.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.xml.bind.JAXBElement;
import org.entur.netex2.model.ObjectFactory;
import org.entur.netex2.model.PassengerStopAssignment;
import org.entur.netex2.model.QuayRefStructure;
import org.entur.netex2.model.ScheduledStopPointRefStructure;
import org.entur.netex2.model.Service_VersionFrameStructure;
import org.entur.netex2.model.StopPlaceRefStructure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.netex.index.NetexEntityIndex;
import org.opentripplanner.netex.index.hierarchy.HierarchicalMapById;
import org.opentripplanner.netex.mapping.MappingSupport;

class ServiceFrameParserTest {

  private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

  private static final String STOP_POINT_REF_1 = "RUT:ScheduledStopPoint:1";
  private static final String STOP_POINT_REF_2 = "RUT:ScheduledStopPoint:2";
  private static final String QUAY_REF = "NSR:Quay:1";
  private static final String STOP_PLACE_REF = "NSR:StopPlace:1";

  private ServiceFrameParser serviceFrameParser;
  private Service_VersionFrameStructure serviceFrame;
  private NetexEntityIndex netexEntityIndex;

  @BeforeEach
  void setUp() {
    serviceFrameParser = new ServiceFrameParser(new HierarchicalMapById<>(), true);
    serviceFrame = OBJECT_FACTORY.createService_VersionFrameStructure();
    netexEntityIndex = new NetexEntityIndex();
  }

  @Test
  void testPassengerStopAssignmentWithQuayRef() {
    addPassengerStopAssignment(
      new PassengerStopAssignment()
        .withScheduledStopPointRef(createScheduledStopPointRef(STOP_POINT_REF_1))
        .withQuayRef(createQuayRef(QUAY_REF))
    );

    serviceFrameParser.parse(serviceFrame);
    serviceFrameParser.setResultOnIndex(netexEntityIndex);

    assertEquals(QUAY_REF, netexEntityIndex.quayIdByStopPointRef.lookup(STOP_POINT_REF_1));
    assertNull(netexEntityIndex.stopPlaceIdByStopPointRef.lookup(STOP_POINT_REF_1));
  }

  @Test
  void testPassengerStopAssignmentWithStopPlaceRef() {
    addPassengerStopAssignment(
      new PassengerStopAssignment()
        .withScheduledStopPointRef(createScheduledStopPointRef(STOP_POINT_REF_2))
        .withStopPlaceRef(createStopPlaceRef(STOP_PLACE_REF))
    );

    serviceFrameParser.parse(serviceFrame);
    serviceFrameParser.setResultOnIndex(netexEntityIndex);

    assertEquals(
      STOP_PLACE_REF,
      netexEntityIndex.stopPlaceIdByStopPointRef.lookup(STOP_POINT_REF_2)
    );
    assertNull(netexEntityIndex.quayIdByStopPointRef.lookup(STOP_POINT_REF_2));
  }

  @Test
  void testPassengerStopAssignmentWithStopPlaceRefIsIgnoredWhenFeatureDisabled() {
    serviceFrameParser = new ServiceFrameParser(new HierarchicalMapById<>(), false);
    addPassengerStopAssignment(
      new PassengerStopAssignment()
        .withScheduledStopPointRef(createScheduledStopPointRef(STOP_POINT_REF_2))
        .withStopPlaceRef(createStopPlaceRef(STOP_PLACE_REF))
    );

    serviceFrameParser.parse(serviceFrame);
    serviceFrameParser.setResultOnIndex(netexEntityIndex);

    assertNull(netexEntityIndex.stopPlaceIdByStopPointRef.lookup(STOP_POINT_REF_2));
    assertNull(netexEntityIndex.quayIdByStopPointRef.lookup(STOP_POINT_REF_2));
  }

  @Test
  void testPassengerStopAssignmentWithoutQuayOrStopPlaceRefIsIgnored() {
    addPassengerStopAssignment(
      new PassengerStopAssignment().withScheduledStopPointRef(
        createScheduledStopPointRef(STOP_POINT_REF_1)
      )
    );

    serviceFrameParser.parse(serviceFrame);
    serviceFrameParser.setResultOnIndex(netexEntityIndex);

    assertNull(netexEntityIndex.quayIdByStopPointRef.lookup(STOP_POINT_REF_1));
    assertNull(netexEntityIndex.stopPlaceIdByStopPointRef.lookup(STOP_POINT_REF_1));
  }

  @Test
  void testPassengerStopAssignmentWithoutScheduledStopPointRefIsIgnored() {
    addPassengerStopAssignment(new PassengerStopAssignment().withQuayRef(createQuayRef(QUAY_REF)));

    serviceFrameParser.parse(serviceFrame);
    serviceFrameParser.setResultOnIndex(netexEntityIndex);

    assertTrue(netexEntityIndex.quayIdByStopPointRef.localKeys().isEmpty());
    assertTrue(netexEntityIndex.stopPlaceIdByStopPointRef.localKeys().isEmpty());
  }

  @Test
  void testPassengerStopAssignmentWithoutScheduledStopPointRefButWithStopPlaceRefIsIgnored() {
    addPassengerStopAssignment(
      new PassengerStopAssignment().withStopPlaceRef(createStopPlaceRef(STOP_PLACE_REF))
    );

    serviceFrameParser.parse(serviceFrame);
    serviceFrameParser.setResultOnIndex(netexEntityIndex);

    assertTrue(netexEntityIndex.quayIdByStopPointRef.localKeys().isEmpty());
    assertTrue(netexEntityIndex.stopPlaceIdByStopPointRef.localKeys().isEmpty());
  }

  private void addPassengerStopAssignment(PassengerStopAssignment assignment) {
    serviceFrame.setStopAssignments(OBJECT_FACTORY.createStopAssignmentsInFrame_RelStructure());
    serviceFrame
      .getStopAssignments()
      .getStopAssignment()
      .add(OBJECT_FACTORY.createPassengerStopAssignment(assignment));
  }

  private static JAXBElement<ScheduledStopPointRefStructure> createScheduledStopPointRef(
    String id
  ) {
    return MappingSupport.createWrappedRef(id, ScheduledStopPointRefStructure.class);
  }

  private static JAXBElement<QuayRefStructure> createQuayRef(String id) {
    return MappingSupport.createWrappedRef(id, QuayRefStructure.class);
  }

  private static JAXBElement<StopPlaceRefStructure> createStopPlaceRef(String id) {
    return MappingSupport.createWrappedRef(id, StopPlaceRefStructure.class);
  }
}
