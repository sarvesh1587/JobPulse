import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Search } from 'lucide-react'
import { adminApi } from '../../lib/api'
import { formatDate, jobStatusLabel } from '../../lib/format'
import type { AdminJob, AdminUser, Role } from '../../lib/types'
import { useAuth } from '../../auth/AuthProvider'
import { Page } from '../../components/layout/Page'
import { Button } from '../../components/ui/Button'
import { Select } from '../../components/ui/Field'
import { EmptyState, ErrorState, ListSkeleton, Skeleton, errorMessage } from '../../components/ui/Feedback'
import { Pagination } from '../../components/ui/Pagination'
import { useToast } from '../../components/ui/Toast'

function useDebouncedParam(key: string, params: URLSearchParams, setParams: (p: URLSearchParams) => void) {
  const value = params.get(key) ?? ''
  const [draft, setDraft] = useState(value)
  useEffect(() => { setDraft(value) }, [value])
  useEffect(() => {
    const t = window.setTimeout(() => {
      if (draft === value) return
      const next = new URLSearchParams(params)
      if (draft) next.set(key, draft); else next.delete(key)
      next.delete('page')
      setParams(next)
    }, 300)
    return () => window.clearTimeout(t)
  }, [draft, value, key, params, setParams])
  return [draft, setDraft] as const
}

function UserRow({ user }: { user: AdminUser }) {
  const { user: me } = useAuth()
  const queryClient = useQueryClient()
  const toast = useToast()
  const toggle = useMutation({
    mutationFn: () => adminApi.setUserActive(user.id, !user.active),
    onSuccess: (u) => {
      toast.success(u.active ? `${u.fullName} reactivated` : `${u.fullName} deactivated. They're signed out on their next request.`)
      queryClient.invalidateQueries({ queryKey: ['admin'] })
    },
    onError: (e) => toast.error(errorMessage(e)),
  })
  return (
    <li className="flex flex-wrap items-center gap-x-4 gap-y-1 px-5 py-3.5">
      <div className="min-w-0 flex-1">
        <div className="text-sm font-semibold">{user.fullName} {!user.active && <span className="ml-1 font-mono text-[11px] text-miss">DEACTIVATED</span>}</div>
        <div className="truncate text-[13px] text-muted">{user.email}</div>
      </div>
      <span className="w-24 font-mono text-[11px] text-muted">{user.role}</span>
      <span className="w-28 text-[13px] text-muted">{formatDate(user.createdAt)}</span>
      {me?.id !== user.id && (
        <Button size="sm" variant={user.active ? 'danger' : 'secondary'} loading={toggle.isPending} onClick={() => toggle.mutate()}>
          {user.active ? 'Deactivate' : 'Reactivate'}
        </Button>
      )}
    </li>
  )
}

function JobRow({ job }: { job: AdminJob }) {
  const queryClient = useQueryClient()
  const toast = useToast()
  const open = job.status === 'ACTIVE'
  const toggle = useMutation({
    mutationFn: () => adminApi.setJobOpen(job.id, !open),
    onSuccess: () => {
      toast.success(open ? 'Listing taken down' : 'Listing restored')
      queryClient.invalidateQueries({ queryKey: ['admin'] })
      queryClient.invalidateQueries({ queryKey: ['jobs'] })
    },
    onError: (e) => toast.error(errorMessage(e)),
  })
  return (
    <li className="flex flex-wrap items-center gap-x-4 gap-y-1 px-5 py-3.5">
      <div className="min-w-0 flex-1">
        <Link to={`/jobs/${job.id}`} className="text-sm font-semibold hover:text-accent-text">{job.title}</Link>
        <div className="truncate text-[13px] text-muted">{job.companyName} · {job.origin === 'EMPLOYER' ? 'Employer post' : 'Imported'}</div>
      </div>
      <span className={`w-24 text-[13px] ${open ? 'text-good' : 'text-muted'}`}>{jobStatusLabel[job.status]}</span>
      <Button size="sm" variant={open ? 'danger' : 'secondary'} loading={toggle.isPending} onClick={() => toggle.mutate()}>
        {open ? 'Take down' : 'Restore'}
      </Button>
    </li>
  )
}

