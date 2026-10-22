import { Link, useSearchParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ClipboardList, ExternalLink } from 'lucide-react'
import { applicationsApi } from '../../lib/api'
import { PIPELINE, formatDate, formatDateTime, relativeTime, statusLabel } from '../../lib/format'
import type { ApplicationStatus } from '../../lib/types'
import { Page } from '../../components/layout/Page'
import { StatusBadge } from '../../components/ui/Badge'
import { ButtonLink } from '../../components/ui/Button'
import { EmptyState, ErrorState, ListSkeleton } from '../../components/ui/Feedback'

const TABS: (ApplicationStatus | 'ALL')[] = ['ALL', ...PIPELINE, 'REJECTED', 'WITHDRAWN']

/** Where an application sits in the forward pipeline: dots for passed and current stages. */
function Progress({ status }: { status: ApplicationStatus }) {
  const index = PIPELINE.indexOf(status)
  if (index < 0) return null
  return (
    <ol className="flex items-center gap-1" aria-label={`Stage ${index + 1} of ${PIPELINE.length}`}>
      {PIPELINE.map((s, i) => (
        <li key={s} className={`h-1 w-5 rounded-full ${i <= index ? 'bg-accent' : 'bg-border-strong'}`} title={statusLabel[s]} />
      ))}
    </ol>
  )
}

export default function ApplicationsPage() {
  const [params, setParams] = useSearchParams()
  const tab = (params.get('status') as ApplicationStatus | null) ?? 'ALL'
  const status = tab === 'ALL' ? undefined : tab
  const apps = useQuery({ queryKey: ['applications', status ?? 'ALL'], queryFn: () => applicationsApi.list(status) })

  return (
    <Page title="Applications" eyebrow="TRACKER" description="Everything you've applied to, on JobPulse or on company sites, in one place.">
      <div role="tablist" aria-label="Filter by status" className="-mx-4 mb-6 flex gap-1 overflow-x-auto px-4 pb-1 sm:mx-0 sm:px-0">
        {TABS.map((t) => (
          <button key={t} role="tab" aria-selected={tab === t}
            onClick={() => setParams(t === 'ALL' ? {} : { status: t })}
            className={`h-8 shrink-0 rounded-full px-3.5 text-[13px] font-medium transition-colors ${
              tab === t ? 'bg-ink text-paper' : 'text-muted hover:bg-raised hover:text-ink'
            }`}>
            {t === 'ALL' ? 'All' : statusLabel[t]}
          </button>
        ))}
      </div>

      {apps.isLoading && <ListSkeleton rows={4} className="h-[76px]" />}
      {apps.isError && <ErrorState error={apps.error} onRetry={() => apps.refetch()} />}
      {apps.data && apps.data.length === 0 && (
        <EmptyState icon={<ClipboardList size={18} />}
          title={status ? `Nothing ${statusLabel[status].toLowerCase()}` : 'No applications yet'}
          action={!status ? <ButtonLink to="/jobs">Find a role</ButtonLink> : undefined}>
          {!status && 'Apply in-app to employers on JobPulse, or press "I\'ve applied" on any external listing to track it here.'}
        </EmptyState>
      )}
      {apps.data && apps.data.length > 0 && (
        <div className="overflow-hidden rounded-xl border border-border">
          <div className="hidden grid-cols-[1.6fr_1fr_1fr_auto] gap-4 border-b border-border bg-raised px-5 py-2.5 font-mono text-label text-muted md:grid">
            <span>ROLE</span><span>APPLIED</span><span>PROGRESS</span><span>STATUS</span>
          </div>
          <ul className="divide-y divide-border">
            {apps.data.map((a, i) => (
              <li key={a.id} className="rise" style={{ ['--i' as string]: i }}>
                <Link to={`/applications/${a.id}`} className="grid gap-2 px-5 py-4 transition-colors hover:bg-raised md:grid-cols-[1.6fr_1fr_1fr_auto] md:items-center md:gap-4">
                  <div className="min-w-0">
                    <div className="flex items-center gap-2 truncate text-[15px] font-semibold">
                      {a.job.title}
                      {a.job.origin === 'INGESTED' && <ExternalLink size={12} className="shrink-0 text-muted" aria-label="Applied on company site" />}
                    </div>
                    <div className="truncate text-[13px] text-muted">
                      {a.job.companyName}
                      {a.status === 'INTERVIEW' && a.interviewAt && <span className="text-accent-text"> · Interview {formatDateTime(a.interviewAt)}</span>}
                      {a.nextAction && a.status !== 'INTERVIEW' && <> · Next: {a.nextAction}</>}
                    </div>
                  </div>
                  <div className="text-[13px] text-muted">{formatDate(a.appliedAt)} <span className="md:hidden">· updated {relativeTime(a.updatedAt)}</span></div>
                  <div className="hidden md:block"><Progress status={a.status} /></div>
                  <div><StatusBadge status={a.status} /></div>
                </Link>
              </li>
            ))}
          </ul>
        </div>
      )}
    </Page>
  )
}
