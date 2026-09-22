import { Link } from 'react-router-dom'
import { ArrowUpRight, CheckCircle2 } from 'lucide-react'
import StatCard from '../components/StatCard'
import { mockJobs } from '../data/mockJobs'

const topMatches = [...mockJobs].sort((a, b) => b.match - a.match).slice(0, 3)

export default function Overview() {
  return (
    <div className="flex-1 overflow-y-auto">
      <div className="max-w-[880px] mx-auto px-8 py-10">
        <div className="font-mono text-[11px] text-charcoal tracking-wide mb-2">
          OVERVIEW
        </div>
        <h1 className="text-[30px] font-extrabold tracking-tight mb-1">
          Here's where things stand.
        </h1>
        <p className="text-[14px] text-charcoal mb-8">
          A quick read on your strongest matches, open applications, and what's left to learn.
        </p>

        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-10">
          <StatCard label="Strong matches" value="2" accentValue trend="+1 since yesterday" />
          <StatCard label="Roles indexed" value="1,204" />
          <StatCard label="Applications active" value="0" />
          <StatCard label="Skills to close" value="3" />
        </div>

        <div className="flex items-center justify-between mb-4">
          <h2 className="text-[17px] font-bold tracking-tight">Today's top matches</h2>
          <Link
            to="/discover"
            className="flex items-center gap-1 text-[13px] font-semibold text-accent hover:gap-1.5 transition-all"
          >
            View all <ArrowUpRight size={14} />
          </Link>
        </div>

        <div className="border border-border-strong rounded-lg overflow-hidden mb-10">
          {topMatches.map((job) => (
            <Link
              key={job.id}
              to="/discover"
              className="flex items-center justify-between px-5 py-4 border-b border-border last:border-b-0 hover:bg-surface-raised transition-colors"
            >
              <div>
                <div className="font-bold text-[14px]">{job.title}</div>
                <div className="font-mono text-[11.5px] text-charcoal mt-1">
                  {job.company} · {job.location} · {job.postedAgo}
                </div>
              </div>
              <div className="text-right">
                <div className="font-mono font-semibold text-[18px] bg-gradient-to-br from-accent-soft to-accent bg-clip-text text-transparent">
                  {job.match}
                </div>
                <div className="font-mono text-[9px] text-charcoal tracking-wide">MATCH</div>
              </div>
            </Link>
          ))}
        </div>

        <div className="grid md:grid-cols-2 gap-4">
          <div className="border border-border-strong rounded-lg p-5">
            <div className="font-mono text-[11px] text-charcoal tracking-wide mb-3">
              APPLICATIONS
            </div>
            <p className="text-[13.5px] text-charcoal leading-relaxed">
              Nothing in your pipeline yet. Once you apply to a role from Discover, it'll show up
              here with its current stage.
            </p>
            <Link
              to="/applications"
              className="inline-flex items-center gap-1 text-[13px] font-semibold text-accent mt-3"
            >
              Open Applications <ArrowUpRight size={13} />
            </Link>
          </div>
          <div className="border border-border-strong rounded-lg p-5">
            <div className="font-mono text-[11px] text-charcoal tracking-wide mb-3">
              SKILL GAP
            </div>
            <div className="flex items-center gap-2 text-[13.5px] mb-1.5">
              <CheckCircle2 size={14} className="text-good shrink-0" /> Java, Spring Boot, MySQL — ready
            </div>
            <div className="text-[13.5px] text-charcoal">
              Docker and Kafka come up most often in your target roles.
            </div>
            <Link
              to="/skill-gap"
              className="inline-flex items-center gap-1 text-[13px] font-semibold text-accent mt-3"
            >
              See full breakdown <ArrowUpRight size={13} />
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}
