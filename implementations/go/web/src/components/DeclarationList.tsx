import type { TradeDeclaration } from '../types/declaration';
import { formatCurrency } from '../format';

interface Props {
  declarations: TradeDeclaration[];
  selectedId: string | null;
  loading: boolean;
  onSelect: (id: string) => void;
}

export function DeclarationList({ declarations, selectedId, loading, onSelect }: Props) {
  return (
    <div className="records" aria-busy={loading}>
      {declarations.length === 0 && (
        <p className="muted">
          {loading ? 'Loading declarations…' : 'Save your first draft to begin.'}
        </p>
      )}
      {declarations.map((declaration) => (
        <button
          type="button"
          className={`record ${selectedId === declaration.id ? 'active' : ''}`}
          key={declaration.id}
          aria-pressed={selectedId === declaration.id}
          onClick={() => onSelect(declaration.id)}
        >
          <strong>{declaration.data.shipmentReference}</strong>
          <span className={`badge ${declaration.status}`}>{declaration.status}</span>
          <small>
            {declaration.data.importer} · {declaration.data.originCountry} ·{' '}
            {formatCurrency(declaration.data.declaredValueCents)}
          </small>
        </button>
      ))}
    </div>
  );
}
