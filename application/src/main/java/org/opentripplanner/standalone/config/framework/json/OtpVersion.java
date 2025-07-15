package org.opentripplanner.standalone.config.framework.json;

public enum OtpVersion {
  /** @deprecated Replace this and remove when not in use. */
  @Deprecated
  NA("na"),
  V1_5("1.5"),
  V2_0("2.0"),
  V2_1("2.1"),
  V2_2("2.2"),
  V2_3("2.3"),
  V2_4("2.4"),
  V2_5("2.5"),
  V2_6("2.6"),
  V2_7("2.7"),
  V2_8("2.8");

  private final String text;

  OtpVersion(String text) {
    this.text = text;
  }

  @Override
  public String toString() {
    return text;
  }
}
