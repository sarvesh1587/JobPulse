import { describe, expect, it } from 'vitest'
import { activeFilterCount, parseJobQuery, toSearchParams } from './jobFilters'

describe('job search URL state', () => {
  it('round-trips every filter through the URL', () => {
    const url = 'q=java&location=pune&type=INTERNSHIP&type=FULL_TIME&mode=REMOTE&minSalary=500000&postedWithinDays=7&sort=SALARY&page=2'
    const query = parseJobQuery(new URLSearchParams(url))

    expect(query.type).toEqual(['INTERNSHIP', 'FULL_TIME'])
    expect(query.page).toBe(2)
    expect(toSearchParams(query).toString()).toBe(url)
  })

  it('drops defaults so URLs stay short', () => {
    expect(toSearchParams({ q: 'react', sort: 'RECENT', page: 0 }).toString()).toBe('q=react')
  })

  it('ignores a nonsense page number', () => {
    expect(parseJobQuery(new URLSearchParams('page=-3')).page).toBe(0)
    expect(parseJobQuery(new URLSearchParams('page=abc')).page).toBe(0)
  })

  it('counts filters but not the keyword or page', () => {
    expect(activeFilterCount(parseJobQuery(new URLSearchParams('q=x&type=INTERNSHIP&mode=REMOTE&mode=HYBRID&page=3')))).toBe(3)
  })
})
