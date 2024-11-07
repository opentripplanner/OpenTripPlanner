package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.transit.model.organization.ContactInfo;

public class ContactInfoImpl implements GraphQLDataFetchers.GraphQLContactInfo {

  @Override
  public DataFetcher<String> additionalDetails() {
    return environment -> getSource(environment).getAdditionalDetails();
  }

  @Override
  public DataFetcher<String> bookingUrl() {
    return environment -> getSource(environment).getBookingUrl();
  }

  @Override
  public DataFetcher<String> contactPerson() {
    return environment -> getSource(environment).getContactPerson();
  }

  @Override
  public DataFetcher<String> eMail() {
    return environment -> getSource(environment).geteMail();
  }

  @Override
  public DataFetcher<String> faxNumber() {
    return environment -> getSource(environment).getFaxNumber();
  }

  @Override
  public DataFetcher<String> infoUrl() {
    return environment -> getSource(environment).getInfoUrl();
  }

  @Override
  public DataFetcher<String> phoneNumber() {
    return environment -> getSource(environment).getPhoneNumber();
  }

  private ContactInfo getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
