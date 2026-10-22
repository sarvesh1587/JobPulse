import { useQuery } from '@tanstack/react-query'
import { CheckCircle2, CircleAlert, Plus } from 'lucide-react'
import { Link } from 'react-router-dom'
import { jobsApi } from '../../lib/api'
import { ErrorState, Skeleton } from '../ui/Feedback'

const DIMENSIONS = [
  ['skillScore', 'Skills'],
  ['experienceScore', 'Experience'],
  ['educationScore', 'Education'],
  ['locationScore', 'Location'],
  ['freshnessScore', 'Freshness'],
] as const

function verdict(score: number) {
  if (score >= 80) return { text: 'Strong match', tone: 'text-good' }
  if (score >= 60) return { text: 'Worth a look', tone: 'text-accent-text' }
  return { text: 'Stretch role', tone: 'text-muted' }
}

/**
 * The explainable match score: the overall number, the per-dimension bars
 * that produce it, and the concrete reasons. It's computed on the server
 * from the candidate's real profile.
 */
export function MatchPanel({ jobId }: { jobId: string }) {
  const match = useQuery({ queryKey: ['match', jobId], queryFn: () => jobsApi.match(jobId) })

  if (match.isLoading) {
    return (
      <div className="space-y-3">
        <Skeleton className="h-10 w-24" />
        {DIMENSIONS.map(([key]) => <Skeleton key={key} className="h-3 w-full" />)}
      </div>
    )
  }
  if (match.isError) return <ErrorState error={match.error} onRetry={() => match.refetch()} />

  const m = match.data!
  const v = verdict(m.overallScore)

  return (
    <div>
      <div className="flex items-end gap-3">
        <span className="font-mono text-[40px] font-semibold leading-none tracking-tight">{Math.round(m.overallScore)}</span>
        <div className="pb-1">
          <div className={`text-sm font-bold ${v.tone}`}>{v.text}</div>
          <div className="font-mono text-label text-muted">MATCH SCORE</div>
        </div>
      </div>

      <dl className="mt-5 space-y-2.5">
        {DIMENSIONS.map(([key, label]) => (
          <div key={key} className="grid grid-cols-[84px_1fr_36px] items-center gap-3">
            <dt className="text-[13px] text-muted">{label}</dt>
            <dd className="h-1.5 overflow-hidden rounded-full bg-border" aria-hidden>
              <div className="h-full rounded-full bg-accent transition-[width] duration-700 ease-out" style={{ width: `${m[key]}%` }} />
            </dd>
            <dd className="text-right font-mono text-xs text-muted">{Math.round(m[key])}</dd>
          </div>
        ))}
      </dl>

      {(m.matchedSkills.length > 0 || m.missingSkills.length > 0) && (
        <div className="mt-5 space-y-2 text-[13px]">
          {m.matchedSkills.length > 0 && (
            <p className="flex gap-2"><CheckCircle2 size={14} className="mt-0.5 shrink-0 text-good" /> You have {m.matchedSkills.join(', ')}</p>
          )}
          {m.missingSkills.length > 0 && (
            <p className="flex gap-2"><Plus size={14} className="mt-0.5 shrink-0 text-miss" /> Missing {m.missingSkills.join(', ')}</p>
          )}
        </div>
      )}

      {(m.positiveSignals.length > 0 || m.concerns.length > 0) && (
        <ul className="mt-4 space-y-1.5 border-t border-border pt-4 text-[13px]">
          {m.positiveSignals.map((s) => (
            <li key={s} className="flex gap-2"><CheckCircle2 size={14} className="mt-0.5 shrink-0 text-good" />{s}</li>
          ))}
          {m.concerns.map((c) => (
            <li key={c} className="flex gap-2 text-muted"><CircleAlert size={14} className="mt-0.5 shrink-0" />{c}</li>
          ))}
        </ul>
      )}

      <p className="mt-4 text-xs text-muted">
        Calculated from your <Link to="/profile" className="font-medium text-accent-text hover:underline">profile</Link>. Update it and the score updates too.
      </p>
    </div>
  )
}
