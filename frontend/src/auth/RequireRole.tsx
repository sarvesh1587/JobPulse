import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { ShieldAlert } from 'lucide-react'
import { homeFor, useAuth } from './AuthProvider'
import type { Role } from '../lib/types'
import { ButtonLink } from '../components/ui/Button'
import { Skeleton } from '../components/ui/Feedback'

/**
 * Client-side route guard, for user experience only: it sends signed-out
 * visitors to sign in and keeps people out of screens their role can't use.
 * It isn't the security boundary. The API returns 401/403 regardless.
 */
export function RequireRole({ role, children }: { role: Role; children: ReactNode }) {
  const { user, isLoading } = useAuth()
  const location = useLocation()

  if (isLoading) {
    return (
      <div className="mx-auto max-w-page space-y-4 px-4 py-10 sm:px-6">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-40 w-full" />
      </div>
    )
  }

  if (!user) {
    const next = encodeURIComponent(location.pathname + location.search)
    return <Navigate to={`/login?next=${next}`} replace />
  }

  if (user.role !== role) {
    return (
      <div className="mx-auto max-w-md px-4 py-24 text-center">
        <ShieldAlert className="mx-auto mb-4 text-muted" />
        <h1 className="text-xl font-bold">This area isn't for your account type</h1>
        <p className="mt-2 text-sm text-muted">
          You're signed in as {user.role.toLowerCase()}. This page is for {role.toLowerCase()} accounts.
        </p>
        <ButtonLink to={homeFor(user.role)} className="mt-6">Go to your home</ButtonLink>
      </div>
    )
  }

  return <>{children}</>
}
