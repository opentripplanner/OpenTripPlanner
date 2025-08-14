import { ChangeEvent, useState } from 'react';
import { Form } from 'react-bootstrap';

type MultiSelectDropdownOption<T> = {
  id: T;
  label: string;
};

type MultiSelectDropdownProps<T> = {
  label: string;
  options: MultiSelectDropdownOption<T>[];
  values: T[];
  onChange: (value: T[]) => void;
};

const MultiSelectDropdown = <T = unknown,>({ label, options, values, onChange }: MultiSelectDropdownProps<T>) => {
  const [isOpen, setIsOpen] = useState(false);

  const toggleDropdown = () => {
    setIsOpen(!isOpen);
  };

  const handleOptionChange = (event: ChangeEvent<HTMLInputElement>) => {
    const optionId = event.target.value as T;
    const isChecked = event.target.checked;

    if (isChecked) {
      onChange([...values, optionId]);
    } else {
      onChange(values.filter((id) => id !== optionId));
    }
  };

  return (
    <div className={`dropdown ${isOpen ? 'show' : ''}`}>
      <Form.Label column="sm" htmlFor="multiSelectDropdown">
        {label}
      </Form.Label>
      <Form.Control
        type="text"
        id="multiSelectDropdown"
        size="sm"
        className="input-medium"
        value={values.length > 0 ? values.join(', ') : 'Not selected'}
        onClick={toggleDropdown}
        onChange={() => {}}
      />
      <div style={{ width: '20%' }} className={`dropdown-menu ${isOpen ? 'show' : ''}`}>
        {options.map((option) => (
          <Form.Check
            style={{ marginLeft: '10%' }}
            key={`${option.id}`}
            type="checkbox"
            id={`option_${option.id}`}
            label={option.label}
            checked={values.includes(option.id)}
            onChange={handleOptionChange}
            value={`${option.id}`}
          />
        ))}
      </div>
    </div>
  );
};

export default MultiSelectDropdown;
