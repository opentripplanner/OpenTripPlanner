import React, { useEffect, useState } from 'react';
//import tripArgumentsData from '../../gql/query-arguments.json';
import { useTripSchema } from './TripSchemaContext';
import { TripQueryVariables } from '../../gql/graphql';
import { getNestedValue, setNestedValue } from './nestedUtils';
import ArgumentTooltip from './ArgumentTooltip.tsx';
import { excludedArguments } from './excluded-arguments.ts';

interface TripQueryArgumentsProps {
  tripQueryVariables: TripQueryVariables;
  setTripQueryVariables: (tripQueryVariables: TripQueryVariables) => void;
}

function formatArgumentName(input: string): string {
  if (!input) {
    return ' ';
  }
  const parts = input.split('.');
  const formatted = parts[parts.length - 1].replace(/([A-Z])/g, ' $1').trim();
  return formatted.replace(/\b\w/g, (char) => char.toUpperCase()) + ' ';
}
type ArgumentConfig = {
  path: string;
  type: string;
  defaultValue?: string;
  enumValues?: string[];
  isComplex?: boolean;
  isList?: boolean;
};

const TripQueryArguments: React.FC<TripQueryArgumentsProps> = ({ tripQueryVariables, setTripQueryVariables }) => {
  const [argumentsList, setArgumentsList] = useState<
    {
      path: string;
      type: string;
      subtype?: string;
      defaultValue?: string;
      enumValues?: string[];
      isComplex?: boolean;
      isList?: boolean;
    }[]
  >([]);
  const [expandedArguments, setExpandedArguments] = useState<{ [key: string]: boolean }>({});
  const [searchText] = useState('');

  const { tripArgs, loading, error } = useTripSchema();

  useEffect(() => {
    if (!tripArgs) return; // Don't run if the data isn't loaded yet
    if (loading || error) return; // Optionally handle error/loading

    // Example: tripArgs has shape { trip: { arguments: {} } }
    const extractedArgs = extractAllArgs(tripArgs.trip.arguments);
    setArgumentsList(extractedArgs);
  }, [tripArgs, loading, error]);

  /**useEffect(() => {
    const tripArgs = tripArgumentsData.trip.arguments;
    const extractedArgs = extractAllArgs(tripArgs);
    setArgumentsList(extractedArgs);
  }, []);
**/
  const extractAllArgs = (
    args: { [key: string]: any },
    parentPath: string[] = [],
  ): {
    path: string;
    type: string;
    name?: string;
    defaultValue?: string;
    enumValues?: string[];
    isComplex?: boolean;
    isList?: boolean;
  }[] => {
    let allArgs: {
      path: string;
      type: string;
      name?: string;
      defaultValue?: string;
      enumValues?: string[];
      isComplex?: boolean;
      isList?: boolean;
    }[] = [];

    Object.entries(args).forEach(([argName, argData]) => {
      const currentPath = [...parentPath, argName].join('.');
      allArgs = allArgs.concat(processArgument(argName, argData, currentPath, parentPath));
    });
    return allArgs;
  };

  const processArgument = (
    argName: string,
    argData: any,
    currentPath: string,
    parentPath: string[],
  ): {
    path: string;
    type: string;
    name?: string;
    defaultValue?: string;
    enumValues?: string[];
    isComplex?: boolean;
    isList?: boolean;
  }[] => {
    let allArgs: {
      path: string;
      type: string;
      name?: string;
      defaultValue?: string;
      enumValues?: string[];
      isComplex?: boolean;
      isList?: boolean;
    }[] = [];

    if (typeof argData === 'object' && argData.type) {
      if (argData.type.type === 'Enum') {
        const enumValues = ['Not selected', ...argData.type.values];
        const defaultValue = argData.defaultValue !== undefined ? argData.defaultValue : 'Not selected';
        allArgs.push({
          path: currentPath,
          type: 'Enum',
          defaultValue,
          enumValues,
          isList: argData.isList,
        });
      } else if (argData.type.type === 'InputObject' && argData.isList) {
        // This is a list of InputObjects
        allArgs.push({
          path: currentPath,
          type: 'Group', // We'll still call this 'Group'
          defaultValue: argData.defaultValue,
          isComplex: true,
          isList: true,
        });

        // NEW: Also extract subfields with a wildcard
        // e.g. for `accessEgressPenalty`, we'll get `accessEgressPenalty.*.costFactor`, etc.
        allArgs = allArgs.concat(extractAllArgs(argData.type.fields, [...parentPath, `${argName}.*`]));
      } else if (argData.type.type === 'InputObject') {
        // Single InputObject
        allArgs.push({
          path: currentPath,
          type: 'Group',
          isComplex: true,
          isList: false,
        });
        allArgs = allArgs.concat(extractAllArgs(argData.type.fields, [...parentPath, argName]));
      } else if (argData.type.type === 'Scalar') {
        allArgs.push({
          path: currentPath,
          type: argData.type.subtype,
          defaultValue: argData.defaultValue,
          isList: argData.isList,
        });
      }
    } else if (typeof argData === 'object' && argData.fields) {
      allArgs.push({ path: currentPath, type: 'Group', isComplex: true });
      allArgs = allArgs.concat(extractAllArgs(argData.fields, [...parentPath, argName]));
    } else {
      allArgs.push({ path: currentPath, type: argData.type ?? typeof argData, defaultValue: argData.defaultValue });
    }

    return allArgs;
  };

  const normalizePathForList = (path: string): string => {
    // Replace numeric segments with `*`
    return path.replace(/\.\d+/g, '.*');
  };

  const handleInputChange = (path: string, value: any) => {
    const normalizedPath = normalizePathForList(path); // Normalize the path to match `argumentsList`
    const argumentConfig = argumentsList.find((arg) => arg.path === normalizedPath);

    if (!argumentConfig) {
      console.error(`No matching argumentConfig found for path: ${path}`);
      return;
    }

    // Handle `ID` types with `isList=true`
    if (['String', 'DoubleFunction', 'ID', 'Duration'].includes(argumentConfig.type) && argumentConfig.isList) {
      if (typeof value === 'string') {
        // Convert comma-separated string into an array
        const idsArray = value.split(',').map((id) => id.trim()); // Remove whitespace

        // Update the `tripQueryVariables` with the array
        let updatedTripQueryVariables = setNestedValue(tripQueryVariables, path, idsArray);

        // Clean up parent structure if necessary
        updatedTripQueryVariables = cleanUpParentIfEmpty(updatedTripQueryVariables, path);
        setTripQueryVariables(updatedTripQueryVariables);
        return;
      }
    }

    // Default handling for other cases
    let updatedTripQueryVariables = setNestedValue(tripQueryVariables, path, value);

    updatedTripQueryVariables = cleanUpParentIfEmpty(updatedTripQueryVariables, path);
    setTripQueryVariables(updatedTripQueryVariables);
  };

  const cleanUpParentIfEmpty = (variables: any, path: string): any => {
    // Handle the case where `path` is top-level (no dots)
    if (!path.includes('.')) {
      const topValue = getNestedValue(variables, path);

      // If it’s an empty array, remove it entirely from `variables`
      if (Array.isArray(topValue) && topValue.length === 0) {
        const { [path]: _, ...rest } = variables;
        return rest;
      }

      // If it's an object and all keys are undefined/null or empty, remove it
      if (topValue && typeof topValue === 'object') {
        const allKeysEmpty = Object.keys(topValue).every((key) => {
          const childVal = topValue[key];
          return childVal === undefined || childVal === null || (Array.isArray(childVal) && childVal.length === 0);
        });
        if (allKeysEmpty) {
          const { [path]: _, ...rest } = variables;
          return rest;
        }
      }

      return variables; // Otherwise leave it as is
    }

    const pathParts = path.split('.');
    for (let i = pathParts.length - 1; i > 0; i--) {
      const parentPath = pathParts.slice(0, i).join('.');
      const parentValue = getNestedValue(variables, parentPath);

      if (parentValue == null) {
        // Already null or undefined, nothing to do
        continue;
      }

      if (Array.isArray(parentValue)) {
        // If the parent array is now empty, remove it
        if (parentValue.length === 0) {
          variables = setNestedValue(variables, parentPath, undefined);
        }
      } else if (typeof parentValue === 'object') {
        // If all child values are null/undefined or empty, remove the parent
        const allKeysEmpty = Object.keys(parentValue).every((key) => {
          const childPath = `${parentPath}.${key}`;
          const childValue = getNestedValue(variables, childPath);
          return (
            childValue === undefined || childValue === null || (Array.isArray(childValue) && childValue.length === 0)
          );
        });

        if (allKeysEmpty) {
          variables = setNestedValue(variables, parentPath, undefined);
        }
      }
    }

    return variables;
  };

  const toggleExpand = (path: string) => {
    setExpandedArguments((prev) => {
      const newState = { ...prev };
      newState[path] = !prev[path];
      return newState;
    });
  };

  const filteredArgumentsList = argumentsList
    .filter(({ path }) => formatArgumentName(path).toLowerCase().includes(searchText.toLowerCase()))
    .filter(({ path }) => !excludedArguments.has(path));

  const renderListOfInputObjects = (listPath: string, allArgs: ArgumentConfig[], level: number, tripArgs: any) => {
    const arrayVal = getNestedValue(tripQueryVariables, listPath) || [];

    // Dynamically determine the button label based on the type name
    const argumentsData = tripArgs as Record<
      string,
      {
        type: {
          type: string;
          name?: string;
          fields?: Record<string, any>;
        };
        defaultValue?: string;
        isList?: boolean;
      }
    >;

    const parentArg = argumentsList.find((arg) => arg.path === listPath);
    let typeName = 'Item'; // Default fallback

    if (parentArg?.type === 'Group' && argumentsData[listPath]?.type) {
      const typeDetails = argumentsData[listPath].type;
      if (typeDetails.type === 'InputObject' && typeDetails.name) {
        typeName = typeDetails.name; // Use the type name directly
      }
    }

    return (
      <div>
        {arrayVal.map((_item: unknown, index: number) => {
          const itemPath = `${listPath}.${index}`;

          const itemNestedArgs = allArgs
            .filter((arg) => arg.path.startsWith(`${listPath}.*.`) && arg.path !== `${listPath}.*`)
            .map((arg) => ({
              ...arg,
              path: arg.path.replace(`${listPath}.*`, itemPath),
            }));

          const immediateNestedArgs = itemNestedArgs.filter(
            (arg) => arg.path.split('.').length === itemPath.split('.').length + 1,
          );

          const isExpandedItem = expandedArguments[itemPath];

          return (
            <div key={itemPath} style={{ marginLeft: 15 }}>
              <span style={{ cursor: 'pointer' }} onClick={() => toggleExpand(itemPath)}>
                {isExpandedItem ? '▼ ' : '▶ '} [#{index + 1}]
              </span>
              <button style={{ marginLeft: 10 }} onClick={() => handleRemoveItem(listPath, index)}>
                Remove
              </button>

              {isExpandedItem && (
                <div style={{ marginLeft: 20 }}>
                  {renderArgumentInputs(immediateNestedArgs, level + 1, itemNestedArgs)}
                </div>
              )}
            </div>
          );
        })}

        {/* Add button with a dynamic name */}
        <button onClick={() => handleAddItem(listPath)}>+ Add {typeName}</button>
      </div>
    );
  };

  const handleAddItem = (listPath: string) => {
    const currentValue = getNestedValue(tripQueryVariables, listPath) || [];
    // Insert an empty object or a default shape
    const newValue = [...currentValue, {}];
    const updatedTripQueryVariables = setNestedValue(tripQueryVariables, listPath, newValue);
    setTripQueryVariables(updatedTripQueryVariables);
  };

  const handleRemoveItem = (listPath: string, index: number) => {
    const currentValue = getNestedValue(tripQueryVariables, listPath) || [];
    const newValue = currentValue.filter((_: any, i: number) => i !== index);
    let updatedTripQueryVariables = setNestedValue(tripQueryVariables, listPath, newValue);

    updatedTripQueryVariables = cleanUpParentIfEmpty(updatedTripQueryVariables, listPath);
    setTripQueryVariables(updatedTripQueryVariables);
  };

  const handleRemoveArgument = (path: string) => {
    let updatedTripQueryVariables = setNestedValue(tripQueryVariables, path, undefined);
    // Then let your cleanup function remove it if it’s an empty object/array
    updatedTripQueryVariables = cleanUpParentIfEmpty(updatedTripQueryVariables, path);
    setTripQueryVariables(updatedTripQueryVariables);
  };

  const renderArgumentInputs = (
    args: {
      path: string;
      type: string;
      defaultValue?: string;
      enumValues?: string[];
      isComplex?: boolean;
      isList?: boolean;
    }[],
    level: number,
    allArgs: {
      path: string;
      type: string;
      defaultValue?: string;
      enumValues?: string[];
      isComplex?: boolean;
      isList?: boolean;
    }[],
  ) => {
    return args.map(({ path, type, defaultValue, enumValues, isComplex, isList }) => {
      const isExpanded = expandedArguments[path];
      const currentDepth = path.split('.').length;
      const nestedArgs = allArgs.filter((arg) => {
        const argDepth = arg.path.split('.').length;
        return arg.path.startsWith(`${path}.`) && arg.path !== path && argDepth === currentDepth + 1;
      });
      const nestedLevel = level + 1;

      return (
        <div key={path} style={{ marginLeft: `${level * 15}px`, marginBottom: '10px' }}>
          {isComplex ? (
            <div>
              <span style={{ cursor: 'pointer' }} onClick={() => toggleExpand(path)}>
                {isExpanded ? '▼ ' : '▶ '} {formatArgumentName(path)}
              </span>

              {isExpanded && isList ? (
                <div style={{ marginLeft: 20 }}>{renderListOfInputObjects(path, allArgs, nestedLevel, tripArgs)}</div>
              ) : isExpanded ? (
                /* original single-object rendering */
                renderArgumentInputs(nestedArgs, nestedLevel, allArgs)
              ) : null}
            </div>
          ) : (
            <div>
              <label className="argument-label" htmlFor={path}>
                {formatArgumentName(path)} <ArgumentTooltip defaultValue={defaultValue} type={type} />
              </label>
              {type === 'Boolean' &&
                (() => {
                  const currentValue = getNestedValue(tripQueryVariables, path);
                  const isInUse = currentValue !== undefined;

                  return (
                    <span>
                      <input
                        type="checkbox"
                        id={path}
                        checked={currentValue ?? false}
                        onChange={(e) => handleInputChange(path, e.target.checked)}
                      />
                      {isInUse && (
                        <a onClick={() => handleRemoveArgument(path)} className={'remove-argument'}>
                          x
                        </a>
                      )}
                    </span>
                  );
                })()}

              {['String', 'DoubleFunction', 'ID', 'Duration'].includes(type) && isList && (
                <input
                  className={'comma-separated-input'}
                  type="text"
                  id={path}
                  value={(() => {
                    const currentValue = getNestedValue(tripQueryVariables, path);
                    return Array.isArray(currentValue) ? currentValue.join(', ') : ''; // Join array into a comma-separated string
                  })()}
                  onChange={(e) => handleInputChange(path, e.target.value)}
                  placeholder="Comma-separated list"
                />
              )}
              {['String', 'DoubleFunction', 'ID', 'Duration'].includes(type) && !isList && (
                <input
                  type="text"
                  id={path}
                  value={getNestedValue(tripQueryVariables, path) ?? ''}
                  onChange={(e) => handleInputChange(path, e.target.value || undefined)}
                />
              )}
              {type === 'Int' && (
                <input
                  type="number"
                  id={path}
                  value={getNestedValue(tripQueryVariables, path) ?? ''}
                  onChange={(e) => handleInputChange(path, parseInt(e.target.value, 10) || undefined)}
                />
              )}
              {type === 'Float' && (
                <input
                  type="number"
                  id={path}
                  step="any"
                  value={getNestedValue(tripQueryVariables, path) ?? ''}
                  onChange={(e) => handleInputChange(path, parseFloat(e.target.value) || undefined)}
                />
              )}

              {type === 'DateTime' && (
                <input
                  type="datetime-local"
                  id={path}
                  value={getNestedValue(tripQueryVariables, path)?.slice(0, 16) ?? ''}
                  onChange={(e) => {
                    const newValue = e.target.value ? new Date(e.target.value).toISOString() : undefined;
                    handleInputChange(path, newValue);
                  }}
                />
              )}
              {type === 'Enum' && enumValues && isList && (
                <select
                  id={path}
                  multiple
                  value={(() => {
                    const currentValue = getNestedValue(tripQueryVariables, path);
                    if (!Array.isArray(currentValue)) {
                      return [];
                    }
                    return currentValue; // Use the array directly
                  })()}
                  onChange={(e) => {
                    const selectedOptions = Array.from(e.target.selectedOptions, (option) => option.value);
                    handleInputChange(path, selectedOptions);
                  }}
                >
                  {enumValues.map((enumValue) => (
                    <option key={enumValue} value={enumValue}>
                      {enumValue}
                    </option>
                  ))}
                </select>
              )}

              {type === 'Enum' && enumValues && !isList && (
                <select
                  id={path}
                  value={getNestedValue(tripQueryVariables, path) ?? 'Not selected'}
                  onChange={(e) => {
                    handleInputChange(path, e.target.value || undefined);
                  }}
                >
                  {enumValues.map((enumValue) => (
                    <option key={enumValue} value={enumValue}>
                      {enumValue}
                    </option>
                  ))}
                </select>
              )}
            </div>
          )}
        </div>
      );
    });
  };

  const handleReset = () => {
    // Start with an empty object
    // @ts-ignore
    let newVars: TripQueryVariables = {};

    // For each path in our excluded set, copy over that value (if any)
    excludedArguments.forEach((excludedPath) => {
      const value = getNestedValue(tripQueryVariables, excludedPath);
      if (value !== undefined) {
        newVars = setNestedValue(newVars, excludedPath, value);
      }
    });

    // Now newVars contains only the excluded arguments from the old object
    setTripQueryVariables(newVars);
  };

  return (
    <div className={'left-pane-container below-content'}>
      <div className="panel-header">
        Filters
        <button className="reset-button" onClick={handleReset}>
          Reset
        </button>
      </div>
      {filteredArgumentsList.length === 0 ? (
        <p>No arguments found.</p>
      ) : (
        <div className={'argument-list'}>
          {renderArgumentInputs(
            filteredArgumentsList.filter((arg) => arg.path.split('.').length === 1),
            0,
            filteredArgumentsList,
          )}
        </div>
      )}
    </div>
  );
};

export default TripQueryArguments;
