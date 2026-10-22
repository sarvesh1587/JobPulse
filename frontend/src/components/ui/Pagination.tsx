import { ChevronLeft, ChevronRight } from 'lucide-react'

interface PaginationProps {
  page: number          // zero-based, like the API
  totalPages: number
  onChange: (page: number) => void
}

export function Pagination({ page, totalPages, onChange }: PaginationProps) {
  if (totalPages <= 1) return null
  const btn = 'inline-flex h-9 items-center gap-1 rounded-lg border border-border-strong px-3 text-sm font-medium hover:bg-raised disabled:opacity-40 disabled:pointer-events-none'
  return (
    <nav aria-label="Pagination" className="flex items-center justify-between gap-3 pt-2">
      <button className={btn} disabled={page === 0} onClick={() => onChange(page - 1)}>
        <ChevronLeft size={15} /> Previous
      </button>
      <span className="font-mono text-label text-muted">
        PAGE {page + 1} OF {totalPages}
      </span>
      <button className={btn} disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>
        Next <ChevronRight size={15} />
      </button>
    </nav>
  )
}
