package org.opentripplanner.ext.vdv.ojp;

import de.vdv.ojp20.OJP;
import de.vdv.ojp20.OJPResponseStructure;
import de.vdv.ojp20.siri.ErrorDescriptionStructure;
import de.vdv.ojp20.siri.ServiceDelivery;
import de.vdv.ojp20.siri.ServiceDeliveryStructure;

public class ErrorMapper {

  public static OJP error(String value) {
    return new OJP()
      .withOJPResponse(
        new OJPResponseStructure()
          .withServiceDelivery(
            new ServiceDelivery()
              .withErrorCondition(
                new ServiceDeliveryStructure.ErrorCondition()
                  .withDescription(new ErrorDescriptionStructure().withValue(value))
              )
          )
      );
  }
}
