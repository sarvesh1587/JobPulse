import { useRef, useState, type FormEvent, type ReactNode } from 'react'
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom'
import { Briefcase, UserRound } from 'lucide-react'
import { homeFor, useAuth } from '../auth/AuthProvider'
import { ApiError } from '../lib/http'
import type { Role } from '../lib/types'
import { useTitle } from '../components/layout/Page'
import { LogoMark } from '../components/layout/Logo'
import { Button } from '../components/ui/Button'
import { Field, Input } from '../components/ui/Field'
import { FormError } from '../components/ui/Feedback'

/** Only follow "next" to a path inside this app, never to another site (open-redirect protection). */
function safeNext(next: string | null): string | null {
  return next && next.startsWith('/') && !next.startsWith('//') ? next : null
}

function AuthFrame({ title, subtitle, children, footer }: { title: string; subtitle: string; children: ReactNode; footer: ReactNode }) {
  return (
    <div className="mx-auto flex min-h-[calc(100vh-56px)] max-w-md flex-col justify-center px-4 py-12">
      <div className="rise">
        <LogoMark size={32} />
        <h1 className="mt-6 text-[28px] font-extrabold tracking-tight">{title}</h1>
        <p className="mt-1.5 text-[15px] text-muted">{subtitle}</p>
      </div>
      <div className="rise mt-8" style={{ ['--i' as string]: 1 }}>{children}</div>
      <p className="mt-6 text-center text-sm text-muted">{footer}</p>
    </div>
  )
}

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export function LoginPage() {
  useTitle('Sign in')
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<unknown>(null)
  const [submitting, setSubmitting] = useState(false)

  if (user) return <Navigate to={safeNext(params.get('next')) ?? homeFor(user.role)} replace />

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      const signedIn = await login(email.trim(), password)
      navigate(safeNext(params.get('next')) ?? homeFor(signedIn.role), { replace: true })
    } catch (err) {
      setError(err)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthFrame title="Welcome back" subtitle="Sign in to pick up where you left off."
      footer={<>No account yet? <Link to="/register" className="font-semibold text-accent-text hover:underline">Create one</Link></>}>
      <form onSubmit={onSubmit} className="space-y-4" noValidate>
        <FormError error={error} />
        <Field label="Email">
          {(id) => <Input id={id} type="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} />}
        </Field>
        <Field label="Password">
          {(id) => <Input id={id} type="password" autoComplete="current-password" required value={password} onChange={(e) => setPassword(e.target.value)} />}
        </Field>
        <Button type="submit" size="lg" className="w-full" loading={submitting} disabled={!email || !password}>Sign in</Button>
      </form>
    </AuthFrame>
  )
}

interface RegisterErrors { email?: string; password?: string; fullName?: string; companyName?: string }

function validateRegistration(v: { email: string; password: string; fullName: string; role: Role; companyName: string }): RegisterErrors {
  const errors: RegisterErrors = {}
  if (!v.fullName.trim()) errors.fullName = 'Enter your name.'
  if (!EMAIL_PATTERN.test(v.email.trim())) errors.email = 'Enter a valid email address.'
  if (v.password.length < 8) errors.password = 'Use at least 8 characters.'
  else if (v.password.length > 72) errors.password = 'Use 72 characters or fewer.'
  if (v.role === 'EMPLOYER' && !v.companyName.trim()) errors.companyName = 'Enter your company name.'
  return errors
}

export function RegisterPage() {
  useTitle('Create account')
  const { user, register } = useAuth()
  const [params] = useSearchParams()
  const [role, setRole] = useState<Role>(params.get('role') === 'EMPLOYER' ? 'EMPLOYER' : 'CANDIDATE')
  const [form, setForm] = useState({ fullName: '', email: '', password: '', companyName: '' })
  const [errors, setErrors] = useState<RegisterErrors>({})
  const [serverError, setServerError] = useState<unknown>(null)
  const [submitting, setSubmitting] = useState(false)
  // Set when this page creates the account, so the redirect below sends new
  // candidates to their profile instead of the generic home for their role.
  const justRegistered = useRef(false)

  if (user) {
    const destination = justRegistered.current && user.role === 'CANDIDATE' ? '/profile?welcome=1' : homeFor(user.role)
    return <Navigate to={destination} replace />
  }

  const set = (key: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) => setForm((f) => ({ ...f, [key]: e.target.value }))

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setServerError(null)
    const clientErrors = validateRegistration({ ...form, role })
    setErrors(clientErrors)
    if (Object.keys(clientErrors).length) return

    setSubmitting(true)
    try {
      justRegistered.current = true
      await register({
        email: form.email.trim(), password: form.password, fullName: form.fullName.trim(), role,
        companyName: role === 'EMPLOYER' ? form.companyName.trim() : undefined,
      })
    } catch (err) {
      // The server validates the same rules; show its field errors next to the fields.
      justRegistered.current = false
      if (err instanceof ApiError && Object.keys(err.fieldErrors).length) setErrors(err.fieldErrors as RegisterErrors)
      setServerError(err)
    } finally {
      setSubmitting(false)
    }
  }

  const roleOption = (value: Role, icon: ReactNode, title: string, body: string) => (
    <button type="button" role="radio" aria-checked={role === value} onClick={() => setRole(value)}
      className={`flex flex-1 items-start gap-3 rounded-lg border p-3.5 text-left transition-colors ${
        role === value ? 'border-accent bg-accent/[0.07]' : 'border-border-strong hover:bg-raised'
      }`}>
      <span className={role === value ? 'text-accent-text' : 'text-muted'}>{icon}</span>
      <span>
        <span className="block text-sm font-semibold">{title}</span>
        <span className="block text-xs text-muted">{body}</span>
      </span>
    </button>
  )

  return (
    <AuthFrame title="Create your account" subtitle="Free for candidates and employers."
      footer={<>Already have an account? <Link to="/login" className="font-semibold text-accent-text hover:underline">Sign in</Link></>}>
      <form onSubmit={onSubmit} className="space-y-4" noValidate>
        <div role="radiogroup" aria-label="Account type" className="flex gap-2">
          {roleOption('CANDIDATE', <UserRound size={18} />, "I'm job hunting", 'Find and track roles')}
          {roleOption('EMPLOYER', <Briefcase size={18} />, "I'm hiring", 'Post roles, review applicants')}
        </div>
        {serverError != null && !(serverError instanceof ApiError && serverError.code === 'VALIDATION_ERROR') && <FormError error={serverError} />}
        <Field label="Full name" error={errors.fullName}>
          {(id, d) => <Input id={id} aria-describedby={d} autoComplete="name" value={form.fullName} onChange={set('fullName')} error={errors.fullName} />}
        </Field>
        {role === 'EMPLOYER' && (
          <Field label="Company name" error={errors.companyName}>
            {(id, d) => <Input id={id} aria-describedby={d} autoComplete="organization" value={form.companyName} onChange={set('companyName')} error={errors.companyName} />}
          </Field>
        )}
        <Field label="Email" error={errors.email}>
          {(id, d) => <Input id={id} aria-describedby={d} type="email" autoComplete="email" value={form.email} onChange={set('email')} error={errors.email} />}
        </Field>
        <Field label="Password" error={errors.password} hint="At least 8 characters.">
          {(id, d) => <Input id={id} aria-describedby={d} type="password" autoComplete="new-password" value={form.password} onChange={set('password')} error={errors.password} />}
        </Field>
        <Button type="submit" size="lg" className="w-full" loading={submitting}>Create account</Button>
      </form>
    </AuthFrame>
  )
}
