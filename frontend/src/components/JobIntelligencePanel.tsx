import { CheckCircle2, AlertTriangle, ExternalLink, Bookmark } from 'lucide-react'
import type { Job } from '../data/mockJobs'

type Props = { job: Job | null }

const BREAKDOWN = (job: Job) => [
  { label: 'Technical Skills', value: job.skillFit },
  { label: 'Experience', value: 88 },
  { label: 'Education', value: 100 },
  { label: 'Location', value: job.location === 'Pune' ? 100 : 60 },
  { label: 'Freshness', value: 94 },
]

export default function JobIntelligencePanel({ job }: Props) {
  if (!job) {
    return (
      <div className="w-[340px] shrink-0 border-l border-border p-6 hidden xl:flex items-center justify-center text-center">
        <p className="text-[13px] text-charcoal max-w-[22ch]">
          Select a role to see why it matches — or doesn't.
        </p>
      </div>
    )
  }

  const breakdown = BREAKDOWN(job)

  return (
    <div className="w-[340px] shrink-0 border-l border-border p-6 hidden xl:block overflow-y-auto">
      <div className="font-mono text-[11px] text-charcoal tracking-wide mb-1">SHOULD I APPLY?</div>
      <div
        className={`inline-block font-mono text-[11px] font-semibold px-2.5 py-1 rounded-full mt-2 mb-5 ${
          job.applyability === 'HIGH'
            ? 'bg-gradient-to-br from-accent-soft to-accent text-white'
            : job.applyability === 'MEDIUM'
            ? 'border border-border-strong text-charcoal'
            : 'border border-miss text-miss'
        }`}
      >
        {job.applyability === 'HIGH' ? 'YES — STRONG TARGET' : job.applyability === 'MEDIUM' ? 'MAYBE — WORTH REVIEW' : 'LIKELY SKIP'}
      </div>

      <div className="space-y-2 mb-6">
        <div className="flex items-center gap-2 text-[13.5px]">
          <CheckCircle2 size={14} className="text-good shrink-0" /> Graduate year accepted
        </div>
        <div className="flex items-center gap-2 text-[13.5px]">
          <CheckCircle2 size={14} className="text-good shrink-0" /> Recently verified ({job.postedAgo})
        </div>
        {job.missingSkills.length > 0 && (
          <div className="flex items-center gap-2 text-[13.5px]">
            <AlertTriangle size={14} className="text-miss shrink-0" /> {job.missingSkills.join(', ')} missing
          </div>
        )}
      </div>

      <div className="border-t border-border pt-5">
        <div className="text-[13px] font-semibold mb-3">Why this score</div>
        <div className="space-y-2.5">
          {breakdown.map((row) => (
            <div key={row.label} className="grid grid-cols-[90px_1fr_32px] gap-2 items-center">
              <div className="text-[12px] text-charcoal">{row.label}</div>
              <div className="h-[5px] rounded-full bg-border overflow-hidden">
                <div
                  className="h-full rounded-full bg-accent"
                  style={{ width: `${row.value}%` }}
                />
              </div>
              <div className="font-mono text-[11px] text-charcoal text-right">{row.value}%</div>
            </div>
          ))}
        </div>
      </div>

      <div className="flex flex-col gap-2 mt-6">
        <button className="flex items-center justify-center gap-2 text-[13.5px] font-semibold px-4 py-2.5 rounded-full text-white bg-gradient-to-br from-accent-soft to-accent">
          <ExternalLink size={14} /> View application
        </button>
        <button className="flex items-center justify-center gap-2 text-[13.5px] font-medium px-4 py-2.5 rounded-full border border-border-strong">
          <Bookmark size={14} /> Save for later
        </button>
      </div>
    </div>
  )
}
