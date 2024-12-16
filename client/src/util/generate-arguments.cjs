const fs = require('fs');
const path = require('path');
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

  // Debug: Log the named type
  console.log('Resolving type:', namedType.name);

  if (isScalarType(namedType)) {
    return { type: 'Scalar', subtype: namedType.name };
  }

  if (isEnumType(namedType)) {
    // Debug: Log enum type values
    console.log('Enum type detected:', namedType.name, 'Values:', namedType.getValues().map((val) => val.name));
    // Return enum type explicitly
    return { type: 'Enum', values: namedType.getValues().map((val) => val.name) };
  }

  if (isInputObjectType(namedType)) {
    const fields = namedType.getFields();
    const fieldTypes = {};

    // Debug: Log the fields of the input object type
    console.log('Input object type detected:', namedType.name, 'Fields:', Object.keys(fields));

    Object.keys(fields).forEach((fieldName) => {
      const field = fields[fieldName];

      // Exclude deprecated fields within input objects
      if (field.deprecationReason) {
        return; // Skip deprecated fields
      }

      const fieldType = field.type;
      const fieldDefaultValue = field.defaultValue !== undefined ? field.defaultValue : null;

      // Include defaultValue consistently, setting it to null if not defined
      fieldTypes[fieldName] = {
        type: resolveType(fieldType, schema),
        defaultValue: fieldDefaultValue
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
 * and including their types and default values,
 * breaking down complex types into primitives.
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

      // Debug: Log each argument being processed
      console.log('Processing argument:', arg.name, 'Type:', arg.type.toString());

      const argName = arg.name;
      const argType = resolveType(arg.type, schema);
      const argDefaultValue = arg.defaultValue !== undefined ? arg.defaultValue : null;

      // Consistent representation for enum types
      if (argDefaultValue !== null) {
        argsJson[argName] = {
          type: argType,
          defaultValue: argDefaultValue,
        };
      } else {
        argsJson[argName] = {
          type: argType,
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
