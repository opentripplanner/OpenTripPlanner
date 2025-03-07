package org.opentripplanner.netex.mapping.support;

import org.rutebanken.netex.model.ServiceAlterationEnumeration;

public class ServiceAlterationFilter {

  public static boolean isRunning(ServiceAlterationEnumeration serviceAlteration) {
    return (
      serviceAlteration == null ||
      (!serviceAlteration.equals(ServiceAlterationEnumeration.CANCELLATION) &&
        !serviceAlteration.equals(ServiceAlterationEnumeration.REPLACED))
    );
  }
}
