import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ArrowRight, Bookmark, CalendarClock, CircleDashed, Compass } from 'lucide-react'
import { profileApi } from '../../lib/api'
import { PIPELINE, formatDateTime, relativeTime, statusLabel } from '../../lib/format'
import type { Application, Dashboard } from '../../lib/types'
import { Page, Section } from '../../components/layout/Page'
import { JobCard } from '../../components/jobs/JobCard'
import { StatusBadge } from '../../components/ui/Badge'
import { ButtonLink } from '../../components/ui/Button'
import { EmptyState, ErrorState, ListSkeleton, Skeleton } from '../../components/ui/Feedback'

function greeting(): string {
  const hour = new Date().getHours()
  return hour < 12 ? 'Good morning' : hour < 18 ? 'Good afternoon' : 'Good evening'
}

/** Counts per stage, straight from the API. Zeros stay zeros. */
function PipelineStrip({ data }: { data: Dashboard }) {
  const closed = data.applicationsByStatus.REJECTED + data.applicationsByStatus.WITHDRAWN
  return (
    <div className="grid grid-cols-2 gap-px overflow-hidden rounded-xl border border-border bg-border sm:grid-cols-3 lg:grid-cols-6">
      {PIPELINE.map((s) => (
        <Link key={s} to={`/applications?status=${s}`} className="bg-surface px-4 py-4 transition-colors hover:bg-raised">
          <div className="font-mono text-2xl font-semibold">{data.applicationsByStatus[s]}</div>
          <div className="mt-1 font-mono text-label text-muted">{statusLabel[s].toUpperCase()}</div>
        </Link>
      ))}
      <Link to="/applications" className="bg-surface px-4 py-4 transition-colors hover:bg-raised">
        <div className="font-mono text-2xl font-semibold text-muted">{closed}</div>
        <div className="mt-1 font-mono text-label text-muted">CLOSED</div>
      </Link>
    </div>
  )
}

function Completeness({ data }: { data: Dashboard }) {
  const { percent, missing } = data.profileCompleteness
  return (
    <div className="rounded-xl border border-border-strong bg-surface p-5">
      <div className="flex items-center justify-between">
        <h2 className="text-[15px] font-bold">Profile strength</h2>
        <span className="font-mono text-sm font-semibold">{percent}%</span>
      </div>
      <div className="mt-3 h-1.5 overflow-hidden rounded-full bg-border" role="progressbar" aria-valuenow={percent} aria-valuemin={0} aria-valuemax={100} aria-label="Profile completeness">
        <div className="h-full rounded-full bg-accent transition-[width] duration-700" style={{ width: `${percent}%` }} />
      </div>
      {missing.length > 0 ? (
        <>
          <p className="mt-4 text-[13px] text-muted">A complete profile makes your match scores more accurate.</p>
          <ul className="mt-2 space-y-1.5 text-sm">
            {missing.slice(0, 4).map((m) => (
              <li key={m} className="flex items-center gap-2"><CircleDashed size={13} className="text-muted" /> {m}</li>
            ))}
          </ul>
          <ButtonLink to="/profile" variant="secondary" size="sm" className="mt-4">Complete profile</ButtonLink>
        </>
      ) : (
        <p className="mt-4 text-sm text-muted">Your profile is complete. Match scores use everything you've added.</p>
      )}
    </div>
  )
}

function ApplicationRow({ app }: { app: Application }) {
  return (
    <Link to={`/applications/${app.id}`} className="flex items-center justify-between gap-4 px-4 py-3.5 transition-colors hover:bg-raised">
      <div className="min-w-0">
        <div className="truncate text-sm font-semibold">{app.job.title}</div>
        <div className="truncate text-[13px] text-muted">{app.job.companyName} · updated {relativeTime(app.updatedAt)}</div>
      </div>
      <StatusBadge status={app.status} />
    </Link>
  )
}

