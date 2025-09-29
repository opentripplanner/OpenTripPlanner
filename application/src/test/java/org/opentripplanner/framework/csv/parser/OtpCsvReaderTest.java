package org.opentripplanner.framework.csv.parser;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csvreader.CsvReader;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.datastore.configure.DataStoreModule;
import org.opentripplanner.framework.csv.HeadersDoNotMatch;
import org.opentripplanner.framework.csv.OtpCsvReader;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.utils.lang.IntRange;

class OtpCsvReaderTest {

  @Test
  void read() throws HeadersDoNotMatch {
    var ds = DataStoreModule.dataSource(
      "OtpCsvReaderTest",
      FileType.GTFS,
      """
      a,b,c
      1, "Cat", 1
      2, "Boot", 0
      """
    );
    var expected = List.of(new AType(1, "Cat", true), new AType(2, "Boot", false));

    var list = new ArrayList<AType>();

    // Without logger
    OtpCsvReader.<AType>of()
      .withProgressLogger(null)
      .withDataSource(ds)
      .withParserFactory(Parser::new)
      .withRowHandler(row -> list.add(row))
      .read();

    assertEquals(expected, list);

    // With logger
    var log = new StringBuilder();
    list.clear();
    OtpCsvReader.<AType>of()
      .withProgressLogger(m -> log.append(m).append('\n'))
      .withDataSource(ds)
      .withParserFactory(Parser::new)
      .withRowHandler(row -> list.add(row))
      .read();
    assertEquals(expected, list);

    assertThat(log.toString()).contains("Read OtpCsvReaderTest progress tracking started.");
    assertThat(log.toString()).contains("Read OtpCsvReaderTest progress tracking complete.");
  }

  static class Parser extends AbstractCsvParser<AType> {

    public Parser(CsvReader reader) {
      super(DataImportIssueStore.NOOP, reader, "issueType");
    }

    @Override
    public List<String> headers() {
      return List.of("a", "b", "c");
    }

    @Nullable
    @Override
    protected AType createNextRow() throws HandledCsvParseException {
      return new AType(
        getInt("a", IntRange.ofInclusive(0, 10)),
        getString("b"),
        getGtfsBoolean("c")
      );
    }
  }

  record AType(int nr, String name, boolean ok) {}
}
