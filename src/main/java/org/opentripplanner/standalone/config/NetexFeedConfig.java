package org.opentripplanner.standalone.config;

import java.util.regex.Pattern;

/**
 * Configure a NeTEx feed. Overrides default values specified in {@link NetexDefaultsConfig}
 */
public class NetexFeedConfig extends TransitFeedConfig {

  /**
   * Overrides {@link NetexDefaultsConfig#sharedFilePattern}
   */
  public final Pattern sharedFilePattern;

  /**
   * Overrides {@link NetexDefaultsConfig#sharedGroupFilePattern}
   */
  public final Pattern sharedGroupFilePattern;

  public NetexFeedConfig(NodeAdapter config) {
    super(config);
    this.sharedFilePattern = config.asPattern("sharedFilePattern", null);
    this.sharedGroupFilePattern = config.asPattern("sharedGroupFilePattern", null);
  }
}
