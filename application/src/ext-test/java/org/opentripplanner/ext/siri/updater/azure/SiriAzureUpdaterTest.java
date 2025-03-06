package org.opentripplanner.ext.siri.updater.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.azure.core.util.ExpandableStringEnum;
import com.azure.messaging.servicebus.ServiceBusErrorSource;
import com.azure.messaging.servicebus.ServiceBusException;
import com.azure.messaging.servicebus.ServiceBusFailureReason;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.opentripplanner.framework.io.OtpHttpClientException;
import org.opentripplanner.updater.spi.WriteToGraphCallback;
import uk.org.siri.siri20.ServiceDelivery;

class SiriAzureUpdaterTest {

  private SiriAzureUpdaterParameters mockConfig;
  private SiriAzureUpdater updater;
  private SiriAzureUpdater.CheckedRunnable task;

  @BeforeEach
  public void setUp() throws Exception {
    mockConfig = mock(SiriAzureUpdaterParameters.class);
    when(mockConfig.getType()).thenReturn("siri-azure-test-updater");
    when(mockConfig.configRef()).thenReturn("testConfigRef");
    when(mockConfig.getAuthenticationType()).thenReturn(AuthenticationType.SharedAccessKey);
    when(mockConfig.getFullyQualifiedNamespace()).thenReturn("testNamespace");
    when(mockConfig.getServiceBusUrl()).thenReturn("testServiceBusUrl");
    when(mockConfig.getTopicName()).thenReturn("testTopic");
    when(mockConfig.getDataInitializationUrl()).thenReturn("http://testurl.com");
    when(mockConfig.getTimeout()).thenReturn(5000);
    when(mockConfig.feedId()).thenReturn("testFeedId");
    when(mockConfig.getAutoDeleteOnIdle()).thenReturn(Duration.ofHours(1));
    when(mockConfig.getPrefetchCount()).thenReturn(10);
    when(mockConfig.isFuzzyTripMatching()).thenReturn(true);

    // Create a spy on AbstractAzureSiriUpdater with the mock configuration
    updater = spy(
      new SiriAzureUpdater(
        mockConfig,
        new SiriAzureMessageHandler() {
          @Override
          public void setup(WriteToGraphCallback writeToGraphCallback) {}

          @Override
          @Nullable
          public Future<?> handleMessage(ServiceDelivery serviceDelivery, String messageId) {
            return null;
          }
        }
      )
    );

    task = mock(SiriAzureUpdater.CheckedRunnable.class);
  }

  /**
   * Tests the retry mechanism when a retryable ServiceBusException is thrown multiple times
   * and checks that it follows the backoff sequence.
   */
  @Test
  void testExecuteWithRetry_FullBackoffSequence() throws Throwable {
    final int totalRunCalls = 10; // 9 failures + 1 success
    final int totalSleepCalls = 9; // 9 retries

    doNothing().when(updater).sleep(anyInt());

    // Configure the task to throw a retryable exception for 9 attempts and then succeed
    doThrow(createServiceBusException(ServiceBusFailureReason.SERVICE_BUSY))
      .doThrow(createServiceBusException(ServiceBusFailureReason.SERVICE_BUSY))
      .doThrow(createServiceBusException(ServiceBusFailureReason.SERVICE_BUSY))
      .doThrow(createServiceBusException(ServiceBusFailureReason.SERVICE_BUSY))
      .doThrow(createServiceBusException(ServiceBusFailureReason.SERVICE_BUSY))
      .doThrow(createServiceBusException(ServiceBusFailureReason.SERVICE_BUSY))
      .doThrow(createServiceBusException(ServiceBusFailureReason.SERVICE_BUSY))
      .doThrow(createServiceBusException(ServiceBusFailureReason.SERVICE_BUSY))
      .doThrow(createServiceBusException(ServiceBusFailureReason.SERVICE_BUSY))
      .doNothing() // Succeed on the 10th attempt
      .when(task)
      .run();

    updater.executeWithRetry(task, "Test Task");

    verify(updater, times(totalSleepCalls)).sleep(anyInt());

    InOrder inOrder = inOrder(updater);
    inOrder.verify(updater).sleep(1000);
    inOrder.verify(updater).sleep(2000);
    inOrder.verify(updater).sleep(4000);
    inOrder.verify(updater).sleep(8000);
    inOrder.verify(updater).sleep(16000);
    inOrder.verify(updater).sleep(32000);

    for (int i = 0; i < 3; i++) {
      inOrder.verify(updater).sleep(60000);
    }

    verify(task, times(totalRunCalls)).run();
  }

