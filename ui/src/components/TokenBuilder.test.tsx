import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { TokenBuilder } from './TokenBuilder';

describe('TokenBuilder', () => {
  it('renders the current mask in the input field', () => {
    render(<TokenBuilder value="%S [%sx%0e] %t" onChange={vi.fn()} />);
    const input = screen.getByRole('textbox');
    expect((input as HTMLInputElement).value).toBe('%S [%sx%0e] %t');
  });

  it('calls onChange when the mask input is edited directly', () => {
    const onChange = vi.fn();
    render(<TokenBuilder value="%S" onChange={onChange} />);
    fireEvent.change(screen.getByRole('textbox'), { target: { value: '%S - %t' } });
    expect(onChange).toHaveBeenCalledWith('%S - %t');
  });

  it('renders all 4 available tokens as buttons', () => {
    render(<TokenBuilder value="" onChange={vi.fn()} />);
    // Use exact case for %S and %s since they differ only by case
    expect(screen.getByRole('button', { name: /%S/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /%s/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /%0e/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /%t/i })).toBeInTheDocument();
  });

  it('appends token to mask when token button is clicked', () => {
    const onChange = vi.fn();
    render(<TokenBuilder value="%S " onChange={onChange} />);
    fireEvent.click(screen.getByRole('button', { name: /%t/i }));
    expect(onChange).toHaveBeenCalledWith('%S %t');
  });

  it('shows a live preview of the mask', () => {
    render(<TokenBuilder value="%S [%sx%0e] %t" onChange={vi.fn()} />);
    // Preview uses hardcoded example values: Show Name, season 1, ep 1, Episode Title
    expect(screen.getByText(/Show Name \[1x01\] Episode Title/)).toBeInTheDocument();
  });
});
