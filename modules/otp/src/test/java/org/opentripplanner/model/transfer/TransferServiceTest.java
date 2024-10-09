package org.opentripplanner.model.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.transfer.TransferTestData.ANY_POS;
import static org.opentripplanner.model.transfer.TransferTestData.ANY_TRIP;
import static org.opentripplanner.model.transfer.TransferTestData.POS_1;
import static org.opentripplanner.model.transfer.TransferTestData.POS_3;
import static org.opentripplanner.model.transfer.TransferTestData.ROUTE_POINT_1A;
import static org.opentripplanner.model.transfer.TransferTestData.ROUTE_POINT_1S;
import static org.opentripplanner.model.transfer.TransferTestData.ROUTE_POINT_2B;
import static org.opentripplanner.model.transfer.TransferTestData.ROUTE_POINT_2S;
import static org.opentripplanner.model.transfer.TransferTestData.STATION_POINT;
import static org.opentripplanner.model.transfer.TransferTestData.STOP_A;
import static org.opentripplanner.model.transfer.TransferTestData.STOP_B;
import static org.opentripplanner.model.transfer.TransferTestData.STOP_POINT_A;
import static org.opentripplanner.model.transfer.TransferTestData.STOP_POINT_B;
import static org.opentripplanner.model.transfer.TransferTestData.STOP_S;
import static org.opentripplanner.model.transfer.TransferTestData.TRIP_11;
import static org.opentripplanner.model.transfer.TransferTestData.TRIP_12;
import static org.opentripplanner.model.transfer.TransferTestData.TRIP_21;
import static org.opentripplanner.model.transfer.TransferTestData.TRIP_POINT_11_1;
import static org.opentripplanner.model.transfer.TransferTestData.TRIP_POINT_21_3;

import java.util.List;
import org.junit.jupiter.api.Test;

public class TransferServiceTest {

  private final DefaultTransferService subject = new DefaultTransferService();

  @Test
  public void findTransfer() {
    // Given:                                                               // Ranking
    var A = transfer(TRIP_POINT_11_1, TRIP_POINT_21_3); // 84
    var B = transfer(TRIP_POINT_11_1, ROUTE_POINT_2B); // 74
    var C = transfer(TRIP_POINT_11_1, ROUTE_POINT_2S); // 64
    var D = transfer(STOP_POINT_A, TRIP_POINT_21_3); // 51
    var E = transfer(ROUTE_POINT_1A, STOP_POINT_B); // 43
    var F = transfer(ROUTE_POINT_1S, STOP_POINT_B); // 32
    var G = transfer(STATION_POINT, ROUTE_POINT_2B); // 30
    var H = transfer(STATION_POINT, ROUTE_POINT_2S); // 20
    var I = transfer(STATION_POINT, STATION_POINT); // 11

    // When: all transfers is added to service
    subject.addAll(List.of(A, B, C, D, E, F, G, H, I));

    // Then:

    // Find the most specific transfer TRIP to TRIP
    // Trip and stop position must match, stops are ignored
    assertEquals(A, subject.findTransfer(TRIP_11, POS_1, STOP_A, TRIP_21, POS_3, STOP_B));

    // Find the TRIP to ROUTE+STOP transfer when TO stop position does not match
    assertEquals(B, subject.findTransfer(TRIP_11, POS_1, STOP_A, TRIP_21, ANY_POS, STOP_B));

    // Find the TRIP to ROUTE+STATION transfer when TO stop does not match
    assertEquals(C, subject.findTransfer(TRIP_11, POS_1, STOP_A, TRIP_21, ANY_POS, STOP_S));

    // Find transfer: STOP to TRIP when FROM trip does not match
    assertEquals(D, subject.findTransfer(TRIP_12, POS_1, STOP_A, TRIP_21, POS_3, STOP_B));

    // Find the ROUTE+STOP to STOP transfer when FROM trip and TO stopPos do not match
    assertEquals(E, subject.findTransfer(TRIP_12, POS_1, STOP_A, TRIP_21, ANY_POS, STOP_B));

    // Find the ROUTE+STATION to STOP transfer when FROM stop position does not match
    assertEquals(F, subject.findTransfer(TRIP_11, ANY_POS, STOP_S, TRIP_21, POS_3, STOP_B));

    // Find STOP to STOP transfer, when FROM trip and TO stop position do not match
    assertEquals(G, subject.findTransfer(ANY_TRIP, POS_1, STOP_A, TRIP_21, ANY_POS, STOP_B));

    // Find STATION to ROUTE+STATION when FROM trip and route and TO Stop do not match
    assertEquals(H, subject.findTransfer(ANY_TRIP, ANY_POS, STOP_S, TRIP_21, POS_3, STOP_S));

    // Find STATION to STATION when there are no match for FROM/TO trips and patterns
    assertEquals(I, subject.findTransfer(ANY_TRIP, POS_1, STOP_S, ANY_TRIP, ANY_POS, STOP_S));
  }

  @Test
  public void addSameTransferTwiceRetrieveFirstAdded() {
    var A = transfer(STOP_POINT_A, STOP_POINT_B);
    var A_EQ = transfer(STOP_POINT_A, STOP_POINT_B);

    // Adding two transfers between the same stops
    // should result in only the first being added
    subject.addAll(List.of(A, A_EQ));

    assertEquals(List.of(A), subject.listAll());
  }

  @Test
  public void listAll() {
    // Given:
    var A = transfer(STATION_POINT, ROUTE_POINT_1A);
    var B = transfer(STOP_POINT_A, STOP_POINT_B);
    var C = transfer(STOP_POINT_A, TRIP_POINT_21_3);

    // When: all transfers is added to service
    subject.addAll(List.of(A, B, C));

    // Then
    assertEquals(List.of(A, B, C), subject.listAll());
  }

  ConstrainedTransfer transfer(TransferPoint from, TransferPoint to) {
    var c = TransferConstraint.of().build();
    return new ConstrainedTransfer(null, from, to, c);
  }
}
