import { createContext, useCallback, useContext, useMemo, type ReactNode } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { authApi } from '../lib/api'
import { ApiError } from '../lib/http'
import type { CurrentUser, Role } from '../lib/types'

interface AuthContextValue {
  user: CurrentUser | null
  isLoading: boolean
  login: (email: string, password: string) => Promise<CurrentUser>
  register: (body: { email: string; password: string; fullName: string; role: Role; companyName?: string }) => Promise<CurrentUser>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

const ME_KEY = ['auth', 'me'] as const

/**
 * The session lives in an HttpOnly cookie the browser manages; JS never sees
 * the token. "Who am I" comes from GET /api/auth/me, cached by React Query.
 * The UI uses the role only to choose what to show. The backend enforces
 * every permission on its own.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()

  const me = useQuery({
    queryKey: ME_KEY,
    queryFn: async () => {
      try {
        return await authApi.me()
      } catch (e) {
        if (e instanceof ApiError && e.isUnauthenticated) return null
        throw e
      }
    },
    staleTime: 5 * 60_000,
    retry: false,
  })

  // Switching accounts must never show the previous user's cached data, so
  // every other query is dropped. The "me" query itself is updated in place
  // rather than removed: removing it would detach the mounted observer, and
  // the new value would never reach the UI.
  const resetUserData = useCallback((user: CurrentUser | null) => {
    queryClient.removeQueries({ predicate: (q) => q.queryKey[0] !== ME_KEY[0] })
    queryClient.setQueryData(ME_KEY, user)
  }, [queryClient])

  const startSession = useCallback((user: CurrentUser) => {
    resetUserData(user)
    return user
  }, [resetUserData])

  const login = useCallback(async (email: string, password: string) =>
    startSession(await authApi.login(email, password)), [startSession])

  const register = useCallback(async (body: Parameters<typeof authApi.register>[0]) =>
    startSession(await authApi.register(body)), [startSession])

  const logout = useCallback(async () => {
    await authApi.logout()
    resetUserData(null)
  }, [resetUserData])

  const value = useMemo<AuthContextValue>(() => ({
    user: me.data ?? null,
    isLoading: me.isLoading,
    login,
    register,
    logout,
  }), [me.data, me.isLoading, login, register, logout])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}

/** Where each role lands after signing in. */
export function homeFor(role: Role): string {
  return role === 'EMPLOYER' ? '/employer' : role === 'ADMIN' ? '/admin' : '/dashboard'
}
