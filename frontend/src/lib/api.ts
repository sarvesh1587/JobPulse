import { http, toQuery } from './http'
import type {
  AdminJob,
  AdminUser,
  Application,
  ApplicationDetail,
  ApplicationStatus,
  CompanyDetail,
  CompanySummary,
  CurrentUser,
  Dashboard,
  EmployerApplication,
  EmployerCompany,
  EmployerJob,
  EmployerOverview,
  JobDetail,
  JobSummary,
  JobUpsertRequest,
  MatchScore,
  PageResponse,
  PlatformStats,
  Profile,
  ProfileRequest,
  ResumeAnalysis,
  Role,
  SkillGap,
} from './types'

/** Search state as it lives in the /jobs URL. See lib/jobFilters.ts. */
export interface JobQuery {
  q?: string
  location?: string
  type?: string[]
  level?: string[]
  mode?: string[]
  minSalary?: string
  postedWithinDays?: string
  sort?: string
  page?: number
}

export const authApi = {
  me: () => http.get<CurrentUser>('/auth/me'),
  login: (email: string, password: string) => http.post<CurrentUser>('/auth/login', { email, password }),
  register: (body: { email: string; password: string; fullName: string; role: Role; companyName?: string }) =>
    http.post<CurrentUser>('/auth/register', body),
  logout: () => http.post<void>('/auth/logout'),
}

export const jobsApi = {
  search: (q: JobQuery) => http.get<PageResponse<JobSummary>>(`/jobs${toQuery({ ...q, size: 15 })}`),
  featured: () => http.get<JobSummary[]>('/jobs/featured'),
  get: (id: string) => http.get<JobDetail>(`/jobs/${id}`),
  match: (id: string) => http.get<MatchScore>(`/jobs/${id}/match`),
  skillSuggestions: (q: string) => http.get<string[]>(`/skills${toQuery({ q })}`),
}

export const savedApi = {
  list: (page: number) => http.get<PageResponse<JobSummary>>(`/saved-jobs${toQuery({ page, size: 20 })}`),
  save: (jobId: string) => http.put<void>(`/saved-jobs/${jobId}`),
  unsave: (jobId: string) => http.delete<void>(`/saved-jobs/${jobId}`),
}

export const applicationsApi = {
  list: (status?: ApplicationStatus) => http.get<Application[]>(`/applications${toQuery({ status })}`),
  get: (id: string) => http.get<ApplicationDetail>(`/applications/${id}`),
  apply: (jobId: string, coverLetter?: string) => http.post<ApplicationDetail>('/applications', { jobId, coverLetter }),
  update: (id: string, body: { status?: ApplicationStatus; interviewAt?: string | null; notes?: string; nextAction?: string }) =>
    http.patch<ApplicationDetail>(`/applications/${id}`, body),
  withdraw: (id: string) => http.post<ApplicationDetail>(`/applications/${id}/withdraw`),
}

export const profileApi = {
  get: () => http.get<Profile>('/profile'),
  update: (body: ProfileRequest) => http.put<Profile>('/profile', body),
  uploadResume: (file: File, consentToStore: boolean) => {
    const form = new FormData()
    form.append('file', file)
    form.append('consentToStore', String(consentToStore))
    return http.upload<ResumeAnalysis>('/profile/resume', form)
  },
  dashboard: () => http.get<Dashboard>('/dashboard'),
  skillGap: () => http.get<SkillGap>('/skill-gap'),
}

export const companiesApi = {
  list: (q: string, page: number) => http.get<PageResponse<CompanySummary>>(`/companies${toQuery({ q, page })}`),
  get: (id: string) => http.get<CompanyDetail>(`/companies/${id}`),
}

export const employerApi = {
  overview: () => http.get<EmployerOverview>('/employer/overview'),
  company: () => http.get<EmployerCompany>('/employer/company'),
  updateCompany: (body: Omit<EmployerCompany, 'id'>) => http.put<EmployerCompany>('/employer/company', body),
  jobs: (page: number) => http.get<PageResponse<EmployerJob>>(`/employer/jobs${toQuery({ page })}`),
  job: (id: string) => http.get<EmployerJob>(`/employer/jobs/${id}`),
  createJob: (body: JobUpsertRequest) => http.post<EmployerJob>('/employer/jobs', body),
  updateJob: (id: string, body: JobUpsertRequest) => http.put<EmployerJob>(`/employer/jobs/${id}`, body),
  setJobOpen: (id: string, open: boolean) => http.post<EmployerJob>(`/employer/jobs/${id}/${open ? 'reopen' : 'close'}`),
  applications: (jobId: string, status?: ApplicationStatus, page = 0) =>
    http.get<PageResponse<EmployerApplication>>(`/employer/jobs/${jobId}/applications${toQuery({ status, page })}`),
  application: (id: string) => http.get<EmployerApplication>(`/employer/applications/${id}`),
  updateStatus: (id: string, body: { status: ApplicationStatus; interviewAt?: string | null; note?: string }) =>
    http.patch<EmployerApplication>(`/employer/applications/${id}/status`, body),
}

export const adminApi = {
  stats: () => http.get<PlatformStats>('/admin/stats'),
  users: (q: string, role: Role | '', page: number) =>
    http.get<PageResponse<AdminUser>>(`/admin/users${toQuery({ q, role, page })}`),
  setUserActive: (id: string, active: boolean) => http.patch<AdminUser>(`/admin/users/${id}`, { active }),
  jobs: (q: string, page: number) => http.get<PageResponse<AdminJob>>(`/admin/jobs${toQuery({ q, page })}`),
  setJobOpen: (id: string, open: boolean) => http.post<AdminJob>(`/admin/jobs/${id}/${open ? 'reopen' : 'close'}`),
}
