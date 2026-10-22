import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ArrowRight, MapPin, Search } from 'lucide-react'
import { jobsApi } from '../lib/api'
import { useTitle } from '../components/layout/Page'
import { JobCard } from '../components/jobs/JobCard'
import { ButtonLink } from '../components/ui/Button'
import { EmptyState, ErrorState, ListSkeleton } from '../components/ui/Feedback'
import { useAuth, homeFor } from '../auth/AuthProvider'

const QUICK_FILTERS = [
  { label: 'Internships', to: '/jobs?type=INTERNSHIP' },
  { label: 'Entry level', to: '/jobs?level=ENTRY' },
  { label: 'Remote', to: '/jobs?mode=REMOTE' },
  { label: 'Java', to: '/jobs?q=java' },
  { label: 'React', to: '/jobs?q=react' },
]

const STEPS = [
  { n: '01', title: 'Build your profile once', body: 'Skills, education, experience and the roles you want. Upload a resume and we suggest the skills it mentions.' },
  { n: '02', title: 'See why each job fits', body: 'Every match score breaks down into skills, experience, location and freshness, along with what you are missing.' },
  { n: '03', title: 'Track every application', body: 'Apply in-app to employers on JobPulse, or log applications you sent elsewhere. Every stage sits in one tracker.' },
]

/** Illustration of the match breakdown. Labelled as an example because it isn't anyone's real score. */
function ScorePreview() {
  const rows = [['Skills', 86], ['Experience', 100], ['Education', 100], ['Location', 100], ['Freshness', 90]] as const
  return (
    <figure className="rise rounded-xl border border-border-strong bg-surface p-5 shadow-pop sm:p-6" style={{ ['--i' as string]: 3 }}>
      <div className="flex items-center justify-between">
        <span className="font-mono text-label text-muted">WHY THIS SCORE</span>
        <span className="rounded-full border border-border-strong px-2 py-0.5 font-mono text-[10px] text-muted">EXAMPLE</span>
      </div>
      <div className="mt-4 flex items-end gap-3">
        <span className="font-mono text-5xl font-semibold leading-none tracking-tight">91</span>
        <div className="pb-1">
          <div className="text-sm font-bold text-good">Strong match</div>
          <div className="text-[13px] text-muted">Java Backend Engineer · Pune</div>
        </div>
      </div>
      <dl className="mt-6 space-y-3">
        {rows.map(([label, value], i) => (
          <div key={label} className="grid grid-cols-[86px_1fr_30px] items-center gap-3">
            <dt className="text-[13px] text-muted">{label}</dt>
            <dd className="h-1.5 overflow-hidden rounded-full bg-border" aria-hidden>
              <div className="h-full origin-left rounded-full bg-accent" style={{ width: `${value}%`, animation: `rise 600ms ${200 + i * 70}ms both` }} />
            </dd>
            <dd className="text-right font-mono text-xs text-muted">{value}</dd>
          </div>
        ))}
      </dl>
      <figcaption className="mt-5 border-t border-border pt-4 text-[13px] text-muted">
        <span className="font-semibold text-miss">Missing:</span> Kafka. Appears in 4 of your target roles.
      </figcaption>
    </figure>
  )
}

