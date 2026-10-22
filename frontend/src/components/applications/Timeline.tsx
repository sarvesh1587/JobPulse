import type { StatusChange } from '../../lib/types'
import { formatDateTime, statusLabel } from '../../lib/format'

const ACTOR_LABEL = { YOU: 'You', EMPLOYER: 'Employer', SYSTEM: 'System' } as const

/** The application's audit trail, oldest first, as recorded by the backend. */
export function Timeline({ history, viewer = 'candidate' }: { history: StatusChange[]; viewer?: 'candidate' | 'employer' }) {
  return (
    <ol className="relative space-y-5 border-l border-border-strong pl-5">
      {history.map((h, i) => {
        const latest = i === history.length - 1
        const actor = viewer === 'employer'
          ? (h.changedBy === 'YOU' ? 'Candidate' : h.changedBy === 'EMPLOYER' ? 'Your team' : 'System')
          : ACTOR_LABEL[h.changedBy]
        return (
          <li key={`${h.at}-${i}`} className="relative">
            <span className={`absolute -left-[26px] top-1 h-2.5 w-2.5 rounded-full border-2 border-paper ${latest ? 'bg-accent' : 'bg-border-strong'}`} aria-hidden />
            <div className="text-sm font-semibold">
              {h.fromStatus ? <>{statusLabel[h.fromStatus]} → {statusLabel[h.toStatus]}</> : statusLabel[h.toStatus]}
            </div>
            <div className="font-mono text-[11px] text-muted">{formatDateTime(h.at)} · {actor.toUpperCase()}</div>
            {h.note && <p className="mt-1 text-[13px] text-muted">“{h.note}”</p>}
          </li>
        )
      })}
    </ol>
  )
}
