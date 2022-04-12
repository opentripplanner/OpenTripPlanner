package org.opentripplanner.transit.raptor.rangeraptor.transit;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider.defaultSlackProvider;

import org.junit.Test;
import org.opentripplanner.transit.raptor._data.RaptorTestConstants;
import org.opentripplanner.transit.raptor._data.transit.TestTripPattern;
import org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider;
import org.opentripplanner.transit.raptor.rangeraptor.SlackProvider;
import org.opentripplanner.transit.raptor.rangeraptor.workerlifecycle.LifeCycleEventPublisher;
import org.opentripplanner.transit.raptor.rangeraptor.workerlifecycle.LifeCycleSubscriptions;

public class SlackProviderAdapterTest implements RaptorTestConstants {

  private static final int BOARD_SLACK = D20s;
  private static final int ALIGHT_SLACK = D10s;
  private static final int TRANSFER_SLACK = D1m;
  private static final RaptorSlackProvider EXTERNAL_SLACK_PROVIDER = defaultSlackProvider(
    TRANSFER_SLACK,
    BOARD_SLACK,
    ALIGHT_SLACK
  );
  private static final int ANY_SLACK_INDEX = 0;

  @Test
  public void forwardSlackProvider() {
    LifeCycleSubscriptions subscriptions = new LifeCycleSubscriptions();
    SlackProvider subject = SlackProviderAdapter.forwardSlackProvider(
      EXTERNAL_SLACK_PROVIDER,
      subscriptions
    );
    var lifeCycle = new LifeCycleEventPublisher(subscriptions);

    lifeCycle.prepareForNextRound(0);
    assertEquals(ALIGHT_SLACK, subject.alightSlack(ANY_SLACK_INDEX));
    assertEquals(BOARD_SLACK, subject.boardSlack(ANY_SLACK_INDEX));

    lifeCycle.prepareForNextRound(1);
    assertEquals(ALIGHT_SLACK, subject.alightSlack(ANY_SLACK_INDEX));
    assertEquals(BOARD_SLACK, subject.boardSlack(ANY_SLACK_INDEX));

    lifeCycle.prepareForNextRound(2);
    assertEquals(ALIGHT_SLACK, subject.alightSlack(ANY_SLACK_INDEX));
    assertEquals(BOARD_SLACK + TRANSFER_SLACK, subject.boardSlack(ANY_SLACK_INDEX));
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
    assertEquals(BOARD_SLACK, subject.alightSlack(ANY_SLACK_INDEX));
    assertEquals(ALIGHT_SLACK, subject.boardSlack(ANY_SLACK_INDEX));

    lifeCycle.prepareForNextRound(1);
    assertEquals(BOARD_SLACK, subject.alightSlack(ANY_SLACK_INDEX));
    assertEquals(ALIGHT_SLACK, subject.boardSlack(ANY_SLACK_INDEX));

    lifeCycle.prepareForNextRound(2);
    assertEquals(BOARD_SLACK, subject.alightSlack(ANY_SLACK_INDEX));
    assertEquals(ALIGHT_SLACK + TRANSFER_SLACK, subject.boardSlack(ANY_SLACK_INDEX));
  }
}
