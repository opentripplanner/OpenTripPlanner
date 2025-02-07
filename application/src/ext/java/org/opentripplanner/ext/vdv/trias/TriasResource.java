package org.opentripplanner.ext.vdv.trias;

import de.vdv.ojp20.CallAtNearStopStructure;
import de.vdv.ojp20.CallAtStopStructure;
import de.vdv.ojp20.InternationalTextStructure;
import de.vdv.ojp20.OJP;
import de.vdv.ojp20.OJPResponseStructure;
import de.vdv.ojp20.OJPStopEventDeliveryStructure;
import de.vdv.ojp20.StopEventResultStructure;
import de.vdv.ojp20.StopEventStructure;
import de.vdv.ojp20.siri.DefaultedTextStructure;
import de.vdv.ojp20.siri.ServiceDelivery;
import de.vdv.ojp20.siri.StopPointRefStructure;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.annotation.XmlSchema;
import jakarta.xml.bind.annotation.XmlType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import javax.xml.namespace.QName;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/trias/v1/")
@Produces("application/xml")
public class TriasResource {

  private static final Logger LOG = LoggerFactory.getLogger(TriasResource.class);

  @GET
  public Response index() {
    try {
      var ojp = makeOjp();
      return Response.ok(ojp).build();
    } catch (Exception e) {
      LOG.error("Error producing TRIAS response", e);
      return Response.serverError().build();
    }
  }

  public static void transform(OJP ojp) {
    try {
      var context = JAXBContext.newInstance(OJP.class);
      var marshaller = context.createMarshaller();

      // Convert Java object to XML string
      var outputStream = new ByteArrayOutputStream();
      var xmlWriter = new OutputStreamWriter(outputStream);
      marshaller.marshal(ojp, xmlWriter);

      var xslt = TriasResource.class.getResource("trias_to_ojp2.0_response.xslt").openStream();

      // Create a Source for the XML and XSLT files
      Source xmlSource = new StreamSource(new ByteArrayInputStream(outputStream.toByteArray()));
      Source xsltSource = new StreamSource(xslt);

      // Create the Transformer using the XSLT
      TransformerFactory factory = TransformerFactory.newInstance();
      Transformer transformer = factory.newTransformer(xsltSource);

      // Set optional properties for the transformer
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");

      // Output the transformation result to the console or a file
      Result result = new StreamResult(System.out); // For console output
      transformer.transform(xmlSource, result);
    } catch (IOException | JAXBException | TransformerException e) {
      throw new RuntimeException(e);
    }
  }

  static OJP makeOjp() {
    var ojp = new OJP();
    var stopPointRef = new StopPointRefStructure().withValue("de:08128:13410:0:RiN");
    var call = new CallAtNearStopStructure()
      .withCallAtStop(
        new CallAtStopStructure()
          .withStopPointRef(stopPointRef)
          .withStopPointName(
            new InternationalTextStructure()
              .withText(new DefaultedTextStructure().withValue("Wertheim, Waldhaus").withLang("de"))
          )
      );
    var stopEvent = new StopEventStructure().withThisCall(call);
    var result = new StopEventResultStructure().withStopEvent(stopEvent);
    var sed = new OJPStopEventDeliveryStructure().withStatus(true).withRest(jaxbElement(result));
    var serviceDelivery = new ServiceDelivery()
      .withAbstractFunctionalServiceDelivery(jaxbElement(sed));

    var response = new OJPResponseStructure().withServiceDelivery(serviceDelivery);
    ojp.setOJPResponse(response);
    return ojp;
  }

  public static <T> JAXBElement<T> jaxbElement(T value) {
    var xmlType = value.getClass().getAnnotation(XmlType.class);
    var schema = value.getClass().getPackage().getAnnotation(XmlSchema.class);
    return new JAXBElement<>(
      new QName(schema.namespace(), getName(xmlType)),
      (Class<T>) value.getClass(),
      value
    );
  }

  private static String getName(XmlType xmlType) {
    return xmlType.name().replaceAll("Structure", "");
  }
}
