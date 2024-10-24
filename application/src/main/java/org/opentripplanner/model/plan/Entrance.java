package org.opentripplanner.model.plan;

public final class Entrance extends StepEntity {

  private final String code;
  private final String gtfsId;
  private final String name;

  public Entrance(String code, String gtfsId, String name) {
    this.code = code;
    this.gtfsId = gtfsId;
    this.name = name;
  }

  public static Entrance withCode(String code) {
    return new Entrance(code, null, null);
  }
}