export default function DashboardPage() {
  const dashboard = useQuery({ queryKey: ['dashboard'], queryFn: profileApi.dashboard })
  const d = dashboard.data

  return (
    <Page title={d ? `${greeting()}, ${d.firstName}` : 'Dashboard'} eyebrow="DASHBOARD"
      description={d ? (d.activeApplications > 0
        ? `${d.activeApplications} application${d.activeApplications === 1 ? '' : 's'} in progress, ${d.savedJobs} saved.`
        : 'Nothing in progress yet. Find a role worth applying to.') : undefined}
      actions={<ButtonLink to="/jobs" icon={<Compass size={15} />}>Find jobs</ButtonLink>}>
      {dashboard.isLoading && (
        <div className="space-y-6"><Skeleton className="h-24 w-full" /><ListSkeleton rows={3} /></div>
      )}
      {dashboard.isError && <ErrorState error={dashboard.error} onRetry={() => dashboard.refetch()} />}
      {d && (
        <div className="space-y-10">
          <PipelineStrip data={d} />

          <div className="grid gap-8 lg:grid-cols-[1fr_320px]">
            <div className="space-y-10">
              {d.upcomingInterviews.length > 0 && (
                <Section title="Upcoming interviews">
                  <ul className="divide-y divide-border overflow-hidden rounded-xl border border-accent/40 bg-accent/[0.04]">
                    {d.upcomingInterviews.map((a) => (
                      <li key={a.id}>
                        <Link to={`/applications/${a.id}`} className="flex items-center gap-3 px-4 py-3.5 hover:bg-accent/[0.06]">
                          <CalendarClock size={18} className="shrink-0 text-accent-text" />
                          <div className="min-w-0 flex-1">
                            <div className="truncate text-sm font-semibold">{a.job.title} · {a.job.companyName}</div>
                            <div className="text-[13px] text-muted">{formatDateTime(a.interviewAt!)}</div>
                          </div>
                        </Link>
                      </li>
                    ))}
                  </ul>
                </Section>
              )}

              <Section title={d.recommendationBasis === 'SKILLS' ? 'Recommended for your skills' : 'Recently posted'}
                action={<Link to="/jobs" className="inline-flex items-center gap-1 text-sm font-semibold text-accent-text">Browse <ArrowRight size={14} /></Link>}>
                {d.recommendationBasis === 'RECENT' && (
                  <p className="-mt-1 mb-3 text-[13px] text-muted">
                    Add skills to your <Link to="/profile" className="font-semibold text-accent-text hover:underline">profile</Link> to get recommendations ranked by fit.
                  </p>
                )}
                {d.recommended.length === 0
                  ? <EmptyState compact title="No matching open roles right now">New listings will appear here as they're posted.</EmptyState>
                  : <div className="space-y-3">{d.recommended.map((j, i) => <JobCard key={j.id} job={j} index={i} />)}</div>}
              </Section>
            </div>

            <div className="space-y-8">
              <Completeness data={d} />

              <Section title="Recent applications" action={<Link to="/applications" className="text-sm font-semibold text-accent-text">All</Link>}>
                {d.recentApplications.length === 0
                  ? <EmptyState compact title="No applications yet">When you apply, or track an application you sent elsewhere, it shows up here.</EmptyState>
                  : <div className="divide-y divide-border overflow-hidden rounded-xl border border-border">{d.recentApplications.map((a) => <ApplicationRow key={a.id} app={a} />)}</div>}
              </Section>

              <Section title="Recently saved" action={<Link to="/saved" className="text-sm font-semibold text-accent-text">All</Link>}>
                {d.recentlySaved.length === 0
                  ? <EmptyState compact icon={<Bookmark size={16} />} title="Nothing saved">Use the bookmark on any job to keep it for later.</EmptyState>
                  : (
                    <ul className="divide-y divide-border overflow-hidden rounded-xl border border-border">
                      {d.recentlySaved.map((j) => (
                        <li key={j.id}>
                          <Link to={`/jobs/${j.id}`} className="block px-4 py-3 hover:bg-raised">
                            <div className="truncate text-sm font-semibold">{j.title}</div>
                            <div className="truncate text-[13px] text-muted">{j.company.name}</div>
                          </Link>
                        </li>
                      ))}
                    </ul>
                  )}
              </Section>
            </div>
          </div>
        </div>
      )}
    </Page>
  )
}
