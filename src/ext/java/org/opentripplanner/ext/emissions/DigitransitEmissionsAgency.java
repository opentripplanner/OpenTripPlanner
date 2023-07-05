package org.opentripplanner.ext.emissions;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.framework.lang.Sandbox;
import org.opentripplanner.transit.model.basic.TransitMode;

@Sandbox
public class DigitransitEmissionsAgency implements Serializable {

  private String db;
  private String agency_id;
  private String agency_name;
  private Map<String, DigitransitEmissionsMode> modes;

  public DigitransitEmissionsAgency(String db, String agency_id, String agency_name) {
    this.db = db;
    this.agency_id = agency_id;
    this.agency_name = agency_name;
    this.modes = new HashMap<>();
  }

  public String getDb() {
    return db;
  }

  public String getAgency_id() {
    return agency_id;
  }

  public String getAgency_name() {
    return agency_name;
  }

  public Map<String, DigitransitEmissionsMode> getModes() {
    return modes;
  }

  public void setModes(Map<String, DigitransitEmissionsMode> modes) {
    this.modes = modes;
  }

  public void addMode(DigitransitEmissionsMode mode) {
    this.modes.put(mode.getName(), mode);
  }

  public DigitransitEmissionsMode getMode(TransitMode mode) {
    if (this.modes.containsKey(mode.name())) {
      return this.modes.get(mode.name());
    }
    return null;
  }

  /**
   * Returns the average CO2 emissions (g/km) per person for a specific transit mode.
   * @param modeName name of transit mode
   * @return CO2 emissions (g/km) per person
   */
  public float getAverageCo2EmissionsByModePerPerson(String modeName) {
    if (this.modes.containsKey(modeName)) {
      return this.modes.get(modeName).getAverageCo2EmissionsPerPersonPerKm();
    }
    return 0;
  }

  /**
   * Returns the CO2 emissions (g/km) per person for a specific transit mode and the number of
   * passengers.
   * @param modeName name of transit mode
   * @return CO2 emissions (g/km) per person
   */
  public float getCo2EmissionsByModeAndNumberOfPassengers(String modeName, int numberOfPassengers) {
    if (this.modes.containsKey(modeName)) {
      return this.modes.get(modeName).getEmissionsPerPersonByNumberOfPassengers(numberOfPassengers);
    }
    return 0;
  }
}
