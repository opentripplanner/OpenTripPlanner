package org.opentripplanner.framework.transaction.internal;

import org.opentripplanner.framework.transaction.Transaction;

enum RedBlueTransaction implements Transaction {
  RED,
  BLUE;

  RedBlueTransaction next() {
    return this == RED ? BLUE : RED;
  }
}
