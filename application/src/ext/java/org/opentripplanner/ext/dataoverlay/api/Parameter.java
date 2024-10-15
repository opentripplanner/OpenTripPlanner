package org.opentripplanner.ext.dataoverlay.api;

record Parameter(ParameterName name, ParameterType type) {
  String keyString() {
    return name.name() + '_' + type.name();
  }
}
