package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.ext.fares.model.FareRuleSet;

public class TicketTypeImpl implements GraphQLDataFetchers.GraphQLTicketType {

  @Override
  public DataFetcher<String> currency() {
    return environment ->
      ((FareRuleSet) environment.getSource()).getFareAttribute()
        .getPrice()
        .currency()
        .getCurrencyCode();
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
    return environment -> {
      // This is needed to overcome float prices becoming inexact in output, e.g. 2.8 becoming 2.7999...
      DecimalFormat format = new DecimalFormat("#.00");
      DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
      symbols.setDecimalSeparator('.');
      format.setDecimalFormatSymbols(symbols);
      String price = format.format(
        ((FareRuleSet) environment.getSource()).getFareAttribute()
          .getPrice()
          .fractionalAmount()
          .floatValue()
      );
      return Double.valueOf(price);
    };
  }

  @Override
  public DataFetcher<Iterable<String>> zones() {
    return environment -> ((FareRuleSet) environment.getSource()).getContains();
  }
}
