# GraphQL API Client Developer Guide

## Introduction to GraphQL and to the OpenTripPlanner's GraphQL API

GraphQL is a query language. What can be queried depends on the schema that is defined on the server (in OpenTripPlanner in this case). The GraphQL organization's webpage contains [an introduction to GraphQL](https://graphql.org/learn/) which is useful to read if you are unfamiliar with GraphQL. OpenTripPlanner's GraphQL API doesn't contain any mutations, subscriptions, authentication or authorization.

## Client software

The GraphQL API follows best practices and it is designed to work with all standard GraphQL client software. It contains the node and nodes queries to fulfill the [Global Object Identification Specification](https://graphql.org/learn/global-object-identification/) and paging options that follow the [GraphQL Cursor Connections Specification](https://facebook.github.io/relay/graphql/connections.htm) so the API is designed to work well with [Relay](https://relay.dev/) but those features can be utilized with other client software as well. The GraphQL organization's webpage contains [a list of client software options](https://graphql.org/code/).

Using linters and/or type definitions in the client code works well together with the GraphQL as the API defines the available fields and their types, and this information can be fetched to the client through [introspection](https://graphql.org/learn/introspection/).

## Development tips

It's possible to use autocomplete on some IDEs (with or without plugins) when using the GraphQL types and fields in code.

There exist tools that can be used to explore the structure of the GraphQL API and to conveniently write queries that can be tested through a graphical user interface. These tools can be used to explore and design queries that can then be used in the client code.

## Best practices

1. Implement default handling for Enums, Interfaces and Unions as more values or types might be added to them.
2. Define arguments instead of relying on default values if it's important that the behavior doesn't change. The default values can change.
3. Keep track of what has been deprecated and follow the suggestions from the deprecation messages in order to make the appropriate changes to the client. Since the GraphQL specification does not allow arguments to be marked with the `@deprecated` directive in the schema, some of the deprecations are written in descriptions instead.