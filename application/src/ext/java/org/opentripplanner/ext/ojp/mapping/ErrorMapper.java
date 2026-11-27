package org.opentripplanner.ext.ojp.mapping;

import java.time.ZonedDateTime;
import org.opentripplanner.ojp.schema.ojp.OJP;
import org.opentripplanner.ojp.schema.ojp.OJPResponseStructure;
import org.opentripplanner.ojp.schema.siri.ErrorDescriptionStructure;
import org.opentripplanner.ojp.schema.siri.ServiceDeliveryStructure;

public class ErrorMapper {

  public static OJP error(String value, ZonedDateTime timestamp) {
    return new OJP().withOJPResponse(
      new OJPResponseStructure().withServiceDelivery(
        ServiceDeliveryMapper.serviceDelivery(timestamp).withErrorCondition(
          new ServiceDeliveryStructure.ErrorCondition().withDescription(
            new ErrorDescriptionStructure().withValue(value)
          )
        )
      )
    );
  }
}
