import { graphql } from '../../gql';
import { print } from 'graphql/index';

export const query = graphql(`
  query quay($id: String!) {
    quay(id: $id) {
      stopPlace {
        id
        name
      }
    }
  }
`);

export const quayQueryAsString = print(query);
