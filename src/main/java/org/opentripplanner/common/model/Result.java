package org.opentripplanner.common.model;

import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nonnull;

/**
 * A type for containing either a success or a failure type as the result of a computation.
 * <p>
 * It's very similar to the Either or Validation type found in functional programming languages.
 */
public abstract class Result<L, R> {

  private Result() {}

  public static <L, R> Result<L, R> failure(@Nonnull L failure) {
    return new Failure<>(failure);
  }

  public static <L, R> Result<L, R> success(@Nonnull R success) {
    return new Success<>(success);
  }

  /**
   * Get the value contained with an erased type. If you want to use the typed version of the value
   * use {@link Result#ifFailure(Consumer)} or  {@link Result#ifSuccess(Consumer)}}.
   */
  public abstract Object value();

  /**
   * If the type contained is of the success type.
   */
  public abstract boolean isSuccess();

  /**
   * If the type contained is of the failure type.
   */
  public boolean isFailure() {
    return !isSuccess();
  }

  /**
   * If the type contained is a success then execute the function passed into this method.
   */
  public abstract void ifSuccess(Consumer<R> func);

  /**
   * If the type contained is a failure then execute the function passed into this method.
   */
  public abstract void ifFailure(Consumer<L> func);

  private static class Failure<L, R> extends Result<L, R> {

    private final L failure;

    private Failure(L failure) {
      Objects.requireNonNull(failure, "failure must not be null");
      this.failure = failure;
    }

    public L value() {
      return failure;
    }

    @Override
    public boolean isSuccess() {
      return false;
    }

    @Override
    public void ifSuccess(Consumer<R> func) {}

    @Override
    public void ifFailure(Consumer<L> func) {
      func.accept(failure);
    }
  }

  private static class Success<L, R> extends Result<L, R> {

    private final R success;

    private Success(R success) {
      Objects.requireNonNull(success);
      this.success = success;
    }

    public R value() {
      return success;
    }

    @Override
    public boolean isSuccess() {
      return true;
    }

    @Override
    public void ifSuccess(Consumer<R> func) {
      func.accept(success);
    }

    @Override
    public void ifFailure(Consumer<L> func) {}
  }
}
