// TypeScript mirrors of the backend DTOs (com.jobpulse.dto.*).
// Keep these in sync when a response record changes.

export type Role = 'CANDIDATE' | 'EMPLOYER' | 'ADMIN'
export type WorkMode = 'REMOTE' | 'HYBRID' | 'ONSITE'
export type EmploymentType = 'INTERNSHIP' | 'FULL_TIME' | 'CONTRACT'
export type ExperienceLevel = 'INTERN' | 'ENTRY' | 'MID' | 'SENIOR'
export type JobOrigin = 'INGESTED' | 'EMPLOYER'
export type JobStatus = 'ACTIVE' | 'STALE' | 'EXPIRED' | 'UNKNOWN' | 'CLOSED'
export type ApplicationStatus =
  | 'APPLIED'
  | 'IN_REVIEW'
  | 'ASSESSMENT'
  | 'INTERVIEW'
  | 'OFFER'
  | 'REJECTED'
  | 'WITHDRAWN'

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface CurrentUser {
  id: string
  email: string
  fullName: string
  role: Role
  companyId: string | null
}

export interface CompanyRef {
  id: string
  name: string
}

export interface Salary {
  min: number | null
  max: number | null
  currency: string | null
}

export interface JobSummary {
  id: string
  title: string
  company: CompanyRef
  location: string | null
  workMode: WorkMode | null
  employmentType: EmploymentType | null
  experienceLevel: ExperienceLevel | null
  salary: Salary | null
  skills: string[]
  origin: JobOrigin
  postedAt: string | null
  matchScore: number | null
  saved: boolean
}

export interface JobDetail {
  id: string
  title: string
  company: CompanyRef
  companyDescription: string | null
  companyWebsite: string | null
  location: string | null
  workMode: WorkMode | null
  employmentType: EmploymentType | null
  experienceLevel: ExperienceLevel | null
  salary: Salary | null
  skills: string[]
  description: string | null
  origin: JobOrigin
  status: JobStatus
  applicationUrl: string | null
  postedAt: string | null
  lastVerifiedAt: string | null
  saved: boolean
  myApplication: { id: string; status: ApplicationStatus; appliedAt: string } | null
}

export interface MatchScore {
  jobId: string
  overallScore: number
  skillScore: number
  experienceScore: number
  educationScore: number
  locationScore: number
  freshnessScore: number
  matchedSkills: string[]
  missingSkills: string[]
  concerns: string[]
  positiveSignals: string[]
}

export interface ApplicationJob {
  id: string
  title: string
  companyId: string
  companyName: string
  location: string | null
  origin: JobOrigin
  status: JobStatus
}

export interface Application {
  id: string
  job: ApplicationJob
  status: ApplicationStatus
  appliedAt: string
  interviewAt: string | null
  nextAction: string | null
  updatedAt: string
}

export type Actor = 'YOU' | 'EMPLOYER' | 'SYSTEM'

export interface StatusChange {
  fromStatus: ApplicationStatus | null
  toStatus: ApplicationStatus
  changedBy: Actor
  note: string | null
  at: string
}

export interface ApplicationDetail {
  id: string
  job: ApplicationJob
  status: ApplicationStatus
  appliedAt: string
  interviewAt: string | null
  coverLetter: string | null
  notes: string | null
  nextAction: string | null
  allowedNextStatuses: ApplicationStatus[]
  candidateManagesStatus: boolean
  history: StatusChange[]
  updatedAt: string
}

export interface Education {
  id?: string | null
  institution: string
  degree: string
  fieldOfStudy?: string | null
  startYear?: number | null
  endYear?: number | null
}

export interface Experience {
  id?: string | null
  companyName: string
  title: string
  startDate: string
  endDate?: string | null
  description?: string | null
}

export interface Completeness {
  percent: number
  missing: string[]
}

