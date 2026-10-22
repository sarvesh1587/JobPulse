import { Link, useParams, useSearchParams } from 'react-router-dom'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { ArrowLeft, ExternalLink, Pencil, Users } from 'lucide-react'
import { employerApi } from '../../lib/api'
import { PIPELINE, formatDate, statusLabel } from '../../lib/format'
import type { ApplicationStatus } from '../../lib/types'
import { useTitle } from '../../components/layout/Page'
import { JobStatusBadge, StatusBadge } from '../../components/ui/Badge'
import { ButtonLink } from '../../components/ui/Button'
import { EmptyState, ErrorState, ListSkeleton, Skeleton } from '../../components/ui/Feedback'
import { Pagination } from '../../components/ui/Pagination'

const TABS: (ApplicationStatus | 'ALL')[] = ['ALL', ...PIPELINE, 'REJECTED', 'WITHDRAWN']

export default function ApplicantsPage() {
  const { id = '' } = useParams()
  const [params, setParams] = useSearchParams()
  const tab = (params.get('status') as ApplicationStatus | null) ?? 'ALL'
  const page = Math.max(0, Number(params.get('page') ?? 0) || 0)
  const status = tab === 'ALL' ? undefined : tab

  const job = useQuery({ queryKey: ['employer', 'job', id], queryFn: () => employerApi.job(id) })
  const applicants = useQuery({
    queryKey: ['employer', 'applications', id, status ?? 'ALL', page],
    queryFn: () => employerApi.applications(id, status, page),
    placeholderData: keepPreviousData,
  })
  useTitle(job.data ? `Applicants for ${job.data.title}` : 'Applicants')

  if (job.isError) return <div className="mx-auto max-w-page px-4 py-10 sm:px-6"><ErrorState error={job.error} /></div>

  return (
    <div className="mx-auto max-w-page px-4 pb-10 pt-6 sm:px-6">
      <Link to="/employer" className="inline-flex items-center gap-1.5 text-sm text-muted hover:text-ink"><ArrowLeft size={15} /> Workspace</Link>
      {job.isLoading ? <Skeleton className="mt-6 h-16 w-1/2" /> : job.data && (
        <header className="mt-6 flex flex-wrap items-end justify-between gap-4">
          <div>
            <JobStatusBadge status={job.data.status} />
            <h1 className="mt-3 text-[28px] font-extrabold tracking-tight">{job.data.title}</h1>
            <p className="mt-1 text-sm text-muted">{job.data.location} · posted {formatDate(job.data.postedAt)}</p>
          </div>
          <div className="flex gap-2">
            <ButtonLink to={`/jobs/${id}`} variant="ghost" icon={<ExternalLink size={14} />}>Public view</ButtonLink>
            <ButtonLink to={`/employer/jobs/${id}/edit`} variant="secondary" icon={<Pencil size={14} />}>Edit</ButtonLink>
          </div>
        </header>
      )}

      <div role="tablist" aria-label="Filter applicants" className="-mx-4 mb-6 mt-8 flex gap-1 overflow-x-auto px-4 pb-1 sm:mx-0 sm:px-0">
        {TABS.map((t) => (
          <button key={t} role="tab" aria-selected={tab === t} onClick={() => setParams(t === 'ALL' ? {} : { status: t })}
            className={`h-8 shrink-0 rounded-full px-3.5 text-[13px] font-medium transition-colors ${tab === t ? 'bg-ink text-paper' : 'text-muted hover:bg-raised hover:text-ink'}`}>
            {t === 'ALL' ? 'All' : statusLabel[t]}
          </button>
        ))}
      </div>

      {applicants.isLoading && <ListSkeleton rows={3} className="h-[84px]" />}
      {applicants.isError && <ErrorState error={applicants.error} onRetry={() => applicants.refetch()} />}
      {applicants.data && applicants.data.content.length === 0 && (
        <EmptyState icon={<Users size={18} />} title={status ? `No applicants ${statusLabel[status].toLowerCase()}` : 'No applicants yet'}>
          {!status && 'When candidates apply, they appear here with their profile and cover note.'}
        </EmptyState>
      )}
      {applicants.data && applicants.data.content.length > 0 && (
        <>
          <ul className="divide-y divide-border overflow-hidden rounded-xl border border-border">
            {applicants.data.content.map((a, i) => (
              <li key={a.id} className="rise" style={{ ['--i' as string]: i }}>
                <Link to={`/employer/applications/${a.id}`} className="flex flex-wrap items-center gap-x-4 gap-y-2 px-5 py-4 hover:bg-raised">
                  <div className="min-w-0 flex-1">
                    <div className="font-semibold">{a.applicant.fullName}</div>
                    <div className="truncate text-[13px] text-muted">
                      {a.applicant.headline ?? a.applicant.email}
                      {a.applicant.location && <> · {a.applicant.location}</>}
                    </div>
                    {a.applicant.skills.length > 0 && (
                      <div className="mt-2 flex flex-wrap gap-1">
                        {a.applicant.skills.slice(0, 6).map((s) => <span key={s} className="rounded bg-raised px-1.5 py-0.5 font-mono text-[11px] text-muted">{s}</span>)}
                      </div>
                    )}
                  </div>
                  <span className="text-[13px] text-muted">Applied {formatDate(a.appliedAt)}</span>
                  <StatusBadge status={a.status} />
                </Link>
              </li>
            ))}
          </ul>
          <div className="mt-4">
            <Pagination page={applicants.data.page} totalPages={applicants.data.totalPages}
              onChange={(p) => setParams({ ...(status ? { status } : {}), ...(p ? { page: String(p) } : {}) })} />
          </div>
        </>
      )}
    </div>
  )
}
