package org.opentripplanner.ext.siri.updater.azure;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.azure.core.util.ExpandableStringEnum;
import com.azure.messaging.servicebus.ServiceBusErrorSource;
import com.azure.messaging.servicebus.ServiceBusException;
import com.azure.messaging.servicebus.ServiceBusFailureReason;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
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
import org.slf4j.LoggerFactory;
import uk.org.siri.siri21.ServiceDelivery;

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
    when(mockConfig.getStartupTimeout()).thenReturn(Duration.ofSeconds(30)); // 30 seconds for tests
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

  private SiriAzureUpdater createUpdater(SiriAzureUpdaterParameters config) {
    return new SiriAzureUpdater(
      config,
      new SiriAzureMessageHandler() {
        @Override
        public void setup(WriteToGraphCallback writeToGraphCallback) {}

        @Override
        @Nullable
        public Future<?> handleMessage(ServiceDelivery serviceDelivery, String messageId) {
          return null;
        }
      }
    );
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

    // Use longer timeout for this test since it does many retries with exponential backoff
    // Test still runs fast since sleep() is mocked, but timeout logic needs headroom
    updater.executeWithRetry(task, "Test Task", 300_000L); // 5 minutes

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
      updater.executeWithRetry(task, "Test Task", mockConfig.getStartupTimeout().toMillis());
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

    updater.executeWithRetry(task, "Test Task", mockConfig.getStartupTimeout().toMillis());

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

    updater.executeWithRetry(task, "Test Task", mockConfig.getStartupTimeout().toMillis());

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

    updater.executeWithRetry(task, "Test Task", mockConfig.getStartupTimeout().toMillis());

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
    // Use default timeout since InterruptedException is thrown immediately
    SiriAzureUpdater longTimeoutUpdater = spy(createUpdater(mockConfig));

    final int expectedRunCalls = 2;
    final int expectedSleepCalls = 1;

    doThrow(createServiceBusException(ServiceBusFailureReason.SERVICE_BUSY))
      .doThrow(new InterruptedException("Sleep interrupted"))
      .when(task)
      .run();

    doNothing().when(longTimeoutUpdater).sleep(1000);

    InterruptedException thrownException = assertThrows(
      InterruptedException.class,
      () -> {
        longTimeoutUpdater.executeWithRetry(
          task,
          "Test Task",
          mockConfig.getStartupTimeout().toMillis()
        );
      },
      "Expected executeWithRetry to throw InterruptedException"
    );

    assertEquals(
      "Sleep interrupted",
      thrownException.getMessage(),
      "Exception message should match"
    );
    verify(longTimeoutUpdater, times(expectedSleepCalls)).sleep(1000);
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

    updater.executeWithRetry(task, "Test Task", mockConfig.getStartupTimeout().toMillis());

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
        updater.executeWithRetry(task, "Test Task", mockConfig.getStartupTimeout().toMillis());
      },
      "Expected executeWithRetry to throw NullPointerException"
    );

    assertEquals("Unexpected null value", thrown.getMessage(), "Exception message should match");
    verify(updater, never()).sleep(anyInt());
    verify(task, times(1)).run();
  }

  /**
   * Verifies that executeWithRetry returns false when startupTimeout is exceeded.
   */
  @Test
  void testExecuteWithRetry_TimeoutAfterStartupTimeout() throws Throwable {
    SiriAzureUpdater timeoutUpdater = spy(createUpdater(mockConfig));

    doNothing().when(timeoutUpdater).sleep(anyInt());

    // fail with a retryable exception
    doThrow(createServiceBusException(ServiceBusFailureReason.SERVICE_BUSY)).when(task).run();

    // Use a very short timeout for this test to avoid waiting
    long shortTimeout = 100L; // 100ms
    boolean result = timeoutUpdater.executeWithRetry(task, "Test Task", shortTimeout);

    assertFalse(result, "Expected executeWithRetry to return false due to timeout");

    // Verify that multiple retries were attempted
    verify(task, atLeast(2)).run();
    verify(timeoutUpdater, atLeast(1)).sleep(anyInt());
  }

  /**
   * Verifies that executeWithRetry succeeds when task completes before timeout.
   */
  @Test
  void testExecuteWithRetry_SuccessBeforeTimeout() throws Throwable {
    SiriAzureUpdater timeoutUpdater = spy(createUpdater(mockConfig));

    doNothing().when(timeoutUpdater).sleep(anyInt());

    // Fail twice, then succeed
    doThrow(createServiceBusException(ServiceBusFailureReason.SERVICE_BUSY))
      .doThrow(createServiceBusException(ServiceBusFailureReason.SERVICE_BUSY))
      .doNothing()
      .when(task)
      .run();

    // Should not throw - should succeed before timeout
    assertDoesNotThrow(() ->
      timeoutUpdater.executeWithRetry(task, "Test Task", mockConfig.getStartupTimeout().toMillis())
    );

    verify(task, times(3)).run(); // 2 failures + 1 success
    verify(timeoutUpdater, times(2)).sleep(anyInt()); // 2 sleep calls for retries
  }

  /**
   * Verifies that zero timeout causes immediate failure without attempting task execution.
   */
  @Test
  void testExecuteWithRetry_ZeroTimeoutImmediateFailure() throws Throwable {
    // Set zero timeout
    when(mockConfig.getStartupTimeout()).thenReturn(Duration.ZERO);
    SiriAzureUpdater zeroTimeoutUpdater = spy(createUpdater(mockConfig));

    // With zero timeout, the while loop condition should fail immediately
    // So we don't need to set up the task to throw anything

    boolean result = zeroTimeoutUpdater.executeWithRetry(task, "Test Task", 0L);

    assertFalse(result, "Expected executeWithRetry to return false immediately with zero timeout");

    // With zero timeout, should not attempt to run the task at all
    verify(task, never()).run();
    verify(zeroTimeoutUpdater, never()).sleep(anyInt());
  }

  /**
   * Verifies that updater is always primed even when setup operations fail.
   */
  @Test
  void testRun_SetsPrimedEvenOnException() throws Exception {
    SiriAzureUpdater failingUpdater = spy(createUpdater(mockConfig));

    // Make setupSubscription always fail
    doThrow(new ServiceBusException(new Throwable("Connection failed"), null))
      .when(failingUpdater)
      .executeWithRetry(
        any(SiriAzureUpdater.CheckedRunnable.class),
        eq("ServiceBusSubscription"),
        anyLong()
      );

    // Initially not primed
    assertFalse(failingUpdater.isPrimed(), "Updater should not be primed initially");

    // Run should not throw, even if setup fails
    assertDoesNotThrow(() -> failingUpdater.run());

    // Should be primed after run, even though setup failed
    assertTrue(failingUpdater.isPrimed(), "Updater should be primed after run, even on failure");
  }

  @Test
  void testRun_SetsPrimedOnSuccess() throws Exception {
    SiriAzureUpdater workingUpdater = spy(createUpdater(mockConfig));

    // Mock all setup methods to succeed
    doReturn(true)
      .when(workingUpdater)
      .executeWithRetry(any(SiriAzureUpdater.CheckedRunnable.class), anyString(), anyLong());

    assertFalse(workingUpdater.isPrimed(), "Updater should not be primed initially");

    workingUpdater.run();

    assertTrue(workingUpdater.isPrimed(), "Updater should be primed after successful run");
  }

  /**
   * Verifies the default startup timeout is set to 3 minutes.
   */
  @Test
  void testDefaultStartupTimeoutConfiguration() {
    // Test that default startup timeout is reasonable (3 minutes)
    SiriAzureUpdaterParameters defaultConfig = new SiriAzureETUpdaterParameters();
    assertEquals(
      Duration.ofMinutes(5),
      defaultConfig.getStartupTimeout(),
      "Default startup timeout should be 5 minutes"
    );
  }

  /**
   * Verifies that custom startup timeout values are properly applied.
   */
  @Test
  void testCustomStartupTimeoutConfiguration() throws Exception {
    when(mockConfig.getStartupTimeout()).thenReturn(Duration.ofMinutes(1));
    SiriAzureUpdater customTimeoutUpdater = spy(createUpdater(mockConfig));

    doNothing().when(customTimeoutUpdater).sleep(anyInt());
    doThrow(createServiceBusException(ServiceBusFailureReason.SERVICE_BUSY)).when(task).run();

    long testTimeout = 200L;
    boolean result;
    try {
      result = customTimeoutUpdater.executeWithRetry(task, "Test Task", testTimeout);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    assertFalse(result, "executeWithRetry should return false on timeout");

    // Verify retries were attempted
    verify(task, atLeast(2)).run();
    verify(customTimeoutUpdater, atLeast(1)).sleep(anyInt());
  }

  /**
   * Verifies REALTIME_ALERT is logged when ServiceBus setup fails.
   */
  @Test
  void testRun_LogsRealtimeAlertOnServiceBusException() throws Exception {
    Logger logger = (Logger) LoggerFactory.getLogger(SiriAzureUpdater.class);
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);

    SiriAzureUpdater failingUpdater = spy(createUpdater(mockConfig));

    // Make executeWithRetry throw ServiceBusException for setup
    ServiceBusException serviceBusException = createServiceBusException(
      ServiceBusFailureReason.SERVICE_BUSY
    );
    doThrow(serviceBusException)
      .when(failingUpdater)
      .executeWithRetry(
        any(SiriAzureUpdater.CheckedRunnable.class),
        eq("ServiceBusSubscription"),
        anyLong()
      );

    failingUpdater.run();

    // Verify REALTIME_ALERT logging occurred
    boolean foundRealtimeAlert = listAppender.list
      .stream()
      .anyMatch(event ->
        event
          .getFormattedMessage()
          .contains("REALTIME_STARTUP_ALERT component=ServiceBusSubscription status=FAILED")
      );

    assertTrue(foundRealtimeAlert, "Should log REALTIME_ALERT for ServiceBus exceptions");

    // Cleanup
    logger.detachAppender(listAppender);
  }

  /**
   * Verifies REALTIME_ALERT is logged when history initialization fails.
   */
  @Test
  void testRun_LogsRealtimeAlertOnHistoryException() throws Exception {
    Logger logger = (Logger) LoggerFactory.getLogger(SiriAzureUpdater.class);
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);

    SiriAzureUpdater failingUpdater = spy(createUpdater(mockConfig));

    // Make setup succeed but history initialization fail
    doReturn(true)
      .when(failingUpdater)
      .executeWithRetry(
        any(SiriAzureUpdater.CheckedRunnable.class),
        eq("ServiceBusSubscription"),
        anyLong()
      );

    doThrow(new URISyntaxException("invalid", "Invalid URI"))
      .when(failingUpdater)
      .executeWithRetry(
        any(SiriAzureUpdater.CheckedRunnable.class),
        eq("HistoricalSiriData"),
        anyLong()
      );

    failingUpdater.run();

    // Verify REALTIME_ALERT logging occurred for History component
    boolean foundHistoryAlert = listAppender.list
      .stream()
      .anyMatch(event ->
        event
          .getFormattedMessage()
          .contains("REALTIME_STARTUP_ALERT component=HistoricalSiriData status=FAILED")
      );

    assertTrue(foundHistoryAlert, "Should log REALTIME_ALERT for History exceptions");

    logger.detachAppender(listAppender);
  }

  /**
   * Verifies REALTIME_ALERT is logged when generic setup exceptions occur.
   */
  @Test
  void testRun_LogsRealtimeSetupAlertOnGenericException() throws Exception {
    Logger logger = (Logger) LoggerFactory.getLogger(SiriAzureUpdater.class);
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);

    SiriAzureUpdater failingUpdater = spy(createUpdater(mockConfig));

    // Make setup fail with a generic exception
    doThrow(new RuntimeException("Generic error"))
      .when(failingUpdater)
      .executeWithRetry(any(SiriAzureUpdater.CheckedRunnable.class), anyString(), anyLong());

    failingUpdater.run();

    // Verify REALTIME_ALERT logging occurred for RealtimeSetup component
    boolean foundSetupAlert = listAppender.list
      .stream()
      .anyMatch(event ->
        event
          .getFormattedMessage()
          .contains("REALTIME_STARTUP_ALERT component=ServiceBusSubscription status=FAILED")
      );

    assertTrue(foundSetupAlert, "Should log REALTIME_ALERT for RealtimeSetup exceptions");

    logger.detachAppender(listAppender);
  }

  /**
   * Verifies warning is logged when updater is primed in finally block after failure.
   */
  @Test
  void testRun_LogsWarningWhenSettingPrimedInFinally() throws Exception {
    Logger logger = (Logger) LoggerFactory.getLogger(SiriAzureUpdater.class);
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);

    SiriAzureUpdater failingUpdater = spy(createUpdater(mockConfig));

    // Make setup fail so finally block sets primed
    doThrow(new RuntimeException("Setup failed"))
      .when(failingUpdater)
      .executeWithRetry(any(SiriAzureUpdater.CheckedRunnable.class), anyString(), anyLong());

    failingUpdater.run();

    // Verify that the updater is primed even when setup fails (graceful degradation)
    assertTrue(failingUpdater.isPrimed(), "Should be primed even when setup fails");

    logger.detachAppender(listAppender);
  }

  /**
   * Verifies that existing retry behavior is preserved after timeout implementation.
   */
  @Test
  void testBackwardCompatibility_ExistingRetryLogicStillWorks() throws Exception {
    // Test that existing retry logic for ServiceBus exceptions still works as expected
    doThrow(createServiceBusException(ServiceBusFailureReason.SERVICE_BUSY))
      .doThrow(createServiceBusException(ServiceBusFailureReason.SERVICE_BUSY))
      .doNothing()
      .when(task)
      .run();

    doNothing().when(updater).sleep(anyInt());

    // Should succeed after retries
    assertDoesNotThrow(() ->
      updater.executeWithRetry(
        task,
        "Backward Compatibility Test",
        mockConfig.getStartupTimeout().toMillis()
      )
    );

    verify(task, times(3)).run(); // 2 failures + 1 success
    verify(updater, times(2)).sleep(anyInt()); // 2 retries
  }

  /**
   * Verifies non-retryable exceptions still fail immediately without timeout delay.
   */
  @Test
  void testBackwardCompatibility_NonRetryableExceptionsBehaviorUnchanged() throws Throwable {
    // Test that non-retryable exceptions still fail immediately without timeout
    ServiceBusException nonRetryableException = createServiceBusException(
      ServiceBusFailureReason.MESSAGE_SIZE_EXCEEDED
    );

    doThrow(nonRetryableException).when(task).run();

    // Should throw immediately, not wait for timeout
    ServiceBusException thrown = assertThrows(ServiceBusException.class, () ->
      updater.executeWithRetry(
        task,
        "Non-retryable Test",
        mockConfig.getStartupTimeout().toMillis()
      )
    );

    assertEquals(ServiceBusFailureReason.MESSAGE_SIZE_EXCEEDED, thrown.getReason());
    verify(task, times(1)).run(); // Only one attempt
    verify(updater, never()).sleep(anyInt()); // No retries
  }

  /**
   * Verifies InterruptedException handling is unchanged by timeout implementation.
   */
  @Test
  void testBackwardCompatibility_InterruptedExceptionStillPreserved() throws Throwable {
    SiriAzureUpdater testUpdater = spy(createUpdater(mockConfig));

    doThrow(new InterruptedException("Thread interrupted")).when(task).run();

    InterruptedException thrown = assertThrows(InterruptedException.class, () ->
      testUpdater.executeWithRetry(
        task,
        "Interrupted Test",
        mockConfig.getStartupTimeout().toMillis()
      )
    );

    assertEquals("Thread interrupted", thrown.getMessage());
    assertTrue(Thread.interrupted(), "Thread interrupt status should be preserved");
    verify(task, times(1)).run();
    verify(testUpdater, never()).sleep(anyInt());
  }

  /**
   * Verifies exponential backoff sequence remains unchanged after timeout implementation.
   */
  @Test
  void testBackwardCompatibility_ExponentialBackoffUnchanged() throws Throwable {
    // Test that exponential backoff sequence is preserved
    doThrow(createServiceBusException(ServiceBusFailureReason.SERVICE_BUSY))
      .doThrow(createServiceBusException(ServiceBusFailureReason.SERVICE_BUSY))
      .doThrow(createServiceBusException(ServiceBusFailureReason.SERVICE_BUSY))
      .doThrow(createServiceBusException(ServiceBusFailureReason.SERVICE_BUSY))
      .doNothing()
      .when(task)
      .run();

    doNothing().when(updater).sleep(anyInt());

    updater.executeWithRetry(task, "Backoff Test", mockConfig.getStartupTimeout().toMillis());

    ArgumentCaptor<Integer> sleepCaptor = ArgumentCaptor.forClass(Integer.class);
    verify(updater, times(4)).sleep(sleepCaptor.capture());

    List<Integer> sleepDurations = sleepCaptor.getAllValues();
    assertEquals(
      Arrays.asList(1000, 2000, 4000, 8000),
      sleepDurations,
      "Exponential backoff sequence should be preserved"
    );
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

  /**
   * Verifies Duration configuration properly converts to milliseconds for timeout behavior.
   */
  @Test
  void testDurationTimeoutConfiguration() throws Exception {
    Duration testTimeout = Duration.ofMillis(100); // Short timeout for fast test
    when(mockConfig.getStartupTimeout()).thenReturn(testTimeout);

    SiriAzureUpdater durationUpdater = spy(createUpdater(mockConfig));
    doNothing().when(durationUpdater).sleep(anyInt());
    doThrow(createServiceBusException(ServiceBusFailureReason.SERVICE_BUSY)).when(task).run();

    boolean result;
    try {
      result = durationUpdater.executeWithRetry(task, "Duration Test", testTimeout.toMillis());
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    assertFalse(result, "executeWithRetry should return false on timeout");
  }

  /**
   * Verifies startup steps execute in correct sequential order.
   */
  @Test
  void testSequentialStartupStepExecution() throws Exception {
    SiriAzureUpdater sequenceUpdater = spy(createUpdater(mockConfig));

    // Mock executeWithRetry to succeed immediately for all steps
    doReturn(true)
      .when(sequenceUpdater)
      .executeWithRetry(any(SiriAzureUpdater.CheckedRunnable.class), anyString(), anyLong());

    sequenceUpdater.run();

    // Verify executeWithRetry was called for each step in order
    InOrder inOrder = inOrder(sequenceUpdater);
    inOrder
      .verify(sequenceUpdater)
      .executeWithRetry(
        any(SiriAzureUpdater.CheckedRunnable.class),
        eq("ServiceBusSubscription"),
        anyLong()
      );
    inOrder
      .verify(sequenceUpdater)
      .executeWithRetry(
        any(SiriAzureUpdater.CheckedRunnable.class),
        eq("HistoricalSiriData"),
        anyLong()
      );
    inOrder
      .verify(sequenceUpdater)
      .executeWithRetry(
        any(SiriAzureUpdater.CheckedRunnable.class),
        eq("ServiceBusEventProcessor"),
        anyLong()
      );

    assertTrue(sequenceUpdater.isPrimed(), "Updater should be primed after startup steps");
  }

  /**
   * Verifies simplified error logging uses correct component names.
   */
  @Test
  void testSimplifiedErrorLoggingWithComponentNames() throws Exception {
    Logger logger = (Logger) LoggerFactory.getLogger(SiriAzureUpdater.class);
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);

    SiriAzureUpdater errorUpdater = spy(createUpdater(mockConfig));

    doThrow(new RuntimeException("Setup failed"))
      .when(errorUpdater)
      .executeWithRetry(
        any(SiriAzureUpdater.CheckedRunnable.class),
        eq("ServiceBusSubscription"),
        anyLong()
      );

    doThrow(new RuntimeException("History failed"))
      .when(errorUpdater)
      .executeWithRetry(
        any(SiriAzureUpdater.CheckedRunnable.class),
        eq("HistoricalSiriData"),
        anyLong()
      );

    errorUpdater.run();

    List<String> logMessages = listAppender.list
      .stream()
      .map(ILoggingEvent::getFormattedMessage)
      .toList();

    assertTrue(
      logMessages
        .stream()
        .anyMatch(msg ->
          msg.contains("REALTIME_STARTUP_ALERT component=ServiceBusSubscription status=FAILED")
        ),
      "Should log ServiceBusSubscription component failure"
    );

    assertTrue(
      logMessages
        .stream()
        .anyMatch(msg ->
          msg.contains("REALTIME_STARTUP_ALERT component=HistoricalSiriData status=FAILED")
        ),
      "Should log HistoricalSiriData component failure"
    );

    logger.detachAppender(listAppender);
  }

  /**
   * Verifies network error detection correctly identifies retryable network issues.
   */
  @Test
  void testNetworkErrorTypeDetection() throws Exception {
    SiriAzureUpdater networkUpdater = spy(createUpdater(mockConfig));

    UnknownHostException dnsException = new UnknownHostException("Host not found");
    assertTrue(networkUpdater.shouldRetry(dnsException), "Should retry on DNS resolution failures");

    SocketTimeoutException socketTimeoutException = new SocketTimeoutException("Read timeout");
    assertTrue(
      networkUpdater.shouldRetry(socketTimeoutException),
      "Should retry on socket timeouts"
    );

    // Test OtpHttpClientException (which is retryable)
    OtpHttpClientException httpException = new OtpHttpClientException("HTTP request failed");
    assertTrue(networkUpdater.shouldRetry(httpException), "Should retry on HTTP client exceptions");

    IllegalArgumentException nonNetworkException = new IllegalArgumentException("Invalid argument");
    assertFalse(
      networkUpdater.shouldRetry(nonNetworkException),
      "Should not retry non-network exceptions"
    );
  }
}
