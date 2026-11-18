package org.opentripplanner.netex.loader.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.xml.bind.JAXBElement;
import org.entur.netex2.model.Authority;
import org.entur.netex2.model.DataManagedObjectStructure;
import org.entur.netex2.model.ObjectFactory;
import org.entur.netex2.model.ResourceFrame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.netex.index.NetexEntityIndex;

class ResourceFrameParserTest {

  private ResourceFrameParser resourceFrameParser;
  private ResourceFrame resourceFrame;
  private ObjectFactory objectFactory;
  private NetexEntityIndex netexEntityIndex;

  @BeforeEach
  void setUp() {
    resourceFrameParser = new ResourceFrameParser();
    resourceFrame = new ResourceFrame();
    objectFactory = new ObjectFactory();
    netexEntityIndex = new NetexEntityIndex();
  }

  @Test
  void testResourceFrameWithOrganization() {
    JAXBElement<? extends DataManagedObjectStructure> organisation = objectFactory.createAuthority(
      new Authority()
    );
    resourceFrame.setOrganisations(objectFactory.createOrganisationsInFrame_RelStructure());
    resourceFrame.getOrganisations().getOrganisation().add(organisation);
    resourceFrameParser.parse(resourceFrame);
    resourceFrameParser.setResultOnIndex(netexEntityIndex);
    assertEquals(1, netexEntityIndex.authoritiesById.size());
  }

  @Test
  void testResourceFrameWithoutOrganization() {
    resourceFrameParser.parse(resourceFrame);
    resourceFrameParser.setResultOnIndex(netexEntityIndex);
    assertEquals(0, netexEntityIndex.authoritiesById.size());
  }
}