  /**
   * Tests the executeWithRetry method when a non-retryable exception is thrown.
   * Ensures that no further retries are attempted and sleep is not called.
   */
  @Test
  public void testExecuteWithRetry_NonRetryableException() throws Throwable {
    doNothing().when(updater).sleep(anyInt());

    ServiceBusException serviceBusException = createServiceBusException(
      ServiceBusFailureReason.MESSAGE_SIZE_EXCEEDED
    );

    doThrow(serviceBusException).when(task).run();

    try {
      updater.executeWithRetry(task, "Test Task");
    } catch (ServiceBusException e) {
      assertEquals(
        ServiceBusFailureReason.MESSAGE_SIZE_EXCEEDED,
        e.getReason(),
        "Exception should have reason MESSAGE_SIZE_EXCEEDED"
      );
    }

    verify(updater, never()).sleep(anyInt());
    verify(task, times(1)).run();
  }

  /**
   * Tests the executeWithRetry method when the task fails multiple times with retryable exceptions
   * and then succeeds, ensuring that sleep is called the expected number of times with correct durations.
   */
  @Test
  public void testExecuteWithRetry_MultipleRetriesThenSuccess() throws Throwable {
    final int retriesBeforeSuccess = 3;
    CountDownLatch latch = new CountDownLatch(retriesBeforeSuccess);

    doThrow(createServiceBusException(ServiceBusFailureReason.SERVICE_BUSY))
      .doThrow(createServiceBusException(ServiceBusFailureReason.SERVICE_BUSY))
      .doThrow(createServiceBusException(ServiceBusFailureReason.SERVICE_BUSY))
      .doNothing()
      .when(task)
      .run();

    doAnswer(invocation -> {
      latch.countDown();
      return null;
    })
      .when(updater)
      .sleep(anyInt());

    updater.executeWithRetry(task, "Test Task");

    boolean completed = latch.await(5, TimeUnit.SECONDS);
    assertTrue(completed, "Expected sleep calls were not made.");

    ArgumentCaptor<Integer> sleepCaptor = ArgumentCaptor.forClass(Integer.class);
    verify(updater, times(retriesBeforeSuccess)).sleep(sleepCaptor.capture());

    var sleepDurations = sleepCaptor.getAllValues();
    long[] expectedBackoffSequence = { 1000, 2000, 4000 };

    for (int i = 0; i < expectedBackoffSequence.length; i++) {
      assertEquals(
        expectedBackoffSequence[i],
        Long.valueOf(sleepDurations.get(i)),
        "Backoff duration mismatch at retry " + (i + 1)
      );
    }

    verify(task, times(retriesBeforeSuccess + 1)).run();
  }

  /**
   * Tests the executeWithRetry method when the task succeeds on the first attempt.
   * Ensures that no sleep calls are made.
   */
  @Test
  public void testExecuteWithRetry_ImmediateSuccess() throws Throwable {
    doNothing().when(task).run();
    doNothing().when(updater).sleep(anyInt());

    updater.executeWithRetry(task, "Test Task");

    verify(updater, never()).sleep(anyInt());
    verify(task, times(1)).run();
  }

  /**
   * Tests the executeWithRetry method when the task fails once with a retryable exception
   * and then succeeds on the first retry.
   */
  @Test
  public void testExecuteWithRetry_OneRetryThenSuccess() throws Throwable {
    final int expectedSleepCalls = 1;
    CountDownLatch latch = new CountDownLatch(expectedSleepCalls);

    doThrow(createServiceBusException(ServiceBusFailureReason.SERVICE_BUSY))
      .doNothing()
      .when(task)
      .run();

    doAnswer(invocation -> {
      if (invocation.getArgument(0).equals(1000)) {
        latch.countDown();
      }
      return null;
    })
      .when(updater)
      .sleep(anyInt());

    updater.executeWithRetry(task, "Test Task");

    boolean completed = latch.await(5, TimeUnit.SECONDS);
    assertTrue(completed, "Expected sleep call was not made.");

    verify(updater, times(expectedSleepCalls)).sleep(1000);
    verify(task, times(2)).run();
  }

