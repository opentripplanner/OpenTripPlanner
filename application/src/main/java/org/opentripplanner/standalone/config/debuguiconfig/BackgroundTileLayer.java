package org.opentripplanner.standalone.config.debuguiconfig;

public record BackgroundTileLayer(
  String name,
  String templateUrl,
  int tileSize,
  String attribution
) {}
