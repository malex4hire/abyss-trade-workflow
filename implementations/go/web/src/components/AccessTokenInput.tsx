interface Props {
  token: string;
  disabled: boolean;
  onChange: (token: string) => void;
}

export function AccessTokenInput({ token, disabled, onChange }: Props) {
  return (
    <div className="access">
      <label>
        Demo access token
        <input
          type="password"
          autoComplete="off"
          value={token}
          disabled={disabled}
          onChange={(event) => onChange(event.target.value)}
          placeholder="Enter token from .env"
        />
      </label>
      <a href={`http://${window.location.hostname}:18081`} target="_blank" rel="noreferrer">
        API documentation ↗
      </a>
    </div>
  );
}
