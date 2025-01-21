import {
  buildClientSchema,
  getIntrospectionQuery,
  GraphQLSchema,
  GraphQLType,
  GraphQLNamedType,
  isNonNullType,
  isListType,
  isScalarType,
  isEnumType,
  isInputObjectType,
} from 'graphql';

//
// Types
//
export interface ResolvedType {
  type: 'Scalar' | 'Enum' | 'InputObject' | 'Group';
  // For scalars or fallback, e.g. "String", "Int", etc.
  subtype?: string;
  // For input objects
  name?: string;
  fields?: {
    [fieldName: string]: {
      type: ResolvedType;
      defaultValue?: string | number | boolean | object | null; // Updated type
      isList: boolean;
    };
  };
  // For enums
  values?: string[];
}

export interface ArgumentRepresentation {
  [argName: string]: {
    type: ResolvedType;
    defaultValue?: string | number | boolean | object | null; // Updated type
    isList: boolean;
  };
}

export interface TripArgsRepresentation {
  trip: {
    arguments: ArgumentRepresentation;
  };
}

/**
 * Repeatedly unwraps NonNull and List wrappers until we get a named type.
 */
function getNamedType(type: GraphQLType): GraphQLNamedType {
  let current: GraphQLType = type;

  while (true) {
    if (isNonNullType(current)) {
      current = current.ofType;
    } else if (isListType(current)) {
      current = current.ofType;
    } else {
      break;
    }
  }

  // At this point, current should be a GraphQLNamedType
  return current as GraphQLNamedType;
}

function resolveType(type: GraphQLType): ResolvedType {
  const namedType = getNamedType(type);

  if (isScalarType(namedType)) {
    return { type: 'Scalar', subtype: namedType.name };
  }

  if (isEnumType(namedType)) {
    return {
      type: 'Enum',
      values: namedType.getValues().map((val) => val.name),
    };
  }

  if (isInputObjectType(namedType)) {
    const fields = namedType.getFields();
    const fieldTypes: Record<
      string,
      { type: ResolvedType; defaultValue?: string | number | boolean | object | null; isList: boolean } // Updated type
    > = {};

    for (const fieldName of Object.keys(fields)) {
      const field = fields[fieldName];

      // Exclude deprecated fields
      if (field.deprecationReason) {
        continue;
      }

      const isList = isListType(field.type);
      const defaultValue = field.defaultValue !== undefined ? field.defaultValue : null;

      fieldTypes[fieldName] = {
        type: resolveType(field.type),
        defaultValue: defaultValue,
        isList,
      };
    }

    return {
      type: 'InputObject',
      name: namedType.name,
      fields: fieldTypes,
    };
  }

  return { type: 'Scalar', subtype: 'String' };
}

function generateTripArgs(schema: GraphQLSchema): TripArgsRepresentation {
  const queryType = schema.getQueryType();
  if (!queryType) {
    throw new Error('No Query type found in the schema.');
  }

  const tripField = queryType.getFields()['trip'];
  if (!tripField) {
    throw new Error('No trip query found in the schema.');
  }

  const argsJson: ArgumentRepresentation = {};

  tripField.args.forEach((arg) => {
    if (arg.deprecationReason) {
      // Skip deprecated arguments
      return;
    }

    const argName = arg.name;
    const argType = resolveType(arg.type);
    const argDefaultValue = arg.defaultValue !== null ? arg.defaultValue : null;
    const isList = isListType(arg.type);

    argsJson[argName] = {
      type: argType,
      ...(argDefaultValue !== null && { defaultValue: argDefaultValue }),
      isList,
    };
  });

  return {
    trip: {
      arguments: argsJson,
    },
  };
}

//Fetch the remote GraphQL schema via introspection
export async function fetchTripArgs(graphqlEndpointUrl: string): Promise<TripArgsRepresentation> {
  const introspectionQuery = getIntrospectionQuery();

  const response = await fetch(graphqlEndpointUrl, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ query: introspectionQuery }),
  });

  if (!response.ok) {
    throw new Error(`Failed to fetch schema. HTTP error: ${response.status}`);
  }

  const { data } = await response.json();

  const schema = buildClientSchema(data);

  return generateTripArgs(schema);
}
