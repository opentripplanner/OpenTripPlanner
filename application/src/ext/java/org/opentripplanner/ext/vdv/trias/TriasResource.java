package org.opentripplanner.ext.vdv.trias;

import de.vdv.ojp20.CallAtNearStopStructure;
import de.vdv.ojp20.OJP;
import de.vdv.ojp20.OJPResponseStructure;
import de.vdv.ojp20.OJPStopEventDeliveryStructure;
import de.vdv.ojp20.StopEventResultStructure;
import de.vdv.ojp20.StopEventStructure;
import de.vdv.ojp20.siri.ServiceDelivery;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlSchema;
import jakarta.xml.bind.annotation.XmlType;
import java.time.Duration;
import javax.xml.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/trias/v2/")
@Produces("application/xml")
public class TriasResource {

  private static final Logger LOG = LoggerFactory.getLogger(TriasResource.class);

  @GET
  public Response index() {
    try {
      final var ojp = makeOjp();
      return Response.ok(ojp).build();
    } catch (Exception e) {
      LOG.error("Error producing TRIAS response", e);
      return Response.serverError().build();
    }
  }

  static OJP makeOjp() {
    var ojp = new OJP();
    var call = new CallAtNearStopStructure().withWalkDuration(Duration.ofMinutes(10));
    var stopEvent = new StopEventStructure().withThisCall(call);
    var result = new StopEventResultStructure().withStopEvent(stopEvent);
    var sed = new OJPStopEventDeliveryStructure().withStatus(true).withRest(jaxbElement(result));
    var serviceDelivery = new ServiceDelivery()
      .withAbstractFunctionalServiceDelivery(jaxbElement(sed));

    final OJPResponseStructure value = new OJPResponseStructure()
      .withServiceDelivery(serviceDelivery);
    ojp.setOJPResponse(value);
    return ojp;
  }

  public static <T> JAXBElement<T> jaxbElement(T value) {
    var xmlType = value.getClass().getAnnotation(XmlType.class);
    var schema = value.getClass().getPackage().getAnnotation(XmlSchema.class);
    return new JAXBElement<>(new QName(schema.namespace(), xmlType.name()), (Class<T>) value.getClass(), value);
  }
}