export default function LandingPage() {
  useTitle('')
  const navigate = useNavigate()
  const { user } = useAuth()
  const [q, setQ] = useState('')
  const [location, setLocation] = useState('')
  const featured = useQuery({ queryKey: ['jobs', 'featured'], queryFn: jobsApi.featured })

  const onSearch = (e: FormEvent) => {
    e.preventDefault()
    const params = new URLSearchParams()
    if (q.trim()) params.set('q', q.trim())
    if (location.trim()) params.set('location', location.trim())
    navigate(`/jobs${params.toString() ? `?${params}` : ''}`)
  }

  return (
    <>
      <section className="relative overflow-hidden border-b border-border">
        {/* One soft warm glow behind the hero: the only decorative gradient on the site. */}
        <div aria-hidden className="pointer-events-none absolute -left-40 -top-40 h-[520px] w-[520px] rounded-full bg-accent/10 blur-[120px]" />
        <div className="relative mx-auto grid max-w-page items-center gap-12 px-4 pb-16 pt-14 sm:px-6 lg:grid-cols-[1.15fr_0.85fr] lg:pb-24 lg:pt-20">
          <div>
            <p className="rise mb-5 flex items-center gap-2 font-mono text-label text-muted">
              <span className="h-px w-6 bg-accent" /> JOB DISCOVERY FOR EARLY-CAREER DEVELOPERS
            </p>
            <h1 className="rise text-[44px] font-extrabold leading-[1.02] tracking-[-0.035em] sm:text-[60px] lg:text-[68px] text-balance" style={{ ['--i' as string]: 1 }}>
              Stop searching.<br /><span className="text-accent-text">Start targeting.</span>
            </h1>
            <p className="rise mt-5 max-w-[46ch] text-[17px] leading-relaxed text-muted text-pretty" style={{ ['--i' as string]: 2 }}>
              JobPulse ranks roles against your actual profile and shows its reasoning, so you spend your time on applications that have a chance.
            </p>

            <form onSubmit={onSearch} role="search" className="rise mt-8 flex flex-col gap-2 rounded-xl border border-border-strong bg-surface p-2 shadow-pop sm:flex-row" style={{ ['--i' as string]: 3 }}>
              <label className="flex flex-1 items-center gap-2.5 rounded-lg px-3 focus-within:bg-raised">
                <Search size={17} className="shrink-0 text-muted" />
                <span className="sr-only">Role, skill or company</span>
                <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Role, skill or company"
                  className="h-11 w-full bg-transparent text-[15px] outline-none placeholder:text-muted/70" />
              </label>
              <label className="flex items-center gap-2.5 rounded-lg px-3 focus-within:bg-raised sm:w-48 sm:border-l sm:border-border">
                <MapPin size={17} className="shrink-0 text-muted" />
                <span className="sr-only">Location</span>
                <input value={location} onChange={(e) => setLocation(e.target.value)} placeholder="City or remote"
                  className="h-11 w-full bg-transparent text-[15px] outline-none placeholder:text-muted/70" />
              </label>
              <button type="submit" className="h-11 rounded-lg bg-accent px-6 text-[15px] font-semibold text-accent-ink transition-colors hover:bg-accent/90">
                Search jobs
              </button>
            </form>

            <div className="rise mt-4 flex flex-wrap items-center gap-2" style={{ ['--i' as string]: 4 }}>
              <span className="font-mono text-label text-muted">TRY</span>
              {QUICK_FILTERS.map((f) => (
                <Link key={f.label} to={f.to} className="rounded-full border border-border-strong px-3 py-1 text-[13px] text-muted transition-colors hover:border-accent/60 hover:text-ink">
                  {f.label}
                </Link>
              ))}
            </div>
          </div>
          <ScorePreview />
        </div>
      </section>

      <section className="mx-auto max-w-page px-4 pt-16 sm:px-6">
        <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
          <div>
            <p className="mb-2 font-mono text-label text-muted">LATEST LISTINGS</p>
            <h2 className="text-2xl font-extrabold tracking-tight">Fresh on JobPulse</h2>
          </div>
          <Link to="/jobs" className="inline-flex items-center gap-1 text-sm font-semibold text-accent-text hover:gap-2 transition-all">
            Browse all jobs <ArrowRight size={15} />
          </Link>
        </div>
        {featured.isLoading && <div className="grid gap-3 md:grid-cols-2"><ListSkeleton rows={4} className="h-[150px]" /></div>}
        {featured.isError && <ErrorState error={featured.error} onRetry={() => featured.refetch()} />}
        {featured.data && featured.data.length === 0 && (
          <EmptyState title="No open listings yet">
            Employers can post roles from their workspace, and an admin can import public job boards.
          </EmptyState>
        )}
        {featured.data && featured.data.length > 0 && (
          <div className="grid gap-3 md:grid-cols-2">
            {featured.data.map((job, i) => <JobCard key={job.id} job={job} index={i} />)}
          </div>
        )}
      </section>

      <section className="mx-auto max-w-page px-4 pt-24 sm:px-6">
        <p className="mb-2 font-mono text-label text-muted">HOW IT WORKS</p>
        <h2 className="max-w-[22ch] text-2xl font-extrabold tracking-tight sm:text-3xl text-balance">Don't rank jobs. Rank your chances.</h2>
        <ol className="mt-10 grid border-t border-border md:grid-cols-3">
          {STEPS.map((s, i) => (
            <li key={s.n} className={`py-8 md:pr-8 ${i > 0 ? 'border-t border-border md:border-l md:border-t-0 md:pl-8' : ''}`}>
              <span className="font-mono text-sm text-accent-text">{s.n}</span>
              <h3 className="mt-3 text-lg font-bold tracking-tight">{s.title}</h3>
              <p className="mt-2 text-[15px] leading-relaxed text-muted text-pretty">{s.body}</p>
            </li>
          ))}
        </ol>
      </section>

      <section className="mx-auto max-w-page px-4 pt-16 sm:px-6">
        <div className="grid gap-8 rounded-xl border border-border-strong bg-surface p-6 sm:p-10 md:grid-cols-[1.2fr_1fr] md:items-center">
          <div>
            <p className="mb-2 font-mono text-label text-muted">FOR EMPLOYERS</p>
            <h2 className="text-2xl font-extrabold tracking-tight text-balance">Hire people who already know why they fit.</h2>
            <p className="mt-3 max-w-[52ch] text-[15px] leading-relaxed text-muted">
              Post a role, review applicants with their skills and cover note side by side, and move each one through a clear pipeline. Candidates see every update.
            </p>
          </div>
          <div className="flex flex-col gap-2 sm:flex-row md:justify-end">
            {user ? (
              <ButtonLink to={homeFor(user.role)} size="lg">Go to your workspace</ButtonLink>
            ) : (
              <>
                <ButtonLink to="/register?role=EMPLOYER" size="lg">Post a job</ButtonLink>
                <ButtonLink to="/register" size="lg" variant="secondary">I'm looking for work</ButtonLink>
              </>
            )}
          </div>
        </div>
      </section>
    </>
  )
}
