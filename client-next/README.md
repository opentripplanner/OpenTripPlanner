# OTP debug client (next)

This is the next version of the debug client, intended to replace `../src/client`.

It is designed to work with the Transmodel GraphQL API.

## Stack notes

This is a true Single Page Application (SPA) written in TypeScript on the React framework. It uses `vite` to run 
(locally) and for building static assets. It requires no server runtime, and can be served from a CDN and run entirely 
in the browser.

The design framework is Bootstrap with React support from `react-bootstrap`.

The map framework is MapLibre, using `MapLibre GL JS` with React support from `react-map-gl`.

GraphQL integration is provided by `graphql-request`, with type support added with `@graphql-codegen`. Types are 
generated during build and are not checked into the repository.

## Prerequisites

Use latest LTS version of Node/npm (currently v18). Recommend using av version manager such as `nvm`. 

The dev and production builds require graphql schema to be present at
`../src/ext/graphql/transmodelapi/schema.graphql`. 
Running `maven verify` in parent project should solve that.

## Getting started

Change directory to `client-next` (current) if you haven't already.

    npm install

Then

    npm run dev
