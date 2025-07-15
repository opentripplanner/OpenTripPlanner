package org.opentripplanner.ext.trias.mapping;

import de.vdv.ojp20.OJP;
import de.vdv.ojp20.OJPResponseStructure;
import de.vdv.ojp20.siri.ErrorDescriptionStructure;
import de.vdv.ojp20.siri.ServiceDeliveryStructure;
import java.time.ZonedDateTime;

public class ErrorMapper {

  public static OJP error(String value, ZonedDateTime timestamp) {
    return new OJP()
      .withOJPResponse(
        new OJPResponseStructure()
          .withServiceDelivery(
            ServiceDeliveryMapper.serviceDelivery(timestamp).withErrorCondition(
              new ServiceDeliveryStructure.ErrorCondition()
                .withDescription(new ErrorDescriptionStructure().withValue(value))
            )
          )
      );
  }
}
