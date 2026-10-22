import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, CalendarClock, Mail } from 'lucide-react'
import { employerApi } from '../../lib/api'
import { formatDate, formatDateTime, statusLabel } from '../../lib/format'
import { fromLocalInput, toLocalInput } from '../../lib/dates'
import type { ApplicationStatus, EmployerApplication } from '../../lib/types'
import { useTitle } from '../../components/layout/Page'
import { Timeline } from '../../components/applications/Timeline'
import { StatusBadge } from '../../components/ui/Badge'
import { Button } from '../../components/ui/Button'
import { Field, Input, Select } from '../../components/ui/Field'
import { ErrorState, FormError, Skeleton } from '../../components/ui/Feedback'
import { useToast } from '../../components/ui/Toast'

function StatusForm({ app }: { app: EmployerApplication }) {
  const queryClient = useQueryClient()
  const toast = useToast()
  const [next, setNext] = useState<ApplicationStatus | ''>('')
  const [interviewAt, setInterviewAt] = useState(toLocalInput(app.interviewAt))
  const [note, setNote] = useState('')
  const target = (next || app.status) as ApplicationStatus

  const update = useMutation({
    mutationFn: () => employerApi.updateStatus(app.id, {
      status: target,
      interviewAt: target === 'INTERVIEW' ? fromLocalInput(interviewAt) : null,
      note: note.trim() || undefined,
    }),
    onSuccess: (data) => {
      queryClient.setQueryData(['employer', 'application', app.id], data)
      queryClient.invalidateQueries({ queryKey: ['employer'] })
      setNext(''); setNote('')
      toast.success(`Moved to ${statusLabel[data.status]}. The candidate can see this now.`)
    },
  })

  if (app.allowedNextStatuses.length === 0 && app.status !== 'INTERVIEW') {
    return <p className="text-sm text-muted">This application is {statusLabel[app.status].toLowerCase()} and can't change any further.</p>
  }

  const rescheduling = next === '' && app.status === 'INTERVIEW'
  const canSubmit = next !== '' || (rescheduling && fromLocalInput(interviewAt) !== app.interviewAt)

  return (
    <div className="space-y-3">
      <FormError error={update.error} />
      <Field label="Move to">
        {(id) => (
          <Select id={id} value={next} onChange={(e) => setNext(e.target.value as ApplicationStatus | '')}>
            <option value="">{app.status === 'INTERVIEW' ? 'Keep in Interview' : 'Choose a stage…'}</option>
            {app.allowedNextStatuses.map((s) => <option key={s} value={s}>{statusLabel[s]}</option>)}
          </Select>
        )}
      </Field>
      {target === 'INTERVIEW' && (
        <Field label="Interview time" hint="Shown on the candidate's dashboard.">
          {(id, d) => <Input id={id} aria-describedby={d} type="datetime-local" value={interviewAt} onChange={(e) => setInterviewAt(e.target.value)} />}
        </Field>
      )}
      <Field label="Note for the history" optional hint="The candidate sees this.">
        {(id, d) => <Input id={id} aria-describedby={d} maxLength={500} value={note} onChange={(e) => setNote(e.target.value)} placeholder="e.g. Technical round with the platform team" />}
      </Field>
      <Button className="w-full" onClick={() => update.mutate()} loading={update.isPending} disabled={!canSubmit}>
        {rescheduling ? 'Update interview time' : 'Update status'}
      </Button>
    </div>
  )
}

export default function EmployerApplicationPage() {
  const { id = '' } = useParams()
  const app = useQuery({ queryKey: ['employer', 'application', id], queryFn: () => employerApi.application(id) })
  useTitle(app.data ? `${app.data.applicant.fullName}, ${app.data.jobTitle}` : 'Applicant')

  if (app.isLoading) return <div className="mx-auto max-w-page space-y-4 px-4 py-10 sm:px-6"><Skeleton className="h-10 w-1/3" /><Skeleton className="h-64 w-full" /></div>
  if (app.isError) return <div className="mx-auto max-w-page px-4 py-10 sm:px-6"><ErrorState error={app.error} onRetry={() => app.refetch()} /></div>

  const a = app.data!
  const p = a.applicant
  return (
    <div className="mx-auto max-w-page px-4 pb-10 pt-6 sm:px-6">
      <Link to={`/employer/jobs/${a.jobId}`} className="inline-flex items-center gap-1.5 text-sm text-muted hover:text-ink"><ArrowLeft size={15} /> {a.jobTitle}</Link>
      <header className="mt-6 flex flex-wrap items-start justify-between gap-4">
        <div>
          <StatusBadge status={a.status} />
          <h1 className="mt-3 text-[28px] font-extrabold tracking-tight">{p.fullName}</h1>
          <p className="mt-1 text-[15px] text-muted">{p.headline ?? 'No headline'}{p.location && <> · {p.location}</>}</p>
        </div>
        <a href={`mailto:${p.email}`} className="inline-flex h-10 items-center gap-2 rounded-lg border border-border-strong px-4 text-sm font-semibold hover:bg-raised">
          <Mail size={15} /> {p.email}
        </a>
      </header>

      {a.status === 'INTERVIEW' && a.interviewAt && (
        <div className="mt-6 flex items-center gap-3 rounded-xl border border-accent/40 bg-accent/[0.06] px-4 py-3.5 text-sm">
          <CalendarClock size={18} className="text-accent-text" /> Interview scheduled for <span className="font-semibold">{formatDateTime(a.interviewAt)}</span>
        </div>
      )}

      <div className="mt-8 grid gap-8 lg:grid-cols-[1fr_320px]">
        <div className="space-y-8">
          <dl className="grid grid-cols-2 gap-px overflow-hidden rounded-xl border border-border bg-border sm:grid-cols-3">
            {[
              ['Experience', `${p.experienceYears} ${Number(p.experienceYears) === 1 ? 'year' : 'years'}`],
              ['Education', p.educationLevel ?? '—'],
              ['Graduating', p.graduationYear ? String(p.graduationYear) : '—'],
            ].map(([label, value]) => (
              <div key={label} className="bg-surface px-4 py-3">
                <dt className="font-mono text-label text-muted">{label.toUpperCase()}</dt>
                <dd className="mt-1 text-sm font-semibold">{value}</dd>
              </div>
            ))}
          </dl>
          <section>
            <h2 className="mb-2 font-mono text-label text-muted">SKILLS</h2>
            {p.skills.length
              ? <ul className="flex flex-wrap gap-1.5">{p.skills.map((s) => <li key={s} className="rounded-md border border-border-strong px-2.5 py-1 text-[13px] font-medium">{s}</li>)}</ul>
              : <p className="text-sm text-muted">No skills listed.</p>}
          </section>
          <section>
            <h2 className="mb-2 font-mono text-label text-muted">COVER NOTE · APPLIED {formatDate(a.appliedAt).toUpperCase()}</h2>
            {a.coverLetter
              ? <p className="whitespace-pre-line rounded-xl border border-border bg-surface p-4 text-sm leading-relaxed">{a.coverLetter}</p>
              : <p className="text-sm text-muted">The candidate didn't include a cover note.</p>}
          </section>
        </div>
        <aside className="space-y-8">
          <section className="rounded-xl border border-border-strong bg-surface p-5">
            <h2 className="mb-4 text-[15px] font-bold">Pipeline</h2>
            <StatusForm app={a} />
          </section>
          <section>
            <h2 className="mb-4 font-mono text-label text-muted">HISTORY</h2>
            <Timeline history={a.history} viewer="employer" />
          </section>
        </aside>
      </div>
    </div>
  )
}
