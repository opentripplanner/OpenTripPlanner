package org.opentripplanner.ext.transmodelapi.model.timetable;

import graphql.Scalars;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import org.opentripplanner.ext.transmodelapi.model.EnumTypes;
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;
import org.opentripplanner.model.BookingInfo;
import org.opentripplanner.model.ContactInfo;

public class BookingArrangementType {

  public static GraphQLObjectType create(GqlUtil gqlUtil) {
    GraphQLOutputType contactType = GraphQLObjectType
        .newObject()
        .name("Contact")
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("contactPerson")
            .description("Name of person to contact")
            .type(Scalars.GraphQLString)//
            .dataFetcher(environment -> ((contactInfo(environment)).getContactPerson()))
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("email")
            .description("Email adress for contact")
            .type(Scalars.GraphQLString)//
            .dataFetcher(environment -> ((contactInfo(environment)).geteMail()))
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("url")
            .description("Url for contact")
            .type(Scalars.GraphQLString)//
            .dataFetcher(environment -> ((contactInfo(environment)).getBookingUrl()))
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("phone")
            .description("Phone number for contact")
            .type(Scalars.GraphQLString)//
            .dataFetcher(environment -> ((contactInfo(environment)).getPhoneNumber()))
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("furtherDetails")
            .description("Textual description of how to get in contact")
            .type(Scalars.GraphQLString)//
            .dataFetcher(environment -> ((contactInfo(environment)).getAdditionalDetails()))
            .build())
        .build();


    return GraphQLObjectType.newObject()
        .name("BookingArrangement")
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("bookingMethods")
            .description("How should service be booked?")
            .type(new GraphQLList(EnumTypes.BOOKING_METHOD))
            .dataFetcher(environment -> ((bookingInfo(environment)).bookingMethods()))
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("latestBookingTime")
            .description("Latest time service can be booked. ISO 8601 timestamp")
            .type(gqlUtil.localTimeScalar)
            .dataFetcher(environment -> ((bookingInfo(environment)).getLatestBookingTime().getTime()))
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("latestBookingDay")
            .description("Latest time service can be booked. ISO 8601 timestamp")
            .type(gqlUtil.localTimeScalar)
            .dataFetcher(environment -> ((bookingInfo(environment)).getLatestBookingTime().getDaysPrior()))
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("minimumBookingPeriod")
            .description("Minimum period in advance service can be booked as a ISO 8601 duration")
            .type(Scalars.GraphQLString)
            .dataFetcher(environment -> ((bookingInfo(environment)).getMinimumBookingNotice()))
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("bookingNote")
            .description("Textual description of booking arrangement for service")
            .type(Scalars.GraphQLString)
            .dataFetcher(environment -> ((bookingInfo(environment)).getMessage()))
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("bookingContact")
            .description("Who should ticket be contacted for booking")
            .type(contactType)
            .dataFetcher(environment -> ((bookingInfo(environment)).getContactInfo()))
            .build())
        .build();
  }

  private static ContactInfo contactInfo(DataFetchingEnvironment environment) {
    return environment.getSource();
  }

  private static BookingInfo bookingInfo(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
