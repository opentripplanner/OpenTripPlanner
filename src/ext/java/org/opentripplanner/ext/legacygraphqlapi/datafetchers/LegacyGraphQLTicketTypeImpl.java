package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import java.text.DecimalFormat;
import org.opentripplanner.ext.fares.model.FareRuleSet;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;

public class LegacyGraphQLTicketTypeImpl
  implements LegacyGraphQLDataFetchers.LegacyGraphQLTicketType {

  @Override
  public DataFetcher<String> currency() {
    return environment ->
      ((FareRuleSet) environment.getSource()).getFareAttribute().getCurrencyType();
  }

  @Override
  public DataFetcher<String> fareId() {
    return environment ->
      ((FareRuleSet) environment.getSource()).getFareAttribute().getId().toString();
  }

  @Override
  public DataFetcher<Relay.ResolvedGlobalId> id() {
    return environment ->
      new Relay.ResolvedGlobalId(
        "TicketType",
        ((FareRuleSet) environment.getSource()).getFareAttribute().getId().toString()
      );
  }

  @Override
  public DataFetcher<Double> price() {
    return environment ->
      // This is needed to overcome float prices becoming inexact in output, e.g. 2.8 becoming 2.7999...
      Double.valueOf(
        new DecimalFormat("#.00")
          .format(((FareRuleSet) environment.getSource()).getFareAttribute().getPrice())
      );
  }

  @Override
  public DataFetcher<Iterable<String>> zones() {
    return environment -> ((FareRuleSet) environment.getSource()).getContains();
  }
}
