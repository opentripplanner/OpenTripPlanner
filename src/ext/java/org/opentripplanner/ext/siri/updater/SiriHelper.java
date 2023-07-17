package org.opentripplanner.ext.siri.updater;

import jakarta.xml.bind.JAXBException;
import java.io.InputStream;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.UUID;
import javax.xml.stream.XMLStreamException;
import org.rutebanken.siri20.util.SiriXml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.EstimatedTimetableRequestStructure;
import uk.org.siri.siri20.MessageQualifierStructure;
import uk.org.siri.siri20.RequestorRef;
import uk.org.siri.siri20.ServiceRequest;
import uk.org.siri.siri20.Siri;
import uk.org.siri.siri20.SituationExchangeRequestStructure;
import uk.org.siri.siri20.VehicleMonitoringRequestStructure;

public class SiriHelper {

  private static final Logger LOG = LoggerFactory.getLogger(SiriHelper.class);

  public static Siri unmarshal(InputStream is) throws JAXBException, XMLStreamException {
    return SiriXml.parseXml(is);
  }

  public static String createSXServiceRequestAsXml(String requestorRef) throws JAXBException {
    Siri request = createSXServiceRequest(requestorRef);
    return SiriXml.toXml(request);
  }

  public static String createVMServiceRequestAsXml(String requestorRef) throws JAXBException {
    Siri request = createVMServiceRequest(requestorRef);
    return SiriXml.toXml(request);
  }

  public static String createETServiceRequestAsXml(String requestorRef) throws JAXBException {
    Siri request = createETServiceRequest(requestorRef, null);
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

  private static Siri createVMServiceRequest(String requestorRefValue) {
    Siri request = createSiriObject();

    ServiceRequest serviceRequest = new ServiceRequest();
    serviceRequest.setRequestTimestamp(ZonedDateTime.now());

    RequestorRef requestorRef = new RequestorRef();
    requestorRef.setValue(requestorRefValue);
    serviceRequest.setRequestorRef(requestorRef);

    VehicleMonitoringRequestStructure vmRequest = new VehicleMonitoringRequestStructure();
    vmRequest.setRequestTimestamp(ZonedDateTime.now());
    vmRequest.setVersion("2.0");

    MessageQualifierStructure messageIdentifier = new MessageQualifierStructure();
    messageIdentifier.setValue(UUID.randomUUID().toString());

    vmRequest.setMessageIdentifier(messageIdentifier);
    serviceRequest.getVehicleMonitoringRequests().add(vmRequest);

    request.setServiceRequest(serviceRequest);

    return request;
  }
}
