import { useState } from 'react';
import type { FormEvent } from 'react';
import { errorMessage } from '../api/client';
import { initialDeclarationForm, toDeclarationData } from '../forms/declaration';
import type {
  DeclarationData,
  DeclarationFormValues,
  DeclarationResult,
} from '../types/declaration';

interface Props {
  disabled: boolean;
  onSave: (data: DeclarationData) => Promise<DeclarationResult | undefined>;
}

export function DeclarationForm({ disabled, onSave }: Props) {
  const [form, setForm] = useState<DeclarationFormValues>(initialDeclarationForm);
  const [validationError, setValidationError] = useState<string | null>(null);

  function update(field: keyof DeclarationFormValues, value: string) {
    setForm((previous) => ({ ...previous, [field]: value }));
    setValidationError(null);
  }

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (disabled) return;
    setValidationError(null);
    try {
      await onSave(toDeclarationData(form));
    } catch (error) {
      setValidationError(errorMessage(error));
    }
  }

  return (
    <section>
      <small>01 · ENTER INFORMATION</small>
      <h2>New declaration</h2>
      <form onSubmit={(event) => void save(event)}>
        <fieldset disabled={disabled}>
          <label>
            Importer
            <input
              required
              maxLength={120}
              value={form.importer}
              onChange={(event) => update('importer', event.target.value)}
            />
          </label>
          <label>
            Origin country (2 letters)
            <input
              required
              pattern="[A-Z]{2}"
              maxLength={2}
              value={form.originCountry}
              onChange={(event) => update('originCountry', event.target.value)}
            />
          </label>
          <label>
            Commodity code (6 digits)
            <input
              required
              pattern="[0-9]{6}"
              maxLength={6}
              value={form.commodityCode}
              onChange={(event) => update('commodityCode', event.target.value)}
            />
          </label>
          <label>
            Declared value (USD)
            <input
              required
              type="number"
              min="0.01"
              max="1000000000"
              step="0.01"
              value={form.declaredValue}
              onChange={(event) => update('declaredValue', event.target.value)}
            />
          </label>
          <label>
            Shipment reference
            <input
              required
              maxLength={80}
              value={form.shipmentReference}
              onChange={(event) => update('shipmentReference', event.target.value)}
            />
          </label>
          {validationError && (
            <p className="error" role="alert">
              {validationError}
            </p>
          )}
          <button type="submit">Save draft</button>
        </fieldset>
      </form>
    </section>
  );
}
