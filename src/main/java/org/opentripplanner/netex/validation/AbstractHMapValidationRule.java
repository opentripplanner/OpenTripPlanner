package org.opentripplanner.netex.validation;

import org.opentripplanner.netex.index.api.HMapValidationRule;
import org.opentripplanner.netex.index.api.NetexEntityIndexReadOnlyView;

abstract class AbstractHMapValidationRule<K, V> implements HMapValidationRule<K, V> {

  protected NetexEntityIndexReadOnlyView index;

  void setup(NetexEntityIndexReadOnlyView index) {
    this.index = index;
  }
}
