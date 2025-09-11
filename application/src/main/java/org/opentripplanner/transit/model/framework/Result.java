package org.opentripplanner.transit.model.framework;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A type for containing either a success or a failure type as the result of a computation.
 * <p>
 * It's very similar to the Either or Validation type found in functional programming languages.
 *
 * @deprecated This not possible to use inside a constructor - can only return one thing. Which,
 *     then makes encapsulation harder and leaves the door open to forget. Also, having to create
 *     error codes, mapping and handling code for hundreds of different possible validation error
 *     types make changes harder, and cleanup impossible. This is a nice way to design APIs, but
 *     faced with hundreds or even thousands of different validation error types and the same
 *     amount of code branches this breaks.
 *     <p>
 *     I will use the {@link DataValidationException} for now, but we need to make an error
 *     handling strategy which take all use-cases and goals into account, also pragmatic goals
 *     like maintainability. The {@link DataValidationException} is not a solution, but it
 *     at least it allows me to omit returning all possible error on every method ...
 *     <p>
 *     See https://github.com/opentripplanner/OpenTripPlanner/issues/5070
 */
@Deprecated
public abstract sealed class Result<T, E> {

  private Result() {}

  public static <T, E> Result<T, E> failure(E failure) {
    return new Failure<>(failure);
  }

  public static <T, E> Result<T, E> success(T success) {
    return new Success<>(success);
  }

  public static <E> Result<Void, E> success() {
    return new Success<>(null);
  }

  /**
   * If this instance is a success then the mapper transforms its value. If this instance is a
   * failure then a new failed instance with the correct success type is returned.
   *
   * @param <N> The success type of the new Result instance.
   */
  public <N> Result<N, E> mapSuccess(Function<T, N> mapper) {
    if (isSuccess()) {
      return Result.success(mapper.apply(successValue()));
    } else {
      return Result.failure(failureValue());
    }
  }

  /**
   * If this instance is a success then the mapper tries to transform its value, unwrapping any failures in the mapper.
   * If this instance is a failure then a new failed instance with the correct success type is returned.
   *
   * @param <N> The success type of the new Result instance.
   */
  public <N> Result<N, E> flatMap(Function<T, Result<N, E>> mapper) {
    if (isSuccess()) {
      return mapper.apply(successValue());
    } else {
      return Result.failure(failureValue());
    }
  }

  /**
   * Creates a new instance of this class with a new success type. This is useful if you know
   * that it is a failure and want to return it in a method without having to cast the success type.
   * <p>
   * If this instance is not a failure an exception is thrown.
   *
   * @param <N> The success type of the new Result instance.
   */
  public <N> Result<N, E> toFailureResult() {
    return Result.failure(failureValue());
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
