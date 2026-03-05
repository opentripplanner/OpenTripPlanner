package org.opentripplanner.ext.ojp.resource;

import de.vdv.ojp20.OJP;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

class OjpCodec {

  private static final JAXBContext CONTEXT = jaxbContext();

  static OJP deserialize(String trias) throws JAXBException, TransformerException {
    var source = new StreamSource(new ByteArrayInputStream(trias.getBytes(StandardCharsets.UTF_8)));
    var unmarshaller = CONTEXT.createUnmarshaller();
    return (OJP) unmarshaller.unmarshal(source);
  }

  static StreamingOutput serialize(OJP ojpOutput) {
    return os -> {
      try {
        var marshaller = CONTEXT.createMarshaller();
        marshaller.marshal(ojpOutput, os);
        var writer = new OutputStreamWriter(os);
        writer.flush();
      } catch (JAXBException e) {
        throw new RuntimeException(e);
      }
    };
  }

  private static JAXBContext jaxbContext() {
    try {
      return JAXBContext.newInstance(OJP.class);
    } catch (JAXBException e) {
      throw new RuntimeException(e);
    }
  }
}
