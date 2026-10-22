import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Link, useLocation } from 'react-router-dom'
import { ArrowUpRight, CheckCircle2, Send } from 'lucide-react'
import { useAuth } from '../../auth/AuthProvider'
import { applicationsApi } from '../../lib/api'
import type { JobDetail } from '../../lib/types'
import { formatDate } from '../../lib/format'
import { Button, ButtonLink } from '../ui/Button'
import { Dialog } from '../ui/Dialog'
import { Field, Textarea } from '../ui/Field'
import { FormError } from '../ui/Feedback'
import { StatusBadge } from '../ui/Badge'
import { useToast } from '../ui/Toast'

const COVER_LIMIT = 5000

/**
 * What "apply" means depends on where the job came from:
 *  - EMPLOYER jobs: apply here, and the employer sees the application.
 *  - INGESTED jobs: apply on the company's site, then record it here to
 *    track it. We never claim to submit anything to a company we aren't
 *    connected to.
 */
export function ApplyPanel({ job }: { job: JobDetail }) {
  const { user } = useAuth()
  const location = useLocation()
  const queryClient = useQueryClient()
  const toast = useToast()
  const [open, setOpen] = useState(false)
  const [coverLetter, setCoverLetter] = useState('')

  const apply = useMutation({
    mutationFn: () => applicationsApi.apply(job.id, coverLetter.trim() || undefined),
    onSuccess: () => {
      setOpen(false)
      toast.success(job.origin === 'EMPLOYER' ? `Application sent to ${job.company.name}` : 'Added to your application tracker')
      queryClient.invalidateQueries({ queryKey: ['job', job.id] })
      queryClient.invalidateQueries({ queryKey: ['applications'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
  })

  if (job.myApplication && job.myApplication.status !== 'WITHDRAWN') {
    return (
      <div className="space-y-3">
        <div className="flex items-center gap-2 text-sm font-semibold text-good">
          <CheckCircle2 size={16} /> You applied {formatDate(job.myApplication.appliedAt)}
        </div>
        <StatusBadge status={job.myApplication.status} />
        <ButtonLink to={`/applications/${job.myApplication.id}`} variant="secondary" className="w-full">View application</ButtonLink>
      </div>
    )
  }

  if (job.status !== 'ACTIVE') {
    return <p className="text-sm text-muted">This listing is closed and no longer accepts applications.</p>
  }

  if (!user) {
    return (
      <div className="space-y-2">
        <ButtonLink to={`/login?next=${encodeURIComponent(location.pathname)}`} className="w-full">Sign in to apply</ButtonLink>
        <p className="text-center text-xs text-muted">
          New here? <Link to="/register" className="font-semibold text-accent-text hover:underline">Create a free account</Link>
        </p>
      </div>
    )
  }

  if (user.role !== 'CANDIDATE') {
    return <p className="text-sm text-muted">Applications are for candidate accounts.</p>
  }

  const isExternal = job.origin === 'INGESTED'

  return (
    <div className="space-y-2">
      {isExternal && job.applicationUrl && (
        <a href={job.applicationUrl} target="_blank" rel="noopener noreferrer"
          className="inline-flex h-10 w-full items-center justify-center gap-2 rounded-lg bg-accent px-4 text-sm font-semibold text-accent-ink hover:bg-accent/90">
          Apply on {job.company.name}'s site <ArrowUpRight size={15} />
        </a>
      )}
      <Button variant={isExternal ? 'secondary' : 'primary'} className="w-full" icon={isExternal ? <CheckCircle2 size={15} /> : <Send size={15} />}
        onClick={() => { apply.reset(); setOpen(true) }}>
        {isExternal ? "I've applied, track it" : 'Apply now'}
      </Button>
      {job.myApplication?.status === 'WITHDRAWN' && (
        <p className="text-xs text-muted">You withdrew an earlier application. Applying again reopens it.</p>
      )}

      <Dialog
        open={open}
        onClose={() => setOpen(false)}
        title={isExternal ? 'Track this application' : `Apply to ${job.company.name}`}
        description={isExternal
          ? "We'll add it to your tracker so you can follow its progress."
          : `${job.title}. Your profile goes with your application.`}
        footer={
          <>
            <Button variant="ghost" onClick={() => setOpen(false)}>Cancel</Button>
            <Button loading={apply.isPending} onClick={() => apply.mutate()}>
              {isExternal ? 'Add to tracker' : 'Send application'}
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          <FormError error={apply.error} />
          <Field
            label={isExternal ? 'Notes about this application' : 'Cover note'}
            optional
            hint={`${coverLetter.length}/${COVER_LIMIT}${isExternal ? '' : '. Visible to the employer.'}`}
          >
            {(id, describedBy) => (
              <Textarea id={id} aria-describedby={describedBy} rows={6} maxLength={COVER_LIMIT}
                value={coverLetter} onChange={(e) => setCoverLetter(e.target.value)}
                placeholder={isExternal ? 'Which resume you sent, the referral, anything worth remembering.' : 'Why this role, in a few sentences.'} />
            )}
          </Field>
        </div>
      </Dialog>
    </div>
  )
}
