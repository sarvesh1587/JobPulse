import { useEffect, useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { employerApi } from '../../lib/api'
import { ApiError } from '../../lib/http'
import type { EmployerCompany } from '../../lib/types'
import { Page } from '../../components/layout/Page'
import { Button, ButtonLink } from '../../components/ui/Button'
import { Field, Input, Textarea } from '../../components/ui/Field'
import { ErrorState, FormError, Skeleton } from '../../components/ui/Feedback'
import { useToast } from '../../components/ui/Toast'

type Form = Omit<EmployerCompany, 'id'>

export default function CompanyProfilePage() {
  const toast = useToast()
  const queryClient = useQueryClient()
  const company = useQuery({ queryKey: ['employer', 'company'], queryFn: employerApi.company })
  const [form, setForm] = useState<Form>({ name: '', website: '', headquarters: '', description: '' })
  const [errors, setErrors] = useState<Record<string, string>>({})

  useEffect(() => {
    if (company.data) {
      const { name, website, headquarters, description } = company.data
      setForm({ name, website: website ?? '', headquarters: headquarters ?? '', description: description ?? '' })
    }
  }, [company.data])

  const save = useMutation({
    mutationFn: () => employerApi.updateCompany({
      name: form.name.trim(),
      website: form.website?.trim() || null,
      headquarters: form.headquarters?.trim() || null,
      description: form.description?.trim() || null,
    }),
    onSuccess: (data) => {
      queryClient.setQueryData(['employer', 'company'], data)
      queryClient.invalidateQueries({ queryKey: ['company', data.id] })
      queryClient.invalidateQueries({ queryKey: ['companies'] })
      toast.success('Company profile saved')
    },
    onError: (e) => { if (e instanceof ApiError) setErrors(e.fieldErrors) },
  })

  const onSubmit = (e: FormEvent) => {
    e.preventDefault()
    const next: Record<string, string> = {}
    if (!form.name.trim()) next.name = 'Enter the company name.'
    if (form.website && !/^https?:\/\/.+/.test(form.website.trim())) next.website = 'Start with http:// or https://'
    setErrors(next)
    if (Object.keys(next).length === 0) save.mutate()
  }

  const set = (key: keyof Form) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => setForm((f) => ({ ...f, [key]: e.target.value }))

  return (
    <Page title="Company profile" eyebrow="EMPLOYER" width="narrow" description="Shown on your company page and on every listing you post.">
      {company.isLoading && <Skeleton className="h-80 w-full" />}
      {company.isError && <ErrorState error={company.error} onRetry={() => company.refetch()} />}
      {company.data && (
        <form onSubmit={onSubmit} noValidate className="space-y-5">
          {save.isError && !(save.error instanceof ApiError && save.error.code === 'VALIDATION_ERROR') && <FormError error={save.error} />}
          <Field label="Company name" error={errors.name}>{(id) => <Input id={id} value={form.name} onChange={set('name')} error={errors.name} />}</Field>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Website" optional error={errors.website}>{(id) => <Input id={id} type="url" value={form.website ?? ''} onChange={set('website')} error={errors.website} placeholder="https://" />}</Field>
            <Field label="Headquarters" optional>{(id) => <Input id={id} value={form.headquarters ?? ''} onChange={set('headquarters')} placeholder="Pune" />}</Field>
          </div>
          <Field label="About the company" optional>{(id) => <Textarea id={id} rows={6} maxLength={4000} value={form.description ?? ''} onChange={set('description')} />}</Field>
          <div className="flex gap-2 border-t border-border pt-6">
            <Button type="submit" loading={save.isPending}>Save</Button>
            <ButtonLink to={`/companies/${company.data.id}`} variant="ghost">View public page</ButtonLink>
          </div>
        </form>
      )}
    </Page>
  )
}
