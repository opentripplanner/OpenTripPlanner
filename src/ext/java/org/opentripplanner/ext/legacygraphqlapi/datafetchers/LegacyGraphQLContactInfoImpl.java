package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.model.ContactInfo;

public class LegacyGraphQLContactInfoImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLContactInfo {
    @Override
    public DataFetcher<String> contactPerson() {
        return environment -> getSource(environment).getContactPerson();
    }

    @Override
    public DataFetcher<String> phoneNumber() {
        return environment -> getSource(environment).getPhoneNumber();
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
    public DataFetcher<String> bookingUrl() {
        return environment -> getSource(environment).getBookingUrl();
    }

    @Override
    public DataFetcher<String> additionalDetails() {
        return environment -> getSource(environment).getAdditionalDetails();
    }

    private ContactInfo getSource(DataFetchingEnvironment environment) {
        return environment.getSource();
    }
}
