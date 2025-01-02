package org.opentripplanner.updater.spi;

import java.time.Duration;

/**
 * This is named PollingGraphUpdaterConfig instead of Config in order to not conflict with the
 * config interfaces of child classes.
 */
public interface PollingGraphUpdaterParameters {
  Duration frequency();

  /** The config name/type for the updater. Used to reference the configuration element. */
  String configRef();
}
