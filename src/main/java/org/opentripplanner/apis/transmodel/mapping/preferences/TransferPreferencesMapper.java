package org.opentripplanner.apis.transmodel.mapping.preferences;

import java.time.Duration;
import java.util.function.Consumer;
import org.opentripplanner.apis.transmodel.support.DataFetcherDecorator;
import org.opentripplanner.routing.api.request.preference.TransferPreferences;

public class TransferPreferencesMapper {

  public static void mapTransferPreferences(
    TransferPreferences.Builder transfer,
    DataFetcherDecorator callWith
  ) {
    callWith.argument("transferPenalty", transfer::withCost);

    // 'minimumTransferTime' is deprecated, that's why we are mapping 'slack' twice.
    callWith.argument("minimumTransferTime", setSlackFromSeconds(transfer));
    callWith.argument("transferSlack", setSlackFromSeconds(transfer));

    callWith.argument("waitReluctance", transfer::withWaitReluctance);
    callWith.argument("maximumTransfers", transfer::withMaxTransfers);
    callWith.argument("maximumAdditionalTransfers", transfer::withMaxAdditionalTransfers);
  }

  private static Consumer<Integer> setSlackFromSeconds(TransferPreferences.Builder transfer) {
    return (Integer secs) -> transfer.withSlack(Duration.ofSeconds(secs));
  }
}
