package org.opentripplanner.model.plan;

public final class Entrance extends StepEntity {

  private final String code;
  private final String gtfsId;
  private final String name;
  private final boolean accessible;

  public Entrance(String code, String gtfsId, String name, boolean accessible) {
    this.code = code;
    this.gtfsId = gtfsId;
    this.name = name;
    this.accessible = accessible;
  }

  public static Entrance withCodeAndAccessible(String code, boolean accessible) {
    return new Entrance(code, null, null, accessible);
  }
}
