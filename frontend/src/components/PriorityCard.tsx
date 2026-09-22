import { useState } from 'react'
import { ChevronDown, Clock, BookOpen, Hammer } from 'lucide-react'
import type { SkillGapItem } from '../data/mockSkillGap'

export default function PriorityCard({ item }: { item: SkillGapItem }) {
  const [open, setOpen] = useState(item.priority === 1)

  return (
    <div className="border border-border-strong rounded-lg overflow-hidden">
      <button
        onClick={() => setOpen((v) => !v)}
        className="w-full flex items-center justify-between px-5 py-4 hover:bg-surface-raised transition-colors"
      >
        <div className="flex items-center gap-3">
          <span className="font-mono text-[10.5px] px-2 py-0.5 rounded-full border border-border-strong text-charcoal">
            PRIORITY {item.priority}
          </span>
          <span className="font-bold text-[15px]">{item.name}</span>
        </div>
        <ChevronDown
          size={16}
          className={`text-charcoal transition-transform ${open ? 'rotate-180' : ''}`}
        />
      </button>

      {open && (
        <div className="px-5 pb-5 pt-1 border-t border-border space-y-5">
          <div className="flex items-center gap-2 text-[13px] text-charcoal">
            <Clock size={14} className="shrink-0" />
            Suggested preparation effort: <span className="text-ink font-medium">{item.effortEstimate}</span>
          </div>

          <div>
            <div className="flex items-center gap-2 text-[12.5px] font-semibold mb-2.5">
              <BookOpen size={14} className="text-accent shrink-0" /> Where to learn it
            </div>
            <ul className="space-y-1.5">
              {item.resources.map((r) => (
                <li key={r} className="text-[13.5px] text-charcoal pl-4 relative">
                  <span className="absolute left-0 top-[9px] w-1 h-1 rounded-full bg-border-strong" />
                  {r}
                </li>
              ))}
            </ul>
          </div>

          <div>
            <div className="flex items-center gap-2 text-[12.5px] font-semibold mb-2">
              <Hammer size={14} className="text-accent shrink-0" /> Project to prove it
            </div>
            <p className="text-[13.5px] text-charcoal leading-relaxed">{item.projectIdea}</p>
          </div>
        </div>
      )}
    </div>
  )
}
