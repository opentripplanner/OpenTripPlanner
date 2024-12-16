import React, { useEffect, useState } from 'react';
import tripArgumentsData from '../../gql/query-arguments.json';
import { TripQueryVariables } from '../../gql/graphql';
import { getNestedValue, setNestedValue } from './nestedUtils';
import DefaultValueTooltip from './DefaultValueTooltip.tsx';

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
  return formatted.replace(/\b\w/g, (char) => char.toUpperCase())+' ';
}

const TripQueryArguments: React.FC<TripQueryArgumentsProps> = ({ tripQueryVariables, setTripQueryVariables }) => {
  const [argumentsList, setArgumentsList] = useState<
    { path: string; type: string; subtype?: string; defaultValue: any; enumValues?: string[]; isComplex?: boolean }[]
  >([]);
  const [expandedArguments, setExpandedArguments] = useState<{ [key: string]: boolean }>({});
  const [searchText, setSearchText] = useState('');

  useEffect(() => {
    const tripArgs = tripArgumentsData.trip.arguments;
    const extractedArgs = extractAllArgs(tripArgs);
    setArgumentsList(extractedArgs);
  }, []);

  const extractAllArgs = (
    args: { [key: string]: any },
    parentPath: string[] = [],
  ): { path: string; type: string; name?: string; defaultValue: any; enumValues?: string[]; isComplex?: boolean }[] => {
    let allArgs: {
      path: string;
      type: string;
      name?: string;
      defaultValue: any;
      enumValues?: string[];
      isComplex?: boolean;
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
  ): { path: string; type: string; name?: string; defaultValue: any; enumValues?: string[]; isComplex?: boolean }[] => {
    let allArgs: {
      path: string;
      type: string;
      name?: string;
      defaultValue: any;
      enumValues?: string[];
      isComplex?: boolean;
    }[] = [];

    if (typeof argData === 'object' && argData.type) {
      if (argData.type.type === 'Enum') {
        const enumValues = ['Not selected', ...argData.type.values];
        const defaultValue = argData.defaultValue !== undefined ? argData.defaultValue : 'Not selected';
        allArgs.push({ path: currentPath, type: 'Enum', defaultValue, enumValues });
      } else if (argData.type.type === 'InputObject' && argData.type.fields) {
        allArgs.push({ path: currentPath, type: 'Group', defaultValue: null, isComplex: true });
        allArgs = allArgs.concat(extractAllArgs(argData.type.fields, [...parentPath, argName]));
      } else if (argData.type.type === 'Scalar') {
        allArgs.push({ path: currentPath, type: argData.type.subtype, defaultValue: argData.defaultValue });
      }
    } else if (typeof argData === 'object' && argData.fields) {
      allArgs.push({ path: currentPath, type: 'Group', defaultValue: null, isComplex: true });
      allArgs = allArgs.concat(extractAllArgs(argData.fields, [...parentPath, argName]));
    } else {
      allArgs.push({ path: currentPath, type: argData.type ?? typeof argData, defaultValue: argData.defaultValue });
    }

    return allArgs;
  };

  const handleInputChange = (path: string, value: any) => {
    if (typeof value === 'number' && isNaN(value)) {
      value = null;
    }
    let updatedTripQueryVariables = setNestedValue(tripQueryVariables, path, value);
    if (
      value === undefined ||
      value === null ||
      value === argumentsList.find((arg) => arg.path === path)?.defaultValue
    ) {
      updatedTripQueryVariables = setNestedValue(updatedTripQueryVariables, path, undefined);
      updatedTripQueryVariables = cleanUpParentIfEmpty(updatedTripQueryVariables, path);
    }
    setTripQueryVariables(updatedTripQueryVariables);
  };

  const cleanUpParentIfEmpty = (variables: any, path: string): any => {
    const pathParts = path.split('.');
    for (let i = pathParts.length - 1; i > 0; i--) {
      const parentPath = pathParts.slice(0, i).join('.');
      const parentValue = getNestedValue(variables, parentPath);

      if (parentValue && typeof parentValue === 'object') {
        const allKeysUndefinedOrDefault = Object.keys(parentValue).every((key) => {
          const childPath = `${parentPath}.${key}`;
          const childValue = getNestedValue(variables, childPath);
          const defaultValue = argumentsList.find((arg) => arg.path === childPath)?.defaultValue;
          return childValue === undefined || childValue === null || childValue === defaultValue;
        });

        if (allKeysUndefinedOrDefault) {
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

  const filteredArgumentsList = argumentsList.filter(({ path }) =>
    formatArgumentName(path).toLowerCase().includes(searchText.toLowerCase()),
  );

  const renderArgumentInputs = (
    args: { path: string; type: string; defaultValue: any; enumValues?: string[]; isComplex?: boolean }[],
    level: number,
  ) => {
    return args.map(({ path, type, defaultValue, enumValues, isComplex }) => {
      const isExpanded = expandedArguments[path];
      const nestedArgs = argumentsList.filter((arg) => arg.path.startsWith(`${path}.`) && arg.path !== path);
      const nestedLevel = level + 1;

      return (
        <div key={path} style={{ marginLeft: `${level * 15}px`, marginBottom: '10px' }}>
          {isComplex ? (
            <div>
              <span
                style={{ cursor: 'pointer', color: '#007bff', textDecoration: 'underline' }}
                onClick={() => toggleExpand(path)}
              >
                {formatArgumentName(path)} {isExpanded ? '▼' : '▶'}
              </span>
              {isExpanded && renderArgumentInputs(nestedArgs, nestedLevel)}
            </div>
          ) : (
            <div>
              <label htmlFor={path}>
                {formatArgumentName(path)}
                {defaultValue !== undefined && defaultValue !== null && (
                  <DefaultValueTooltip defaultValue={defaultValue} />
                )}
              </label>
              {type === 'Boolean' && (
                <input
                  type="checkbox"
                  id={path}
                  checked={getNestedValue(tripQueryVariables, path) ?? false}
                  onChange={(e) => handleInputChange(path, e.target.checked)}
                />
              )}
              {['String', 'DoubleFunction', 'ID'].includes(type) && (
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
              {type === 'Enum' && enumValues && (
                <select
                  id={path}
                  value={getNestedValue(tripQueryVariables, path) ?? 'Not selected'}
                  onChange={(e) => {
                    const newValue = e.target.value || undefined;
                    handleInputChange(path, newValue);
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

  return (
    <div style={{ padding: '10px', maxWidth: '800px' }}>
      <div style={{ marginBottom: '15px' }}>
        <input
          type="text"
          placeholder="Search arguments..."
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
          style={{ width: '100%', padding: '8px', fontSize: '16px' }}
        />
      </div>
      {filteredArgumentsList.length === 0 ? (
        <p>No arguments found.</p>
      ) : (
        <div>
          {renderArgumentInputs(
            filteredArgumentsList.filter((arg) => arg.path.split('.').length === 1),
            0,
          )}
        </div>
      )}
    </div>
  );
};

export default TripQueryArguments;
