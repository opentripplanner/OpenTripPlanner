package org.opentripplanner.netex.loader.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.netex.NetexTestDataSupport.createQuay;
import static org.opentripplanner.netex.NetexTestDataSupport.createStopPlace;

import jakarta.xml.bind.JAXBElement;
import java.util.Collection;
import org.junit.jupiter.api.Test;
import org.opentripplanner.netex.NetexTestDataSupport;
import org.opentripplanner.netex.index.NetexEntityIndex;
import org.rutebanken.netex.model.ObjectFactory;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.SiteFrame;
import org.rutebanken.netex.model.Site_VersionStructure;
import org.rutebanken.netex.model.StopPlace;

class SiteFrameParserTest {

  private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

  @Test
  void testParseQuays() {
    SiteFrameParser siteFrameParser = new SiteFrameParser();
    SiteFrame siteFrame = OBJECT_FACTORY.createSiteFrame();
    NetexEntityIndex netexEntityIndex = new NetexEntityIndex();

    Quay quay = createQuay();
    StopPlace stopPlace = createStopPlace(quay);
    JAXBElement<? extends Site_VersionStructure> jaxbStopPlace = OBJECT_FACTORY.createStopPlace(
      stopPlace
    );

    siteFrame.setStopPlaces(OBJECT_FACTORY.createStopPlacesInFrame_RelStructure());
    siteFrame.getStopPlaces().getStopPlace_().add(jaxbStopPlace);

    siteFrameParser.parse(siteFrame);
    siteFrameParser.setResultOnIndex(netexEntityIndex);
    Collection<Quay> mappedQuays = netexEntityIndex.quayById.lookup(NetexTestDataSupport.QUAY_ID);
    assertEquals(1, mappedQuays.size());
  }
}
