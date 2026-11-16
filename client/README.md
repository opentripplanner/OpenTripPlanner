# OTP debug client (next)

This is the next version of the debug client, intended to replace `../src/client`.

It is designed to work with the Transmodel GraphQL API.

## Stack notes

This is a true Single Page Application (SPA) written in TypeScript on the React framework. It uses `vite` to run
(locally) and for building static assets. It requires no server runtime, and can be served by any static web server and
run entirely in the browser. The Maven build bundles it into the OTP jar, which serves it.

The design framework is Bootstrap with React support from `react-bootstrap`.

The map framework is MapLibre, using `MapLibre GL JS` with React support from `react-map-gl`.

GraphQL integration is provided by `graphql-request`, with type support added with `@graphql-codegen`. Types are
generated during build and are not checked into the repository.

## Prerequisites

Use the Node version set as `nodeVersion` in `pom.xml` in this directory. The Maven build installs it by itself, so you
only need your own Node and npm to work on the client with `npm run dev`. Recommend using a version manager such as
`nvm`.

The dev and production builds require graphql schema to be present at
`../application/src/main/resources/org/opentripplanner/apis/transmodel/schema.graphql`.

## Getting started (development)

Change directory to `client` (current) if you haven't already.

    npm install

Then

    npm run dev

The debug client will now be available at `http://localhost:5173/`. It has
hot reloading enabled, so you don't have to restart it when you save files.

If you change graphql code during development you can issue the following command:

    npm run codegen

You don't have to restart the development server for the changes to take effect.

## Important when modifying dependencies

If you make any changes to the dependencies in package.json, you must also update the package-lock.json
file. Any divergence will cause CI to fail. To regenerate the lockfile, run `npm install`, and commit
any changes.

Hot tip: The best way to add or remove dependencies is to use the `npm install --save-exact` or `npm uninstall` commands
rather than editing the package.json manually. For devDependencies use `--save-dev`.

## Build for production

Change directory to `client` (current) if you haven't already.

    npm install

Then

    npm run build

## Which OTP instance do I use?

In development mode, the debug client by default assumes you are running an OTP instance locally at
port 8080 (see `.env.development`). If you wish to point to a different OTP instance
(like a remote server), use the environment variable`VITE_API_URL`, either at the command line,
or add it to a new `.env.development.local` file (this file will be ignored by git).

In production mode, the default is to access OTP via the same origin as the client (see `.env`).
This behavior can also be modified by changing the previously mentioned environment variable at
build-time.

The links from the debug client to GraphiQL (`VITE_GRAPHIQL_URL`) assume the API is at its default
path. If OTP is served behind a proxy that exposes the API on another path, add a
`transmodelEndpoint` parameter to the link, for example
`/graphiql?flavor=transmodel&transmodelEndpoint=/my/proxy/graphql`. The GTFS API has its own
parameter, `gtfsEndpoint`. The endpoints must be on the same origin as the GraphiQL page, other
values are ignored, and they are kept when the API is switched in the page.
