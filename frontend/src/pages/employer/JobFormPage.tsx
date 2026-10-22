import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { employerApi } from '../../lib/api'
import { ApiError } from '../../lib/http'
import { employmentTypeLabel, experienceLevelLabel, workModeLabel } from '../../lib/format'
import type { EmploymentType, ExperienceLevel, JobUpsertRequest, WorkMode } from '../../lib/types'
import { Page } from '../../components/layout/Page'
import { Button, ButtonLink } from '../../components/ui/Button'
import { Field, Input, Select, Textarea } from '../../components/ui/Field'
import { ErrorState, FormError, Skeleton } from '../../components/ui/Feedback'
import { TagInput } from '../../components/ui/TagInput'
import { useToast } from '../../components/ui/Toast'

interface FormState {
  title: string; description: string; location: string
  workMode: WorkMode; employmentType: EmploymentType; experienceLevel: ExperienceLevel
  salaryMinLpa: string; salaryMaxLpa: string; skills: string[]
}

const EMPTY: FormState = {
  title: '', description: '', location: '', workMode: 'HYBRID', employmentType: 'FULL_TIME',
  experienceLevel: 'ENTRY', salaryMinLpa: '', salaryMaxLpa: '', skills: [],
}

const LAKH = 100_000

/** Employers enter pay in LPA (lakh per annum). The API stores it in rupees. */
function toRequest(f: FormState): JobUpsertRequest {
  const rupees = (lpa: string) => (lpa.trim() === '' ? null : Math.round(Number(lpa) * LAKH))
  const salaryMin = rupees(f.salaryMinLpa)
  const salaryMax = rupees(f.salaryMaxLpa)
  return {
    title: f.title.trim(), description: f.description.trim(), location: f.location.trim(),
    workMode: f.workMode, employmentType: f.employmentType, experienceLevel: f.experienceLevel,
    salaryMin, salaryMax, salaryCurrency: salaryMin != null || salaryMax != null ? 'INR' : null,
    skills: f.skills,
  }
}

function validate(f: FormState): Record<string, string> {
  const e: Record<string, string> = {}
  if (!f.title.trim()) e.title = 'Give the role a title.'
  if (f.description.trim().length < 50) e.description = 'Write at least 50 characters, so candidates know what the role involves.'
  if (!f.location.trim()) e.location = 'Where is the role based? Use "Remote" if it is remote.'
  const min = f.salaryMinLpa === '' ? null : Number(f.salaryMinLpa)
  const max = f.salaryMaxLpa === '' ? null : Number(f.salaryMaxLpa)
  if (min != null && (Number.isNaN(min) || min < 0)) e.salaryMin = 'Enter a positive number.'
  if (max != null && (Number.isNaN(max) || max < 0)) e.salaryMax = 'Enter a positive number.'
  if (min != null && max != null && min > max) e.salaryMax = 'The maximum must be at least the minimum.'
  return e
}

