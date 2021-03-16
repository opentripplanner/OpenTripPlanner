package org.opentripplanner.netex.mapping;

import org.junit.Test;
import org.opentripplanner.model.BookingInfo;
import org.rutebanken.netex.model.BookingArrangementsStructure;
import org.rutebanken.netex.model.ContactStructure;
import org.rutebanken.netex.model.FlexibleLine;
import org.rutebanken.netex.model.FlexibleServiceProperties;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.PurchaseWhenEnumeration;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.StopPointInJourneyPattern;

import java.time.Duration;
import java.time.LocalTime;

import static org.junit.Assert.*;

public class BookingInfoMapperTest {

  private static final String STOP_POINT_CONTACT = "StopPoint booking info contact";
  private static final String SERVICE_JOURNEY_CONTACT = "ServiceJourney booking info contact";
  private static final String FLEXIBLE_LINE_CONTACT = "StopPoint booking info contact";

  private static final String PERSON = "Person";
  private static final String PHONE = "Phone";
  private static final String EMAIL = "Email";
  private static final String DETAILS = "Details";

  private static final LocalTime FIVE_THIRTY = LocalTime.of(5, 30);
  private static final Duration THIRTY_MINUTES = Duration.ofMinutes(30);

  @Test
  public void testMapBookingInfoPrecedence() {
    StopPointInJourneyPattern emptyStopPoint = new StopPointInJourneyPattern();
    ServiceJourney emptyServiceJourney = new ServiceJourney();

    StopPointInJourneyPattern stopPoint =
        new StopPointInJourneyPattern().withBookingArrangements(
                new BookingArrangementsStructure().withBookingContact(
                    new ContactStructure().withContactPerson(
                        new MultilingualString().withValue(STOP_POINT_CONTACT))));

    ServiceJourney serviceJourney =
        new ServiceJourney().withFlexibleServiceProperties(
            new FlexibleServiceProperties().withBookingContact(
                new ContactStructure().withContactPerson(
                    new MultilingualString().withValue(SERVICE_JOURNEY_CONTACT))));

    FlexibleLine flexibleLine =
        new FlexibleLine().withBookingContact(
            new ContactStructure().withContactPerson(
              new MultilingualString().withValue(FLEXIBLE_LINE_CONTACT)));

    assertEquals(
        STOP_POINT_CONTACT,
        BookingInfoMapper
            .map(stopPoint, serviceJourney, flexibleLine)
            .getContactInfo()
            .getContactPerson()
    );
    assertEquals(
        SERVICE_JOURNEY_CONTACT,
        BookingInfoMapper
            .map(emptyStopPoint, serviceJourney, flexibleLine)
            .getContactInfo()
            .getContactPerson()
    );
    assertEquals(
        FLEXIBLE_LINE_CONTACT,
        BookingInfoMapper
            .map(emptyStopPoint, emptyServiceJourney, flexibleLine)
            .getContactInfo()
            .getContactPerson()
    );
  }

  @Test
  public void testMapBookingInfo() {
    ContactStructure contactStructure = new ContactStructure();
    contactStructure.setContactPerson(new MultilingualString().withValue(PERSON));
    contactStructure.setPhone(PHONE);
    contactStructure.setEmail(EMAIL);
    contactStructure.setFurtherDetails(new MultilingualString().withValue(DETAILS));

    BookingArrangementsStructure bookingArrangements = new BookingArrangementsStructure();
    bookingArrangements.setBookingContact(contactStructure);

    StopPointInJourneyPattern stopPoint =
        new StopPointInJourneyPattern().withBookingArrangements(bookingArrangements);

    BookingInfo bookingInfo = BookingInfoMapper.map(stopPoint, null, null);

    assertEquals(PERSON, bookingInfo.getContactInfo().getContactPerson());
    assertEquals(PHONE, bookingInfo.getContactInfo().getPhoneNumber());
    assertEquals(EMAIL, bookingInfo.getContactInfo().geteMail());
  }

  @Test
  public void testMapEarliestLatestBookingTime() {
    ContactStructure contactStructure = new ContactStructure();
    contactStructure.setContactPerson(new MultilingualString().withValue(PERSON));

    BookingArrangementsStructure bookingArrangements = new BookingArrangementsStructure();
    bookingArrangements.setBookingContact(contactStructure);
    bookingArrangements.setLatestBookingTime(FIVE_THIRTY);

    StopPointInJourneyPattern stopPoint =
        new StopPointInJourneyPattern().withBookingArrangements(bookingArrangements);

    bookingArrangements.setBookWhen(PurchaseWhenEnumeration.ADVANCE_ONLY);

    BookingInfo bookingInfo1 = BookingInfoMapper.map(stopPoint, null, null);
    assertEquals(FIVE_THIRTY,  bookingInfo1.getLatestBookingTime().getTime());
    assertEquals(0, bookingInfo1.getLatestBookingTime().getDaysPrior());
    assertNull(bookingInfo1.getEarliestBookingTime());

    bookingArrangements.setBookWhen(PurchaseWhenEnumeration.UNTIL_PREVIOUS_DAY);

    BookingInfo bookingInfo2 = BookingInfoMapper.map(stopPoint, null, null);
    assertEquals(FIVE_THIRTY,  bookingInfo2.getLatestBookingTime().getTime());
    assertEquals(1, bookingInfo2.getLatestBookingTime().getDaysPrior());
    assertNull(bookingInfo2.getEarliestBookingTime());

    bookingArrangements.setBookWhen(PurchaseWhenEnumeration.DAY_OF_TRAVEL_ONLY);

    BookingInfo bookingInfo3 = BookingInfoMapper.map(stopPoint, null, null);
    assertEquals(FIVE_THIRTY,  bookingInfo3.getLatestBookingTime().getTime());
    assertEquals(0, bookingInfo3.getLatestBookingTime().getDaysPrior());
    assertEquals(0, bookingInfo3.getEarliestBookingTime().getDaysPrior());
    assertEquals(LocalTime.MIDNIGHT, bookingInfo3.getEarliestBookingTime().getTime());
  }

  @Test
  public void testMapMinimumBookingNotice() {
    ContactStructure contactStructure = new ContactStructure();
    contactStructure.setContactPerson(new MultilingualString().withValue(PERSON));

    BookingArrangementsStructure bookingArrangements = new BookingArrangementsStructure();
    bookingArrangements.setBookingContact(contactStructure);
    bookingArrangements.setLatestBookingTime(FIVE_THIRTY);

    StopPointInJourneyPattern stopPoint =
        new StopPointInJourneyPattern().withBookingArrangements(bookingArrangements);

    bookingArrangements.setMinimumBookingPeriod(THIRTY_MINUTES);

    BookingInfo bookingInfo = BookingInfoMapper.map(stopPoint, null, null);

    assertEquals(THIRTY_MINUTES, bookingInfo.getMinimumBookingNotice());
  }
}