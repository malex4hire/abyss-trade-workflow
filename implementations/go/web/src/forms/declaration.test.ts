import { describe, expect, it } from 'vitest';
import { initialDeclarationForm, toDeclarationData } from './declaration';

describe('form-to-API conversion', () => {
  it('converts decimal USD exactly to integer cents', () => {
    expect(
      toDeclarationData({ ...initialDeclarationForm, declaredValue: '1.01' }).declaredValueCents,
    ).toBe(101);
    expect(
      toDeclarationData({ ...initialDeclarationForm, declaredValue: '0.29' }).declaredValueCents,
    ).toBe(29);
    expect(
      toDeclarationData({ ...initialDeclarationForm, declaredValue: '12' }).declaredValueCents,
    ).toBe(1200);
  });
  it.each(['-1', '0', '1.001', '', 'NaN', '1e5', '1000000000.01'])(
    'rejects invalid amount %s',
    (declaredValue) => {
      expect(() => toDeclarationData({ ...initialDeclarationForm, declaredValue })).toThrow();
    },
  );
});
