import { useState } from 'react';
import { invoke } from '@tauri-apps/api/core';

interface ApiKeySetupProps {
  onDismiss: () => void;
  onSaved: () => void;
}

type TestState = 'idle' | 'testing' | 'valid' | 'error';

/**
 * First-launch TMDB API key onboarding modal.
 * Step 1: Explain why the key is needed.
 * Step 2: Direct link to https://www.themoviedb.org/settings/api
 * Step 3: Input field + "Test" button → validate_tmdb_key → on success show "Save" button.
 * Non-blocking: "Skip" dismisses without entering a key.
 */
export function ApiKeySetup({ onDismiss, onSaved }: ApiKeySetupProps) {
  const [key, setKey] = useState('');
  const [testState, setTestState] = useState<TestState>('idle');
  const [errorMessage, setErrorMessage] = useState('');

  async function handleTest() {
    setTestState('testing');
    setErrorMessage('');
    try {
      await invoke('validate_tmdb_key', { key });
      setTestState('valid');
    } catch (e) {
      setTestState('error');
      setErrorMessage(String(e));
    }
  }

  async function handleSave() {
    await invoke('save_tmdb_key', { key });
    onSaved();
  }

  return (
    <div role="dialog" aria-modal="true" style={{ padding: '24px', maxWidth: '480px' }}>
      <h2>TMDB API Key Required</h2>
      <p>
        TVRenamer uses the TMDB API to look up show and episode information.
        You need a free API key from{' '}
        <a href="https://www.themoviedb.org/settings/api" target="_blank" rel="noreferrer">
          themoviedb.org
        </a>
        .
      </p>

      <div style={{ marginTop: '16px' }}>
        <label htmlFor="api-key-input">Enter your key:</label>
        <input
          id="api-key-input"
          type="text"
          value={key}
          onChange={(e) => { setKey(e.target.value); setTestState('idle'); }}
          placeholder="Paste your API key here"
          style={{ display: 'block', width: '100%', marginTop: '8px' }}
        />
      </div>

      {errorMessage && (
        <p style={{ color: 'red', marginTop: '8px' }}>{errorMessage}</p>
      )}

      {testState === 'valid' && (
        <p style={{ color: 'green', marginTop: '8px' }}>Key is valid!</p>
      )}

      <div style={{ marginTop: '16px', display: 'flex', gap: '8px' }}>
        <button onClick={handleTest} disabled={!key || testState === 'testing'}>
          {testState === 'testing' ? 'Testing…' : 'Test'}
        </button>

        {testState === 'valid' && (
          <button onClick={handleSave}>Save</button>
        )}

        <button onClick={onDismiss}>Skip</button>
      </div>
    </div>
  );
}
