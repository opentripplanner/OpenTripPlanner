package org.opentripplanner.ext.transmodelapi.model.timetable;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;

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
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("email")
            .description("Email adress for contact")
            .type(Scalars.GraphQLString)//
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("url")
            .description("Url for contact")
            .type(Scalars.GraphQLString)//
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("phone")
            .description("Phone number for contact")
            .type(Scalars.GraphQLString)//
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("furtherDetails")
            .description("Textual description of how to get in contact")
            .type(Scalars.GraphQLString)//
            .build())
        .build();


    return GraphQLObjectType.newObject()
        .name("BookingArrangement")
        //                                         .field(GraphQLFieldDefinition.newFieldDefinition()
        //                                                        .name("bookingAccess")
        //                                                        .description("Who has access to book service?")
        //                                                        .type(bookingAccessEnum)
        //                                                        .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("bookingMethods")
            .description("How should service be booked?")
            .type(Scalars.GraphQLString)
            .build())
        //                                         .field(GraphQLFieldDefinition.newFieldDefinition()
        //                                                        .name("bookWhen")
        //                                                        .description("When should service be booked?")
        //                                                        .type(purchaseWhenEnum)
        //                                                        .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("latestBookingTime")
            .description("Latest time service can be booked. ISO 8601 timestamp")
            .type(gqlUtil.localTimeScalar)
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("minimumBookingPeriod")
            .description("Minimum period in advance service can be booked as a ISO 8601 duration")
            .type(Scalars.GraphQLString)
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("bookingNote")
            .description("Textual description of booking arrangement for service")
            .type(Scalars.GraphQLString)
            .build())
        //                                         .field(GraphQLFieldDefinition.newFieldDefinition()
        //                                                        .name("buyWhen")
        //                                                        .description("When should ticket be purchased?")
        //                                                        .type(new GraphQLList(purchaseMomentEnum))
        //                                                        .build())
        //
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("bookingContact")
            .description("Who should ticket be contacted for booking")
            .type(contactType)
            .build())
        .build();
  }
}
