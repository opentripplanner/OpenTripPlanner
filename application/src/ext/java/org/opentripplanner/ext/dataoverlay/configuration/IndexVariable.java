package org.opentripplanner.ext.dataoverlay.configuration;

import java.io.Serializable;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * This class describes the variables for the incoming .nc data file
 */
public class IndexVariable implements Serializable {

  private final String name;
  private final String displayName;
  private final String variable;

  public IndexVariable(String name, String displayName, String variable) {
    this.name = name;
    this.displayName = displayName;
    this.variable = variable;
  }

  public String getName() {
    return name;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getVariable() {
    return variable;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(IndexVariable.class)
      .addStr("name", name)
      .addStr("displayName", displayName)
      .addStr("variable", variable)
      .toString();
  }
}
