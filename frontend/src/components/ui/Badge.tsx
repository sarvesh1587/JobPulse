import type { ReactNode } from 'react'
import type { ApplicationStatus, JobStatus } from '../../lib/types'
import { jobStatusLabel, statusLabel } from '../../lib/format'

export function Tag({ children, tone = 'neutral' }: { children: ReactNode; tone?: 'neutral' | 'good' | 'miss' }) {
  const tones = {
    neutral: 'border-border-strong text-muted',
    good: 'border-good/40 text-good bg-good/5',
    miss: 'border-miss/40 text-miss bg-miss/5',
  }
  return (
    <span className={`inline-flex h-6 items-center rounded-full border px-2.5 font-mono text-[11px] ${tones[tone]}`}>
      {children}
    </span>
  )
}

const statusTone: Record<ApplicationStatus, string> = {
  APPLIED: 'text-ink border-border-strong',
  IN_REVIEW: 'text-ink border-border-strong',
  ASSESSMENT: 'text-warn border-warn/40 bg-warn/5',
  INTERVIEW: 'text-accent-text border-accent/40 bg-accent/10',
  OFFER: 'text-good border-good/40 bg-good/10',
  REJECTED: 'text-miss border-miss/30',
  WITHDRAWN: 'text-muted border-border-strong',
}

export function StatusBadge({ status }: { status: ApplicationStatus }) {
  return (
    <span className={`inline-flex h-6 items-center gap-1.5 rounded-full border px-2.5 text-xs font-semibold ${statusTone[status]}`}>
      <span className="h-1.5 w-1.5 rounded-full bg-current" aria-hidden />
      {statusLabel[status]}
    </span>
  )
}

export function JobStatusBadge({ status }: { status: JobStatus }) {
  const open = status === 'ACTIVE'
  return (
    <span className={`inline-flex h-6 items-center gap-1.5 rounded-full border px-2.5 text-xs font-semibold ${
      open ? 'border-good/40 text-good' : 'border-border-strong text-muted'
    }`}>
      <span className="h-1.5 w-1.5 rounded-full bg-current" aria-hidden />
      {jobStatusLabel[status]}
    </span>
  )
}
