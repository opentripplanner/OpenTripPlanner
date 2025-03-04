package org.opentripplanner.apis.transmodel.model.timetable;

import graphql.Scalars;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import org.opentripplanner.apis.transmodel.mapping.BookingInfoMapper;
import org.opentripplanner.apis.transmodel.model.EnumTypes;
import org.opentripplanner.apis.transmodel.model.framework.TransmodelScalars;
import org.opentripplanner.transit.model.organization.ContactInfo;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;
import org.opentripplanner.transit.model.timetable.booking.BookingTime;

public class BookingArrangementType {

  public static GraphQLObjectType create() {
    GraphQLOutputType contactType = GraphQLObjectType.newObject()
      .name("Contact")
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("contactPerson")
          .description("Name of person to contact")
          .type(Scalars.GraphQLString) //
          .dataFetcher(environment -> ((contactInfo(environment)).getContactPerson()))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("email")
          .description("Email adress for contact")
          .type(Scalars.GraphQLString) //
          .dataFetcher(environment -> ((contactInfo(environment)).geteMail()))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("url")
          .description("Url for contact")
          .type(Scalars.GraphQLString) //
          .dataFetcher(environment -> ((contactInfo(environment)).getBookingUrl()))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("phone")
          .description("Phone number for contact")
          .type(Scalars.GraphQLString) //
          .dataFetcher(environment -> ((contactInfo(environment)).getPhoneNumber()))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("furtherDetails")
          .description("Textual description of how to get in contact")
          .type(Scalars.GraphQLString) //
          .dataFetcher(environment -> ((contactInfo(environment)).getAdditionalDetails()))
          .build()
      )
      .build();

    return GraphQLObjectType.newObject()
      .name("BookingArrangement")
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("bookingMethods")
          .description("How should service be booked?")
          .type(new GraphQLList(EnumTypes.BOOKING_METHOD))
          .dataFetcher(environment -> ((bookingInfo(environment)).bookingMethods()))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("latestBookingTime")
          .description("Latest time the service can be booked. ISO 8601 timestamp")
          .type(TransmodelScalars.LOCAL_TIME_SCALAR)
          .dataFetcher(environment -> {
            final BookingTime latestBookingTime = (bookingInfo(environment)).getLatestBookingTime();
            return latestBookingTime == null ? null : latestBookingTime.getTime();
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("latestBookingDay")
          .description("How many days prior to the travel the service needs to be booked")
          .type(Scalars.GraphQLInt)
          .dataFetcher(environment -> {
            final BookingTime latestBookingTime = (bookingInfo(environment)).getLatestBookingTime();
            return latestBookingTime == null ? null : latestBookingTime.getDaysPrior();
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("bookWhen")
          .description("Time constraints for booking")
          .type(EnumTypes.PURCHASE_WHEN)
          .dataFetcher(environment -> BookingInfoMapper.mapToBookWhen(bookingInfo(environment)))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("minimumBookingPeriod")
          .description("Minimum period in advance service can be booked as a ISO 8601 duration")
          .type(Scalars.GraphQLString)
          .dataFetcher(environment -> ((bookingInfo(environment)).getMinimumBookingNotice()))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("bookingNote")
          .description("Textual description of booking arrangement for service")
          .type(Scalars.GraphQLString)
          .dataFetcher(environment -> ((bookingInfo(environment)).getMessage()))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("bookingContact")
          .description("Who should ticket be contacted for booking")
          .type(contactType)
          .dataFetcher(environment -> ((bookingInfo(environment)).getContactInfo()))
          .build()
      )
      .build();
  }

  private static ContactInfo contactInfo(DataFetchingEnvironment environment) {
    return environment.getSource();
  }

  private static BookingInfo bookingInfo(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
