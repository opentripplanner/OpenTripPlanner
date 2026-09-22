package org.opentripplanner.ext.taxi;

import java.io.Serializable;
import java.util.List;
import org.opentripplanner.ext.taxi.model.TaxiRoute;

/**
 * Repository for taxi route data.
 */
public interface TaxiRepository extends Serializable {
  /**
   * Add taxi routes to the repository.
   */
  void addRoutes(List<TaxiRoute> routes);

  /**
   * Return all stored taxi routes.
   */
  List<TaxiRoute> getRoutes();
}
