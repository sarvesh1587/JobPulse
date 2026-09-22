import { useState } from 'react'
import { Search } from 'lucide-react'
import FilterPanel from '../components/FilterPanel'
import JobRow from '../components/JobRow'
import JobIntelligencePanel from '../components/JobIntelligencePanel'
import { mockJobs } from '../data/mockJobs'

export default function Discover() {
  const [query, setQuery] = useState('Java backend internship Pune 2027')
  const [selectedId, setSelectedId] = useState<string>(mockJobs[0].id)

  const selectedJob = mockJobs.find((j) => j.id === selectedId) ?? null

  return (
    <div className="flex h-full min-h-0">
      <FilterPanel />

      <div className="flex-1 min-w-0 flex flex-col">
        <div className="border-b border-border px-6 py-4 flex items-center gap-3">
          <Search size={16} className="text-charcoal shrink-0" />
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Java backend internship Pune 2027"
            className="flex-1 bg-transparent outline-none font-mono text-[13.5px] text-ink placeholder:text-charcoal"
          />
          <span className="font-mono text-[10.5px] text-charcoal shrink-0">
            {mockJobs.length} RESULTS
          </span>
        </div>

        <div className="flex-1 overflow-y-auto">
          {mockJobs.map((job) => (
            <JobRow
              key={job.id}
              job={job}
              selected={job.id === selectedId}
              onSelect={setSelectedId}
            />
          ))}
        </div>
      </div>

      <JobIntelligencePanel job={selectedJob} />
    </div>
  )
}
