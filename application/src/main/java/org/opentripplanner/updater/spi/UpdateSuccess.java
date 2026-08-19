package org.opentripplanner.updater.spi;

import javax.annotation.Nullable;

/**
 * The result of a successful application of a realtime update, for example for trips or
 * vehicle positions. Its extra information is the provider of the update.
 */
public record UpdateSuccess(@Nullable String producer) {
  /**
   * Create an instance without a provider.
   */
  public static UpdateSuccess of() {
    return new UpdateSuccess(null);
  }

  /**
   * Create an instance with a provider.
   */
  public static UpdateSuccess of(@Nullable String producer) {
    return new UpdateSuccess(producer);
  }
}
