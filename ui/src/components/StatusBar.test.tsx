import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { StatusBar } from './StatusBar';

describe('StatusBar', () => {
  it('shows total file count', () => {
    render(<StatusBar total={5} success={0} failed={0} phase="idle" />);
    expect(screen.getByText(/5 files/i)).toBeInTheDocument();
  });

  it('shows success and fail counts during/after rename', () => {
    render(<StatusBar total={5} success={3} failed={1} phase="renaming" />);
    expect(screen.getByText(/3 done/i)).toBeInTheDocument();
    expect(screen.getByText(/1 error/i)).toBeInTheDocument();
  });

  it('shows "Ready to rename" when all rows are ready', () => {
    render(<StatusBar total={3} success={0} failed={0} phase="ready" />);
    expect(screen.getByText(/ready to rename/i)).toBeInTheDocument();
  });

  it('shows "Complete" when rename is done', () => {
    render(<StatusBar total={3} success={3} failed={0} phase="complete" />);
    expect(screen.getByText(/complete/i)).toBeInTheDocument();
  });

  it('renders nothing visible when no files loaded', () => {
    render(<StatusBar total={0} success={0} failed={0} phase="idle" />);
    expect(screen.getByText(/0 files/i)).toBeInTheDocument();
  });
});
