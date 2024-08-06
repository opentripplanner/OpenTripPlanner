package org.opentripplanner.netex.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;
import org.rutebanken.netex.model.BookingArrangementsStructure;
import org.rutebanken.netex.model.ContactStructure;
import org.rutebanken.netex.model.FlexibleLine;
import org.rutebanken.netex.model.FlexibleServiceProperties;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.PurchaseWhenEnumeration;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.StopPointInJourneyPattern;

class BookingInfoMapperTest {

  private static final String STOP_POINT_CONTACT = "StopPoint booking info contact";
  private static final String SERVICE_JOURNEY_CONTACT = "ServiceJourney booking info contact";
  private static final String FLEXIBLE_LINE_CONTACT = "StopPoint booking info contact";

  private static final String PERSON = "Person";
  private static final String PHONE = "Phone";
  private static final String EMAIL = "Email";
  private static final String DETAILS = "Details";

  private static final LocalTime FIVE_THIRTY = LocalTime.of(5, 30);
  private static final Duration THIRTY_MINUTES = Duration.ofMinutes(30);

  private final BookingInfoMapper subject = new BookingInfoMapper(DataImportIssueStore.NOOP);

  @Test
  void testMapBookingInfoPrecedence() {
    StopPointInJourneyPattern emptyStopPoint = new StopPointInJourneyPattern();
    ServiceJourney emptyServiceJourney = new ServiceJourney();

    StopPointInJourneyPattern stopPoint = new StopPointInJourneyPattern()
      .withBookingArrangements(
        new BookingArrangementsStructure()
          .withBookingContact(
            new ContactStructure()
              .withContactPerson(new MultilingualString().withValue(STOP_POINT_CONTACT))
          )
      );

    ServiceJourney serviceJourney = new ServiceJourney()
      .withFlexibleServiceProperties(
        new FlexibleServiceProperties()
          .withBookingContact(
            new ContactStructure()
              .withContactPerson(new MultilingualString().withValue(SERVICE_JOURNEY_CONTACT))
          )
      );

    FlexibleLine flexibleLine = new FlexibleLine()
      .withBookingContact(
        new ContactStructure()
          .withContactPerson(new MultilingualString().withValue(FLEXIBLE_LINE_CONTACT))
      );

    assertEquals(
      STOP_POINT_CONTACT,
      subject.map(stopPoint, serviceJourney, flexibleLine).getContactInfo().getContactPerson()
    );
    assertEquals(
      SERVICE_JOURNEY_CONTACT,
      subject.map(emptyStopPoint, serviceJourney, flexibleLine).getContactInfo().getContactPerson()
    );
    assertEquals(
      FLEXIBLE_LINE_CONTACT,
      subject
        .map(emptyStopPoint, emptyServiceJourney, flexibleLine)
        .getContactInfo()
        .getContactPerson()
    );
  }

  /**
   * When the contact details are set at the flexible line level, and bookWhen information is
   * set at StopPoint level, the BookingInfo should contain both contact details and bookWhen
   * information
   */
  @Test
  void testBookingInfoMergingAndOverriding() {
    StopPointInJourneyPattern emptyStopPoint = new StopPointInJourneyPattern();
    ServiceJourney emptyServiceJourney = new ServiceJourney();
    LocalTime stopPointLatestBookingTime = FIVE_THIRTY;

    StopPointInJourneyPattern stopPoint = new StopPointInJourneyPattern()
      .withBookingArrangements(
        new BookingArrangementsStructure()
          .withLatestBookingTime(stopPointLatestBookingTime)
          .withBookWhen(PurchaseWhenEnumeration.ADVANCE_AND_DAY_OF_TRAVEL)
      );

    FlexibleLine flexibleLine = new FlexibleLine()
      .withBookingContact(
        new ContactStructure()
          .withContactPerson(new MultilingualString().withValue(FLEXIBLE_LINE_CONTACT))
      );

    BookingInfo bookingInfo = subject.map(stopPoint, null, flexibleLine);
    assertEquals(stopPointLatestBookingTime, bookingInfo.getLatestBookingTime().getTime());
    assertEquals(FLEXIBLE_LINE_CONTACT, bookingInfo.getContactInfo().getContactPerson());
  }

