import { useEffect, useRef, useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CheckCircle2, CircleDashed, FileUp, Plus, Trash2 } from 'lucide-react'
import { profileApi } from '../../lib/api'
import { ApiError } from '../../lib/http'
import { formatDate } from '../../lib/format'
import type { Education, Experience, Profile, ProfileRequest, ResumeAnalysis } from '../../lib/types'
import { Page } from '../../components/layout/Page'
import { Button } from '../../components/ui/Button'
import { Field, Input, Textarea } from '../../components/ui/Field'
import { ErrorState, FormError, Skeleton } from '../../components/ui/Feedback'
import { TagInput } from '../../components/ui/TagInput'
import { useToast } from '../../components/ui/Toast'

type Errors = Record<string, string>

function toForm(p: Profile): ProfileRequest {
  return {
    fullName: p.fullName,
    headline: p.headline ?? '',
    location: p.location ?? '',
    educationLevel: p.educationLevel ?? '',
    graduationYear: p.graduationYear,
    experienceYears: p.experienceYears,
    summary: p.summary ?? '',
    preferredRoles: p.preferredRoles,
    skills: p.skills,
    educations: p.educations,
    experiences: p.experiences,
  }
}

/** Same rules as the backend's Bean Validation, checked first for instant feedback. */
function validate(f: ProfileRequest): Errors {
  const e: Errors = {}
  if (!f.fullName.trim()) e.fullName = 'Enter your name.'
  if (f.graduationYear != null && (f.graduationYear < 1950 || f.graduationYear > 2100)) e.graduationYear = 'Enter a real year.'
  if (f.experienceYears < 0 || f.experienceYears > 60) e.experienceYears = 'Between 0 and 60.'
  f.educations.forEach((ed, i) => {
    if (!ed.institution.trim()) e[`educations[${i}].institution`] = 'Required.'
    if (!ed.degree.trim()) e[`educations[${i}].degree`] = 'Required.'
    if (ed.startYear && ed.endYear && ed.startYear > ed.endYear) e[`educations[${i}].endYear`] = "Can't be before the start year."
  })
  f.experiences.forEach((ex, i) => {
    if (!ex.companyName.trim()) e[`experiences[${i}].companyName`] = 'Required.'
    if (!ex.title.trim()) e[`experiences[${i}].title`] = 'Required.'
    if (!ex.startDate) e[`experiences[${i}].startDate`] = 'Required.'
    if (ex.startDate && ex.endDate && ex.endDate < ex.startDate) e[`experiences[${i}].endDate`] = "Can't be before the start date."
  })
  return e
}

/** Blank strings become null so the API stores "not set" rather than "". */
function toRequest(f: ProfileRequest): ProfileRequest {
  const blank = (v: string | null | undefined) => (v && v.trim() ? v.trim() : null)
  return {
    ...f,
    headline: blank(f.headline), location: blank(f.location), educationLevel: blank(f.educationLevel), summary: blank(f.summary),
    educations: f.educations.map((e) => ({ ...e, fieldOfStudy: blank(e.fieldOfStudy) })),
    experiences: f.experiences.map((x) => ({ ...x, endDate: x.endDate || null, description: blank(x.description) })),
  }
}

function ResumeUpload({ onStored }: { onStored: (addedSkills: string[]) => void }) {
  const toast = useToast()
  const fileRef = useRef<HTMLInputElement>(null)
  const [file, setFile] = useState<File | null>(null)
  const [consent, setConsent] = useState(false)
  const [result, setResult] = useState<ResumeAnalysis | null>(null)

  const upload = useMutation({
    mutationFn: () => profileApi.uploadResume(file!, consent),
    onSuccess: (data) => {
      setResult(data)
      if (data.persisted) {
        toast.success(data.skillsAddedToProfile.length ? `Resume saved. Added ${data.skillsAddedToProfile.length} skills.` : 'Resume saved')
        onStored(data.skillsAddedToProfile)
      }
    },
  })

  return (
    <div className="space-y-3">
      <input ref={fileRef} type="file" accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        className="sr-only" id="resume-file" onChange={(e) => { setFile(e.target.files?.[0] ?? null); setResult(null) }} />
      <label htmlFor="resume-file"
        className="flex cursor-pointer items-center gap-3 rounded-lg border border-dashed border-border-strong px-4 py-4 text-sm transition-colors hover:border-accent/60 hover:bg-raised">
        <FileUp size={18} className="text-muted" />
        <span className="min-w-0 flex-1 truncate">{file ? file.name : 'Choose a PDF or DOCX (max 5 MB)'}</span>
      </label>
      <label className="flex items-start gap-2.5 text-[13px] text-muted">
        <input type="checkbox" checked={consent} onChange={(e) => setConsent(e.target.checked)} className="mt-0.5 accent-[rgb(var(--accent))]" />
        <span>Store the file with my profile and add the skills it mentions. Leave this unticked to only preview what we detect. Nothing is saved.</span>
      </label>
      <FormError error={upload.error} />
      <Button variant="secondary" disabled={!file} loading={upload.isPending} onClick={() => upload.mutate()}>
        {consent ? 'Upload and save' : 'Analyse without saving'}
      </Button>
      {result && (
        <div className="rounded-lg border border-border bg-raised p-4 text-sm">
          <p className="font-semibold">{result.persisted ? 'Saved to your profile.' : 'Preview only. Nothing was stored.'}</p>
          <p className="mt-1 text-muted">
            Detected {result.detectedSkills.length ? result.detectedSkills.join(', ') : 'no known skills'}
            {result.likelyEducationLevel && <> · {result.likelyEducationLevel}</>}
            {result.likelyGraduationYear && <> · graduating {result.likelyGraduationYear}?</>}
          </p>
          <p className="mt-1 text-xs text-muted">Detection is keyword-based, so check it against your resume.</p>
        </div>
      )}
    </div>
  )
}

