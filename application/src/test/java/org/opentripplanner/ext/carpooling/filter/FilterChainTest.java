package org.opentripplanner.ext.carpooling.filter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.opentripplanner.ext.carpooling.TestCarpoolTripBuilder.*;
import static org.opentripplanner.ext.carpooling.TestFixtures.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class FilterChainTest {

  @Test
  void accepts_allFiltersAccept_returnsTrue() {
    var filter1 = mock(TripFilter.class);
    var filter2 = mock(TripFilter.class);
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    when(filter1.accepts(any(), any(), any())).thenReturn(true);
    when(filter2.accepts(any(), any(), any())).thenReturn(true);

    var chain = new FilterChain(List.of(filter1, filter2));

    assertTrue(chain.accepts(trip, OSLO_EAST, OSLO_WEST));
    verify(filter1).accepts(trip, OSLO_EAST, OSLO_WEST);
    verify(filter2).accepts(trip, OSLO_EAST, OSLO_WEST);
  }

  @Test
  void accepts_oneFilterRejects_returnsFalse() {
    var filter1 = mock(TripFilter.class);
    var filter2 = mock(TripFilter.class);
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    when(filter1.accepts(any(), any(), any())).thenReturn(true);
    when(filter2.accepts(any(), any(), any())).thenReturn(false); // Rejects

    var chain = new FilterChain(List.of(filter1, filter2));

    assertFalse(chain.accepts(trip, OSLO_EAST, OSLO_WEST));
  }

  @Test
  void accepts_shortCircuits_afterFirstRejection() {
    var filter1 = mock(TripFilter.class);
    var filter2 = mock(TripFilter.class);
    var filter3 = mock(TripFilter.class);
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    when(filter1.accepts(any(), any(), any())).thenReturn(true);
    when(filter2.accepts(any(), any(), any())).thenReturn(false); // Rejects
    // filter3 should not be called

    var chain = new FilterChain(List.of(filter1, filter2, filter3));
    chain.accepts(trip, OSLO_EAST, OSLO_WEST);

    verify(filter1).accepts(any(), any(), any());
    verify(filter2).accepts(any(), any(), any());
    verify(filter3, never()).accepts(any(), any(), any()); // Not called
  }

  @Test
  void accepts_firstFilterRejects_doesNotCallOthers() {
    var filter1 = mock(TripFilter.class);
    var filter2 = mock(TripFilter.class);
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    when(filter1.accepts(any(), any(), any())).thenReturn(false); // First rejects

    var chain = new FilterChain(List.of(filter1, filter2));
    chain.accepts(trip, OSLO_EAST, OSLO_WEST);

    verify(filter1).accepts(any(), any(), any());
    verify(filter2, never()).accepts(any(), any(), any());
  }

  @Test
  void standard_includesAllStandardFilters() {
    var chain = FilterChain.standard();

    // Should contain CapacityFilter and DirectionalCompatibilityFilter
    // Verify by testing behavior with a trip that has no capacity
    var emptyTrip = createTripWithCapacity(0, OSLO_CENTER, List.of(), OSLO_NORTH);

    // Should reject due to capacity filter
    assertFalse(chain.accepts(emptyTrip, OSLO_EAST, OSLO_WEST));
  }

  @Test
  void standard_checksDirectionalCompatibility() {
    var chain = FilterChain.standard();

    // Trip going north, passenger going south
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    // Should reject due to directional filter
    assertFalse(chain.accepts(trip, OSLO_EAST, OSLO_CENTER));
  }

  @Test
  void emptyChain_acceptsAll() {
    var chain = new FilterChain(List.of());
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    // Empty chain accepts everything
    assertTrue(chain.accepts(trip, OSLO_EAST, OSLO_WEST));
  }

  @Test
  void singleFilter_behavesCorrectly() {
    var filter = mock(TripFilter.class);
    when(filter.accepts(any(), any(), any())).thenReturn(true);

    var chain = new FilterChain(List.of(filter));
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    assertTrue(chain.accepts(trip, OSLO_EAST, OSLO_WEST));
    verify(filter).accepts(trip, OSLO_EAST, OSLO_WEST);
  }
}