  @Test
  void testMapBookingInfo() {
    ContactStructure contactStructure = new ContactStructure();
    contactStructure.setContactPerson(new MultilingualString().withValue(PERSON));
    contactStructure.setPhone(PHONE);
    contactStructure.setEmail(EMAIL);
    contactStructure.setFurtherDetails(new MultilingualString().withValue(DETAILS));

    BookingArrangementsStructure bookingArrangements = new BookingArrangementsStructure();
    bookingArrangements.setBookingContact(contactStructure);

    StopPointInJourneyPattern stopPoint = new StopPointInJourneyPattern()
      .withBookingArrangements(bookingArrangements);

    BookingInfo bookingInfo = subject.map(stopPoint, null, null);

    assertEquals(PERSON, bookingInfo.getContactInfo().getContactPerson());
    assertEquals(PHONE, bookingInfo.getContactInfo().getPhoneNumber());
    assertEquals(EMAIL, bookingInfo.getContactInfo().geteMail());
  }

  @Test
  void testMapEarliestLatestBookingTime() {
    ContactStructure contactStructure = new ContactStructure();
    contactStructure.setContactPerson(new MultilingualString().withValue(PERSON));

    BookingArrangementsStructure bookingArrangements = new BookingArrangementsStructure();
    bookingArrangements.setBookingContact(contactStructure);
    bookingArrangements.setLatestBookingTime(FIVE_THIRTY);

    StopPointInJourneyPattern stopPoint = new StopPointInJourneyPattern()
      .withBookingArrangements(bookingArrangements);

    bookingArrangements.setBookWhen(PurchaseWhenEnumeration.ADVANCE_ONLY);

    BookingInfo bookingInfo1 = subject.map(stopPoint, null, null);
    assertEquals(FIVE_THIRTY, bookingInfo1.getLatestBookingTime().getTime());
    assertEquals(0, bookingInfo1.getLatestBookingTime().getDaysPrior());
    assertNull(bookingInfo1.getEarliestBookingTime());

    bookingArrangements.setBookWhen(PurchaseWhenEnumeration.UNTIL_PREVIOUS_DAY);

    BookingInfo bookingInfo2 = subject.map(stopPoint, null, null);
    assertEquals(FIVE_THIRTY, bookingInfo2.getLatestBookingTime().getTime());
    assertEquals(1, bookingInfo2.getLatestBookingTime().getDaysPrior());
    assertNull(bookingInfo2.getEarliestBookingTime());

    bookingArrangements.setBookWhen(PurchaseWhenEnumeration.DAY_OF_TRAVEL_ONLY);

    BookingInfo bookingInfo3 = subject.map(stopPoint, null, null);
    assertEquals(FIVE_THIRTY, bookingInfo3.getLatestBookingTime().getTime());
    assertEquals(0, bookingInfo3.getLatestBookingTime().getDaysPrior());
    assertEquals(0, bookingInfo3.getEarliestBookingTime().getDaysPrior());
    assertEquals(LocalTime.MIDNIGHT, bookingInfo3.getEarliestBookingTime().getTime());

    bookingArrangements.setBookWhen(PurchaseWhenEnumeration.ADVANCE_AND_DAY_OF_TRAVEL);

    BookingInfo bookingInfo4 = subject.map(stopPoint, null, null);
    assertEquals(FIVE_THIRTY, bookingInfo4.getLatestBookingTime().getTime());
    assertEquals(0, bookingInfo4.getLatestBookingTime().getDaysPrior());
    assertNull(bookingInfo4.getEarliestBookingTime());

    bookingArrangements.setBookWhen(PurchaseWhenEnumeration.TIME_OF_TRAVEL_ONLY);

    BookingInfo bookingInfo5 = subject.map(stopPoint, null, null);
    assertNull(bookingInfo5.getLatestBookingTime());
    assertNull(bookingInfo5.getEarliestBookingTime());
  }

  @Test
  void testMapMinimumBookingNotice() {
    ContactStructure contactStructure = new ContactStructure();
    contactStructure.setContactPerson(new MultilingualString().withValue(PERSON));

    BookingArrangementsStructure bookingArrangements = new BookingArrangementsStructure();
    bookingArrangements.setBookingContact(contactStructure);
    bookingArrangements.setLatestBookingTime(FIVE_THIRTY);

    StopPointInJourneyPattern stopPoint = new StopPointInJourneyPattern()
      .withBookingArrangements(bookingArrangements);

    bookingArrangements.setMinimumBookingPeriod(THIRTY_MINUTES);

    BookingInfo bookingInfo = subject.map(stopPoint, null, null);

    assertEquals(THIRTY_MINUTES, bookingInfo.getMinimumBookingNotice());
  }
}
