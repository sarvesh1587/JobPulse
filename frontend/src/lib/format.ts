import type { ApplicationStatus, EmploymentType, ExperienceLevel, JobStatus, Salary, WorkMode } from './types'

export const workModeLabel: Record<WorkMode, string> = { REMOTE: 'Remote', HYBRID: 'Hybrid', ONSITE: 'On-site' }

export const employmentTypeLabel: Record<EmploymentType, string> = {
  INTERNSHIP: 'Internship',
  FULL_TIME: 'Full-time',
  CONTRACT: 'Contract',
}

export const experienceLevelLabel: Record<ExperienceLevel, string> = {
  INTERN: 'Intern',
  ENTRY: 'Entry level',
  MID: 'Mid level',
  SENIOR: 'Senior',
}

export const statusLabel: Record<ApplicationStatus, string> = {
  APPLIED: 'Applied',
  IN_REVIEW: 'In review',
  ASSESSMENT: 'Assessment',
  INTERVIEW: 'Interview',
  OFFER: 'Offer',
  REJECTED: 'Rejected',
  WITHDRAWN: 'Withdrawn',
}

export const jobStatusLabel: Record<JobStatus, string> = {
  ACTIVE: 'Open',
  STALE: 'Possibly stale',
  EXPIRED: 'Expired',
  UNKNOWN: 'Unverified',
  CLOSED: 'Closed',
}

/** The forward stages, in order, for progress displays. */
export const PIPELINE: ApplicationStatus[] = ['APPLIED', 'IN_REVIEW', 'ASSESSMENT', 'INTERVIEW', 'OFFER']

/**
 * INR is shown in lakh ("₹8–12 LPA"), the way Indian job posts state pay.
 * Other currencies use compact international formatting.
 */
export function formatSalary(salary: Salary | null): string | null {
  if (!salary || (salary.min == null && salary.max == null)) return null
  const currency = salary.currency ?? 'INR'
  if (currency === 'INR') {
    const lakh = (n: number) => {
      const v = n / 100_000
      return Number.isInteger(v) ? String(v) : v.toFixed(1)
    }
    if (salary.min != null && salary.max != null) return `₹${lakh(salary.min)}–${lakh(salary.max)} LPA`
    return `₹${lakh((salary.max ?? salary.min)!)} LPA`
  }
  const fmt = new Intl.NumberFormat('en', { style: 'currency', currency, notation: 'compact', maximumFractionDigits: 1 })
  if (salary.min != null && salary.max != null) return `${fmt.format(salary.min)}–${fmt.format(salary.max)}`
  return fmt.format((salary.max ?? salary.min)!)
}

const rtf = new Intl.RelativeTimeFormat('en', { numeric: 'auto' })

/** "3 hours ago", "yesterday", "in 2 days". */
export function relativeTime(iso: string | null, now: Date = new Date()): string {
  if (!iso) return '—'
  const diffSeconds = Math.round((new Date(iso).getTime() - now.getTime()) / 1000)
  const abs = Math.abs(diffSeconds)
  if (abs < 60) return 'just now'
  if (abs < 3600) return rtf.format(Math.round(diffSeconds / 60), 'minute')
  if (abs < 86400) return rtf.format(Math.round(diffSeconds / 3600), 'hour')
  if (abs < 86400 * 30) return rtf.format(Math.round(diffSeconds / 86400), 'day')
  return formatDate(iso)
}

export function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })
}

export function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('en-IN', {
    weekday: 'short', day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit',
  })
}

/**
 * Ingested descriptions come from ATS boards as HTML. They're turned into
 * plain text, never injected as HTML, so third-party markup can't run
 * scripts on our page. Block-level tags become line breaks to keep structure.
 */
export function htmlToText(input: string): string {
  if (!/[<&]/.test(input)) return input
  const doc = new DOMParser().parseFromString(input, 'text/html')
  // Some boards double-encode (&lt;p&gt;), which decodes to markup on the first pass.
  const once = doc.body.textContent ?? ''
  const source = /<\/?[a-z][\s\S]*>/i.test(once) ? once : input
  const withBreaks = source
    .replace(/<\s*br\s*\/?>/gi, '\n')
    .replace(/<\/(p|div|h[1-6]|li|ul|ol)>/gi, '\n')
    .replace(/<li[^>]*>/gi, '- ')
  const text = new DOMParser().parseFromString(withBreaks, 'text/html').body.textContent ?? ''
  return text.replace(/\n{3,}/g, '\n\n').trim()
}

export function initials(name: string): string {
  return name.split(/\s+/).filter(Boolean).slice(0, 2).map((p) => p[0]!.toUpperCase()).join('')
}
