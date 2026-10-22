import { useEffect, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { ArrowLeft, Building2, ExternalLink, MapPin, Search } from 'lucide-react'
import { companiesApi } from '../lib/api'
import { employmentTypeLabel, experienceLevelLabel, relativeTime } from '../lib/format'
import type { EmploymentType, ExperienceLevel } from '../lib/types'
import { Page, Section, useTitle } from '../components/layout/Page'
import { JobCard } from '../components/jobs/JobCard'
import { EmptyState, ErrorState, ListSkeleton, Skeleton } from '../components/ui/Feedback'
import { Pagination } from '../components/ui/Pagination'

export function CompaniesPage() {
  const [params, setParams] = useSearchParams()
  const q = params.get('q') ?? ''
  const page = Math.max(0, Number(params.get('page') ?? 0) || 0)
  const [draft, setDraft] = useState(q)
  useEffect(() => { setDraft(q) }, [q])

  // Search as you type, debounced so it doesn't send a request per keystroke.
  useEffect(() => {
    const t = window.setTimeout(() => { if (draft !== q) setParams(draft ? { q: draft } : {}) }, 300)
    return () => window.clearTimeout(t)
  }, [draft, q, setParams])

  const companies = useQuery({
    queryKey: ['companies', q, page],
    queryFn: () => companiesApi.list(q, page),
    placeholderData: keepPreviousData,
  })

  return (
    <Page title="Companies" eyebrow="COMPANIES" description="Who's hiring right now, ranked by open roles.">
      <label className="mb-6 flex max-w-md items-center gap-2.5 rounded-lg border border-border-strong bg-surface px-3 focus-within:border-accent">
        <Search size={16} className="text-muted" />
        <span className="sr-only">Search companies</span>
        <input value={draft} onChange={(e) => setDraft(e.target.value)} placeholder="Search companies" className="h-10 w-full bg-transparent text-sm outline-none" />
      </label>
      {companies.isLoading && <ListSkeleton rows={6} className="h-[110px]" />}
      {companies.isError && <ErrorState error={companies.error} onRetry={() => companies.refetch()} />}
      {companies.data && companies.data.content.length === 0 && (
        <EmptyState icon={<Building2 size={18} />} title={q ? `No companies match "${q}"` : 'No companies yet'} />
      )}
      {companies.data && companies.data.content.length > 0 && (
        <>
          <ul className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {companies.data.content.map((c, i) => (
              <li key={c.id} className="rise" style={{ ['--i' as string]: i }}>
                <Link to={`/companies/${c.id}`} className="block h-full rounded-xl border border-border bg-surface p-5 transition-colors hover:border-border-strong">
                  <div className="font-bold tracking-tight">{c.name}</div>
                  {c.headquarters && <div className="mt-0.5 flex items-center gap-1 text-[13px] text-muted"><MapPin size={12} /> {c.headquarters}</div>}
                  <div className="mt-4 flex gap-5 font-mono text-xs text-muted">
                    <span><b className="text-base text-ink">{c.openRoles}</b> open</span>
                    <span><b className="text-base text-ink">{c.internships}</b> intern</span>
                    <span><b className="text-base text-ink">{c.entryLevel}</b> entry</span>
                  </div>
                </Link>
              </li>
            ))}
          </ul>
          <div className="mt-6">
            <Pagination page={companies.data.page} totalPages={companies.data.totalPages}
              onChange={(p) => setParams({ ...(q ? { q } : {}), ...(p ? { page: String(p) } : {}) })} />
          </div>
        </>
      )}
    </Page>
  )
}

export function CompanyDetailPage() {
  const { id = '' } = useParams()
  const company = useQuery({ queryKey: ['company', id], queryFn: () => companiesApi.get(id) })
  useTitle(company.data?.name ?? 'Company')

  if (company.isLoading) return <div className="mx-auto max-w-page space-y-4 px-4 py-10 sm:px-6"><Skeleton className="h-10 w-1/3" /><Skeleton className="h-40 w-full" /></div>
  if (company.isError) return <div className="mx-auto max-w-page px-4 py-10 sm:px-6"><ErrorState error={company.error} onRetry={() => company.refetch()} /></div>

  const c = company.data!
  const breakdown = [
    ...Object.entries(c.rolesByEmploymentType).map(([k, v]) => [employmentTypeLabel[k as EmploymentType] ?? k, v] as const),
    ...Object.entries(c.rolesByExperienceLevel).map(([k, v]) => [experienceLevelLabel[k as ExperienceLevel] ?? k, v] as const),
  ]

  return (
    <div className="mx-auto max-w-page px-4 pb-10 pt-6 sm:px-6">
      <Link to="/companies" className="inline-flex items-center gap-1.5 text-sm text-muted hover:text-ink"><ArrowLeft size={15} /> All companies</Link>
      <header className="mt-6">
        <h1 className="text-[30px] font-extrabold tracking-tight">{c.name}</h1>
        <div className="mt-2 flex flex-wrap gap-x-5 gap-y-1 text-sm text-muted">
          {c.headquarters && <span className="inline-flex items-center gap-1"><MapPin size={14} /> {c.headquarters}</span>}
          {c.website && <a href={c.website} target="_blank" rel="noopener noreferrer" className="inline-flex items-center gap-1 hover:text-ink">Website <ExternalLink size={12} /></a>}
          {c.lastVerifiedAt && <span>Listings checked {relativeTime(c.lastVerifiedAt)}</span>}
        </div>
        {c.description && <p className="mt-4 max-w-[70ch] text-[15px] leading-relaxed text-muted">{c.description}</p>}
      </header>

      <div className="mt-10 grid gap-10 lg:grid-cols-[1fr_280px]">
        <Section title={`${c.openRoles} open ${c.openRoles === 1 ? 'role' : 'roles'}`}>
          {c.openJobs.length === 0
            ? <EmptyState compact title="No open roles right now" />
            : <div className="space-y-3">{c.openJobs.map((j, i) => <JobCard key={j.id} job={j} index={i} />)}</div>}
        </Section>
        <aside className="space-y-8">
          {breakdown.length > 0 && (
            <section>
              <h2 className="mb-3 font-mono text-label text-muted">OPEN ROLES BY TYPE</h2>
              <dl className="space-y-1.5 text-sm">
                {breakdown.map(([label, count]) => (
                  <div key={label} className="flex justify-between"><dt className="text-muted">{label}</dt><dd className="font-mono">{count}</dd></div>
                ))}
              </dl>
            </section>
          )}
          {c.topSkills.length > 0 && (
            <section>
              <h2 className="mb-3 font-mono text-label text-muted">SKILLS THEY ASK FOR</h2>
              <ul className="flex flex-wrap gap-1.5">{c.topSkills.map((s) => <li key={s} className="rounded-md bg-raised px-2 py-0.5 font-mono text-[11px] text-muted">{s}</li>)}</ul>
            </section>
          )}
          {c.locations.length > 0 && (
            <section>
              <h2 className="mb-3 font-mono text-label text-muted">LOCATIONS</h2>
              <ul className="space-y-1 text-sm">{c.locations.map((l) => <li key={l}>{l}</li>)}</ul>
            </section>
          )}
        </aside>
      </div>
    </div>
  )
}
