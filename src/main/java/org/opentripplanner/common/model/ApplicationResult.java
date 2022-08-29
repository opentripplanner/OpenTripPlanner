package org.opentripplanner.common.model;

import com.google.common.base.Preconditions;
import java.util.function.Consumer;

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

  public abstract void ifFailure(Consumer<L> func);

  public abstract void ifSuccess(Consumer<R> func);

  private static class Failure<L, R> extends ApplicationResult<L, R> {

    private final L failure;

    private Failure(L left) {
      Preconditions.checkNotNull(left, "failure must not be null");

      this.failure = left;
    }

    @Override
    public boolean isSuccess() {
      return false;
    }

    public L value() {
      return failure;
    }

    @Override
    public void ifFailure(Consumer<L> func) {
      func.accept(failure);
    }

    @Override
    public void ifSuccess(Consumer<R> func) {}
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

    @Override
    public void ifFailure(Consumer<L> func) {}

    @Override
    public void ifSuccess(Consumer<R> func) {
      func.accept(right);
    }
  }
}
