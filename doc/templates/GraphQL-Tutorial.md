<!--
  NOTE! Part of this document is generated. Make sure you edit the template, not the generated doc.

   - Template directory is:  /doc/templates
   - Generated directory is: /doc/user 
-->

# GraphQL tutorial

This document will give you a quick start tutorial on how to get started with OTP's GraphQL APIs. For 
this tutorial we will be using the [GTFS GraphQL API](GTFS-GraphQL-API.md) as this is the most commonly used one.

First of all, make sure that you've loaded street and transit data into your instance by following
the [basic tutorial](../Basic-Tutorial.md)

## Visual GraphQL API client

OTP has a built-in API client to help you build queries and view documentation. After having
started OTP, visit [http://localhost:8080/graphiql](http://localhost:8080/graphiql) to open it.

It should look like this:

![GraphiQL](../images/graphiql.png)

## Sending your first query

For our first query we want to get the list of routes loaded into OTP. Therefore paste the following 
GraphQL query in the left hand panel of the page:

<!-- INSERT: route-query -->

After pressing the "Execute query" button you should see the result of the query on the right hand
side panel.

Now would be a good time to explore the auto-complete capabilities of the tool by moving the
cursor into the query panel and hitting Ctrl-Space to see what other query parameters are possible.

![GraphiQL](../images/graphiql-autocomplete.png)

The explorer also has documentation built into it. If you hover your pointer over a property on the 
left hand side you can see its documentation.

![GraphiQL](../images/graphiql-documentation.png)

## A more advanced query

Most people want to get routing results out of OTP, so lets see the query for this:

<!-- INSERT: plan-query -->

Again, please use the autocomplete and documentation viewers to figure out what each input parameter
and property means.

More examples for a variety of queries can also be found [in the test code](https://github.com/opentripplanner/OpenTripPlanner/tree/dev-2.x/src/test/resources/org/opentripplanner/apis/gtfs/queries).