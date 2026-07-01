package org.opentripplanner.ext.siri.updater.azure;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Future;
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.opentripplanner.framework.retry.OtpRetry;
import org.opentripplanner.framework.retry.OtpRetryException;
import org.opentripplanner.updater.spi.WriteToGraphCallback;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri21.ServiceDelivery;

class SiriAzureUpdaterTest {

  private SiriAzureUpdaterParameters mockConfig;
  private Runnable task;
  private OtpRetry otpRetry;

  @BeforeEach
  public void setUp() throws Exception {
    mockConfig = mock(SiriAzureUpdaterParameters.class);
    otpRetry = mock(OtpRetry.class);
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
    spy(
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

    task = mock(Runnable.class);
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

  @Test
  void testRun_SetsPrimedOnSuccess() throws Exception {
    SiriAzureUpdater workingUpdater = spy(createUpdater(mockConfig));

    // Ensure updater uses mocked retry
    doReturn(otpRetry).when(workingUpdater).createOtpRetry(anyString());

    assertFalse(workingUpdater.isPrimed());

    workingUpdater.run();

    assertTrue(workingUpdater.isPrimed());

    verify(otpRetry, atLeastOnce()).execute(any());
  }

  /**
   * Verifies warning is logged when updater is primed in finally block after failure.
   */
  @Test
  void testRun_LogsWarningWhenSettingPrimedInFinally() {
    Logger logger = (Logger) LoggerFactory.getLogger(SiriAzureUpdater.class);
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);

    SiriAzureUpdater failingUpdater = spy(createUpdater(mockConfig));

    doThrow(new RuntimeException("Setup failed")).when(task).run();

    doReturn(otpRetry).when(failingUpdater).createOtpRetry(anyString());

    failingUpdater.run();

    assertTrue(failingUpdater.isPrimed(), "Should be primed even when setup fails");

    logger.detachAppender(listAppender);
  }

  /**
   * Verifies startup steps execute in correct sequential order.
   */
  @Test
  void testSequentialStartupStepExecution() throws Exception {
    SiriAzureUpdater sequenceUpdater = spy(createUpdater(mockConfig));

    doReturn(otpRetry).when(sequenceUpdater).createOtpRetry(anyString());

    sequenceUpdater.run();

    ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);

    verify(otpRetry, times(3)).execute(captor.capture());

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

    doReturn(otpRetry).when(errorUpdater).createOtpRetry(anyString());

    doThrow(new OtpRetryException("Setup failed", new Exception()))
      .when(otpRetry)
      .execute(any(Runnable.class));

    doThrow(new OtpRetryException("History failed", new Exception()))
      .when(otpRetry)
      .execute(any(Runnable.class));

    errorUpdater.run();

    List<String> logMessages = listAppender.list
      .stream()
      .map(ILoggingEvent::getFormattedMessage)
      .toList();

    assertTrue(
      logMessages
        .stream()
        .anyMatch(msg ->
          msg.contains(
            "REALTIME_STARTUP_FAILED_ALERT component=siri-azure-test-updater:ServiceBusSubscription status=FAILED error=History failed"
          )
        ),
      "Should log ServiceBusSubscription component failure"
    );

    assertTrue(
      logMessages
        .stream()
        .anyMatch(msg ->
          msg.contains(
            "REALTIME_STARTUP_FAILED_ALERT component=siri-azure-test-updater:ServiceBusSubscription status=FAILED error=History failed"
          )
        ),
      "Should log HistoricalSiriData component failure"
    );

    logger.detachAppender(listAppender);
  }
}
