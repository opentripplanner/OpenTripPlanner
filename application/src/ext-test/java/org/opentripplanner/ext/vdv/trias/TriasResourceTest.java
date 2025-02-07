package org.opentripplanner.ext.vdv.trias;

import static jakarta.xml.bind.Marshaller.*;

import de.vdv.ojp20.OJP;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;

class TriasResourceTest {

  @Test
  void test() throws JAXBException {
    var ojp = TriasResource.makeOjp();

    var context = JAXBContext.newInstance(OJP.class);
    var marshaller = context.createMarshaller();

    // Format the XML output
    marshaller.setProperty(JAXB_FORMATTED_OUTPUT, true);

    // Convert Java object to XML string
    StringWriter xmlWriter = new StringWriter();
    marshaller.marshal(ojp, xmlWriter);

    // Print the XML output
    System.out.println(xmlWriter);
  }

  @Test
  void ojpToTrias() {
    var ojp = TriasResource.makeOjp();
    OjpToTriasTransformer.transform(ojp, new PrintWriter(System.out));
  }
}
