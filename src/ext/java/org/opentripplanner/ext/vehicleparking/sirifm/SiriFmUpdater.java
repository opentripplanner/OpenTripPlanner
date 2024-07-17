package org.opentripplanner.ext.vehicleparking.sirifm;

import java.util.List;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.updater.spi.DataSource;
import org.opentripplanner.updater.vehicle_parking.AvailabiltyUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SiriFmUpdater implements DataSource<AvailabiltyUpdate> {

  private static final Logger LOG = LoggerFactory.getLogger(SiriFmUpdater.class);
  private final SiriFmUpdaterParameters params;

  public SiriFmUpdater(SiriFmUpdaterParameters params) {
    this.params = params;
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(this.getClass())
      .addStr("url", this.params.url().toString())
      .toString();
  }

  @Override
  public boolean update() {
    LOG.error("RUNNING {}", this);
    return true;
  }

  @Override
  public List<AvailabiltyUpdate> getUpdates() {
    return List.of();
  }
}
