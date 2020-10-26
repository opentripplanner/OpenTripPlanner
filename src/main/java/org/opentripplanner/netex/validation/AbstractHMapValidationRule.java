package org.opentripplanner.netex.validation;

import org.opentripplanner.netex.index.api.NetexEntityIndexReadOnlyView;
import org.opentripplanner.netex.index.api.HMapValidationRule;

abstract class AbstractHMapValidationRule<K, V> implements HMapValidationRule<K, V> {
  protected NetexEntityIndexReadOnlyView index;

  void setup(NetexEntityIndexReadOnlyView index) {
    this.index = index;
  }
}
