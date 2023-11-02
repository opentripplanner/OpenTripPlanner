package org.opentripplanner.ext.transmodelapi.mapping.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.transmodelapi._support.TestDataFetcherDecorator;
import org.opentripplanner.routing.api.request.preference.WalkPreferences;

class WalkPreferencesMapperTest {

  @Test
  void mapWalkReluctance() {
    var subject = WalkPreferences.of();
    WalkPreferencesMapper.mapWalkPreferences(
      subject,
      new TestDataFetcherDecorator(Map.of("walkReluctance", 7.5))
    );
    assertEquals("WalkPreferences{reluctance: 7.5}", subject.build().toString());
  }

  @Test
  void mapWalkBoardCost() {
    var subject = WalkPreferences.of();
    WalkPreferencesMapper.mapWalkPreferences(
      subject,
      new TestDataFetcherDecorator(Map.of("walkBoardCost", 9000))
    );
    assertEquals("WalkPreferences{boardCost: $9000}", subject.build().toString());
  }

  @Test
  void mapWalkSpeed() {
    var subject = WalkPreferences.of();
    WalkPreferencesMapper.mapWalkPreferences(
      subject,
      new TestDataFetcherDecorator(Map.of("walkSpeed", 2.7))
    );
    assertEquals("WalkPreferences{speed: 2.7}", subject.build().toString());
  }
}
