package org.opentripplanner.ext.taxi.internal;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.ext.taxi.TaxiRepository;
import org.opentripplanner.ext.taxi.model.TaxiRoute;

public class DefaultTaxiRepository implements TaxiRepository {

  private final List<TaxiRoute> routes = new ArrayList<>();

  @Override
  public void addRoutes(List<TaxiRoute> routes) {
    this.routes.addAll(routes);
  }

  @Override
  public List<TaxiRoute> getRoutes() {
    return List.copyOf(routes);
  }
}
