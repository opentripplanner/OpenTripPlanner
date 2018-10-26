package org.opentripplanner.common.diff;

import java.util.List;
import java.util.stream.Collectors;

public class DiffPrinter {
    public String diffListToString(List<Difference> differences) {
        return differences.stream().map(difference -> difference.toString()).collect(Collectors.joining("\n", "\n", "\n"));
    }
}
