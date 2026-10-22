import { describe, expect, it } from 'vitest'
import { readCsrfToken, toQuery } from './http'

describe('readCsrfToken', () => {
  it('finds the XSRF-TOKEN cookie among others', () => {
    expect(readCsrfToken('theme=dark; XSRF-TOKEN=abc%3D123; other=1')).toBe('abc=123')
  })

  it('returns null when there is no token yet', () => {
    expect(readCsrfToken('theme=dark')).toBeNull()
    expect(readCsrfToken('')).toBeNull()
  })
})

describe('toQuery', () => {
  it('skips empty values and repeats array keys', () => {
    expect(toQuery({ q: 'java', location: '', type: ['A', 'B'], page: 0, sort: undefined })).toBe('?q=java&type=A&type=B&page=0')
  })

  it('returns an empty string when nothing is set', () => {
    expect(toQuery({ q: '' })).toBe('')
  })
})
