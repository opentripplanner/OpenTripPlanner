package org.opentripplanner.apis.gtfs.datafetchers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.apis.support.graphql.DataFetchingSupport.dataFetchingEnvironment;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.basic.Notice;

class NoticeImplTest {

  private static final NoticeImpl SUBJECT = new NoticeImpl();
  private static final Notice NOTICE = Notice.of(id(1)).withText("AAA").build();

  @Test
  void text() throws Exception {
    var env = dataFetchingEnvironment(NOTICE);
    assertEquals("AAA", SUBJECT.text().get(env));
  }
}
