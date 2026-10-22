import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, CalendarClock } from 'lucide-react'
import { applicationsApi } from '../../lib/api'
import { formatDate, formatDateTime, statusLabel } from '../../lib/format'
import { fromLocalInput, toLocalInput } from '../../lib/dates'
import type { ApplicationDetail, ApplicationStatus } from '../../lib/types'
import { useTitle } from '../../components/layout/Page'
import { Timeline } from '../../components/applications/Timeline'
import { StatusBadge } from '../../components/ui/Badge'
import { Button } from '../../components/ui/Button'
import { Dialog } from '../../components/ui/Dialog'
import { Field, Input, Select, Textarea } from '../../components/ui/Field'
import { ErrorState, FormError, Skeleton, errorMessage } from '../../components/ui/Feedback'
import { useToast } from '../../components/ui/Toast'

function useApplicationMutation(id: string) {
  const queryClient = useQueryClient()
  return (data: ApplicationDetail) => {
    queryClient.setQueryData(['application', id], data)
    queryClient.invalidateQueries({ queryKey: ['applications'] })
    queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    queryClient.invalidateQueries({ queryKey: ['job', data.job.id] })
  }
}

function NotesCard({ app }: { app: ApplicationDetail }) {
  const toast = useToast()
  const onSaved = useApplicationMutation(app.id)
  const [notes, setNotes] = useState(app.notes ?? '')
  const [nextAction, setNextAction] = useState(app.nextAction ?? '')
  useEffect(() => { setNotes(app.notes ?? ''); setNextAction(app.nextAction ?? '') }, [app.notes, app.nextAction])

  const save = useMutation({
    mutationFn: () => applicationsApi.update(app.id, { notes, nextAction }),
    onSuccess: (data) => { onSaved(data); toast.success('Notes saved') },
  })
  const dirty = notes !== (app.notes ?? '') || nextAction !== (app.nextAction ?? '')

  return (
    <section className="rounded-xl border border-border p-5">
      <h2 className="text-[15px] font-bold">Your notes</h2>
      <p className="mt-0.5 text-[13px] text-muted">Private to you. Employers never see these.</p>
      <div className="mt-4 space-y-4">
        <FormError error={save.error} />
        <Field label="Next step" optional>
          {(id) => <Input id={id} maxLength={300} value={nextAction} onChange={(e) => setNextAction(e.target.value)} placeholder="e.g. Follow up with the recruiter on Friday" />}
        </Field>
        <Field label="Notes" optional>
          {(id) => <Textarea id={id} rows={5} maxLength={5000} value={notes} onChange={(e) => setNotes(e.target.value)} placeholder="Who you spoke to, questions to ask, salary discussed…" />}
        </Field>
        <Button onClick={() => save.mutate()} loading={save.isPending} disabled={!dirty}>Save notes</Button>
      </div>
    </section>
  )
}

/** Only for applications on external sites. For in-app ones, the employer owns the pipeline. */
function SelfTrackedStatus({ app }: { app: ApplicationDetail }) {
  const toast = useToast()
  const onSaved = useApplicationMutation(app.id)
  const options = app.allowedNextStatuses.filter((s) => s !== 'WITHDRAWN')
  const [next, setNext] = useState<ApplicationStatus | ''>('')
  const [interviewAt, setInterviewAt] = useState(toLocalInput(app.interviewAt))

  const update = useMutation({
    mutationFn: () => {
      const target = (next || app.status) as ApplicationStatus
      return applicationsApi.update(app.id, {
        status: target,
        interviewAt: target === 'INTERVIEW' ? fromLocalInput(interviewAt) : undefined,
      })
    },
    onSuccess: (data) => { onSaved(data); setNext(''); toast.success(`Status updated to ${statusLabel[data.status]}`) },
  })

  const target = next || app.status
  const changed = next !== '' || (target === 'INTERVIEW' && fromLocalInput(interviewAt) !== app.interviewAt)

  if (options.length === 0 && app.status !== 'INTERVIEW') return null

  return (
    <div className="space-y-3">
      <p className="text-[13px] text-muted">You applied on the company's site, so you keep this up to date yourself.</p>
      <FormError error={update.error} />
      {options.length > 0 && (
        <Field label="Move to">
          {(id) => (
            <Select id={id} value={next} onChange={(e) => setNext(e.target.value as ApplicationStatus | '')}>
              <option value="">Keep as {statusLabel[app.status]}</option>
              {options.map((s) => <option key={s} value={s}>{statusLabel[s]}</option>)}
            </Select>
          )}
        </Field>
      )}
      {target === 'INTERVIEW' && (
        <Field label="Interview time" optional>
          {(id) => <Input id={id} type="datetime-local" value={interviewAt} onChange={(e) => setInterviewAt(e.target.value)} />}
        </Field>
      )}
      <Button variant="secondary" className="w-full" onClick={() => update.mutate()} loading={update.isPending} disabled={!changed}>Update status</Button>
    </div>
  )
}

