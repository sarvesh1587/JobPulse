import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { CheckCircle2, Target } from 'lucide-react'
import { profileApi } from '../../lib/api'
import { Page } from '../../components/layout/Page'
import { ButtonLink } from '../../components/ui/Button'
import { EmptyState, ErrorState, ListSkeleton } from '../../components/ui/Feedback'

export default function SkillGapPage() {
  const gap = useQuery({ queryKey: ['skill-gap'], queryFn: profileApi.skillGap })
  const g = gap.data

  const description = g
    ? g.targetRoles.length
      ? <>Based on {g.jobsAnalyzed} open {g.jobsAnalyzed === 1 ? 'role' : 'roles'} matching your targets: {g.targetRoles.join(', ')}.</>
      : <>Based on the {g.jobsAnalyzed} most recent open roles. <Link to="/profile" className="font-semibold text-accent-text hover:underline">Add target roles</Link> for a sharper picture.</>
    : undefined

  return (
    <Page title="Skill gap" eyebrow="SKILL GAP" description={description}>
      {gap.isLoading && <ListSkeleton rows={5} className="h-12" />}
      {gap.isError && <ErrorState error={gap.error} onRetry={() => gap.refetch()} />}
      {g && g.jobsAnalyzed === 0 && (
        <EmptyState icon={<Target size={18} />} title="No matching open roles to analyse"
          action={<ButtonLink to="/profile" variant="secondary">Edit target roles</ButtonLink>}>
          None of the current listings match your target roles. Try broader role names, like "Backend" instead of "Senior Backend Engineer".
        </EmptyState>
      )}
      {g && g.jobsAnalyzed > 0 && (
        <div className="grid gap-10 lg:grid-cols-[1fr_300px]">
          <section>
            <h2 className="mb-1 text-[15px] font-bold">What to learn next</h2>
            <p className="mb-5 text-[13px] text-muted">Skills these roles ask for that aren't on your profile, most common first.</p>
            {g.priorityGaps.length === 0 ? (
              <EmptyState compact title="No gaps found">You already list every skill these roles ask for.</EmptyState>
            ) : (
              <ol className="space-y-3">
                {g.priorityGaps.map((item, i) => (
                  <li key={item.skillName} className="rise grid grid-cols-[28px_1fr] gap-3 rounded-xl border border-border p-4" style={{ ['--i' as string]: i }}>
                    <span className="font-mono text-sm text-accent-text">{String(item.priority).padStart(2, '0')}</span>
                    <div>
                      <div className="flex flex-wrap items-baseline justify-between gap-2">
                        <span className="font-bold">{item.skillName}</span>
                        <span className="font-mono text-xs text-muted">{item.frequencyInTargetRoles} of {g.jobsAnalyzed} roles · {item.percentOfJobs}%</span>
                      </div>
                      <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-border" aria-hidden>
                        <div className="h-full rounded-full bg-accent" style={{ width: `${item.percentOfJobs}%` }} />
                      </div>
                      <Link to={`/jobs?q=${encodeURIComponent(item.skillName)}`} className="mt-2 inline-block text-[13px] font-medium text-accent-text hover:underline">
                        See roles asking for {item.skillName}
                      </Link>
                    </div>
                  </li>
                ))}
              </ol>
            )}
          </section>
          <aside>
            <h2 className="mb-3 font-mono text-label text-muted">ALREADY IN DEMAND</h2>
            {g.readySkills.length === 0 ? (
              <p className="text-sm text-muted">None of your listed skills appear in these roles yet.</p>
            ) : (
              <ul className="space-y-2">
                {g.readySkills.map((s) => (
                  <li key={s} className="flex items-center gap-2 text-sm"><CheckCircle2 size={14} className="text-good" /> {s}</li>
                ))}
              </ul>
            )}
            <p className="mt-6 text-xs text-muted">Counts come from skills tagged on each listing. It's a signal of demand, not a guarantee.</p>
          </aside>
        </div>
      )}
    </Page>
  )
}