export default function AdminPage() {
  const [params, setParams] = useSearchParams()
  const tab = params.get('tab') === 'jobs' ? 'jobs' : 'users'
  const page = Math.max(0, Number(params.get('page') ?? 0) || 0)
  const role = (params.get('role') ?? '') as Role | ''
  const [query, setQuery] = useDebouncedParam('q', params, setParams)

  const stats = useQuery({ queryKey: ['admin', 'stats'], queryFn: adminApi.stats })
  const users = useQuery({
    queryKey: ['admin', 'users', params.get('q') ?? '', role, page],
    queryFn: () => adminApi.users(params.get('q') ?? '', role, page),
    enabled: tab === 'users',
    placeholderData: keepPreviousData,
  })
  const jobs = useQuery({
    queryKey: ['admin', 'jobs', params.get('q') ?? '', page],
    queryFn: () => adminApi.jobs(params.get('q') ?? '', page),
    enabled: tab === 'jobs',
    placeholderData: keepPreviousData,
  })

  const setTab = (t: 'users' | 'jobs') => setParams(t === 'jobs' ? { tab: 'jobs' } : {})
  const setPage = (p: number) => { const next = new URLSearchParams(params); if (p) next.set('page', String(p)); else next.delete('page'); setParams(next) }
  const active = tab === 'users' ? users : jobs

  return (
    <Page title="Admin" eyebrow="ADMIN CONSOLE" description="Platform health and moderation. Every action here is checked against the ADMIN role on the server.">
      {stats.isLoading && <Skeleton className="mb-10 h-24 w-full" />}
      {stats.data && (
        <dl className="mb-10 grid grid-cols-2 gap-px overflow-hidden rounded-xl border border-border bg-border sm:grid-cols-5">
          {[
            ['Candidates', stats.data.candidates], ['Employers', stats.data.employers],
            ['Open jobs', stats.data.activeJobs], ['All jobs', stats.data.totalJobs], ['Applications', stats.data.applications],
          ].map(([label, value]) => (
            <div key={label} className="bg-surface px-4 py-4">
              <dd className="font-mono text-2xl font-semibold">{value}</dd>
              <dt className="mt-1 font-mono text-label text-muted">{String(label).toUpperCase()}</dt>
            </div>
          ))}
        </dl>
      )}

      <div className="mb-4 flex flex-wrap items-center gap-3">
        <div role="tablist" className="flex gap-1 rounded-lg border border-border p-1">
          {(['users', 'jobs'] as const).map((t) => (
            <button key={t} role="tab" aria-selected={tab === t} onClick={() => setTab(t)}
              className={`h-8 rounded-md px-3.5 text-sm font-medium ${tab === t ? 'bg-raised text-ink' : 'text-muted hover:text-ink'}`}>
              {t === 'users' ? 'Users' : 'Listings'}
            </button>
          ))}
        </div>
        <label className="flex min-w-[220px] flex-1 items-center gap-2 rounded-lg border border-border-strong bg-surface px-3 focus-within:border-accent">
          <Search size={15} className="text-muted" />
          <span className="sr-only">Search</span>
          <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder={tab === 'users' ? 'Search name or email' : 'Search title or company'}
            className="h-9 w-full bg-transparent text-sm outline-none" />
        </label>
        {tab === 'users' && (
          <Select aria-label="Filter by role" value={role} className="w-auto"
            onChange={(e) => { const next = new URLSearchParams(params); if (e.target.value) next.set('role', e.target.value); else next.delete('role'); next.delete('page'); setParams(next) }}>
            <option value="">All roles</option>
            <option value="CANDIDATE">Candidates</option>
            <option value="EMPLOYER">Employers</option>
            <option value="ADMIN">Admins</option>
          </Select>
        )}
      </div>

      {active.isLoading && <ListSkeleton rows={5} className="h-14" />}
      {active.isError && <ErrorState error={active.error} onRetry={() => active.refetch()} />}
      {tab === 'users' && users.data && (users.data.content.length === 0
        ? <EmptyState compact title="No users match" />
        : <>
            <ul className="divide-y divide-border overflow-hidden rounded-xl border border-border">{users.data.content.map((u) => <UserRow key={u.id} user={u} />)}</ul>
            <div className="mt-4"><Pagination page={users.data.page} totalPages={users.data.totalPages} onChange={setPage} /></div>
          </>)}
      {tab === 'jobs' && jobs.data && (jobs.data.content.length === 0
        ? <EmptyState compact title="No listings match" />
        : <>
            <ul className="divide-y divide-border overflow-hidden rounded-xl border border-border">{jobs.data.content.map((j) => <JobRow key={j.id} job={j} />)}</ul>
            <div className="mt-4"><Pagination page={jobs.data.page} totalPages={jobs.data.totalPages} onChange={setPage} /></div>
          </>)}
    </Page>
  )
}
