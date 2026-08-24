import { graphql } from '../../gql';
import { print } from 'graphql/index';

export const query = graphql(`
  query bikeRentalStation($id: String!) {
    bikeRentalStation(id: $id) {
      id
      name
      networks
      bikesAvailable
      spacesAvailable
    }
  }
`);

export const bikeRentalStationQueryAsString = print(query);
