import { describe, expect, it } from 'vitest'
import { formatSalary, initials, relativeTime } from './format'

describe('formatSalary', () => {
  it('shows INR ranges in lakh per annum', () => {
    expect(formatSalary({ min: 800000, max: 1200000, currency: 'INR' })).toBe('₹8–12 LPA')
    expect(formatSalary({ min: 350000, max: 450000, currency: 'INR' })).toBe('₹3.5–4.5 LPA')
  })

  it('handles a single bound and missing salary', () => {
    expect(formatSalary({ min: null, max: 1500000, currency: 'INR' })).toBe('₹15 LPA')
    expect(formatSalary(null)).toBeNull()
    expect(formatSalary({ min: null, max: null, currency: null })).toBeNull()
  })
})

describe('relativeTime', () => {
  const now = new Date('2026-10-22T12:00:00Z')

  it('describes recent times in words', () => {
    expect(relativeTime('2026-10-22T11:59:30Z', now)).toBe('just now')
    expect(relativeTime('2026-10-22T09:00:00Z', now)).toBe('3 hours ago')
    expect(relativeTime('2026-10-21T12:00:00Z', now)).toBe('yesterday')
  })

  it('handles missing dates', () => {
    expect(relativeTime(null, now)).toBe('—')
  })
})

describe('initials', () => {
  it('takes the first letter of the first two names', () => {
    expect(initials('Riya Menon')).toBe('RM')
    expect(initials('aarav')).toBe('A')
  })
})
