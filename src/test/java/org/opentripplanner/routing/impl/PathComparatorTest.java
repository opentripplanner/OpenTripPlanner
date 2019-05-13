package org.opentripplanner.routing.impl;

import org.junit.Test;
import org.opentripplanner.routing.spt.GraphPath;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PathComparatorTest {

    GraphPath a = mockGraphPath(5, 7);

    GraphPath b = mockGraphPath(0, 8);

    GraphPath c = mockGraphPath(9, 12);

    private List<GraphPath> paths = Arrays.asList(a, b, c);

    @Test
    public void testPathComparator() {
        paths.sort(new PathComparator(false));
        assertEquals(paths, Arrays.asList(a, b, c));
    }

    @Test
    public void testPathComparatorArriveBy() {
        paths.sort(new PathComparator(true));
        assertEquals(paths, Arrays.asList(c, a, b));
    }

    @Test
    public void testDurationComparator() {
        paths.sort(new DurationComparator());
        assertEquals(paths, Arrays.asList(a, c, b));
    }


    private GraphPath mockGraphPath(long startTime, long endTime) {
        GraphPath path = mock(GraphPath.class);
        when(path.getStartTime()).thenReturn(startTime);
        when(path.getEndTime()).thenReturn(endTime);
        when(path.getDuration()).thenReturn((int) (endTime - startTime));
        return path;
    }
}
