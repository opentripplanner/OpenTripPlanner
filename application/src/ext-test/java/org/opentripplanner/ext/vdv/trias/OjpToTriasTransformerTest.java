package org.opentripplanner.ext.vdv.trias;

import jakarta.xml.bind.JAXBException;
import javax.xml.transform.TransformerException;
import org.junit.jupiter.api.Test;
import org.opentripplanner.test.support.ResourceLoader;
import org.rutebanken.netex.model.StopEventRequestRefStructure;

class OjpToTriasTransformerTest {

  @Test
  void transform() throws JAXBException, TransformerException {
    var xmlString = ResourceLoader.of(this).fileToString("stop-event-request.xml");
    var ojp = OjpToTriasTransformer.readTrias(xmlString);

    var x = ojp.getOJPRequest().getServiceRequest().getAbstractFunctionalServiceRequest().getFirst();

    System.out.print(x.getValue());
  }
}
