package org.opentripplanner.ext.empiricaldelay.internal;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import org.opentripplanner.ext.empiricaldelay.EmpiricalDelayRepository;
import org.opentripplanner.ext.empiricaldelay.EmpiricalDelayService;
import org.opentripplanner.ext.empiricaldelay.model.EmpiricalDelay;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class DefaultEmpiricalDelayService implements EmpiricalDelayService {

  private final EmpiricalDelayRepository repository;

  public DefaultEmpiricalDelayService(EmpiricalDelayRepository repository) {
    this.repository = Objects.requireNonNull(repository);
  }

  @Override
  public Optional<EmpiricalDelay> findEmpiricalDelay(
    FeedScopedId tripId,
    LocalDate serviceDate,
    int stopPosInPattern
  ) {
    return repository.findEmpiricalDelay(tripId, serviceDate, stopPosInPattern);
  }
}