export interface Profile {
  userId: string
  fullName: string
  email: string
  headline: string | null
  location: string | null
  educationLevel: string | null
  graduationYear: number | null
  experienceYears: number
  summary: string | null
  preferredRoles: string[]
  skills: string[]
  educations: Education[]
  experiences: Experience[]
  resume: { id: string; fileName: string; uploadedAt: string } | null
  completeness: Completeness
}

export interface ProfileRequest {
  fullName: string
  headline: string | null
  location: string | null
  educationLevel: string | null
  graduationYear: number | null
  experienceYears: number
  summary: string | null
  preferredRoles: string[]
  skills: string[]
  educations: Education[]
  experiences: Experience[]
}

export interface ResumeAnalysis {
  detectedSkills: string[]
  likelyEducationLevel: string | null
  likelyGraduationYear: number | null
  extractedTextPreview: string
  persisted: boolean
  resumeId: string | null
  skillsAddedToProfile: string[]
}

export interface Dashboard {
  firstName: string
  applicationsByStatus: Record<ApplicationStatus, number>
  activeApplications: number
  savedJobs: number
  upcomingInterviews: Application[]
  recentApplications: Application[]
  recentlySaved: JobSummary[]
  recommendationBasis: 'SKILLS' | 'RECENT'
  recommended: JobSummary[]
  profileCompleteness: Completeness
}

export interface SkillGap {
  targetRoles: string[]
  jobsAnalyzed: number
  readySkills: string[]
  priorityGaps: { skillName: string; priority: number; frequencyInTargetRoles: number; percentOfJobs: number }[]
  computedAt: string
}

export interface CompanySummary {
  id: string
  name: string
  headquarters: string | null
  website: string | null
  openRoles: number
  internships: number
  entryLevel: number
}

export interface CompanyDetail {
  id: string
  name: string
  description: string | null
  website: string | null
  headquarters: string | null
  careersUrl: string | null
  openRoles: number
  locations: string[]
  topSkills: string[]
  rolesByEmploymentType: Record<string, number>
  rolesByExperienceLevel: Record<string, number>
  lastVerifiedAt: string | null
  openJobs: JobSummary[]
}

export interface EmployerCompany {
  id: string
  name: string
  website: string | null
  headquarters: string | null
  description: string | null
}

export interface EmployerJob {
  id: string
  title: string
  location: string
  workMode: WorkMode
  employmentType: EmploymentType
  experienceLevel: ExperienceLevel
  salaryMin: number | null
  salaryMax: number | null
  salaryCurrency: string | null
  description: string
  skills: string[]
  status: JobStatus
  postedAt: string
  applicationCount: number
}

export interface JobUpsertRequest {
  title: string
  description: string
  location: string
  workMode: WorkMode
  employmentType: EmploymentType
  experienceLevel: ExperienceLevel
  salaryMin: number | null
  salaryMax: number | null
  salaryCurrency: string | null
  skills: string[]
}

export interface EmployerOverview {
  activeJobs: number
  closedJobs: number
  applicationsByStatus: Record<ApplicationStatus, number>
}

export interface Applicant {
  userId: string
  fullName: string
  email: string
  headline: string | null
  location: string | null
  experienceYears: number
  educationLevel: string | null
  graduationYear: number | null
  skills: string[]
}

export interface EmployerApplication {
  id: string
  jobId: string
  jobTitle: string
  applicant: Applicant
  status: ApplicationStatus
  appliedAt: string
  interviewAt: string | null
  coverLetter: string | null
  allowedNextStatuses: ApplicationStatus[]
  history: StatusChange[]
}

export interface PlatformStats {
  candidates: number
  employers: number
  activeJobs: number
  totalJobs: number
  applications: number
}

export interface AdminUser {
  id: string
  email: string
  fullName: string
  role: Role
  active: boolean
  createdAt: string
}

export interface AdminJob {
  id: string
  title: string
  companyName: string
  origin: JobOrigin
  status: JobStatus
  postedAt: string | null
}