  /**
   * Parameterized test to verify that shouldRetry returns the correct value for each ServiceBusFailureReason.
   *
   * @param reason         The ServiceBusFailureReason to test.
   * @param expectedRetry  The expected result of shouldRetry.
   */
  @ParameterizedTest(name = "shouldRetry with reason {0} should return {1}")
  @MethodSource("provideServiceBusFailureReasons")
  @DisplayName("Test shouldRetry for all ServiceBusFailureReason values")
  void testShouldRetry_ServiceBusFailureReasons(
    ServiceBusFailureReason reason,
    boolean expectedRetry
  ) throws Exception {
    ServiceBusException serviceBusException = createServiceBusException(reason);
    boolean result = updater.shouldRetry(serviceBusException);
    assertEquals(
      expectedRetry,
      result,
      "shouldRetry should return " + expectedRetry + " for reason " + reason
    );
  }

  /**
   * Test that shouldRetry returns false for non-ServiceBus exceptions.
   */
  @Test
  @DisplayName("shouldRetry should return false for non-ServiceBus exceptions")
  public void testShouldRetry_NonServiceBusException() {
    Exception genericException = new Exception("Generic exception");
    boolean result = updater.shouldRetry(genericException);
    assertFalse(result, "shouldRetry should return false for non-ServiceBus exceptions");
  }

  /**
   * Test that shouldRetry handles all ServiceBusFailureReason values.
   * Since enums are closed, this test ensures that the parameterized tests cover all existing values.
   */
  @Test
  @DisplayName("shouldRetry covers all ServiceBusFailureReason values")
  public void testShouldRetry_CoversAllReasons() {
    long enumCount = getExpandableStringEnumValues(ServiceBusFailureReason.class).size();
    long testCaseCount = provideServiceBusFailureReasons().count();
    assertEquals(
      enumCount,
      testCaseCount,
      "All ServiceBusFailureReason values should be covered by tests."
    );
  }

  @Test
  void testExecuteWithRetry_InterruptedException() throws Throwable {
    final int expectedRunCalls = 2;
    final int expectedSleepCalls = 1;

    doThrow(createServiceBusException(ServiceBusFailureReason.SERVICE_BUSY))
      .doThrow(new InterruptedException("Sleep interrupted"))
      .when(task)
      .run();

    doNothing().when(updater).sleep(1000);

    InterruptedException thrownException = assertThrows(
      InterruptedException.class,
      () -> {
        updater.executeWithRetry(task, "Test Task");
      },
      "Expected executeWithRetry to throw InterruptedException"
    );

    assertEquals(
      "Sleep interrupted",
      thrownException.getMessage(),
      "Exception message should match"
    );
    verify(updater, times(expectedSleepCalls)).sleep(1000);
    verify(task, times(expectedRunCalls)).run();
    assertTrue(Thread.currentThread().isInterrupted(), "Thread should be interrupted");
  }

  @Test
  void testExecuteWithRetry_OtpHttpClientException() throws Throwable {
    final int retryAttempts = 3;
    final int expectedSleepCalls = retryAttempts;

    doThrow(new OtpHttpClientException("could not get historical data"))
      .doThrow(new OtpHttpClientException("could not get historical data"))
      .doThrow(new OtpHttpClientException("could not get historical data"))
      .doNothing()
      .when(task)
      .run();

    doNothing().when(updater).sleep(anyInt());

    updater.executeWithRetry(task, "Test Task");

    ArgumentCaptor<Integer> sleepCaptor = ArgumentCaptor.forClass(Integer.class);
    verify(updater, times(expectedSleepCalls)).sleep(sleepCaptor.capture());

    List<Integer> sleepDurations = sleepCaptor.getAllValues();
    List<Integer> expectedBackoffSequence = Arrays.asList(1000, 2000, 4000);

    for (int i = 0; i < retryAttempts; i++) {
      assertEquals(
        expectedBackoffSequence.get(i),
        sleepDurations.get(i),
        "Backoff duration mismatch at retry " + (i + 1)
      );
    }

    verify(task, times(retryAttempts + 1)).run();
  }

