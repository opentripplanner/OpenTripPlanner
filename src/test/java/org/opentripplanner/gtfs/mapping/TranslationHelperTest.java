package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.FeedInfo;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Translation;
import org.opentripplanner.framework.i18n.I18NString;

public class TranslationHelperTest {

  private static final String LANGUAGE_FEED = "fi";
  private static final Locale LOCALE_FEED = new Locale(LANGUAGE_FEED);
  private static final Locale LOCALE_SV = new Locale("sv");

  private static final Collection<Translation> ALL_TRANSLATIONS = new ArrayList<>();
  private static final Collection<FeedInfo> FEED_INFOS = new ArrayList<>();

  private static final TranslationHelper helper = new TranslationHelper();

  //Translation's structure:
  //table_name,field_name,language,translation,record_id,record_sub_id,field_value
  private static final String[][] translations = {
    { "feed_info", "feed_publisher_name", "sv", "SV feed name", null, null, null },
    { "feed_info", "feed_publisher_url", "sv", "http://sv-feed-url", null, null, null },
    { "stops", "stop_name", "sv", "SV stop rec 1", "1", null, null },
    { "stops", "stop_name", "sv", "SV stop field value 1", null, null, "1" },
    { "stops", "stop_name", "sv", "SV stop 2", "2", null, null },
    { "stops", "stop_name", "sv", "SV field value", null, null, "SFV" },
    { "stops", "stop_url", "sv", "http://sv-url2", "2", null, null },
    { "stop_times", "stop_headsign", "sv", "SV headsign rec 1 sub 1", "1", "1", null },
    { "stop_times", "stop_headsign", "sv", "SV headsign rec 1 sub 2", "1", "2", null },
    { "stop_times", "stop_headsign", "sv", "SV headsign field value", null, null, "SHS" },
  };

  static {
    FeedInfo feedInfo = new FeedInfo();
    feedInfo.setLang(LANGUAGE_FEED);
    FEED_INFOS.add(feedInfo);

    for (int x = 0; x < translations.length; x++) {
      Translation t = new Translation();
      t.setId(x + 1);
      t.setTableName(translations[x][0]);
      t.setFieldName(translations[x][1]);
      t.setLanguage(translations[x][2]);
      t.setTranslation(translations[x][3]);
      t.setRecordId(translations[x][4]);
      t.setRecordSubId(translations[x][5]);
      t.setFieldValue(translations[x][6]);
      ALL_TRANSLATIONS.add(t);
    }
    helper.importTranslations(ALL_TRANSLATIONS, FEED_INFOS);
  }

  @Test
  @DisplayName("Stop translations")
  public final void testStopTranslations() {
    Collection<Stop> stops = new ArrayList<>();
    Stop s1 = new Stop();
    s1.setId(new AgencyAndId("Test", "1"));
    s1.setName("Stop 1");
    s1.setUrl("http://url1");
    stops.add(s1);

    Stop s2 = new Stop();
    s2.setId(new AgencyAndId("Test", "2"));
    s2.setName("Stop 2");
    s2.setUrl("http://url2");
    stops.add(s2);

    Stop s3 = new Stop();
    s3.setId(new AgencyAndId("Test", "3"));
    s3.setName("SFV");
    stops.add(s3);

    Stop s4 = new Stop();
    s4.setId(new AgencyAndId("Test", "4"));
    s4.setName("SFV");
    stops.add(s4);

    Stop s5 = new Stop();
    s5.setId(new AgencyAndId("Test", "5"));
    s5.setName("1");
    stops.add(s5);

    for (Stop stop : stops) {
      I18NString nameTranslation = helper.getTranslation(
        org.onebusaway.gtfs.model.Stop.class,
        "name",
        stop.getId().getId(),
        stop.getName()
      );

      I18NString urlTranslation = helper.getTranslation(
        org.onebusaway.gtfs.model.Stop.class,
        "url",
        stop.getId().getId(),
        stop.getUrl()
      );

      String id = stop.getId().getId();
      switch (id) {
        case "1":
          assertEquals("Stop 1", nameTranslation.toString());
          assertEquals("http://url1", urlTranslation.toString());
          assertEquals("Stop 1", nameTranslation.toString(LOCALE_FEED));
          assertEquals("http://url1", urlTranslation.toString(LOCALE_FEED));
          assertEquals("SV stop rec 1", nameTranslation.toString(LOCALE_SV));
          assertEquals("http://url1", urlTranslation.toString(LOCALE_SV));
          break;
        case "2":
          assertEquals("Stop 2", nameTranslation.toString());
          assertEquals("http://url2", urlTranslation.toString());
          assertEquals("Stop 2", nameTranslation.toString(LOCALE_FEED));
          assertEquals("http://url2", urlTranslation.toString(LOCALE_FEED));
          assertEquals("SV stop 2", nameTranslation.toString(LOCALE_SV));
          assertEquals("http://sv-url2", urlTranslation.toString(LOCALE_SV));
          break;
        case "3":
        case "4":
          assertEquals("SFV", nameTranslation.toString());
          assertEquals("SFV", nameTranslation.toString(LOCALE_FEED));
          assertEquals("SV field value", nameTranslation.toString(LOCALE_SV));
          break;
        case "5":
          assertEquals("1", nameTranslation.toString());
          assertEquals("1", nameTranslation.toString(LOCALE_FEED));
          assertEquals("SV stop field value 1", nameTranslation.toString(LOCALE_SV));
          break;
      }
    }
  }

