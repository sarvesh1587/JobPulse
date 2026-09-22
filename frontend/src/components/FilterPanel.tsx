const FILTER_GROUPS: { label: string; options: string[] }[] = [
  { label: 'Experience', options: ['Internship', 'Entry-level', '0–2 years'] },
  { label: 'Graduate Year', options: ['2026', '2027', '2028'] },
  { label: 'Location', options: ['Pune', 'Bangalore', 'Hyderabad', 'Remote'] },
  { label: 'Freshness', options: ['Last 24h', 'Last 3 days', 'Last week'] },
  { label: 'Source', options: ['Greenhouse', 'Lever', 'Ashby'] },
]

export default function FilterPanel() {
  return (
    <div className="w-[240px] shrink-0 border-r border-border p-5 hidden lg:block">
      <div className="font-mono text-[11px] text-charcoal tracking-wide mb-4">FILTERS</div>
      <div className="flex flex-col gap-6">
        {FILTER_GROUPS.map((group) => (
          <div key={group.label}>
            <div className="text-[13px] font-semibold mb-2.5">{group.label}</div>
            <div className="flex flex-col gap-1.5">
              {group.options.map((opt) => (
                <label
                  key={opt}
                  className="flex items-center gap-2 text-[13px] text-charcoal hover:text-ink cursor-pointer"
                >
                  <input
                    type="checkbox"
                    className="w-3.5 h-3.5 rounded-sm border-border-strong accent-[var(--accent)]"
                  />
                  {opt}
                </label>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
