import { NavLink } from 'react-router-dom'
import {
  LayoutGrid,
  Search,
  Bookmark,
  ClipboardList,
  BarChart3,
  Building2,
  LineChart,
  UserCircle,
} from 'lucide-react'

const NAV_ITEMS = [
  { to: '/', label: 'Overview', icon: LayoutGrid },
  { to: '/discover', label: 'Discover', icon: Search },
  { to: '/saved', label: 'Saved', icon: Bookmark },
  { to: '/applications', label: 'Applications', icon: ClipboardList },
  { to: '/skill-gap', label: 'Skill Gap', icon: BarChart3 },
  { to: '/companies', label: 'Companies', icon: Building2 },
  { to: '/insights', label: 'Insights', icon: LineChart },
  { to: '/profile', label: 'Profile', icon: UserCircle },
]

export default function Sidebar() {
  return (
    <aside className="hidden md:flex w-[220px] shrink-0 flex-col border-r border-border bg-surface">
      <div className="flex items-center gap-2.5 px-6 py-5 border-b border-border">
        <span className="flex items-end gap-[2px] h-4 rounded-md bg-gradient-to-br from-accent/15 to-good/10 p-1 -m-1">
          {[0.4, 1, 0.65, 0.85].map((h, i) => (
            <span
              key={i}
              className="w-[3px] rounded-sm bg-gradient-to-b from-accent-soft to-accent"
              style={{ height: `${h * 100}%` }}
            />
          ))}
        </span>
        <span className="font-extrabold tracking-tight text-[15px]">JOBPULSE</span>
      </div>

      <nav className="flex-1 px-3 py-4 flex flex-col gap-1">
        {NAV_ITEMS.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            end={to === '/'}
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2.5 rounded-md text-[14px] font-medium transition-colors ${
                isActive
                  ? 'bg-surface-raised text-ink border border-border-strong'
                  : 'text-charcoal hover:text-ink hover:bg-surface-raised border border-transparent'
              }`
            }
          >
            <Icon size={16} strokeWidth={2} />
            {label}
          </NavLink>
        ))}
      </nav>

      <div className="px-4 py-4 border-t border-border">
        <div className="font-mono text-[10.5px] text-charcoal flex items-center gap-2">
          <span className="w-[5px] h-[5px] rounded-full bg-good" />
          3 SOURCES LIVE
        </div>
      </div>
    </aside>
  )
}
