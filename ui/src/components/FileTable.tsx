import {
  createColumnHelper,
  flexRender,
  getCoreRowModel,
  useReactTable,
} from '@tanstack/react-table';
import type { FileRow, RowStatus } from '../types';

interface FileTableProps {
  rows: FileRow[];
  selectedIds: Set<string>;
  onSelectionChange: (id: string, checked: boolean) => void;
  onSeriesChange: (rowId: string, seriesId: number) => void;
}

const columnHelper = createColumnHelper<FileRow>();

function basename(path: string): string {
  return path.split('/').pop() ?? path;
}

function formatSeriesOption(s: { id: number; name: string; first_air_date: string | null }): string {
  return s.first_air_date ? `${s.name} (${s.first_air_date})` : s.name;
}

function StatusBadge({ status }: { status: RowStatus }) {
  const labels: Record<RowStatus, string> = {
    idle: '–',
    searching: 'Searching…',
    ready: '✓',
    no_match: 'No match',
    parse_failed: 'Parse failed',
    renaming: 'Renaming…',
    success: 'Done',
    error: 'Error',
  };
  return <span data-status={status}>{labels[status]}</span>;
}

export function FileTable({ rows, selectedIds, onSelectionChange, onSeriesChange }: FileTableProps) {
  const columns = [
    // Column 1: Checkbox (30px)
    columnHelper.display({
      id: 'select',
      size: 30,
      header: () => (
        <input
          type="checkbox"
          checked={rows.length > 0 && rows.every((r) => selectedIds.has(r.id))}
          onChange={(e) => rows.forEach((r) => onSelectionChange(r.id, e.target.checked))}
          aria-label="Select all"
        />
      ),
      cell: ({ row }) => (
        <input
          type="checkbox"
          checked={selectedIds.has(row.original.id)}
          onChange={(e) => onSelectionChange(row.original.id, e.target.checked)}
          aria-label={`Select ${basename(row.original.sourcePath)}`}
        />
      ),
    }),

    // Column 2: Current File (550px)
    columnHelper.accessor('sourcePath', {
      id: 'currentFile',
      size: 550,
      header: 'Current File',
      cell: (info) => <span title={info.getValue()}>{basename(info.getValue())}</span>,
    }),

    // Column 3: New Filename (550px) — dropdown if multiple series options
    columnHelper.display({
      id: 'newFilename',
      size: 550,
      header: 'New Filename',
      cell: ({ row }) => {
        const { status, seriesOptions, selectedSeriesId, computedNewName } = row.original;

        if (status === 'searching' || status === 'no_match' || status === 'parse_failed' || status === 'idle') {
          return <span />;
        }

        return (
          <div>
            {seriesOptions.length > 1 ? (
              <select
                value={selectedSeriesId ?? ''}
                onChange={(e) => onSeriesChange(row.original.id, Number(e.target.value))}
              >
                {seriesOptions.map((s) => (
                  <option key={s.id} value={s.id}>
                    {formatSeriesOption(s)}
                  </option>
                ))}
              </select>
            ) : null}
            {computedNewName ? <span>{computedNewName}</span> : null}
          </div>
        );
      },
    }),

    // Column 4: Status (60px)
    columnHelper.accessor('status', {
      id: 'status',
      size: 60,
      header: 'Status',
      cell: (info) => <StatusBadge status={info.getValue()} />,
    }),
  ];

  const table = useReactTable({
    data: rows,
    columns,
    getCoreRowModel: getCoreRowModel(),
    getRowId: (row) => row.id,
  });

  return (
    <table style={{ width: '100%', borderCollapse: 'collapse' }}>
      <thead>
        {table.getHeaderGroups().map((hg) => (
          <tr key={hg.id}>
            {hg.headers.map((h) => (
              <th key={h.id} style={{ width: h.getSize(), textAlign: 'left', padding: '4px' }}>
                {flexRender(h.column.columnDef.header, h.getContext())}
              </th>
            ))}
          </tr>
        ))}
      </thead>
      <tbody>
        {table.getRowModel().rows.map((row) => (
          <tr key={row.id}>
            {row.getVisibleCells().map((cell) => (
              <td key={cell.id} style={{ padding: '4px' }}>
                {flexRender(cell.column.columnDef.cell, cell.getContext())}
              </td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  );
}
