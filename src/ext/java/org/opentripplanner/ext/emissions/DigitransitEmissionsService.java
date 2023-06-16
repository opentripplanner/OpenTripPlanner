package org.opentripplanner.ext.emissions;

import java.util.HashMap;

//interface emissionService
// getEmissionForRoute()
//Sandbox digitransitEmissionService
public class DigitransitEmissionsService implements EmissionsService {

  private String url;
  private HashMap<String, DigitransitEmissions> emissionByAgency;

  public DigitransitEmissionsService(DigitransitEmissions[] emissions) {
    this.url = url;
    this.emissionByAgency = new HashMap<>();
    for (DigitransitEmissions e : emissions) {
      this.emissionByAgency.put(e.getAgency_id(), e);
    }
  }

  public HashMap<String, DigitransitEmissions> getEmissionByAgency() {
    return emissionByAgency;
  }

  @Override
  public void getEmissionForRoute() {}
}
