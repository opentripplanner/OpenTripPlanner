package org.opentripplanner.generate.doc.framework;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.util.OTPFeature;
import org.opentripplanner.util.lang.TableFormatter;

public class OTPFeatureTable {

  private static final String NEW_LINE = "\n";

  public static String otpFeaturesTable() {
    List<List<?>> list = new ArrayList<>();
    list.add(List.of("Feature", "Description", "Enabled by default", "Sandbox"));

    for (var it : OTPFeature.values()) {
      list.add(
        List.of(DocFormatter.code(it.name()), it.doc(), yesNo(it.isOn()), yesNo(it.isSandbox()))
      );
    }
    var rows = TableFormatter.asMarkdownTable(list);
    return String.join(NEW_LINE, rows) + NEW_LINE;
  }

  @Test
  void test() {
    var table = otpFeaturesTable();
    for (OTPFeature it : OTPFeature.values()) {
      assertTrue(table.contains(it.name()), table);
    }
  }

  private static String yesNo(boolean yes) {
    return yes ? "yes" : "no";
  }
}
