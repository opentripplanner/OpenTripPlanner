package org.opentripplanner.ext.vdv.trias;

import de.vdv.ojp20.CallAtNearStopStructure;
import de.vdv.ojp20.CallAtStopStructure;
import de.vdv.ojp20.DatedJourneyStructure;
import de.vdv.ojp20.InternationalTextStructure;
import de.vdv.ojp20.JourneyRefStructure;
import de.vdv.ojp20.OJP;
import de.vdv.ojp20.OJPResponseStructure;
import de.vdv.ojp20.OJPStopEventDeliveryStructure;
import de.vdv.ojp20.ServiceDepartureStructure;
import de.vdv.ojp20.StopEventResultStructure;
import de.vdv.ojp20.StopEventStructure;
import de.vdv.ojp20.siri.DefaultedTextStructure;
import de.vdv.ojp20.siri.ServiceDelivery;
import de.vdv.ojp20.siri.StopPointRefStructure;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlSchema;
import jakarta.xml.bind.annotation.XmlType;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.time.ZonedDateTime;
import javax.xml.namespace.QName;
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

      StreamingOutput stream = os -> {
        Writer writer = new BufferedWriter(new OutputStreamWriter(os));
        OjpToTriasTransformer.transform(ojp, writer);
        writer.flush();
      };
      return Response.ok(stream).build();
    } catch (Exception e) {
      LOG.error("Error producing TRIAS response", e);
      return Response.serverError().build();
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
          .withServiceDeparture(
            new ServiceDepartureStructure().withTimetabledTime(ZonedDateTime.now())
          )
      );
    var stopEvent = new StopEventStructure()
      .withThisCall(call)
      .withService(
        new DatedJourneyStructure()
          .withJourneyRef(new JourneyRefStructure().withValue("vrn:49976::H:s24:13"))
      );
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
