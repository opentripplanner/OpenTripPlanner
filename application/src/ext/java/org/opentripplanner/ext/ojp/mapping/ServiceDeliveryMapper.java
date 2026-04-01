package org.opentripplanner.ext.ojp.mapping;

import de.vdv.ojp20.siri.ParticipantRefStructure;
import de.vdv.ojp20.siri.ServiceDelivery;
import java.time.ZonedDateTime;
import org.opentripplanner.ojp.time.XmlDateTime;

public class ServiceDeliveryMapper {

  static ServiceDelivery serviceDelivery(ZonedDateTime timestamp) {
    return new ServiceDelivery()
      .withResponseTimestamp(XmlDateTime.truncatedToMillis(timestamp))
      .withProducerRef(new ParticipantRefStructure().withValue("OpenTripPlanner"));
  }
}
