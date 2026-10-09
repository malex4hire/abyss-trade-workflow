import type { TradeDeclaration } from '../types/declaration';
import { AuditTrail } from './AuditTrail';

interface Props {
  declaration: TradeDeclaration;
  busy: boolean;
  onSubmit: () => void;
}

export function DeclarationDetails({ declaration, busy, onSubmit }: Props) {
  const message =
    declaration.reason ||
    (declaration.status === 'draft'
      ? 'Ready for submission. Processing continues asynchronously.'
      : 'Submitted. Waiting for the background worker to process this declaration.');
  return (
    <div className="detail">
      <h3>{declaration.data.shipmentReference}</h3>
      <code>{declaration.id}</code>
      <p role="status">{message}</p>
      {declaration.status === 'draft' && (
        <button type="button" disabled={busy} onClick={onSubmit}>
          Submit declaration →
        </button>
      )}
      <AuditTrail entries={declaration.audit ?? []} />
    </div>
  );
}
