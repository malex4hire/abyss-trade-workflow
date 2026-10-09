import type { DeclarationData, DeclarationFormValues } from '../types/declaration';

export const initialDeclarationForm: DeclarationFormValues = {
  importer: 'Abyss Demo Imports',
  originCountry: 'CA',
  commodityCode: '847130',
  declaredValue: '1250.00',
  shipmentReference: 'SHIP-001',
};

export function toDeclarationData(form: DeclarationFormValues): DeclarationData {
  // Parse decimal digits directly, avoiding binary floating-point rounding.
  const match = /^(\d+)(?:\.(\d{1,2}))?$/.exec(form.declaredValue.trim());
  if (!match) throw new Error('Enter a positive USD amount with at most two decimal places');
  const cents = BigInt(match[1]) * 100n + BigInt((match[2] ?? '').padEnd(2, '0'));
  if (cents < 1n || cents > 100000000000n) {
    throw new Error('Declared value must be between $0.01 and $1,000,000,000.00');
  }
  return {
    importer: form.importer,
    originCountry: form.originCountry,
    commodityCode: form.commodityCode,
    declaredValueCents: Number(cents),
    currency: 'USD',
    shipmentReference: form.shipmentReference,
  };
}