export default function JobFormPage() {
  const { id } = useParams()
  const editing = !!id
  const navigate = useNavigate()
  const toast = useToast()
  const queryClient = useQueryClient()
  const existing = useQuery({ queryKey: ['employer', 'job', id], queryFn: () => employerApi.job(id!), enabled: editing })
  const [form, setForm] = useState<FormState>(EMPTY)
  const [errors, setErrors] = useState<Record<string, string>>({})

  useEffect(() => {
    const j = existing.data
    if (!j) return
    setForm({
      title: j.title, description: j.description, location: j.location, workMode: j.workMode,
      employmentType: j.employmentType, experienceLevel: j.experienceLevel,
      salaryMinLpa: j.salaryMin != null ? String(j.salaryMin / LAKH) : '',
      salaryMaxLpa: j.salaryMax != null ? String(j.salaryMax / LAKH) : '',
      skills: j.skills,
    })
  }, [existing.data])

  const save = useMutation({
    mutationFn: (body: JobUpsertRequest) => (editing ? employerApi.updateJob(id!, body) : employerApi.createJob(body)),
    onSuccess: (job) => {
      queryClient.invalidateQueries({ queryKey: ['employer'] })
      queryClient.invalidateQueries({ queryKey: ['jobs'] })
      queryClient.invalidateQueries({ queryKey: ['job', job.id] })
      toast.success(editing ? 'Listing updated' : 'Job posted. It is live now.')
      navigate(`/employer/jobs/${job.id}`)
    },
    onError: (e) => { if (e instanceof ApiError) setErrors(e.fieldErrors) },
  })

  const set = <K extends keyof FormState>(key: K, value: FormState[K]) => setForm((f) => ({ ...f, [key]: value }))

  const onSubmit = (e: FormEvent) => {
    e.preventDefault()
    const clientErrors = validate(form)
    setErrors(clientErrors)
    if (Object.keys(clientErrors).length === 0) save.mutate(toRequest(form))
  }

  if (editing && existing.isLoading) return <Page title="Edit listing"><Skeleton className="h-96 w-full" /></Page>
  if (editing && existing.isError) return <Page title="Edit listing"><ErrorState error={existing.error} /></Page>

  return (
    <Page title={editing ? 'Edit listing' : 'Post a job'} eyebrow="EMPLOYER" width="narrow"
      description={editing ? 'Changes go live as soon as you save.' : 'The listing appears in search as soon as you publish it.'}>
      <form onSubmit={onSubmit} noValidate className="space-y-6">
        {save.isError && !(save.error instanceof ApiError && save.error.code === 'VALIDATION_ERROR') && <FormError error={save.error} />}
        <Field label="Job title" error={errors.title}>
          {(fid) => <Input id={fid} value={form.title} onChange={(e) => set('title', e.target.value)} error={errors.title} placeholder="Backend Engineer" maxLength={300} />}
        </Field>
        <div className="grid gap-4 sm:grid-cols-3">
          <Field label="Job type">{(fid) => (
            <Select id={fid} value={form.employmentType} onChange={(e) => set('employmentType', e.target.value as EmploymentType)}>
              {Object.entries(employmentTypeLabel).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
            </Select>
          )}</Field>
          <Field label="Experience level">{(fid) => (
            <Select id={fid} value={form.experienceLevel} onChange={(e) => set('experienceLevel', e.target.value as ExperienceLevel)}>
              {Object.entries(experienceLevelLabel).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
            </Select>
          )}</Field>
          <Field label="Work mode">{(fid) => (
            <Select id={fid} value={form.workMode} onChange={(e) => set('workMode', e.target.value as WorkMode)}>
              {Object.entries(workModeLabel).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
            </Select>
          )}</Field>
        </div>
        <Field label="Location" error={errors.location}>
          {(fid) => <Input id={fid} value={form.location} onChange={(e) => set('location', e.target.value)} error={errors.location} placeholder="Pune, or Remote (India)" />}
        </Field>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Salary from (LPA)" optional error={errors.salaryMin}>
            {(fid) => <Input id={fid} type="number" min={0} step="0.5" inputMode="decimal" value={form.salaryMinLpa} onChange={(e) => set('salaryMinLpa', e.target.value)} error={errors.salaryMin} placeholder="8" />}
          </Field>
          <Field label="Salary to (LPA)" optional error={errors.salaryMax}>
            {(fid) => <Input id={fid} type="number" min={0} step="0.5" inputMode="decimal" value={form.salaryMaxLpa} onChange={(e) => set('salaryMaxLpa', e.target.value)} error={errors.salaryMax} placeholder="12" />}
          </Field>
        </div>
        <Field label="Skills" hint="Skills drive candidates' match scores, so list the ones that matter.">
          {() => <TagInput ariaLabel="Skills" suggestSkills value={form.skills} onChange={(v) => set('skills', v)} max={20} placeholder="Java, Spring Boot…" />}
        </Field>
        <Field label="Description" error={errors.description} hint={`${form.description.trim().length} characters. Responsibilities, requirements, and what makes the team good to work with.`}>
          {(fid, d) => <Textarea id={fid} aria-describedby={d} rows={12} maxLength={20000} value={form.description} onChange={(e) => set('description', e.target.value)} error={errors.description} />}
        </Field>
        <div className="flex gap-2 border-t border-border pt-6">
          <Button type="submit" loading={save.isPending}>{editing ? 'Save changes' : 'Publish job'}</Button>
          <ButtonLink to={editing ? `/employer/jobs/${id}` : '/employer'} variant="ghost">Cancel</ButtonLink>
        </div>
      </form>
    </Page>
  )
}
