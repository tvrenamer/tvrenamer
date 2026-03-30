export type Phase = 'idle' | 'searching' | 'ready' | 'renaming' | 'complete';

interface StatusBarProps {
  total: number;
  success: number;
  failed: number;
  phase: Phase;
}

const phaseLabel: Record<Phase, string> = {
  idle: 'Drop files to begin',
  searching: 'Looking up shows…',
  ready: 'Ready to rename',
  renaming: 'Renaming…',
  complete: 'Complete',
};

export function StatusBar({ total, success, failed, phase }: StatusBarProps) {
  return (
    <div
      style={{
        display: 'flex',
        gap: '16px',
        padding: '4px 8px',
        borderTop: '1px solid #ccc',
        fontSize: '0.85em',
      }}
    >
      <span>{total} files</span>
      {success > 0 && <span>{success} done</span>}
      {failed > 0 && <span>{failed} error{failed !== 1 ? 's' : ''}</span>}
      <span>{phaseLabel[phase]}</span>
    </div>
  );
}
