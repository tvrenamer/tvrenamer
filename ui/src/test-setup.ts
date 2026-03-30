import '@testing-library/jest-dom';

// Mock the Tauri API — not available in jsdom test environment
vi.mock('@tauri-apps/api/core', () => ({
  invoke: vi.fn(),
}));

vi.mock('@tauri-apps/api/event', () => ({
  listen: vi.fn().mockResolvedValue(() => {}), // returns unlisten fn
}));
