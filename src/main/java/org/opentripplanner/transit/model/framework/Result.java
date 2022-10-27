package org.opentripplanner.transit.model.framework;

import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nonnull;

/**
 * A type for containing either a success or a failure type as the result of a computation.
 * <p>
 * It's very similar to the Either or Validation type found in functional programming languages.
 */
public abstract sealed class Result<T, E> {

  private Result() {}

  public static <T, E> Result<T, E> failure(@Nonnull E failure) {
    return new Failure<>(failure);
  }

  public static <T, E> Result<T, E> success(@Nonnull T success) {
    return new Success<>(success);
  }

  public static <E> Result<Void, E> success() {
    return new Success<>(null);
  }

  /**
   * Get the value contained with an erased type. If you want to use the typed version of the value
   * use {@link Result#ifFailure(Consumer)} or  {@link Result#ifSuccess(Consumer)}}.
   */
  protected abstract Object value();

  /**
   * If the value contained is failure return it cast to the correct type.
   * <p>
   * If it is a success, this method throws an exception.
   */
  public E failureValue() {
    if (isFailure()) {
      return (E) value();
    } else {
      throw new RuntimeException(
        "Value %s is not a failure. Check isFailure() before calling failureValue().".formatted(
            value()
          )
      );
    }
  }

  /**
   * If the type contained is a success.
   */
  public abstract boolean isSuccess();

  /**
   * If the type contained is a failure.
   */
  public boolean isFailure() {
    return !isSuccess();
  }

  /**
   * If the type contained is a success then execute the function passed into this method.
   */
  public abstract void ifSuccess(Consumer<T> func);

  /**
   * If the type contained is a failure then execute the function passed into this method.
   */
  public abstract void ifFailure(Consumer<E> func);

  /**
   * If the value contained is success return it cast to the correct type.
   * <p>
   * If it is a failure, this method throws an exception.
   */
  public T successValue() {
    if (isSuccess()) {
      return (T) value();
    } else {
      throw new RuntimeException(
        "Value %s is not a success. Check isSuccess() before calling successValue().".formatted(
            value()
          )
      );
    }
  }

  private static final class Failure<T, E> extends Result<T, E> {

    private final E failure;

    private Failure(E failure) {
      Objects.requireNonNull(failure, "failure must not be null");
      this.failure = failure;
    }

    protected E value() {
      return failure;
    }

    @Override
    public boolean isSuccess() {
      return false;
    }

    @Override
    public void ifSuccess(Consumer<T> func) {}

    @Override
    public void ifFailure(Consumer<E> func) {
      func.accept(failure);
    }
  }

  private static final class Success<T, E> extends Result<T, E> {

    private final T success;

    private Success(T success) {
      this.success = success;
    }

    protected T value() {
      return success;
    }

    @Override
    public boolean isSuccess() {
      return true;
    }

    @Override
    public void ifSuccess(Consumer<T> func) {
      func.accept(success);
    }

    @Override
    public void ifFailure(Consumer<E> func) {}
  }
}
