package org.opentripplanner.ext.vdv.trias;

import java.io.IOException;
import java.io.Writer;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

public class OjpToTriasTransformer {

  private static final Templates TEMPLATES;

  static {
    try {
      var xslt =
        OjpToTriasTransformer.class.getResource("trias_to_ojp2.0_response.xslt").openStream();
      var xsltSource = new StreamSource(xslt);
      TransformerFactory factory = TransformerFactory.newInstance();
      TEMPLATES = factory.newTemplates(xsltSource);
    } catch (TransformerConfigurationException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  static void transform(Writer writer, StreamSource xmlSource)
    throws IOException, TransformerException {
    var transformer = TEMPLATES.newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.transform(xmlSource, new StreamResult(writer));
  }
}
