package org.opentripplanner.ext.trias.mapping;

import de.vdv.ojp20.siri.ParticipantRefStructure;
import de.vdv.ojp20.siri.ServiceDelivery;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import org.rutebanken.time.XmlDateTime;

public class ServiceDeliveryMapper {

  static ServiceDelivery serviceDelivery(ZonedDateTime timestamp) {
    return new ServiceDelivery()
      .withResponseTimestamp(new XmlDateTime(timestamp.truncatedTo(ChronoUnit.MILLIS)))
      .withProducerRef(new ParticipantRefStructure().withValue("OpenTripPlanner"));
  }
}
