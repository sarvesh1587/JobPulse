import { skillStatuses, skillGapPlan, targetRole } from '../data/mockSkillGap'
import PriorityCard from '../components/PriorityCard'

export default function SkillGap() {
  const readyCount = skillStatuses.filter((s) => s.ready).length

  return (
    <div className="flex-1 overflow-y-auto">
      <div className="max-w-[760px] mx-auto px-8 py-10">
        <div className="font-mono text-[11px] text-charcoal tracking-wide mb-2">
          SKILL GAP · TARGET: {targetRole.toUpperCase()}
        </div>
        <h1 className="text-[28px] font-extrabold tracking-tight mb-1">
          {readyCount} of {skillStatuses.length} skills ready.
        </h1>
        <p className="text-[14px] text-charcoal mb-8">
          Your resume, matched against what {targetRole.toLowerCase()} roles actually ask for.
        </p>

        <div className="border border-border-strong rounded-lg p-5 mb-10">
          <div className="space-y-3">
            {skillStatuses.map((s) => (
              <div key={s.name} className="grid grid-cols-[110px_1fr_36px] gap-3 items-center">
                <div className="text-[13.5px] font-semibold">{s.name}</div>
                <div className="h-[7px] rounded-full bg-border overflow-hidden">
                  <div
                    className={`h-full rounded-full ${
                      s.ready ? 'bg-ink' : 'bg-border-strong'
                    }`}
                    style={{ width: `${s.level}%` }}
                  />
                </div>
                <div className="font-mono text-[12px] text-right">
                  {s.ready ? (
                    <span className="text-good">✓</span>
                  ) : (
                    <span className="text-charcoal">—</span>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="flex items-center justify-between mb-4">
          <h2 className="text-[17px] font-bold tracking-tight">Closing the gap</h2>
          <span className="font-mono text-[11px] text-charcoal">
            {skillGapPlan.length} SKILLS · ORDERED BY IMPACT
          </span>
        </div>

        <div className="space-y-3">
          {skillGapPlan.map((item) => (
            <PriorityCard key={item.name} item={item} />
          ))}
        </div>

        <p className="text-[12px] text-charcoal mt-8 leading-relaxed">
          Effort estimates are rough starting points, not guarantees — actual time depends a lot
          on your background and how deep a role expects you to go with each skill.
        </p>
      </div>
    </div>
  )
}
