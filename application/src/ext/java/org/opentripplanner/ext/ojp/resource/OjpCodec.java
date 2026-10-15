package org.opentripplanner.ext.ojp.resource;

import de.vdv.ojp20.OJP;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import java.io.OutputStreamWriter;
import javax.xml.transform.TransformerException;

class OjpCodec {

  private static final JAXBContext CONTEXT = jaxbContext();

  /**
   * Reads an OJP request received from a client. The input is untrusted, so it is read with
   * {@link SecureXml} rather than relying on the JAXB implementation's default restrictions.
   */
  static OJP deserialize(String trias) throws JAXBException, TransformerException {
    var unmarshaller = CONTEXT.createUnmarshaller();
    return (OJP) unmarshaller.unmarshal(SecureXml.source(trias));
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
