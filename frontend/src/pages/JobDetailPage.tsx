import { useMemo } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ArrowLeft, Building2, Clock, ExternalLink, MapPin } from 'lucide-react'
import { jobsApi } from '../lib/api'
import {
  employmentTypeLabel, experienceLevelLabel, formatSalary, htmlToText, relativeTime, workModeLabel,
} from '../lib/format'
import { useAuth } from '../auth/AuthProvider'
import { useTitle } from '../components/layout/Page'
import { ApplyPanel } from '../components/jobs/ApplyPanel'
import { MatchPanel } from '../components/jobs/MatchPanel'
import { SaveButton } from '../components/jobs/SaveButton'
import { JobStatusBadge } from '../components/ui/Badge'
import { ErrorState, Skeleton } from '../components/ui/Feedback'

export default function JobDetailPage() {
  const { id = '' } = useParams()
  const { user } = useAuth()
  const job = useQuery({ queryKey: ['job', id], queryFn: () => jobsApi.get(id) })
  useTitle(job.data ? `${job.data.title} at ${job.data.company.name}` : 'Job')

  const description = useMemo(() => (job.data?.description ? htmlToText(job.data.description) : ''), [job.data?.description])

  if (job.isLoading) {
    return (
      <div className="mx-auto max-w-page space-y-4 px-4 py-10 sm:px-6">
        <Skeleton className="h-5 w-32" />
        <Skeleton className="h-10 w-2/3" />
        <Skeleton className="h-64 w-full" />
      </div>
    )
  }
  if (job.isError) {
    return <div className="mx-auto max-w-page px-4 py-10 sm:px-6"><ErrorState error={job.error} onRetry={() => job.refetch()} /></div>
  }

  const j = job.data!
  const salary = formatSalary(j.salary)

  return (
    <div className="mx-auto max-w-page px-4 pb-10 pt-6 sm:px-6">
      <Link to="/jobs" className="inline-flex items-center gap-1.5 text-sm text-muted hover:text-ink">
        <ArrowLeft size={15} /> All jobs
      </Link>

      <div className="mt-6 grid gap-10 lg:grid-cols-[1fr_340px]">
        <article className="min-w-0">
          <header className="rise">
            <div className="flex flex-wrap items-center gap-2">
              <JobStatusBadge status={j.status} />
              {j.origin === 'INGESTED' && (
                <span className="inline-flex h-6 items-center gap-1 rounded-full border border-border-strong px-2.5 font-mono text-[11px] text-muted">
                  <ExternalLink size={11} /> APPLY ON COMPANY SITE
                </span>
              )}
            </div>
            <h1 className="mt-4 text-[28px] font-extrabold leading-tight tracking-tight sm:text-[36px] text-balance">{j.title}</h1>
            <div className="mt-3 flex flex-wrap items-center gap-x-5 gap-y-2 text-[15px] text-muted">
              <Link to={`/companies/${j.company.id}`} className="inline-flex items-center gap-1.5 font-semibold text-ink hover:text-accent-text">
                <Building2 size={15} /> {j.company.name}
              </Link>
              {j.location && <span className="inline-flex items-center gap-1.5"><MapPin size={15} /> {j.location}</span>}
              <span className="inline-flex items-center gap-1.5"><Clock size={15} /> Posted {relativeTime(j.postedAt)}</span>
            </div>

            <dl className="mt-6 grid grid-cols-2 gap-px overflow-hidden rounded-xl border border-border bg-border sm:grid-cols-4">
              {[
                ['Type', j.employmentType ? employmentTypeLabel[j.employmentType] : '—'],
                ['Level', j.experienceLevel ? experienceLevelLabel[j.experienceLevel] : '—'],
                ['Work mode', j.workMode ? workModeLabel[j.workMode] : '—'],
                ['Salary', salary ?? 'Not listed'],
              ].map(([label, value]) => (
                <div key={label} className="bg-surface px-4 py-3">
                  <dt className="font-mono text-label text-muted">{label.toUpperCase()}</dt>
                  <dd className="mt-1 text-sm font-semibold">{value}</dd>
                </div>
              ))}
            </dl>
          </header>

          {j.skills.length > 0 && (
            <section className="mt-8">
              <h2 className="mb-3 font-mono text-label text-muted">SKILLS</h2>
              <ul className="flex flex-wrap gap-1.5">
                {j.skills.map((s) => <li key={s} className="rounded-md border border-border-strong px-2.5 py-1 text-[13px] font-medium">{s}</li>)}
              </ul>
            </section>
          )}

          <section className="mt-8">
            <h2 className="mb-3 font-mono text-label text-muted">ABOUT THE ROLE</h2>
            {description
              ? <div className="max-w-[72ch] whitespace-pre-line text-[15px] leading-[1.75] text-ink/90">{description}</div>
              : <p className="text-sm text-muted">The company didn't include a description.</p>}
          </section>

          {(j.companyDescription || j.companyWebsite) && (
            <section className="mt-10 rounded-xl border border-border p-5">
              <h2 className="text-[15px] font-bold">About {j.company.name}</h2>
              {j.companyDescription && <p className="mt-2 text-sm leading-relaxed text-muted">{j.companyDescription}</p>}
              <div className="mt-3 flex flex-wrap gap-4 text-sm">
                <Link to={`/companies/${j.company.id}`} className="font-semibold text-accent-text hover:underline">All open roles</Link>
                {j.companyWebsite && (
                  <a href={j.companyWebsite} target="_blank" rel="noopener noreferrer" className="inline-flex items-center gap-1 text-muted hover:text-ink">
                    Website <ExternalLink size={12} />
                  </a>
                )}
              </div>
            </section>
          )}
        </article>

        <aside className="space-y-4 lg:sticky lg:top-24 lg:self-start">
          <div className="rounded-xl border border-border-strong bg-surface p-5">
            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-[15px] font-bold">{j.myApplication && j.myApplication.status !== 'WITHDRAWN' ? 'Your application' : 'Interested?'}</h2>
              <SaveButton jobId={j.id} saved={j.saved} variant="full" />
            </div>
            <ApplyPanel job={j} />
          </div>

          {user?.role === 'CANDIDATE' && (
            <div className="rounded-xl border border-border-strong bg-surface p-5">
              <h2 className="mb-4 text-[15px] font-bold">How you match</h2>
              <MatchPanel jobId={j.id} />
            </div>
          )}
          {!user && (
            <div className="rounded-xl border border-dashed border-border-strong p-5 text-sm text-muted">
              <Link to="/register" className="font-semibold text-accent-text hover:underline">Create a profile</Link> to see a match score for this role, with the reasons behind it.
            </div>
          )}
        </aside>
      </div>
    </div>
  )
}
