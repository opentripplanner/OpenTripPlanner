package org.opentripplanner.framework.snapshot.application;

import java.util.function.Consumer;
import java.util.function.Function;
import org.opentripplanner.framework.snapshot.domain.world.TransitWorld;

public interface StateAccess {

  <R> R read(Function<TransitWorld, R> work);

  void write(Consumer<TransactionalContext> work);
}
