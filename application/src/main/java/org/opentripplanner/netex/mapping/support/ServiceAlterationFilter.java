package org.opentripplanner.netex.mapping.support;

import org.entur.netex2.model.ServiceAlterationEnumeration;

public class ServiceAlterationFilter {

  public static boolean isRunning(ServiceAlterationEnumeration serviceAlteration) {
    return (
      serviceAlteration == null ||
      (!serviceAlteration.equals(ServiceAlterationEnumeration.CANCELLATION) &&
        !serviceAlteration.equals(ServiceAlterationEnumeration.REPLACED))
    );
  }
}
