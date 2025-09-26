import { TripQueryVariables } from '../gql/graphql.ts';
import { queryAsString } from '../static/query/tripQuery.tsx';
import {
  parse,
  print,
  visit,
  DocumentNode,
  OperationDefinitionNode,
  VariableDefinitionNode,
  ArgumentNode,
  VariableNode,
} from 'graphql';

function extractVariableDefinitions(document: DocumentNode): Record<string, string> {
  const variableTypes: Record<string, string> = {};

  visit(document, {
    OperationDefinition(node: OperationDefinitionNode) {
      if (node.operation === 'query' && node.variableDefinitions) {
        node.variableDefinitions.forEach((varDef: VariableDefinitionNode) => {
          const varName = varDef.variable.name.value;
          variableTypes[varName] = print(varDef.type);
        });
      }
    },
  });

  return variableTypes;
}

export function createPrunedQuery(variables: TripQueryVariables): string {
  const document = parse(queryAsString);
  const variableTypes = extractVariableDefinitions(document);

  const providedVariables = Object.keys(variables).filter(
    (key) => variables[key as keyof TripQueryVariables] !== undefined,
  );

  // Create new document with only the variables we need
  const prunedDocument = visit(document, {
    OperationDefinition(node: OperationDefinitionNode) {
      if (node.operation === 'query') {
        // Filter variable definitions to only include provided variables
        const filteredVariableDefinitions = node.variableDefinitions?.filter((varDef: VariableDefinitionNode) => {
          const varName = varDef.variable.name.value;
          return providedVariables.includes(varName) && variableTypes[varName];
        });

        return {
          ...node,
          variableDefinitions: filteredVariableDefinitions || [],
        };
      }
      return node;
    },

    Field(node) {
      // Update the trip field arguments to only include provided variables
      if (node.name.value === 'trip' && node.arguments) {
        const filteredArguments = node.arguments.filter((arg: ArgumentNode) => {
          if (arg.value.kind === 'Variable') {
            const varName = (arg.value as VariableNode).name.value;
            return providedVariables.includes(varName) && variableTypes[varName];
          }
          return true;
        });

        return {
          ...node,
          arguments: filteredArguments,
        };
      }
      return node;
    },
  });

  return print(prunedDocument);
}

export function createPrunedVariables(variables: TripQueryVariables): TripQueryVariables {
  // Create a new object with only the defined variables
  const prunedVariables: Partial<TripQueryVariables> = {};

  Object.keys(variables).forEach((key) => {
    const value = variables[key as keyof TripQueryVariables];
    if (value !== undefined) {
      prunedVariables[key as keyof TripQueryVariables] = value;
    }
  });

  return prunedVariables as TripQueryVariables;
}
