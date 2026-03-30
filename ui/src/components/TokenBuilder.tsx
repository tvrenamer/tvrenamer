import { applyTemplate } from '../types';

const TOKENS = [
  { token: '%S', description: 'Show name' },
  { token: '%s', description: 'Season' },
  { token: '%0e', description: 'Episode (padded)' },
  { token: '%t', description: 'Episode title' },
] as const;

interface TokenBuilderProps {
  value: string;
  onChange: (newMask: string) => void;
}

/**
 * Rename template builder: editable text input showing the current mask,
 * token buttons that append tokens when clicked, and a live preview.
 */
export function TokenBuilder({ value, onChange }: TokenBuilderProps) {
  const preview = applyTemplate(value, 'Show Name', 1, 1, 'Episode Title');

  return (
    <div>
      <div style={{ marginBottom: '8px' }}>
        {TOKENS.map(({ token, description }) => (
          <button
            key={token}
            type="button"
            title={description}
            onClick={() => onChange(value + token)}
            style={{ marginRight: '4px' }}
          >
            {token}
          </button>
        ))}
      </div>

      <input
        type="text"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        style={{ width: '100%', fontFamily: 'monospace' }}
        aria-label="Rename mask"
      />

      <p style={{ marginTop: '4px', color: '#666', fontSize: '0.85em' }}>
        Preview: <em>{preview}</em>
      </p>
    </div>
  );
}
