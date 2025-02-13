package org.opentripplanner.ext.vdv.trias;

import jakarta.xml.bind.JAXBException;
import java.io.StringWriter;
import java.time.ZonedDateTime;
import javax.xml.transform.TransformerException;
import org.apache.commons.io.output.StringBuilderWriter;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.vdv.ojp.ErrorMapper;
import org.opentripplanner.test.support.ResourceLoader;

class OjpToTriasTransformerTest {

  @Test
  void transform() throws JAXBException, TransformerException {
    var xmlString = ResourceLoader.of(this).fileToString("stop-event-request.xml");
    var ojp = OjpToTriasTransformer.triasToOjp(xmlString);

    var x = ojp
      .getOJPRequest()
      .getServiceRequest()
      .getAbstractFunctionalServiceRequest()
      .getFirst();

    System.out.print(x.getValue());
  }

  @Test
  void error() {
    var ojp = ErrorMapper.error("Foo error", ZonedDateTime.now());

    var writer = new StringWriter();
    OjpToTriasTransformer.transform(ojp, writer);

    System.out.print(writer.getBuffer().toString());
  }
}
