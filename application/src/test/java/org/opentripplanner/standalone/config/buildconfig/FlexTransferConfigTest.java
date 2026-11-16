package org.opentripplanner.standalone.config.buildconfig;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.newNodeAdapterForTest;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.transfer.dataimport.api.FlexTransferParameters;

class FlexTransferConfigTest {

  @Test
  void flexTransfersHaveDefaultsWithoutConfiguration() {
    var flex = FlexTransferConfig.map(transfers("{}"));

    assertThat(flex.maxDuration()).isEqualTo(FlexTransferParameters.DEFAULT.maxDuration());
    assertThat(flex.requests()).containsExactly(RouteRequest.defaultValue());
  }

  @Test
  void mapsTheMaxDurationAndTheRequests() {
    var flex = FlexTransferConfig.map(
      transfers(
        """
        { transfers: { flex: {
          maxDuration: '12m',
          requests: [{ modes: 'WALK' }, { modes: 'WALK', wheelchairAccessibility: { enabled: true } }]
        } } }
        """
      )
    );

    assertThat(flex.maxDuration()).isEqualTo(Duration.ofMinutes(12));
    assertThat(flex.requests()).hasSize(2);
  }

  @Test
  void viaTransfersHaveADefaultMaxDuration() {
    assertThat(ViaTransferConfig.mapMaxDuration(transfers("{}"))).isEqualTo(Duration.ofMinutes(30));
  }

  @Test
  void mapsTheMaxDurationOfTheViaTransfers() {
    var transfers = transfers("{ transfers: { via: { maxDuration: '5m' } } }");

    assertThat(ViaTransferConfig.mapMaxDuration(transfers)).isEqualTo(Duration.ofMinutes(5));
  }

  @Test
  void theOldRootKeysAreReportedAsUnknownParameters() {
    var root = newNodeAdapterForTest("{ maxTransferDuration: '5m', transferRequests: [] }");
    TransferProfilesConfigMapper.map(root);

    assertThat(root.hasUnknownParameters()).isTrue();
  }

  private static NodeAdapter transfers(String json) {
    return TransferProfilesConfigMapper.transfersNode(newNodeAdapterForTest(json));
  }
}
