package org.opentripplanner.updater.support.siri;

import jakarta.xml.bind.JAXBException;
import java.io.InputStream;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.UUID;
import javax.xml.stream.XMLStreamException;
import org.rutebanken.siri20.util.SiriXml;
import uk.org.siri.siri20.EstimatedTimetableRequestStructure;
import uk.org.siri.siri20.MessageQualifierStructure;
import uk.org.siri.siri20.RequestorRef;
import uk.org.siri.siri20.ServiceRequest;
import uk.org.siri.siri20.Siri;
import uk.org.siri.siri20.SituationExchangeRequestStructure;

public class SiriHelper {

  public static Siri unmarshal(InputStream is) throws JAXBException, XMLStreamException {
    return SiriXml.parseXml(is);
  }

  public static String createSXServiceRequestAsXml(String requestorRef) throws JAXBException {
    Siri request = createSXServiceRequest(requestorRef);
    return SiriXml.toXml(request);
  }

  public static String createETServiceRequestAsXml(String requestorRef, Duration previewInterval)
    throws JAXBException {
    Siri request = createETServiceRequest(requestorRef, previewInterval);
    return SiriXml.toXml(request);
  }

  private static Siri createSiriObject() {
    Siri request = new Siri();
    request.setVersion("2.0");

    return request;
  }

  private static Siri createSXServiceRequest(String requestorRefValue) {
    Siri request = createSiriObject();

    ServiceRequest serviceRequest = new ServiceRequest();
    serviceRequest.setRequestTimestamp(ZonedDateTime.now());

    RequestorRef requestorRef = new RequestorRef();
    requestorRef.setValue(requestorRefValue);
    serviceRequest.setRequestorRef(requestorRef);

    SituationExchangeRequestStructure sxRequest = new SituationExchangeRequestStructure();
    sxRequest.setRequestTimestamp(ZonedDateTime.now());
    sxRequest.setVersion("2.0");

    MessageQualifierStructure messageIdentifier = new MessageQualifierStructure();
    messageIdentifier.setValue(UUID.randomUUID().toString());

    sxRequest.setMessageIdentifier(messageIdentifier);
    serviceRequest.getSituationExchangeRequests().add(sxRequest);

    request.setServiceRequest(serviceRequest);

    return request;
  }

  private static Siri createETServiceRequest(String requestorRefValue, Duration previewInterval) {
    Siri request = createSiriObject();

    ServiceRequest serviceRequest = new ServiceRequest();
    serviceRequest.setRequestTimestamp(ZonedDateTime.now());

    RequestorRef requestorRef = new RequestorRef();
    requestorRef.setValue(requestorRefValue);
    serviceRequest.setRequestorRef(requestorRef);

    EstimatedTimetableRequestStructure etRequest = new EstimatedTimetableRequestStructure();
    etRequest.setRequestTimestamp(ZonedDateTime.now());
    etRequest.setVersion("2.0");

    if (previewInterval != null) {
      etRequest.setPreviewInterval(previewInterval);
    }

    MessageQualifierStructure messageIdentifier = new MessageQualifierStructure();
    messageIdentifier.setValue(UUID.randomUUID().toString());

    etRequest.setMessageIdentifier(messageIdentifier);
    serviceRequest.getEstimatedTimetableRequests().add(etRequest);

    request.setServiceRequest(serviceRequest);

    return request;
  }
}
