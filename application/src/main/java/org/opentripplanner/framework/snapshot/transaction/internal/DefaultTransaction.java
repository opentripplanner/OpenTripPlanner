package org.opentripplanner.framework.snapshot.transaction.internal;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import org.opentripplanner.framework.snapshot.transaction.Transaction;

final class DefaultTransaction implements Transaction {
  private static final AtomicLong ID_SEQUENCE = new AtomicLong(0);
  private final long id;

  DefaultTransaction() {
    this.id = ID_SEQUENCE.incrementAndGet();
  }

  public static DefaultTransaction next() {
    return new DefaultTransaction();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DefaultTransaction that = (DefaultTransaction) o;
    return id == that.id;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
