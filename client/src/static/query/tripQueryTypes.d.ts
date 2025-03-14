import { TripQuery } from '../../gql/graphql.ts';

export type TripPattern = TripQuery['trip']['tripPatterns'][0];
export type Leg = TripPattern['legs'][0];
