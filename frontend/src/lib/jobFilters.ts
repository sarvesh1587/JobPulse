import type { JobQuery } from './api'

/**
 * The /jobs page keeps all search state in the URL, so results can be shared
 * and bookmarked, refreshing keeps them, and Back and Forward step through
 * searches. These two functions convert between URLSearchParams and the
 * JobQuery the API takes.
 */
export function parseJobQuery(params: URLSearchParams): JobQuery {
  const page = Number(params.get('page') ?? 0)
  return {
    q: params.get('q') ?? undefined,
    location: params.get('location') ?? undefined,
    type: params.getAll('type'),
    level: params.getAll('level'),
    mode: params.getAll('mode'),
    minSalary: params.get('minSalary') ?? undefined,
    postedWithinDays: params.get('postedWithinDays') ?? undefined,
    sort: params.get('sort') ?? undefined,
    page: Number.isFinite(page) && page > 0 ? Math.floor(page) : 0,
  }
}

export function toSearchParams(query: JobQuery): URLSearchParams {
  const params = new URLSearchParams()
  if (query.q) params.set('q', query.q)
  if (query.location) params.set('location', query.location)
  query.type?.forEach((v) => params.append('type', v))
  query.level?.forEach((v) => params.append('level', v))
  query.mode?.forEach((v) => params.append('mode', v))
  if (query.minSalary) params.set('minSalary', query.minSalary)
  if (query.postedWithinDays) params.set('postedWithinDays', query.postedWithinDays)
  if (query.sort && query.sort !== 'RECENT') params.set('sort', query.sort)
  if (query.page) params.set('page', String(query.page))
  return params
}

/** How many filters (not the keyword or the page) are active, for the mobile "Filters (3)" button. */
export function activeFilterCount(query: JobQuery): number {
  return (query.type?.length ?? 0) + (query.level?.length ?? 0) + (query.mode?.length ?? 0)
    + (query.minSalary ? 1 : 0) + (query.postedWithinDays ? 1 : 0) + (query.location ? 1 : 0)
}
