package org.opentripplanner.transit.raptor.rangeraptor.transit;

import org.junit.Test;
import org.opentripplanner.transit.raptor._data.RaptorTestConstants;
import org.opentripplanner.transit.raptor._data.transit.TestTripPattern;
import org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider;
import org.opentripplanner.transit.raptor.rangeraptor.SlackProvider;
import org.opentripplanner.transit.raptor.rangeraptor.workerlifecycle.LifeCycleEventPublisher;
import org.opentripplanner.transit.raptor.rangeraptor.workerlifecycle.LifeCycleSubscriptions;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider.defaultSlackProvider;

public class SlackProviderAdapterTest implements RaptorTestConstants {

  public static final int BOARD_SLACK = D20s;
  public static final int ALIGHT_SLACK = D10s;
  public static final int TRANSFER_SLACK = D1m;
  public static final RaptorSlackProvider EXTERNAL_SLACK_PROVIDER = defaultSlackProvider(
      TRANSFER_SLACK,
      BOARD_SLACK,
      ALIGHT_SLACK
  );
  public static final TestTripPattern PATTERN = TestTripPattern.pattern("R1");

  @Test
  public void forwardSlackProvider() {
    LifeCycleSubscriptions subscriptions = new LifeCycleSubscriptions();
    SlackProvider subject = SlackProviderAdapter.forwardSlackProvider(
        EXTERNAL_SLACK_PROVIDER,
        subscriptions
    );
    var lifeCycle = new LifeCycleEventPublisher(subscriptions);

    lifeCycle.prepareForNextRound(0);
    subject.setCurrentPattern(PATTERN);
    assertEquals(ALIGHT_SLACK, subject.alightSlack());
    assertEquals(BOARD_SLACK, subject.boardSlack());

    lifeCycle.prepareForNextRound(1);
    subject.setCurrentPattern(PATTERN);
    assertEquals(ALIGHT_SLACK, subject.alightSlack());
    assertEquals(BOARD_SLACK, subject.boardSlack());

    lifeCycle.prepareForNextRound(2);
    subject.setCurrentPattern(PATTERN);
    assertEquals(ALIGHT_SLACK, subject.alightSlack());
    assertEquals(BOARD_SLACK + TRANSFER_SLACK, subject.boardSlack());
  }

  @Test
  public void reverseSlackProvider() {
    LifeCycleSubscriptions subscriptions = new LifeCycleSubscriptions();
    SlackProvider subject = SlackProviderAdapter.reverseSlackProvider(
        EXTERNAL_SLACK_PROVIDER,
        subscriptions
    );
    var lifeCycle = new LifeCycleEventPublisher(subscriptions);

    lifeCycle.prepareForNextRound(0);
    subject.setCurrentPattern(PATTERN);
    assertEquals(BOARD_SLACK, subject.alightSlack());
    assertEquals(ALIGHT_SLACK, subject.boardSlack());

    lifeCycle.prepareForNextRound(1);
    subject.setCurrentPattern(PATTERN);
    assertEquals(BOARD_SLACK, subject.alightSlack());
    assertEquals(ALIGHT_SLACK, subject.boardSlack());

    lifeCycle.prepareForNextRound(2);
    subject.setCurrentPattern(PATTERN);
    assertEquals(BOARD_SLACK, subject.alightSlack());
    assertEquals(ALIGHT_SLACK + TRANSFER_SLACK, subject.boardSlack());
  }
}