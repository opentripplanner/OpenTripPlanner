import { ResolvedType } from './useTripArgs.ts';

export type DefaultValue = string | number | boolean | object | null;

interface ArgData {
  type: ResolvedType;
  name?: string;
  defaultValue?: DefaultValue;
  enumValues?: string[];
  isComplex?: boolean;
  isList?: boolean;
  args?: Record<string, ArgData>; // Recursive for nested arguments
}

export interface ProcessedArgument {
  path: string;
  type: ResolvedType;
  name?: string;
  defaultValue?: DefaultValue;
  enumValues?: string[];
  isComplex?: boolean;
  isList?: boolean;
}
/**
 * Returns a human-readable name from a path like "someNestedArg.subArg".
 */
export function formatArgumentName(input: string): string {
  if (!input) {
    return ' ';
  }
  const parts = input.split('.');
  const formatted = parts[parts.length - 1].replace(/([A-Z])/g, ' $1').trim();
  return formatted.replace(/\b\w/g, (char) => char.toUpperCase()) + ' ';
}
/**
 * Recursively extracts a flat list of arguments (ProcessedArgument[]).
 */
export function extractAllArgs(
  args: Record<string, ArgData> | undefined,
  parentPath: string[] = [],
): ProcessedArgument[] {
  let allArgs: ProcessedArgument[] = [];
  if (!args) return [];

  Object.entries(args).forEach(([argName, argData]) => {
    const currentPath = [...parentPath, argName].join('.');
    allArgs = allArgs.concat(processArgument(argName, argData, currentPath, parentPath));
  });

  return allArgs;
}

/**
 * Converts a single ArgData into one or more ProcessedArgument entries.
 * If the argData is an InputObject with nested fields, we recurse.
 */
function processArgument(
  argName: string,
  argData: ArgData,
  currentPath: string,
  parentPath: string[],
): ProcessedArgument[] {
  let allArgs: ProcessedArgument[] = [];

  if (typeof argData === 'object' && argData.type) {
    if (argData.type.type === 'Enum') {
      const enumValues = ['Not selected', ...(argData.type.values || [])];
      const defaultValue = argData.defaultValue !== undefined ? argData.defaultValue : 'Not selected';

      allArgs.push({
        path: currentPath,
        type: { type: 'Enum' },
        defaultValue,
        enumValues,
        isList: argData.isList,
      });
    } else if (argData.type.type === 'InputObject' && argData.isList) {
      // This is a list of InputObjects
      allArgs.push({
        path: currentPath,
        type: { type: 'Group', name: argData.type.name }, // We'll still call this 'Group'
        defaultValue: argData.defaultValue,
        isComplex: true,
        isList: true,
      });

      allArgs = allArgs.concat(extractAllArgs(argData.type.fields, [...parentPath, `${argName}.*`]));
    } else if (argData.type.type === 'InputObject') {
      // Single InputObject
      allArgs.push({
        path: currentPath,
        type: { type: 'Group', name: argData.type.name },
        isComplex: true,
        isList: false,
      });
      allArgs = allArgs.concat(extractAllArgs(argData.type.fields, [...parentPath, argName]));
    } else if (argData.type.type === 'Scalar') {
      allArgs.push({
        path: currentPath,
        type: { type: argData.type.type, subtype: argData.type.subtype },
        defaultValue: argData.defaultValue,
        isList: argData.isList,
      });
    }
  } else if (typeof argData === 'object' && argData.type?.fields) {
    // Possibly a nested object with fields
    allArgs.push({
      path: currentPath,
      type: { type: 'Group' },
      isComplex: true,
    });
    allArgs = allArgs.concat(extractAllArgs(argData.type.fields, [...parentPath, argName]));
  } else {
    // Fallback case
    allArgs.push({
      path: currentPath,
      type: argData.type ?? (typeof argData as unknown), // <— If argData.type is missing, fallback
      defaultValue: argData.defaultValue,
    });
  }

  return allArgs;
}
