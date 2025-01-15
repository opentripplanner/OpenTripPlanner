import {
    buildClientSchema,
    getIntrospectionQuery,
    GraphQLSchema,
    GraphQLType,
    GraphQLNamedType,
    GraphQLNonNull,
    GraphQLList,
    isNonNullType,
    isListType,
    isScalarType,
    isEnumType,
    isInputObjectType,
} from 'graphql';

//
// Types
//
interface ResolvedType {
    type: 'Scalar' | 'Enum' | 'InputObject';
    // For scalars or fallback, e.g. "String", "Int", etc.
    subtype?: string;
    // For input objects
    name?: string;
    fields?: {
        [fieldName: string]: {
            type: ResolvedType;
            defaultValue: any;
            isList: boolean;
        };
    };
    // For enums
    values?: string[];
}

interface ArgumentRepresentation {
    [argName: string]: {
        type: ResolvedType;
        defaultValue?: any;
        isList: boolean;
    };
}

export interface TripArgsRepresentation {
    trip: {
        arguments: ArgumentRepresentation;
    };
}

//
// 1. Utility function to unwrap NonNull and List until we get a "named" type
//
function getNamedType(type: GraphQLType): GraphQLNamedType {
    let namedType = type;
    while (isNonNullType(namedType) || isListType(namedType)) {
        namedType = (namedType as GraphQLNonNull<any> | GraphQLList<any>).ofType;
    }
    return namedType;
}

//
// 2. Recursively describe a type
//
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
            { type: ResolvedType; defaultValue: any; isList: boolean }
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

    // Fallback (e.g., unions, interfaces)
    return { type: 'Scalar', subtype: 'String' };
}

//
// 3. Main function to generate in-memory representation for "trip" query arguments
//
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
        const argDefaultValue = arg.defaultValue !== undefined ? arg.defaultValue : null;
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

//
// 4. Fetch the remote GraphQL schema via introspection
//    then convert it into an in-memory representation of trip arguments.
//
export async function fetchTripArgs(graphqlEndpointUrl: string): Promise<TripArgsRepresentation> {
    // 1. Perform introspection query
    const introspectionQuery = getIntrospectionQuery();

    // 2. Use browser's fetch (no Node needed)
    const response = await fetch(graphqlEndpointUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ query: introspectionQuery }),
    });

    if (!response.ok) {
        throw new Error(`Failed to fetch schema. HTTP error: ${response.status}`);
    }

    // 3. Parse JSON
    const { data } = await response.json();

    // 4. Build GraphQLSchema from introspection
    const schema = buildClientSchema(data);

    // 5. Generate and return the "trip" arguments representation
    return generateTripArgs(schema);
}
