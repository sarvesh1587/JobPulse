import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { useSearchParams } from 'react-router-dom'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { MapPin, Search, SlidersHorizontal, X } from 'lucide-react'
import { jobsApi, type JobQuery } from '../lib/api'
import { activeFilterCount, parseJobQuery, toSearchParams } from '../lib/jobFilters'
import { employmentTypeLabel, experienceLevelLabel, workModeLabel } from '../lib/format'
import { useTitle } from '../components/layout/Page'
import { JobCard } from '../components/jobs/JobCard'
import { EmptyState, ErrorState, ListSkeleton } from '../components/ui/Feedback'
import { Pagination } from '../components/ui/Pagination'
import { Select, ToggleChip } from '../components/ui/Field'
import { Button } from '../components/ui/Button'

const POSTED_OPTIONS = [
  { value: '', label: 'Any time' },
  { value: '1', label: 'Past 24 hours' },
  { value: '7', label: 'Past week' },
  { value: '30', label: 'Past month' },
]

const SALARY_OPTIONS = [
  { value: '', label: 'Any salary' },
  { value: '500000', label: '₹5 LPA+' },
  { value: '1000000', label: '₹10 LPA+' },
  { value: '2000000', label: '₹20 LPA+' },
]

function FilterGroup({ title, children }: { title: string; children: ReactNode }) {
  return (
    <fieldset>
      <legend className="mb-2.5 font-mono text-label text-muted">{title}</legend>
      <div className="flex flex-wrap gap-1.5">{children}</div>
    </fieldset>
  )
}

function toggle(list: string[] | undefined, value: string, on: boolean): string[] {
  const current = list ?? []
  return on ? [...current, value] : current.filter((v) => v !== value)
}

