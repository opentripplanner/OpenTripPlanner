package org.opentripplanner.framework.transaction.internal;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import org.opentripplanner.framework.transaction.Transaction;

final class DefaultTransaction implements Transaction {
  private static final AtomicLong ID_SEQUENCE = new AtomicLong(0);
  private final long id;

  private DefaultTransaction() {
    this.id = ID_SEQUENCE.incrementAndGet();
  }

  static DefaultTransaction next() {
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
