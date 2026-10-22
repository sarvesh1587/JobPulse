// The one place the app talks HTTP. Components never call fetch directly;
// they use the typed functions in api.ts, which use these helpers.

const BASE_URL = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? '/api'

/** Mirrors the backend's ApiError JSON. */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
    readonly fieldErrors: Record<string, string> = {},
  ) {
    super(message)
    this.name = 'ApiError'
  }

  get isUnauthenticated() {
    return this.status === 401
  }
}

/** Spring sets XSRF-TOKEN as a readable cookie; writes echo it back in a header. */
export function readCsrfToken(cookieString: string): string | null {
  const match = cookieString.split('; ').find((part) => part.startsWith('XSRF-TOKEN='))
  return match ? decodeURIComponent(match.slice('XSRF-TOKEN='.length)) : null
}

async function ensureCsrfCookie() {
  await fetch(`${BASE_URL}/auth/csrf`, { credentials: 'include' })
}

type Method = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'

async function send(method: Method, path: string, body?: unknown): Promise<Response> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  const isForm = body instanceof FormData
  if (body !== undefined && !isForm) headers['Content-Type'] = 'application/json'

  if (method !== 'GET') {
    if (!readCsrfToken(document.cookie)) await ensureCsrfCookie()
    const token = readCsrfToken(document.cookie)
    if (token) headers['X-XSRF-TOKEN'] = token
  }

  return fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    credentials: 'include', // send the HttpOnly auth cookie
    body: body === undefined ? undefined : isForm ? (body as FormData) : JSON.stringify(body),
  })
}

async function toError(response: Response): Promise<ApiError> {
  try {
    const data = await response.json()
    return new ApiError(response.status, data.code ?? 'ERROR', data.message ?? response.statusText, data.fieldErrors ?? {})
  } catch {
    return new ApiError(response.status, 'ERROR', response.status >= 500
      ? 'The server had a problem. Please try again.'
      : 'Something went wrong with that request.')
  }
}

export async function request<T>(method: Method, path: string, body?: unknown): Promise<T> {
  let response: Response
  try {
    response = await send(method, path, body)
    // A stale CSRF token (e.g. after the cookie was cleared) is refreshed once and retried.
    if (response.status === 403 && method !== 'GET') {
      const error = await toError(response.clone())
      if (error.code === 'CSRF_INVALID') {
        await ensureCsrfCookie()
        response = await send(method, path, body)
      }
    }
  } catch {
    throw new ApiError(0, 'NETWORK', "Can't reach JobPulse. Check your connection and try again.")
  }

  if (!response.ok) throw await toError(response)
  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}

export const http = {
  get: <T>(path: string) => request<T>('GET', path),
  post: <T>(path: string, body?: unknown) => request<T>('POST', path, body ?? {}),
  put: <T>(path: string, body?: unknown) => request<T>('PUT', path, body ?? {}),
  patch: <T>(path: string, body?: unknown) => request<T>('PATCH', path, body ?? {}),
  delete: <T>(path: string) => request<T>('DELETE', path),
  upload: <T>(path: string, form: FormData) => request<T>('POST', path, form),
}

/** Builds a query string, skipping empty values and repeating keys for arrays. */
export function toQuery(params: Record<string, string | number | string[] | undefined | null>): string {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') return
    if (Array.isArray(value)) value.forEach((v) => search.append(key, v))
    else search.set(key, String(value))
  })
  const qs = search.toString()
  return qs ? `?${qs}` : ''
}
