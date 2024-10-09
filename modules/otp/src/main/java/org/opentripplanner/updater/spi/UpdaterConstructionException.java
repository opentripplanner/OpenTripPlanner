package org.opentripplanner.updater.spi;

public class UpdaterConstructionException extends RuntimeException {

  public UpdaterConstructionException(Throwable cause) {
    super(cause);
  }

  public UpdaterConstructionException(String message) {
    super(message);
  }
}