export default function ApplicationDetailPage() {
  const { id = '' } = useParams()
  const toast = useToast()
  const onSaved = useApplicationMutation(id)
  const [confirmWithdraw, setConfirmWithdraw] = useState(false)
  const app = useQuery({ queryKey: ['application', id], queryFn: () => applicationsApi.get(id) })
  useTitle(app.data ? `${app.data.job.title} application` : 'Application')

  const withdraw = useMutation({
    mutationFn: () => applicationsApi.withdraw(id),
    onSuccess: (data) => { onSaved(data); setConfirmWithdraw(false); toast.success('Application withdrawn') },
    onError: (e) => toast.error(errorMessage(e)),
  })

  if (app.isLoading) return <div className="mx-auto max-w-page space-y-4 px-4 py-10 sm:px-6"><Skeleton className="h-10 w-1/2" /><Skeleton className="h-64 w-full" /></div>
  if (app.isError) return <div className="mx-auto max-w-page px-4 py-10 sm:px-6"><ErrorState error={app.error} onRetry={() => app.refetch()} /></div>

  const a = app.data!
  const canWithdraw = a.allowedNextStatuses.includes('WITHDRAWN')

  return (
    <div className="mx-auto max-w-page px-4 pb-10 pt-6 sm:px-6">
      <Link to="/applications" className="inline-flex items-center gap-1.5 text-sm text-muted hover:text-ink"><ArrowLeft size={15} /> All applications</Link>

      <header className="mt-6 flex flex-wrap items-start justify-between gap-4">
        <div>
          <StatusBadge status={a.status} />
          <h1 className="mt-3 text-[28px] font-extrabold tracking-tight">
            <Link to={`/jobs/${a.job.id}`} className="hover:text-accent-text">{a.job.title}</Link>
          </h1>
          <p className="mt-1 text-[15px] text-muted">
            <Link to={`/companies/${a.job.companyId}`} className="font-semibold text-ink hover:text-accent-text">{a.job.companyName}</Link>
            {a.job.location && <> · {a.job.location}</>} · Applied {formatDate(a.appliedAt)}
            {a.job.origin === 'INGESTED' && <> · on the company's site</>}
          </p>
        </div>
      </header>

      {a.status === 'INTERVIEW' && a.interviewAt && (
        <div className="mt-6 flex items-center gap-3 rounded-xl border border-accent/40 bg-accent/[0.06] px-4 py-3.5">
          <CalendarClock size={18} className="text-accent-text" />
          <p className="text-sm"><span className="font-semibold">Interview:</span> {formatDateTime(a.interviewAt)}</p>
        </div>
      )}

      <div className="mt-8 grid gap-8 lg:grid-cols-[1fr_320px]">
        <div className="space-y-8">
          {a.coverLetter && (
            <section>
              <h2 className="mb-2 font-mono text-label text-muted">{a.job.origin === 'EMPLOYER' ? 'COVER NOTE YOU SENT' : 'APPLICATION NOTE'}</h2>
              <p className="whitespace-pre-line rounded-xl border border-border bg-surface p-4 text-sm leading-relaxed">{a.coverLetter}</p>
            </section>
          )}
          <NotesCard app={a} />
        </div>

        <aside className="space-y-6">
          <section className="rounded-xl border border-border-strong bg-surface p-5">
            <h2 className="mb-4 text-[15px] font-bold">Status</h2>
            {a.candidateManagesStatus
              ? <SelfTrackedStatus app={a} />
              : <p className="text-[13px] text-muted">{a.job.companyName} updates this as your application moves forward.</p>}
            {canWithdraw && (
              <Button variant="danger" className="mt-4 w-full" onClick={() => setConfirmWithdraw(true)}>Withdraw application</Button>
            )}
          </section>
          <section>
            <h2 className="mb-4 font-mono text-label text-muted">HISTORY</h2>
            <Timeline history={a.history} />
          </section>
        </aside>
      </div>

      <Dialog open={confirmWithdraw} onClose={() => setConfirmWithdraw(false)} title="Withdraw this application?"
        description={a.job.origin === 'EMPLOYER' ? `${a.job.companyName} will see that you withdrew.` : 'It will be marked as withdrawn in your tracker.'}
        footer={<>
          <Button variant="ghost" onClick={() => setConfirmWithdraw(false)}>Keep it</Button>
          <Button variant="danger" loading={withdraw.isPending} onClick={() => withdraw.mutate()}>Withdraw</Button>
        </>}>
        <p className="text-sm text-muted">You can apply again later while the listing is open.</p>
      </Dialog>
    </div>
  )
}
