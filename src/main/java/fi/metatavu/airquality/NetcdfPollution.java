package fi.metatavu.airquality;

import ucar.ma2.Array;
import ucar.ma2.ArrayFloat;

/**
 * This class represents pollution. It contains a field for each supported pollutant.
 */
public class NetcdfPollution {

  private final ArrayFloat.D4 carbonMonoxide;
  private final ArrayFloat.D4 nitrogenMonoxide;
  private final ArrayFloat.D4 nitrogenDioxide;
  private final ArrayFloat.D4 ozone;
  private final ArrayFloat.D4 sulfurDioxide;
  private final ArrayFloat.D4 particles2_5;
  private final ArrayFloat.D4 particles10;

  public NetcdfPollution(Array carbonMonoxide, Array nitrogenMonoxide, Array nitrogenDioxide, Array ozone, Array sulfurDioxide, Array particles2_5, Array particles10) {
    this.carbonMonoxide = (ArrayFloat.D4) carbonMonoxide;
    this.nitrogenMonoxide = (ArrayFloat.D4) nitrogenMonoxide;
    this.nitrogenDioxide = (ArrayFloat.D4) nitrogenDioxide;
    this.ozone = (ArrayFloat.D4) ozone;
    this.sulfurDioxide = (ArrayFloat.D4) sulfurDioxide;
    this.particles2_5 = (ArrayFloat.D4) particles2_5;
    this.particles10 = (ArrayFloat.D4) particles10;
  }

  public ArrayFloat.D4 getCarbonMonoxide() {
    return this.carbonMonoxide;
  }

  public ArrayFloat.D4 getNitrogenMonoxide() {
    return this.nitrogenMonoxide;
  }

  public ArrayFloat.D4 getNitrogenDioxide() {
    return this.nitrogenDioxide;
  }

  public ArrayFloat.D4 getOzone() {
    return this.ozone;
  }

  public ArrayFloat.D4 getSulfurDioxide() {
    return this.sulfurDioxide;
  }

  public ArrayFloat.D4 getParticles2_5() {
    return this.particles2_5;
  }

  public ArrayFloat.D4 getParticles10() {
    return this.particles10;
  }
}
