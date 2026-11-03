package org.opentripplanner.netex.loader.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.netex.NetexTestDataSupport.createQuay;
import static org.opentripplanner.netex.NetexTestDataSupport.createStopPlace;

import java.util.Collection;
import java.util.Set;
import org.entur.netex2.model.ObjectFactory;
import org.entur.netex2.model.Parking;
import org.entur.netex2.model.Quay;
import org.entur.netex2.model.RechargingStation;
import org.entur.netex2.model.SiteFrame;
import org.entur.netex2.model.StopPlace;
import org.junit.jupiter.api.Test;
import org.opentripplanner.netex.NetexTestDataSupport;
import org.opentripplanner.netex.index.NetexEntityIndex;

class SiteFrameParserTest {

  private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

  @Test
  void testParseQuays() {
    SiteFrameParser siteFrameParser = new SiteFrameParser(Set.of());
    SiteFrame siteFrame = OBJECT_FACTORY.createSiteFrame();
    NetexEntityIndex netexEntityIndex = new NetexEntityIndex();

    Quay quay = createQuay();
    StopPlace stopPlace = createStopPlace(quay);

    siteFrame.setStopPlaces(OBJECT_FACTORY.createStopPlacesInFrame_RelStructure());
    siteFrame.getStopPlaces().getStopPlace().add(stopPlace);

    siteFrameParser.parse(siteFrame);
    siteFrameParser.setResultOnIndex(netexEntityIndex);
    Collection<Quay> mappedQuays = netexEntityIndex.quayById.lookup(NetexTestDataSupport.QUAY_ID);
    assertEquals(1, mappedQuays.size());
  }

  @Test
  void testParseParkingsSkipsOtherParkingTypes() {
    SiteFrameParser siteFrameParser = new SiteFrameParser(Set.of());
    SiteFrame siteFrame = OBJECT_FACTORY.createSiteFrame();
    NetexEntityIndex netexEntityIndex = new NetexEntityIndex();

    siteFrame.setParkings(OBJECT_FACTORY.createParkingsInFrame_RelStructure());
    siteFrame
      .getParkings()
      .getParking()
      .add(OBJECT_FACTORY.createParking(new Parking().withId("NSR:Parking:1")));
    siteFrame
      .getParkings()
      .getParking()
      .add(
        OBJECT_FACTORY.createRechargingStation(
          new RechargingStation().withId("NSR:RechargingStation:1")
        )
      );

    siteFrameParser.parse(siteFrame);
    siteFrameParser.setResultOnIndex(netexEntityIndex);
    assertEquals(1, netexEntityIndex.parkings.localValues().size());
    assertEquals("NSR:Parking:1", netexEntityIndex.parkings.lookup("NSR:Parking:1").getId());
  }
}
