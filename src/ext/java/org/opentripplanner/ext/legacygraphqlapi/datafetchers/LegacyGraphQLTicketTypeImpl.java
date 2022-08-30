package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.ext.legacygraphqlapi.model.LegacyGraphQLTicketType;

public class LegacyGraphQLTicketTypeImpl
  implements LegacyGraphQLDataFetchers.LegacyGraphQLTicketType {

  @Override
  public DataFetcher<String> currency() {
    return environment -> ((LegacyGraphQLTicketType) environment.getSource()).getCurrency();
  }

  @Override
  public DataFetcher<String> fareId() {
    return environment -> ((LegacyGraphQLTicketType) environment.getSource()).getFareId();
  }

  @Override
  public DataFetcher<Relay.ResolvedGlobalId> id() {
    return environment ->
      new Relay.ResolvedGlobalId(
        "TicketType",
        ((LegacyGraphQLTicketType) environment.getSource()).getFareId()
      );
  }

  @Override
  public DataFetcher<Float> price() {
    return environment -> ((LegacyGraphQLTicketType) environment.getSource()).getPrice();
  }

  @Override
  public DataFetcher<Iterable<String>> zones() {
    return environment -> ((LegacyGraphQLTicketType) environment.getSource()).getZones();
  }
}
