/** Converts an ISO instant to the local "YYYY-MM-DDTHH:mm" a datetime-local input expects. */
export function toLocalInput(iso: string | null): string {
  if (!iso) return ''
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** The reverse: a datetime-local value (local time) to an ISO instant for the API. */
export function fromLocalInput(value: string): string | null {
  return value ? new Date(value).toISOString() : null
}
