package org.opentripplanner.ext.ojp.mapping;

import java.time.ZonedDateTime;
import org.opentripplanner.ojp.schema.siri.ParticipantRefStructure;
import org.opentripplanner.ojp.schema.siri.ServiceDelivery;
import org.opentripplanner.ojp.time.XmlDateTime;

public class ServiceDeliveryMapper {

  static ServiceDelivery serviceDelivery(ZonedDateTime timestamp) {
    return new ServiceDelivery()
      .withResponseTimestamp(XmlDateTime.truncatedToMillis(timestamp))
      .withProducerRef(new ParticipantRefStructure().withValue("OpenTripPlanner"));
  }
}
