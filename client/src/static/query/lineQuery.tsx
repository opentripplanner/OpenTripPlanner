import { graphql } from '../../gql';
import { print } from 'graphql/index';

export const query = graphql(`
  query line($id: ID!) {
    line(id: $id) {
      name
      publicCode
    }
  }
`);

export const lineQueryAsString = print(query);
