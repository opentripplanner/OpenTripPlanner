import { graphql } from '../../gql';
import { print } from 'graphql/index';

export const query = graphql(`
  query authority($id: String!) {
    authority(id: $id) {
      name
      id
    }
  }
`);

export const authorityQueryAsString = print(query);
