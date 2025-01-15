const {
  isScalarType,
  isInputObjectType,
  isNonNullType,
  isListType,
  isEnumType,
} = require('graphql');

/**
 * Utility function to resolve the named type (unwrapping NonNull and List types)
 */
function getNamedType(type) {
  let namedType = type;
  while (isNonNullType(namedType) || isListType(namedType)) {
    namedType = namedType.ofType;
  }
  return namedType;
}

/**
 * Recursively breaks down a GraphQL type into its primitive fields with default values
 */
function resolveType(type, schema = new Set()) {
  const namedType = getNamedType(type);


  if (isScalarType(namedType)) {
    return { type: 'Scalar', subtype: namedType.name };
  }

  if (isEnumType(namedType)) {
    return { type: 'Enum', values: namedType.getValues().map((val) => val.name) };
  }

  if (isInputObjectType(namedType)) {
    const fields = namedType.getFields();
    const fieldTypes = {};

        Object.keys(fields).forEach((fieldName) => {
      const field = fields[fieldName];

      // Exclude deprecated fields within input objects
      if (field.deprecationReason) {
        return; // Skip deprecated fields
      }

      const fieldType = field.type;
      const isList = isListType(fieldType); // Detect if the field is a list
      const fieldDefaultValue = field.defaultValue !== undefined ? field.defaultValue : null;

      // Include defaultValue consistently, setting it to null if not defined
      fieldTypes[fieldName] = {
        type: resolveType(fieldType, schema),
        defaultValue: fieldDefaultValue,
        isList, // Explicitly indicate if it's a list
      };
    });
    return { type: 'InputObject', name: namedType.name, fields: fieldTypes };
  }

  // Handle interfaces and unions if necessary
  // For simplicity, treating them as strings
  return { type: 'Scalar', subtype: 'String' };
}

/**
 * Plugin to generate a JSON file with all arguments from a specified query,
 * excluding deprecated arguments based on deprecationReason,
 * and including their types, default values,
 * and whether they support multiple selection.
 */
const generateTripArgsJsonPlugin = async (schema) => {
  try {
    const queryType = schema.getQueryType();
    if (!queryType) {
      console.error('No Query type found in the schema.');
      return JSON.stringify({ error: 'No Query type found in the schema' }, null, 2);
    }

    const tripField = queryType.getFields()['trip'];
    if (!tripField) {
      console.error('No trip query found in the schema.');
      return JSON.stringify({ error: 'No trip query found in the schema' }, null, 2);
    }

    const args = tripField.args;
    const argsJson = {};

    args.forEach((arg) => {
      if (arg.deprecationReason) {
        return; // Skip deprecated arguments
      }

            const argName = arg.name;
      const argType = resolveType(arg.type, schema);
      const argDefaultValue = arg.defaultValue !== undefined ? arg.defaultValue : null;
      const isList = isListType(arg.type); // Detect if the argument is a list

      // Consistent representation for enum types
      if (argDefaultValue !== null) {
        argsJson[argName] = {
          type: argType,
          defaultValue: argDefaultValue,
          isList, // Explicitly indicate if it's a list
        };
      } else {
        argsJson[argName] = {
          type: argType,
          isList, // Explicitly indicate if it's a list
        };
      }
    });

    const output = {
      trip: {
        arguments: argsJson,
      },
    };

    // Stringify the JSON with indentation for readability
    return JSON.stringify(output, null, 2);
  } catch (error) {
    console.error('Error generating tripArguments.json:', error);
    return JSON.stringify({ error: 'Failed to generate trip arguments JSON' }, null, 2);
  }
};

module.exports = {
  plugin: generateTripArgsJsonPlugin,
};