function Filters({ query, update }: { query: JobQuery; update: (patch: Partial<JobQuery>) => void }) {
  return (
    <div className="space-y-6">
      <FilterGroup title="JOB TYPE">
        {Object.entries(employmentTypeLabel).map(([value, label]) => (
          <ToggleChip key={value} checked={!!query.type?.includes(value)} onChange={(on) => update({ type: toggle(query.type, value, on) })}>{label}</ToggleChip>
        ))}
      </FilterGroup>
      <FilterGroup title="EXPERIENCE">
        {Object.entries(experienceLevelLabel).map(([value, label]) => (
          <ToggleChip key={value} checked={!!query.level?.includes(value)} onChange={(on) => update({ level: toggle(query.level, value, on) })}>{label}</ToggleChip>
        ))}
      </FilterGroup>
      <FilterGroup title="WORK MODE">
        {Object.entries(workModeLabel).map(([value, label]) => (
          <ToggleChip key={value} checked={!!query.mode?.includes(value)} onChange={(on) => update({ mode: toggle(query.mode, value, on) })}>{label}</ToggleChip>
        ))}
      </FilterGroup>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-1">
        <label className="block">
          <span className="mb-2 block font-mono text-label text-muted">POSTED</span>
          <Select value={query.postedWithinDays ?? ''} onChange={(e) => update({ postedWithinDays: e.target.value || undefined })}>
            {POSTED_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
          </Select>
        </label>
        <label className="block">
          <span className="mb-2 block font-mono text-label text-muted">MINIMUM SALARY</span>
          <Select value={query.minSalary ?? ''} onChange={(e) => update({ minSalary: e.target.value || undefined })}>
            {SALARY_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
          </Select>
        </label>
      </div>
    </div>
  )
}

export default function JobsPage() {
  useTitle('Jobs')
  const [params, setParams] = useSearchParams()
  const query = parseJobQuery(params)
  const [keyword, setKeyword] = useState(query.q ?? '')
  const [location, setLocation] = useState(query.location ?? '')
  const [filtersOpen, setFiltersOpen] = useState(false)

  // Keep the inputs in step when the URL changes from outside (Back, or a link).
  useEffect(() => { setKeyword(query.q ?? ''); setLocation(query.location ?? '') }, [query.q, query.location])

  const results = useQuery({
    queryKey: ['jobs', 'search', params.toString()],
    queryFn: () => jobsApi.search(query),
    placeholderData: keepPreviousData,   // keep the old page visible while the next one loads
  })

  /** Any filter change resets to the first page and becomes a new history entry. */
  const update = (patch: Partial<JobQuery>) => {
    setParams(toSearchParams({ ...query, ...patch, page: 'page' in patch ? patch.page : 0 }))
  }

  const onSearch = (e: FormEvent) => {
    e.preventDefault()
    update({ q: keyword.trim() || undefined, location: location.trim() || undefined })
  }

  const filterCount = activeFilterCount(query)
  const data = results.data

  return (
    <div className="mx-auto max-w-page px-4 pb-10 pt-8 sm:px-6">
      <h1 className="sr-only">Search jobs</h1>
      <form onSubmit={onSearch} role="search" className="flex flex-col gap-2 rounded-xl border border-border-strong bg-surface p-2 sm:flex-row">
        <label className="flex flex-1 items-center gap-2.5 rounded-lg px-3 focus-within:bg-raised">
          <Search size={17} className="shrink-0 text-muted" />
          <span className="sr-only">Keyword</span>
          <input value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="Title, skill or company"
            className="h-11 w-full bg-transparent text-[15px] outline-none placeholder:text-muted/70" />
        </label>
        <label className="flex items-center gap-2.5 rounded-lg px-3 focus-within:bg-raised sm:w-56 sm:border-l sm:border-border">
          <MapPin size={17} className="shrink-0 text-muted" />
          <span className="sr-only">Location</span>
          <input value={location} onChange={(e) => setLocation(e.target.value)} placeholder="City or remote"
            className="h-11 w-full bg-transparent text-[15px] outline-none placeholder:text-muted/70" />
        </label>
        <Button type="submit" size="lg" className="h-11">Search</Button>
      </form>

      <div className="mt-8 grid gap-8 lg:grid-cols-[240px_1fr]">
        <aside className="hidden lg:block" aria-label="Filters">
          <div className="sticky top-24">
            <div className="mb-5 flex items-center justify-between">
              <h2 className="text-sm font-bold">Filters</h2>
              {filterCount > 0 && (
                <button onClick={() => setParams(toSearchParams({ q: query.q, sort: query.sort }))} className="text-[13px] font-medium text-accent-text hover:underline">
                  Clear all
                </button>
              )}
            </div>
            <Filters query={query} update={update} />
          </div>
        </aside>

        <section aria-label="Results" className="min-w-0">
          <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
            <p className="text-sm text-muted" aria-live="polite">
              {data ? <><span className="font-semibold text-ink">{data.totalElements.toLocaleString()}</span> {data.totalElements === 1 ? 'role' : 'roles'}{query.q && <> for <span className="text-ink">"{query.q}"</span></>}</> : ' '}
            </p>
            <div className="flex items-center gap-2">
              <button onClick={() => setFiltersOpen(true)} className="inline-flex h-9 items-center gap-2 rounded-lg border border-border-strong px-3 text-sm font-medium hover:bg-raised lg:hidden">
                <SlidersHorizontal size={15} /> Filters{filterCount > 0 && ` (${filterCount})`}
              </button>
              <label className="flex items-center gap-2 text-sm text-muted">
                <span className="hidden sm:inline">Sort</span>
                <Select value={query.sort ?? 'RECENT'} onChange={(e) => update({ sort: e.target.value })} className="h-9 w-auto">
                  <option value="RECENT">Most recent</option>
                  <option value="SALARY">Highest salary</option>
                </Select>
              </label>
            </div>
          </div>

          {results.isLoading && <ListSkeleton rows={5} className="h-[150px]" />}
          {results.isError && <ErrorState error={results.error} onRetry={() => results.refetch()} />}
          {data && data.content.length === 0 && (
            <EmptyState icon={<Search size={18} />} title="No roles match these filters"
              action={filterCount > 0 || query.q ? <Button variant="secondary" onClick={() => setParams(new URLSearchParams())}>Clear search</Button> : undefined}>
              Try a broader keyword, or remove a filter or two.
            </EmptyState>
          )}
          {data && data.content.length > 0 && (
            <div className={`space-y-3 transition-opacity ${results.isPlaceholderData ? 'opacity-60' : ''}`}>
              {data.content.map((job, i) => <JobCard key={job.id} job={job} index={i} />)}
              <Pagination page={data.page} totalPages={data.totalPages} onChange={(page) => { update({ page }); window.scrollTo({ top: 0, behavior: 'smooth' }) }} />
            </div>
          )}
        </section>
      </div>

      {filtersOpen && (
        <div className="fixed inset-0 z-50 lg:hidden" role="dialog" aria-modal="true" aria-label="Filters">
          <div className="absolute inset-0 bg-black/50" onClick={() => setFiltersOpen(false)} />
          <div className="rise absolute inset-x-0 bottom-0 max-h-[85vh] overflow-y-auto rounded-t-2xl border-t border-border-strong bg-surface p-5">
            <div className="mb-5 flex items-center justify-between">
              <h2 className="text-base font-bold">Filters</h2>
              <button onClick={() => setFiltersOpen(false)} aria-label="Close filters" className="rounded-md p-1.5 hover:bg-raised"><X size={18} /></button>
            </div>
            <Filters query={query} update={update} />
            <Button className="mt-6 w-full" onClick={() => setFiltersOpen(false)}>
              Show {data ? data.totalElements.toLocaleString() : ''} results
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}
