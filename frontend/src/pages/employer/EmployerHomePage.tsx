import { Link, useSearchParams } from 'react-router-dom'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Briefcase, Pencil, Plus, Users } from 'lucide-react'
import { employerApi } from '../../lib/api'
import { PIPELINE, employmentTypeLabel, formatDate, statusLabel, workModeLabel } from '../../lib/format'
import type { EmployerJob } from '../../lib/types'
import { Page, Section } from '../../components/layout/Page'
import { JobStatusBadge } from '../../components/ui/Badge'
import { Button, ButtonLink } from '../../components/ui/Button'
import { EmptyState, ErrorState, ListSkeleton, Skeleton, errorMessage } from '../../components/ui/Feedback'
import { Pagination } from '../../components/ui/Pagination'
import { useToast } from '../../components/ui/Toast'

function OpenToggle({ job }: { job: EmployerJob }) {
  const queryClient = useQueryClient()
  const toast = useToast()
  const open = job.status === 'ACTIVE'
  const mutation = useMutation({
    mutationFn: () => employerApi.setJobOpen(job.id, !open),
    onSuccess: () => {
      toast.success(open ? 'Listing closed to new applications' : 'Listing reopened')
      queryClient.invalidateQueries({ queryKey: ['employer'] })
      queryClient.invalidateQueries({ queryKey: ['jobs'] })
    },
    onError: (e) => toast.error(errorMessage(e)),
  })
  return (
    <Button size="sm" variant="ghost" loading={mutation.isPending} onClick={() => mutation.mutate()}>
      {open ? 'Close' : 'Reopen'}
    </Button>
  )
}

export default function EmployerHomePage() {
  const [params, setParams] = useSearchParams()
  const page = Math.max(0, Number(params.get('page') ?? 0) || 0)
  const overview = useQuery({ queryKey: ['employer', 'overview'], queryFn: employerApi.overview })
  const company = useQuery({ queryKey: ['employer', 'company'], queryFn: employerApi.company })
  const jobs = useQuery({ queryKey: ['employer', 'jobs', page], queryFn: () => employerApi.jobs(page), placeholderData: keepPreviousData })

  const o = overview.data
  return (
    <Page title={company.data?.name ?? 'Workspace'} eyebrow="EMPLOYER WORKSPACE"
      description="Your listings and everyone who has applied to them."
      actions={<>
        <ButtonLink to="/employer/company" variant="secondary" icon={<Pencil size={14} />}>Company profile</ButtonLink>
        <ButtonLink to="/employer/jobs/new" icon={<Plus size={15} />}>Post a job</ButtonLink>
      </>}>
      {overview.isLoading && <Skeleton className="mb-10 h-24 w-full" />}
      {o && (
        <div className="mb-10 grid grid-cols-2 gap-px overflow-hidden rounded-xl border border-border bg-border sm:grid-cols-4 lg:grid-cols-7">
          <div className="bg-surface px-4 py-4"><div className="font-mono text-2xl font-semibold">{o.activeJobs}</div><div className="mt-1 font-mono text-label text-muted">OPEN JOBS</div></div>
          {PIPELINE.map((s) => (
            <div key={s} className="bg-surface px-4 py-4">
              <div className="font-mono text-2xl font-semibold">{o.applicationsByStatus[s]}</div>
              <div className="mt-1 font-mono text-label text-muted">{statusLabel[s].toUpperCase()}</div>
            </div>
          ))}
          <div className="bg-surface px-4 py-4"><div className="font-mono text-2xl font-semibold text-muted">{o.applicationsByStatus.REJECTED}</div><div className="mt-1 font-mono text-label text-muted">REJECTED</div></div>
        </div>
      )}

      <Section title="Listings">
        {jobs.isLoading && <ListSkeleton rows={3} className="h-[68px]" />}
        {jobs.isError && <ErrorState error={jobs.error} onRetry={() => jobs.refetch()} />}
        {jobs.data && jobs.data.content.length === 0 && (
          <EmptyState icon={<Briefcase size={18} />} title="No listings yet" action={<ButtonLink to="/employer/jobs/new">Post your first job</ButtonLink>}>
            Listings go live immediately and appear in search for every candidate.
          </EmptyState>
        )}
        {jobs.data && jobs.data.content.length > 0 && (
          <>
            <div className="overflow-hidden rounded-xl border border-border">
              <ul className="divide-y divide-border">
                {jobs.data.content.map((job) => (
                  <li key={job.id} className="flex flex-wrap items-center gap-x-4 gap-y-2 px-5 py-4">
                    <div className="min-w-0 flex-1">
                      <Link to={`/employer/jobs/${job.id}`} className="font-semibold hover:text-accent-text">{job.title}</Link>
                      <div className="text-[13px] text-muted">
                        {job.location} · {workModeLabel[job.workMode]} · {employmentTypeLabel[job.employmentType]} · posted {formatDate(job.postedAt)}
                      </div>
                    </div>
                    <JobStatusBadge status={job.status} />
                    <Link to={`/employer/jobs/${job.id}`} className="inline-flex items-center gap-1.5 rounded-lg px-2.5 py-1.5 text-sm font-semibold hover:bg-raised">
                      <Users size={14} /> {job.applicationCount} {job.applicationCount === 1 ? 'applicant' : 'applicants'}
                    </Link>
                    <div className="flex items-center gap-1">
                      <ButtonLink to={`/employer/jobs/${job.id}/edit`} size="sm" variant="ghost">Edit</ButtonLink>
                      <OpenToggle job={job} />
                    </div>
                  </li>
                ))}
              </ul>
            </div>
            <div className="mt-4">
              <Pagination page={jobs.data.page} totalPages={jobs.data.totalPages} onChange={(p) => setParams(p ? { page: String(p) } : {})} />
            </div>
          </>
        )}
      </Section>
    </Page>
  )
}
