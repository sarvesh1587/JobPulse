import { Link } from 'react-router-dom'
import { ExternalLink } from 'lucide-react'
import type { JobSummary } from '../../lib/types'
import { employmentTypeLabel, experienceLevelLabel, formatSalary, relativeTime, workModeLabel } from '../../lib/format'
import { SaveButton } from './SaveButton'

/**
 * One row in a job list. The whole card is a link. The save button sits
 * outside the link, so it stays a separate control for keyboard and screen
 * reader users.
 */
export function JobCard({ job, index = 0 }: { job: JobSummary; index?: number }) {
  const salary = formatSalary(job.salary)
  const facts = [
    job.employmentType && employmentTypeLabel[job.employmentType],
    job.experienceLevel && experienceLevelLabel[job.experienceLevel],
    job.workMode && workModeLabel[job.workMode],
  ].filter(Boolean)

  return (
    <article
      className="rise group relative rounded-xl border border-border bg-surface transition-[border-color,transform,box-shadow] duration-200 hover:-translate-y-px hover:border-border-strong hover:shadow-pop"
      style={{ ['--i' as string]: index }}
    >
      <Link to={`/jobs/${job.id}`} className="block rounded-xl p-4 pr-14 sm:p-5 sm:pr-16">
        <div className="flex flex-wrap items-baseline gap-x-2 gap-y-1">
          <h3 className="text-[15.5px] font-bold tracking-tight group-hover:text-accent-text">{job.title}</h3>
          {job.matchScore != null && (
            <span className="font-mono text-[11px] font-semibold text-accent-text" title="Your match score">
              {Math.round(job.matchScore)}% MATCH
            </span>
          )}
        </div>
        <p className="mt-1 text-sm text-muted">
          <span className="font-medium text-ink/90">{job.company.name}</span>
          {job.location && <> · {job.location}</>}
        </p>

        <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-1.5 text-[13px] text-muted">
          {facts.map((f) => <span key={f as string}>{f}</span>)}
          {salary && <span className="font-medium text-ink">{salary}</span>}
        </div>

        {job.skills.length > 0 && (
          <ul className="mt-3 flex flex-wrap gap-1.5" aria-label="Skills">
            {job.skills.slice(0, 6).map((s) => (
              <li key={s} className="rounded-md bg-raised px-2 py-0.5 font-mono text-[11px] text-muted">{s}</li>
            ))}
            {job.skills.length > 6 && <li className="px-1 font-mono text-[11px] text-muted">+{job.skills.length - 6}</li>}
          </ul>
        )}

        <div className="mt-3.5 flex items-center gap-3 font-mono text-[11px] text-muted">
          <span>{relativeTime(job.postedAt)}</span>
          {job.origin === 'INGESTED' && (
            <span className="inline-flex items-center gap-1" title="You apply on the company's own careers page">
              <ExternalLink size={11} /> COMPANY SITE
            </span>
          )}
        </div>
      </Link>
      <div className="absolute right-3 top-3 sm:right-4 sm:top-4">
        <SaveButton jobId={job.id} saved={job.saved} />
      </div>
    </article>
  )
}
