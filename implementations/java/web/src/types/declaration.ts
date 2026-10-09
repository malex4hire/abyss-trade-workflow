export type DeclarationStatus = 'draft' | 'submitted' | 'accepted' | 'rejected';

export interface DeclarationData {
  importer: string;
  originCountry: string;
  commodityCode: string;
  declaredValueCents: number;
  currency: 'USD';
  shipmentReference: string;
}

export interface DeclarationResult {
  id: string;
  status: DeclarationStatus;
}

export interface AuditEntry {
  action: string;
  detail: string;
  at: string;
}

export interface TradeDeclaration extends DeclarationResult {
  reason: string;
  data: DeclarationData;
  createdAt: string;
  audit?: AuditEntry[];
}

// Form values differ from API data so a partially edited amount stays editable.
export interface DeclarationFormValues {
  importer: string;
  originCountry: string;
  commodityCode: string;
  declaredValue: string;
  shipmentReference: string;
}