  @Test
  @DisplayName("Feed info translations")
  public final void testFeedInfoTranslations() {
    FeedInfo feed = new FeedInfo();
    feed.setLang(LANGUAGE_FEED);
    feed.setPublisherName("Feed name");
    feed.setPublisherUrl("http://feed-url");

    I18NString nameTranslation = helper.getTranslation(
      org.onebusaway.gtfs.model.FeedInfo.class,
      "publisherName",
      feed.getId(),
      feed.getPublisherName()
    );

    I18NString urlTranslation = helper.getTranslation(
      org.onebusaway.gtfs.model.FeedInfo.class,
      "publisherUrl",
      feed.getId(),
      feed.getPublisherUrl()
    );
    assertEquals("Feed name", nameTranslation.toString());
    assertEquals("http://feed-url", urlTranslation.toString());
    assertEquals("Feed name", nameTranslation.toString(LOCALE_FEED));
    assertEquals("http://feed-url", urlTranslation.toString(LOCALE_FEED));
    assertEquals("SV feed name", nameTranslation.toString(LOCALE_SV));
    assertEquals("http://sv-feed-url", urlTranslation.toString(LOCALE_SV));
  }

  @Test
  @DisplayName("Stop times translations")
  public final void testStopTimesTranslations() {
    Collection<StopTime> stopTimes = new ArrayList<>();
    StopTime st1 = new StopTime();
    st1.setId(1);
    st1.setStopSequence(1);
    st1.setStopHeadsign("Dest via 1");
    stopTimes.add(st1);

    StopTime st2 = new StopTime();
    st2.setId(1);
    st2.setStopSequence(2);
    st2.setStopHeadsign("Dest via 2");
    stopTimes.add(st2);

    StopTime st3 = new StopTime();
    st3.setId(3);
    st3.setStopSequence(1);
    st3.setStopHeadsign("SHS");
    stopTimes.add(st3);

    StopTime st4 = new StopTime();
    st4.setId(4);
    st4.setStopSequence(1);
    st4.setStopHeadsign("SHS");
    stopTimes.add(st4);

    for (StopTime stopTime : stopTimes) {
      String id = String.join(
        "_",
        String.valueOf(stopTime.getId()),
        String.valueOf(stopTime.getStopSequence())
      );

      I18NString headSignTranslation = helper.getTranslation(
        org.onebusaway.gtfs.model.StopTime.class,
        "stopHeadsign",
        id,
        stopTime.getStopHeadsign()
      );

      switch (id) {
        case "1_1":
          assertEquals("Dest via 1", headSignTranslation.toString());
          assertEquals("Dest via 1", headSignTranslation.toString(LOCALE_FEED));
          assertEquals("SV headsign rec 1 sub 1", headSignTranslation.toString(LOCALE_SV));
          break;
        case "1_2":
          assertEquals("Dest via 2", headSignTranslation.toString());
          assertEquals("Dest via 2", headSignTranslation.toString(LOCALE_FEED));
          assertEquals("SV headsign rec 1 sub 2", headSignTranslation.toString(LOCALE_SV));
          break;
        case "3_1":
        case "4_1":
          assertEquals("SHS", headSignTranslation.toString());
          assertEquals("SHS", headSignTranslation.toString(LOCALE_FEED));
          assertEquals("SV headsign field value", headSignTranslation.toString(LOCALE_SV));
          break;
      }
    }
  }
}
