package org.opentripplanner.netex.loader.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.xml.bind.JAXBElement;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.netex.index.NetexEntityIndex;
import org.rutebanken.netex.model.ObjectFactory;
import org.rutebanken.netex.model.OperatingPeriod;
import org.rutebanken.netex.model.OperatingPeriod_VersionStructure;
import org.rutebanken.netex.model.ServiceCalendarFrame_VersionFrameStructure;

class ServiceCalendarFrameParserTest {

  private static final LocalDateTime FROM_DATE = LocalDateTime.of(2023, 1, 1, 0, 0);
  private static final LocalDateTime TO_DATE = LocalDateTime.of(2023, 2, 1, 0, 0);
  private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();
  private ServiceCalendarFrameParser serviceCalendarFrameParser;
  private ServiceCalendarFrame_VersionFrameStructure serviceCalendarFrame;
  private NetexEntityIndex netexEntityIndex;

  @BeforeEach
  void setUp() {
    serviceCalendarFrameParser = new ServiceCalendarFrameParser();
    serviceCalendarFrame = OBJECT_FACTORY.createServiceCalendarFrame_VersionFrameStructure();
    netexEntityIndex = new NetexEntityIndex();
  }

  @Test
  void testParseOperatingPeriodInServiceFrame() {
    serviceCalendarFrame.setOperatingPeriods(
      OBJECT_FACTORY.createOperatingPeriodsInFrame_RelStructure()
    );

    OperatingPeriod_VersionStructure operatingPeriod =
      OBJECT_FACTORY.createOperatingPeriod_VersionStructure()
        .withFromDate(FROM_DATE)
        .withToDate(TO_DATE);
    serviceCalendarFrame
      .getOperatingPeriods()
      .getOperatingPeriodOrUicOperatingPeriod()
      .add(operatingPeriod);

    serviceCalendarFrameParser.parse(serviceCalendarFrame);
    serviceCalendarFrameParser.setResultOnIndex(netexEntityIndex);
    assertEquals(1, netexEntityIndex.operatingPeriodById.size());
  }

  @Test
  void testParseOperatingPeriodInServiceCalendar() {
    serviceCalendarFrame.setServiceCalendar(OBJECT_FACTORY.createServiceCalendar());
    serviceCalendarFrame
      .getServiceCalendar()
      .setOperatingPeriods(OBJECT_FACTORY.createOperatingPeriods_RelStructure());

    OperatingPeriod operatingPeriod = OBJECT_FACTORY.createOperatingPeriod()
      .withFromDate(FROM_DATE)
      .withToDate(TO_DATE);
    JAXBElement<?> jaxbOperatingPeriod = OBJECT_FACTORY.createOperatingPeriod(operatingPeriod);
    serviceCalendarFrame
      .getServiceCalendar()
      .getOperatingPeriods()
      .getOperatingPeriodRefOrOperatingPeriodOrUicOperatingPeriod()
      .add(jaxbOperatingPeriod);

    serviceCalendarFrameParser.parse(serviceCalendarFrame);
    serviceCalendarFrameParser.setResultOnIndex(netexEntityIndex);
    assertEquals(1, netexEntityIndex.operatingPeriodById.size());
  }
}