export default function ProfilePage() {
  const toast = useToast()
  const queryClient = useQueryClient()
  const [params] = useSearchParams()
  const profile = useQuery({ queryKey: ['profile'], queryFn: profileApi.get })
  const [form, setForm] = useState<ProfileRequest | null>(null)
  const [errors, setErrors] = useState<Errors>({})

  useEffect(() => { if (profile.data && !form) setForm(toForm(profile.data)) }, [profile.data, form])

  const save = useMutation({
    mutationFn: (body: ProfileRequest) => profileApi.update(body),
    onSuccess: (data) => {
      queryClient.setQueryData(['profile'], data)
      setForm(toForm(data))
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      queryClient.invalidateQueries({ queryKey: ['match'] })
      queryClient.invalidateQueries({ queryKey: ['skill-gap'] })
      queryClient.invalidateQueries({ queryKey: ['auth', 'me'] })
      toast.success('Profile saved')
    },
    onError: (e) => {
      if (e instanceof ApiError) setErrors(e.fieldErrors)
    },
  })

  if (profile.isLoading || (!form && !profile.isError)) {
    return <Page title="Your profile" eyebrow="PROFILE"><div className="space-y-4"><Skeleton className="h-48 w-full" /><Skeleton className="h-48 w-full" /></div></Page>
  }
  if (profile.isError) return <Page title="Your profile"><ErrorState error={profile.error} onRetry={() => profile.refetch()} /></Page>

  const f = form!
  const p = profile.data!
  const set = <K extends keyof ProfileRequest>(key: K, value: ProfileRequest[K]) => setForm({ ...f, [key]: value })
  const setEducation = (i: number, patch: Partial<Education>) => set('educations', f.educations.map((e, j) => (j === i ? { ...e, ...patch } : e)))
  const setExperience = (i: number, patch: Partial<Experience>) => set('experiences', f.experiences.map((x, j) => (j === i ? { ...x, ...patch } : x)))

  const onSubmit = (e: FormEvent) => {
    e.preventDefault()
    const clientErrors = validate(f)
    setErrors(clientErrors)
    if (Object.keys(clientErrors).length) {
      toast.error('Some fields need attention.')
      return
    }
    save.mutate(toRequest(f))
  }

  const numberOrNull = (v: string) => (v === '' ? null : Number(v))

  return (
    <Page title="Your profile" eyebrow="PROFILE"
      description={params.get('welcome') ? 'Welcome to JobPulse. Fill this in once and every match score uses it.' : 'Match scores, recommendations and skill gaps all come from what you enter here.'}>
      <form onSubmit={onSubmit} noValidate className="grid gap-10 lg:grid-cols-[1fr_300px]">
        <div className="min-w-0 space-y-10">
          <section className="space-y-4">
            <h2 className="font-mono text-label text-muted">BASICS</h2>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="Full name" error={errors.fullName}>{(id) => <Input id={id} value={f.fullName} onChange={(e) => set('fullName', e.target.value)} error={errors.fullName} />}</Field>
              <Field label="Location" optional>{(id) => <Input id={id} value={f.location ?? ''} onChange={(e) => set('location', e.target.value)} placeholder="Pune" />}</Field>
            </div>
            <Field label="Headline" optional hint="One line about what you do or want to do.">
              {(id, d) => <Input id={id} aria-describedby={d} maxLength={160} value={f.headline ?? ''} onChange={(e) => set('headline', e.target.value)} placeholder="Final-year CS student focused on backend development" />}
            </Field>
            <div className="grid gap-4 sm:grid-cols-3">
              <Field label="Education level" optional>{(id) => <Input id={id} value={f.educationLevel ?? ''} onChange={(e) => set('educationLevel', e.target.value)} placeholder="B.E., B.Tech, MCA…" />}</Field>
              <Field label="Graduation year" optional error={errors.graduationYear}>{(id) => <Input id={id} type="number" inputMode="numeric" value={f.graduationYear ?? ''} onChange={(e) => set('graduationYear', numberOrNull(e.target.value))} error={errors.graduationYear} />}</Field>
              <Field label="Years of experience" error={errors.experienceYears}>{(id) => <Input id={id} type="number" step="0.5" min={0} value={f.experienceYears} onChange={(e) => set('experienceYears', Number(e.target.value || 0))} error={errors.experienceYears} />}</Field>
            </div>
            <Field label="Summary" optional>
              {(id) => <Textarea id={id} rows={4} maxLength={2000} value={f.summary ?? ''} onChange={(e) => set('summary', e.target.value)} placeholder="What you've built and what you're looking for." />}
            </Field>
          </section>

          <section className="space-y-4">
            <h2 className="font-mono text-label text-muted">WHAT YOU'RE LOOKING FOR</h2>
            <Field label="Target roles" hint="Used for the skill-gap analysis. Press Enter after each.">
              {() => <TagInput ariaLabel="Target roles" value={f.preferredRoles} onChange={(v) => set('preferredRoles', v)} max={10} placeholder="Backend developer" />}
            </Field>
            <Field label="Skills" hint="Start typing for suggestions. These drive your match scores.">
              {() => <TagInput ariaLabel="Skills" suggestSkills value={f.skills} onChange={(v) => set('skills', v)} placeholder="Java, React, PostgreSQL…" />}
            </Field>
          </section>

          <section className="space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="font-mono text-label text-muted">EDUCATION</h2>
              <Button type="button" size="sm" variant="ghost" icon={<Plus size={14} />}
                onClick={() => set('educations', [...f.educations, { institution: '', degree: '', fieldOfStudy: '', startYear: null, endYear: null }])}>Add</Button>
            </div>
            {f.educations.length === 0 && <p className="text-sm text-muted">No education added.</p>}
            {f.educations.map((ed, i) => (
              <div key={ed.id ?? `new-${i}`} className="rise space-y-3 rounded-xl border border-border p-4">
                <div className="grid gap-3 sm:grid-cols-2">
                  <Field label="Institution" error={errors[`educations[${i}].institution`]}>{(id) => <Input id={id} value={ed.institution} onChange={(e) => setEducation(i, { institution: e.target.value })} error={errors[`educations[${i}].institution`]} />}</Field>
                  <Field label="Degree" error={errors[`educations[${i}].degree`]}>{(id) => <Input id={id} value={ed.degree} onChange={(e) => setEducation(i, { degree: e.target.value })} error={errors[`educations[${i}].degree`]} />}</Field>
                </div>
                <div className="grid gap-3 sm:grid-cols-[1fr_120px_120px_auto] sm:items-end">
                  <Field label="Field of study" optional>{(id) => <Input id={id} value={ed.fieldOfStudy ?? ''} onChange={(e) => setEducation(i, { fieldOfStudy: e.target.value })} />}</Field>
                  <Field label="Start" optional>{(id) => <Input id={id} type="number" value={ed.startYear ?? ''} onChange={(e) => setEducation(i, { startYear: numberOrNull(e.target.value) })} />}</Field>
                  <Field label="End" optional error={errors[`educations[${i}].endYear`]}>{(id) => <Input id={id} type="number" value={ed.endYear ?? ''} onChange={(e) => setEducation(i, { endYear: numberOrNull(e.target.value) })} error={errors[`educations[${i}].endYear`]} />}</Field>
                  <Button type="button" variant="ghost" aria-label="Remove education" onClick={() => set('educations', f.educations.filter((_, j) => j !== i))}><Trash2 size={15} /></Button>
                </div>
              </div>
            ))}
          </section>

          <section className="space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="font-mono text-label text-muted">EXPERIENCE &amp; PROJECTS</h2>
              <Button type="button" size="sm" variant="ghost" icon={<Plus size={14} />}
                onClick={() => set('experiences', [...f.experiences, { companyName: '', title: '', startDate: '', endDate: '', description: '' }])}>Add</Button>
            </div>
            {f.experiences.length === 0 && <p className="text-sm text-muted">Internships, jobs or substantial projects all count.</p>}
            {f.experiences.map((ex, i) => (
              <div key={ex.id ?? `new-${i}`} className="rise space-y-3 rounded-xl border border-border p-4">
                <div className="grid gap-3 sm:grid-cols-2">
                  <Field label="Title" error={errors[`experiences[${i}].title`]}>{(id) => <Input id={id} value={ex.title} onChange={(e) => setExperience(i, { title: e.target.value })} error={errors[`experiences[${i}].title`]} />}</Field>
                  <Field label="Company or project" error={errors[`experiences[${i}].companyName`]}>{(id) => <Input id={id} value={ex.companyName} onChange={(e) => setExperience(i, { companyName: e.target.value })} error={errors[`experiences[${i}].companyName`]} />}</Field>
                </div>
                <div className="grid gap-3 sm:grid-cols-[1fr_1fr_auto] sm:items-end">
                  <Field label="Start date" error={errors[`experiences[${i}].startDate`]}>{(id) => <Input id={id} type="date" value={ex.startDate} onChange={(e) => setExperience(i, { startDate: e.target.value })} error={errors[`experiences[${i}].startDate`]} />}</Field>
                  <Field label="End date" optional hint="Leave empty if current." error={errors[`experiences[${i}].endDate`]}>{(id, d) => <Input id={id} aria-describedby={d} type="date" value={ex.endDate ?? ''} onChange={(e) => setExperience(i, { endDate: e.target.value })} error={errors[`experiences[${i}].endDate`]} />}</Field>
                  <Button type="button" variant="ghost" aria-label="Remove experience" onClick={() => set('experiences', f.experiences.filter((_, j) => j !== i))}><Trash2 size={15} /></Button>
                </div>
                <Field label="What you did" optional>{(id) => <Textarea id={id} rows={3} maxLength={3000} value={ex.description ?? ''} onChange={(e) => setExperience(i, { description: e.target.value })} />}</Field>
              </div>
            ))}
          </section>

          <div className="sticky bottom-0 -mx-4 flex items-center gap-3 border-t border-border bg-paper/90 px-4 py-3 backdrop-blur sm:mx-0 sm:rounded-xl sm:border">
            <Button type="submit" loading={save.isPending}>Save profile</Button>
            {save.isError && !(save.error instanceof ApiError && save.error.code === 'VALIDATION_ERROR') && <FormError error={save.error} />}
            {save.isError && save.error instanceof ApiError && save.error.code === 'VALIDATION_ERROR' && <span className="text-sm text-miss">Some fields need attention.</span>}
          </div>
        </div>

        <aside className="space-y-8 lg:sticky lg:top-24 lg:self-start">
          <section className="rounded-xl border border-border-strong bg-surface p-5">
            <div className="flex items-center justify-between">
              <h2 className="text-[15px] font-bold">Profile strength</h2>
              <span className="font-mono text-sm font-semibold">{p.completeness.percent}%</span>
            </div>
            <div className="mt-3 h-1.5 overflow-hidden rounded-full bg-border">
              <div className="h-full rounded-full bg-accent transition-[width] duration-700" style={{ width: `${p.completeness.percent}%` }} />
            </div>
            <ul className="mt-4 space-y-1.5 text-[13px]">
              {p.completeness.missing.length === 0
                ? <li className="flex items-center gap-2 text-good"><CheckCircle2 size={13} /> Everything's filled in</li>
                : p.completeness.missing.map((m) => <li key={m} className="flex items-center gap-2 text-muted"><CircleDashed size={13} /> {m}</li>)}
            </ul>
            <p className="mt-3 text-xs text-muted">Updates when you save.</p>
          </section>

          <section>
            <h2 className="mb-3 font-mono text-label text-muted">RESUME</h2>
            {p.resume && (
              <p className="mb-3 text-sm">
                <span className="font-semibold">{p.resume.fileName}</span>
                <span className="text-muted"> · uploaded {formatDate(p.resume.uploadedAt)}</span>
              </p>
            )}
            <ResumeUpload onStored={(added) => {
              // Merge the new skills into the form instead of reloading it, so unsaved edits survive.
              setForm((current) => current && ({ ...current, skills: [...current.skills, ...added.filter((s) => !current.skills.includes(s))] }))
              queryClient.invalidateQueries({ queryKey: ['profile'] })
              queryClient.invalidateQueries({ queryKey: ['dashboard'] })
            }} />
          </section>
        </aside>
      </form>
    </Page>
  )
}
