package org.opentripplanner.ext.dataoverlay;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.tokenizer.UnknownFunctionOrVariableException;
import org.opentripplanner.ext.dataoverlay.configuration.TimeUnit;
import org.opentripplanner.ext.dataoverlay.routing.DataOverlayContext;
import org.opentripplanner.ext.dataoverlay.routing.Parameter;
import org.opentripplanner.street.model.edge.StreetEdgeCostExtension;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.state.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract grid data description class which is stored at StreetEdge.
 *
 * @author Katja Danilova
 */
class DataOverlayStreetEdgeCostExtension implements StreetEdgeCostExtension, Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(
    DataOverlayStreetEdgeCostExtension.class
  );

  private final Instant dataStartTime;
  private final Map<String, float[]> variableValues;
  private final TimeUnit timeUnit;

  /**
   * Sets the abstract grid data
   *
   * @param dataStartTime  the time when the grid records start
   * @param variableValues map of variable names and arrays of their values
   * @param timeUnit       time unit of the data overlay
   */
  DataOverlayStreetEdgeCostExtension(
    Instant dataStartTime,
    Map<String, float[]> variableValues,
    TimeUnit timeUnit
  ) {
    this.dataStartTime = dataStartTime;
    this.variableValues = variableValues;
    this.timeUnit = timeUnit;
  }

  @Override
  public double calculateExtraCost(State state, int edgeLength, TraverseMode traverseMode) {
    if (traverseMode.isWalking() || traverseMode.isCyclingIsh()) {
      return (calculateDataOverlayPenalties(state) * edgeLength) / 1000;
    }
    return 0d;
  }

  /**
   * Calculates the total penalties based on request parameters and overlay data
   *
   * @return total penalty
   */
  private double calculateDataOverlayPenalties(State s) {
    if (variableValues == null) {
      return 0d;
    }
    double totalPenalty = 0d;
    Instant requestInstant = s.getRequest().startTime();
    DataOverlayContext context = s.dataOverlayContext();

    for (Parameter parameter : context.getParameters()) {
      var threshold = parameter.getThreshold();
      var penalty = parameter.getPenalty();
      String indexVariableName = parameter.getVariable();

      Instant dataStartTime = Instant.EPOCH;
      float[] genDataValuesForTime = new float[0];

      if (variableValues.containsKey(indexVariableName)) {
        dataStartTime = this.dataStartTime;
        genDataValuesForTime = variableValues.get(indexVariableName);
      }

      //calculate time format based on the input file settings
      int dataQualityRequestedTime = timeUnit.between(dataStartTime, requestInstant);

      if (dataQualityRequestedTime >= 0) {
        if (dataQualityRequestedTime < genDataValuesForTime.length) {
          float value = genDataValuesForTime[dataQualityRequestedTime];
          String penaltyFormulaString = parameter.getFormula();

          double penaltyForParameters = calculatePenaltyFromParameters(
            penaltyFormulaString,
            value,
            threshold,
            penalty
          );

          if (penaltyForParameters >= 0) {
            totalPenalty += penaltyForParameters;
          }
        } else {
          LOG.warn("No available data overlay for the given time");
        }
      }
    }
    return totalPenalty;
  }

  /**
   * Uses the formula from the penalty parameter and calculates the penalty based on that
   *
   * @param formula   penalty formula
   * @param value     data
   * @param threshold threshold parameter value
   * @param penalty   penalty parameter value
   * @return penalty
   */
  private double calculatePenaltyFromParameters(
    String formula,
    float value,
    double threshold,
    double penalty
  ) {
    Map<String, Double> variables = new HashMap<>();

    variables.put("THRESHOLD", threshold);
    variables.put("PENALTY", penalty);
    variables.put("VALUE", (double) value);

    try {
      Expression expression = new ExpressionBuilder(formula)
        .variables(variables.keySet().toArray(new String[0]))
        .build()
        .setVariables(variables);
      return expression.evaluate();
    } catch (UnknownFunctionOrVariableException ex) {
      throw new IllegalArgumentException(
        String.format("Formula %s did not receive all the required parameters", formula)
      );
    }
  }
}
