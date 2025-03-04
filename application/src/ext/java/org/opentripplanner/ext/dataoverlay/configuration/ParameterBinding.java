package org.opentripplanner.ext.dataoverlay.configuration;

import java.io.Serializable;
import org.opentripplanner.ext.dataoverlay.api.ParameterName;
import org.opentripplanner.ext.dataoverlay.routing.Parameter;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * This class describes the expected routing request parameter for the generic data
 * <p>
 * Example parameter config:
 * <pre>
 * {
 *   "name": "harmful_micro_particles_PM2_5",
 *   "variable": "harmfulMicroparticlesPM2_5",
 *   "formula": "(VALUE + 1 - THRESHOLD) * PENALTY"
 * }
 * </pre>
 */
public class ParameterBinding implements Serializable {

  private final ParameterName name;
  private final String variable;
  private final String formula;

  public ParameterBinding(ParameterName name, String variable, String formula) {
    this.name = name;
    this.variable = variable;
    this.formula = formula;
  }

  public ParameterName getName() {
    return name;
  }

  public String getVariable() {
    return variable;
  }

  public String getFormula() {
    return formula;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(Parameter.class)
      .addEnum("name", name)
      .addStr("variable", variable)
      .addStr("formula", formula)
      .toString();
  }
}
