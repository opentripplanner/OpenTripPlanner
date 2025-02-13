package org.opentripplanner.ext.vdv.trias;

import de.vdv.ojp20.OJP;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

public class OjpToTriasTransformer {

  private static final Templates OJP_TO_TRIAS_TEMPLATE = loadTemplate(
    "trias_to_ojp2.0_response.xslt"
  );
  private static final Templates TRIAS_TO_OJP_TEMPLATE = loadTemplate(
    "trias_to_ojp2.0_request.xslt"
  );

  public static void transform(OJP ojp, Writer writer) {
    try {
      var context = JAXBContext.newInstance(OJP.class);
      var marshaller = context.createMarshaller();

      // Convert Java object to XML string
      var outputStream = new ByteArrayOutputStream();
      marshaller.marshal(ojp, outputStream);

      var xmlSource = new StreamSource(new ByteArrayInputStream(outputStream.toByteArray()));

      transform(writer, xmlSource);
    } catch (IOException | JAXBException | TransformerException e) {
      throw new RuntimeException(e);
    }
  }

  public static OJP triasToOjp(String trias) throws JAXBException, TransformerException {
    var context = JAXBContext.newInstance(OJP.class);

    var xmlSource = new StreamSource(
      new ByteArrayInputStream(trias.getBytes(StandardCharsets.UTF_8))
    );

    var transformer = TRIAS_TO_OJP_TEMPLATE.newTransformer();
    var writer = new ByteArrayOutputStream();
    transformer.transform(xmlSource, new StreamResult(writer));
    var transformedXml = writer.toString(StandardCharsets.UTF_8);

    var unmarshaller = context.createUnmarshaller();
    return (OJP) unmarshaller.unmarshal(
      new ByteArrayInputStream(transformedXml.getBytes(StandardCharsets.UTF_8))
    );
  }

  private static void transform(Writer writer, StreamSource xmlSource)
    throws IOException, TransformerException {
    var transformer = OJP_TO_TRIAS_TEMPLATE.newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.transform(xmlSource, new StreamResult(writer));
  }

  private static Templates loadTemplate(String name) {
    try {
      var xslt = OjpToTriasTransformer.class.getResource(name).openStream();
      var xsltSource = new StreamSource(xslt);
      TransformerFactory factory = TransformerFactory.newInstance();
      return factory.newTemplates(xsltSource);
    } catch (TransformerConfigurationException | IOException e) {
      throw new RuntimeException(e);
    }
  }
}
