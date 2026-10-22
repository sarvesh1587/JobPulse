import type { ReactNode } from 'react'
import { AlertTriangle, RotateCw } from 'lucide-react'
import { ApiError } from '../../lib/http'

export function Spinner({ size = 16 }: { size?: number }) {
  return (
    <span
      role="status"
      aria-label="Loading"
      className="inline-block animate-spin rounded-full border-2 border-current border-r-transparent"
      style={{ width: size, height: size }}
    />
  )
}

export function Skeleton({ className = '' }: { className?: string }) {
  return <div aria-hidden className={`skeleton rounded-md ${className}`} />
}

/** Placeholder rows shaped like the content they stand in for, so the layout doesn't jump. */
export function ListSkeleton({ rows = 4, className = 'h-[92px]' }: { rows?: number; className?: string }) {
  return (
    <div className="space-y-3" aria-busy="true" aria-label="Loading">
      {Array.from({ length: rows }, (_, i) => (
        <Skeleton key={i} className={`w-full ${className}`} />
      ))}
    </div>
  )
}

interface EmptyStateProps {
  icon?: ReactNode
  title: string
  children?: ReactNode
  action?: ReactNode
  compact?: boolean
}

export function EmptyState({ icon, title, children, action, compact }: EmptyStateProps) {
  return (
    <div className={`rounded-xl border border-dashed border-border-strong text-center ${compact ? 'px-5 py-8' : 'px-6 py-14'}`}>
      {icon && <div className="mx-auto mb-3 flex h-10 w-10 items-center justify-center rounded-full bg-raised text-muted">{icon}</div>}
      <h3 className="text-[15px] font-bold">{title}</h3>
      {children && <p className="mx-auto mt-1.5 max-w-[46ch] text-sm text-muted text-pretty">{children}</p>}
      {action && <div className="mt-5 flex justify-center">{action}</div>}
    </div>
  )
}

export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) return error.message
  return 'Something went wrong. Please try again.'
}

export function ErrorState({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  const notFound = error instanceof ApiError && error.status === 404
  return (
    <div role="alert" className="rounded-xl border border-miss/30 bg-miss/5 px-6 py-10 text-center">
      <AlertTriangle className="mx-auto mb-3 text-miss" size={20} />
      <h3 className="text-[15px] font-bold">{notFound ? 'Not found' : "This didn't load"}</h3>
      <p className="mx-auto mt-1 max-w-[46ch] text-sm text-muted">{errorMessage(error)}</p>
      {onRetry && !notFound && (
        <button onClick={onRetry} className="mt-4 inline-flex items-center gap-1.5 text-sm font-semibold text-accent-text hover:underline">
          <RotateCw size={14} /> Try again
        </button>
      )}
    </div>
  )
}

/** Inline banner for a failed form submission. */
export function FormError({ error }: { error: unknown }) {
  if (!error) return null
  return (
    <div role="alert" className="rounded-lg border border-miss/30 bg-miss/5 px-3.5 py-2.5 text-sm text-miss">
      {errorMessage(error)}
    </div>
  )
}