  @Test
  void testExecuteWithRetry_UnexpectedException() throws Throwable {
    doNothing().when(updater).sleep(anyInt());

    Exception unexpectedException = new NullPointerException("Unexpected null value");
    doThrow(unexpectedException).when(task).run();

    Exception thrown = assertThrows(
      NullPointerException.class,
      () -> {
        updater.executeWithRetry(task, "Test Task");
      },
      "Expected executeWithRetry to throw NullPointerException"
    );

    assertEquals("Unexpected null value", thrown.getMessage(), "Exception message should match");
    verify(updater, never()).sleep(anyInt());
    verify(task, times(1)).run();
  }

  /**
   * Provides test arguments for each ServiceBusFailureReason and the expected shouldRetry outcome.
   *
   * @return Stream of Arguments containing ServiceBusFailureReason and expected boolean.
   */
  private static Stream<Arguments> provideServiceBusFailureReasons() {
    return Stream.of(
      // Retryable (Transient) Errors
      Arguments.of(ServiceBusFailureReason.SERVICE_BUSY, true),
      Arguments.of(ServiceBusFailureReason.SERVICE_TIMEOUT, true),
      Arguments.of(ServiceBusFailureReason.SERVICE_COMMUNICATION_ERROR, true),
      Arguments.of(ServiceBusFailureReason.MESSAGE_LOCK_LOST, true),
      Arguments.of(ServiceBusFailureReason.SESSION_LOCK_LOST, true),
      Arguments.of(ServiceBusFailureReason.SESSION_CANNOT_BE_LOCKED, true),
      Arguments.of(ServiceBusFailureReason.QUOTA_EXCEEDED, true),
      Arguments.of(ServiceBusFailureReason.GENERAL_ERROR, true),
      Arguments.of(ServiceBusFailureReason.UNAUTHORIZED, true),
      // Non-Retryable Errors
      Arguments.of(ServiceBusFailureReason.MESSAGING_ENTITY_NOT_FOUND, false),
      Arguments.of(ServiceBusFailureReason.MESSAGING_ENTITY_DISABLED, false),
      Arguments.of(ServiceBusFailureReason.MESSAGE_SIZE_EXCEEDED, false),
      Arguments.of(ServiceBusFailureReason.MESSAGE_NOT_FOUND, false),
      Arguments.of(ServiceBusFailureReason.MESSAGING_ENTITY_ALREADY_EXISTS, false)
    );
  }

  /**
   * Helper method to create a ServiceBusException with a specified failure reason.
   *
   * @param reason The ServiceBusFailureReason to set.
   * @return A ServiceBusException instance with the specified reason.
   */
  private ServiceBusException createServiceBusException(ServiceBusFailureReason reason) {
    ServiceBusException exception = new ServiceBusException(
      new Throwable(),
      ServiceBusErrorSource.RECEIVE
    );
    try {
      Field reasonField = ServiceBusException.class.getDeclaredField("reason");
      reasonField.setAccessible(true);
      reasonField.set(exception, reason);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException("Failed to set ServiceBusFailureReason via reflection", e);
    }
    return exception;
  }

  /**
   * Helper method to retrieve all instances of an ExpandableStringEnum subclass.
   *
   * @param clazz The class of the ExpandableStringEnum subclass.
   * @param <T>   The type parameter extending ExpandableStringEnum.
   * @return A Collection of all registered instances.
   */
  private static <T extends ExpandableStringEnum<T>> Collection<T> getExpandableStringEnumValues(
    Class<T> clazz
  ) {
    try {
      Method valuesMethod = ExpandableStringEnum.class.getDeclaredMethod("values", Class.class);
      valuesMethod.setAccessible(true);
      @SuppressWarnings("unchecked")
      Collection<T> values = (Collection<T>) valuesMethod.invoke(null, clazz);
      return values;
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException("Failed to retrieve values from ExpandableStringEnum.", e);
    }
  }
}
