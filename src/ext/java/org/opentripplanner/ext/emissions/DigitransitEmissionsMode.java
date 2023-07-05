package org.opentripplanner.ext.emissions;

import java.io.Serializable;
import org.opentripplanner.framework.lang.Sandbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Sandbox
public class DigitransitEmissionsMode implements Serializable {

  private String name;
  private float avg;
  private int p_avg;
  private float averageCo2EmissionsPerPersonPerKm;

  private static final Logger LOG = LoggerFactory.getLogger(DigitransitEmissionsMode.class);

  /**
   * @param mode  transit mode name
   * @param avg   average CO2 emissions in grams per kilometre
   * @param p_avg average number of passengers per vehicle
   */
  public DigitransitEmissionsMode(String mode, String avg, int p_avg) {
    this.name = mode;
    this.p_avg = p_avg;

    try {
      this.avg = Float.parseFloat(avg);
    } catch (NumberFormatException e) {
      LOG.warn("Converting Digitransit emissions average value failed.", e);
      this.avg = 0.0F;
    }
    this.averageCo2EmissionsPerPersonPerKm = getEmissionsPerPersonByNumberOfPassengers(p_avg);
  }

  public String getName() {
    return name;
  }

  public double getAvg() {
    return avg;
  }

  public int getP_avg() {
    return p_avg;
  }

  public float getAverageCo2EmissionsPerPersonPerKm() {
    return this.averageCo2EmissionsPerPersonPerKm;
  }

  public float getEmissionsPerPersonByNumberOfPassengers(int numberOfPassengers) {
    return Math.round(this.avg / numberOfPassengers);
  }
}
