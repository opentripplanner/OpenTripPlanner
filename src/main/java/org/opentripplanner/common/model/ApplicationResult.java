package org.opentripplanner.common.model;

import com.google.common.base.Preconditions;

public abstract class ApplicationResult<L, R> {

  private ApplicationResult() {}

  public static <L, R> ApplicationResult<L, R> failure(L left) {
    return new Failure<>(left);
  }

  public static <L, R> ApplicationResult<L, R> success(R right) {
    return new Success<>(right);
  }

  public abstract boolean isSuccess();

  public abstract Object value();

  public boolean isFailure() {
    return !isSuccess();
  }

  private static class Failure<L, R> extends ApplicationResult<L, R> {

    private final L left;

    private Failure(L left) {
      Preconditions.checkNotNull(left, "failure must not be null");

      this.left = left;
    }

    @Override
    public boolean isSuccess() {
      return false;
    }

    public L value() {
      return left;
    }
  }

  private static class Success<L, R> extends ApplicationResult<L, R> {

    private final R right;

    private Success(R right) {
      Preconditions.checkNotNull(right, "right must not be null");

      this.right = right;
    }

    @Override
    public boolean isSuccess() {
      return true;
    }

    public R value() {
      return right;
    }
  }
}
