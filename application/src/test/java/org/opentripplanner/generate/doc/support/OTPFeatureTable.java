package org.opentripplanner.generate.doc.support;

import static org.opentripplanner.utils.text.MarkdownFormatter.checkMark;
import static org.opentripplanner.utils.text.Table.Align.Center;
import static org.opentripplanner.utils.text.Table.Align.Left;

import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.utils.text.MarkdownFormatter;
import org.opentripplanner.utils.text.Table;

@SuppressWarnings("NewClassNamingConvention")
public class OTPFeatureTable {

  private static final String NEW_LINE = "\n";

  public static String otpFeaturesTable() {
    var table = Table.of()
      .withHeaders("Feature", "Description", "Enabled by default", "Sandbox")
      .withAlights(Left, Left, Center, Center);

    for (var it : OTPFeature.values()) {
      table.addRow(
        MarkdownFormatter.code(it.name()),
        it.doc(),
        checkMark(it.isEnabledByDefault()),
        checkMark(it.isSandbox())
      );
    }
    return String.join(NEW_LINE, table.build().toMarkdownRows()) + NEW_LINE;
  }
}
