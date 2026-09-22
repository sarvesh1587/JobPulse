import { CheckCircle2 } from 'lucide-react'
import type { Job } from '../data/mockJobs'
import MatchScore from './MatchScore'

type Props = {
  job: Job
  selected: boolean
  onSelect: (id: string) => void
}

export default function JobRow({ job, selected, onSelect }: Props) {
  return (
    <button
      onClick={() => onSelect(job.id)}
      className={`w-full text-left px-5 py-4 border-b border-border transition-colors ${
        selected ? 'bg-accent/[0.06] border-l-2 border-l-accent' : 'hover:bg-surface-raised border-l-2 border-l-transparent'
      }`}
    >
      <div className="grid grid-cols-[1fr_auto] gap-4 items-start">
        <div className="min-w-0">
          <div className="font-bold text-[14.5px] tracking-tight truncate">{job.title}</div>
          <div className="font-mono text-[11.5px] text-charcoal mt-1">
            {job.company} · {job.location} · {job.workMode}
          </div>
          <div className="flex flex-wrap gap-1.5 mt-2.5">
            {job.skills.map((s) => (
              <span
                key={s}
                className="font-mono text-[10.5px] px-2 py-0.5 rounded-full border border-border-strong text-charcoal"
              >
                {s}
              </span>
            ))}
          </div>
          <div className="flex items-center gap-3 mt-2.5 font-mono text-[10.5px] text-charcoal">
            <span>{job.postedAgo}</span>
            {job.officialSource && (
              <span className="flex items-center gap-1 text-good">
                <CheckCircle2 size={11} /> Official source
              </span>
            )}
          </div>
        </div>
        <MatchScore score={job.match} />
      </div>
    </button>
  )
}
