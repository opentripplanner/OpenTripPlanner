


## Support for more thaan one dialect of the GraphQL Schema
OTP have two endpoint with the same structure, but translated in the vocabulary of GTFS and
Transmodel/NeTEx. We provide these two enpoint so orangisation using one of these standards have
a "more familiar" API to work with. The two endpoints are equal in functionality and structure, and 
all data, independent of the format of the source, is available in both. We achieve this by using 
one schema kept in the language of the OTP internal model, and translating this into the 
NeTEx and GTFS dialect.

See the [GraphQL Schema Dialect Design](ApiDialectDesing.md) to get an overview of the 
implementation.