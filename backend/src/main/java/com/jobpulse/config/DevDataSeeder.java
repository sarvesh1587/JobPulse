package com.jobpulse.config;

import com.jobpulse.entity.ApplicationStatus;
import com.jobpulse.entity.CandidateProfile;
import com.jobpulse.entity.CandidateSkill;
import com.jobpulse.entity.Company;
import com.jobpulse.entity.Job;
import com.jobpulse.entity.JobApplication;
import com.jobpulse.entity.JobSkill;
import com.jobpulse.entity.Role;
import com.jobpulse.entity.User;
import com.jobpulse.repository.CandidateProfileRepository;
import com.jobpulse.repository.CompanyRepository;
import com.jobpulse.repository.JobApplicationRepository;
import com.jobpulse.repository.JobRepository;
import com.jobpulse.repository.JobSkillRepository;
import com.jobpulse.repository.UserRepository;
import com.jobpulse.service.SkillCatalogService;
import com.jobpulse.service.TextNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * DEMO DATA FOR LOCAL DEVELOPMENT ONLY. It runs only with the "dev" profile
 * and only when the demo accounts don't exist yet. It creates three fictional
 * employers (all on @example.com addresses, a domain reserved for examples),
 * their job listings, a demo candidate and a demo admin, so every screen has
 * something to show on a fresh database. Nothing here runs in production.
 */
@Slf4j
@Component
@Profile("dev")
@Order(2)
@RequiredArgsConstructor
public class DevDataSeeder implements ApplicationRunner {

    static final String CANDIDATE_EMAIL = "candidate@example.com";

    private final UserRepository userRepository;
    private final CandidateProfileRepository profileRepository;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;
    private final JobApplicationRepository applicationRepository;
    private final SkillCatalogService skillCatalog;
    private final PasswordEncoder passwordEncoder;

    @Value("${jobpulse.dev-seed.password}")
    private String demoPassword;

    private record DemoJob(String title, String location, Job.WorkMode mode, Job.EmploymentType type,
                           Job.ExperienceLevel level, Integer salaryMinLakh, Integer salaryMaxLakh,
                           List<String> skills, String summary, List<String> duties, int daysAgo) {}

    private record DemoCompany(String name, String email, String recruiter, String website, String hq,
                               String description, List<DemoJob> jobs) {}

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(CANDIDATE_EMAIL)) return;

        Job firstJob = null;
        for (DemoCompany demo : companies()) {
            User employer = userRepository.save(new User(demo.email(), passwordEncoder.encode(demoPassword),
                    demo.recruiter(), Role.EMPLOYER));
            Company company = new Company(demo.name(), TextNormalizer.normalize(demo.name()));
            company.setOwner(employer);
            company.setWebsite(demo.website());
            company.setHeadquarters(demo.hq());
            company.setDescription(demo.description());
            companyRepository.save(company);
            for (DemoJob dj : demo.jobs()) {
                Job job = saveJob(company, employer, dj);
                if (firstJob == null) firstJob = job;
            }
        }

        User candidate = userRepository.save(new User(CANDIDATE_EMAIL, passwordEncoder.encode(demoPassword),
                "Aarav Kulkarni", Role.CANDIDATE));
        CandidateProfile profile = new CandidateProfile(candidate);
        profile.setHeadline("Final-year CS student · Java backend");
        profile.setLocation("Pune");
        profile.setEducationLevel("B.E.");
        profile.setGraduationYear(2027);
        profile.setPreferredRoles(new ArrayList<>(List.of("Backend", "Java")));
        skillCatalog.resolve(List.of("Java", "Spring Boot", "PostgreSQL", "Git", "REST"))
                .forEach(s -> profile.getSkills().add(new CandidateSkill(profile, s, CandidateSkill.SOURCE_MANUAL)));
        profileRepository.save(profile);

        JobApplication application = new JobApplication(candidate, firstJob, Instant.now().minus(2, ChronoUnit.DAYS));
        application.setCoverLetter("I've built two Spring Boot services with PostgreSQL and would love to work on your platform team.");
        application.recordInitialStatus(candidate);
        application.changeStatus(ApplicationStatus.IN_REVIEW, firstJob.getPostedBy(), null);
        applicationRepository.save(application);

        userRepository.save(new User("admin@example.com", passwordEncoder.encode(demoPassword), "Demo Admin", Role.ADMIN));
        log.info("Seeded demo data (dev profile). Demo accounts use the password from jobpulse.dev-seed.password.");
    }

    private Job saveJob(Company company, User employer, DemoJob dj) {
        Job job = new Job();
        job.setOrigin(Job.Origin.EMPLOYER);
        job.setCompany(company);
        job.setPostedBy(employer);
        job.setTitle(dj.title());
        job.setNormalizedTitle(TextNormalizer.normalize(dj.title()));
        job.setLocation(dj.location());
        job.setNormalizedLocation(TextNormalizer.normalize(dj.location()));
        job.setWorkMode(dj.mode());
        job.setEmploymentType(dj.type());
        job.setExperienceLevel(dj.level());
        if (dj.salaryMinLakh() != null) {
            job.setSalaryMin(BigDecimal.valueOf(dj.salaryMinLakh() * 100_000L));
            job.setSalaryMax(BigDecimal.valueOf(dj.salaryMaxLakh() * 100_000L));
            job.setSalaryCurrency("INR");
        }
        job.setDescription(describe(dj));
        Instant posted = Instant.now().minus(dj.daysAgo(), ChronoUnit.DAYS).minus(dj.title().length(), ChronoUnit.MINUTES);
        job.setPostedAt(posted);
        job.setLastVerifiedAt(posted);
        job.setStatus(Job.JobStatus.ACTIVE);
        Job saved = jobRepository.save(job);
        skillCatalog.resolve(dj.skills()).forEach(skill -> jobSkillRepository.save(new JobSkill(saved, skill)));
        return saved;
    }

    private String describe(DemoJob dj) {
        StringBuilder sb = new StringBuilder();
        sb.append(dj.summary()).append("\n\n");
        sb.append("What you'll do\n");
        dj.duties().forEach(d -> sb.append("- ").append(d).append('\n'));
        sb.append("\nWhat we're looking for\n");
        sb.append("- Working knowledge of ").append(String.join(", ", dj.skills())).append('\n');
        sb.append("- Clear written communication and a habit of testing your own work");
        return sb.toString();
    }

    private static List<DemoCompany> companies() {
        var R = Job.WorkMode.REMOTE; var H = Job.WorkMode.HYBRID; var O = Job.WorkMode.ONSITE;
        var FT = Job.EmploymentType.FULL_TIME; var IN = Job.EmploymentType.INTERNSHIP; var CT = Job.EmploymentType.CONTRACT;
        var INTERN = Job.ExperienceLevel.INTERN; var ENTRY = Job.ExperienceLevel.ENTRY;
        var MID = Job.ExperienceLevel.MID; var SENIOR = Job.ExperienceLevel.SENIOR;

        return List.of(
            new DemoCompany("Northwind Labs", "hiring@northwind.example.com", "Meera Iyer",
                "https://northwind.example.com", "Pune",
                "Northwind Labs builds payment reconciliation software for mid-sized retailers. (Demo company for local development.)",
                List.of(
                    new DemoJob("Java Backend Engineer", "Pune", H, FT, ENTRY, 8, 12,
                        List.of("Java", "Spring Boot", "PostgreSQL", "REST", "JUnit"),
                        "Join the platform team that settles millions of transactions a day.",
                        List.of("Build and own REST services in Spring Boot", "Write migrations and tune PostgreSQL queries", "Review code and pair with senior engineers"), 1),
                    new DemoJob("Backend Engineering Intern", "Pune", O, IN, INTERN, 3, 4,
                        List.of("Java", "SQL", "Git"),
                        "A six-month internship on the reconciliation engine, with a mentor and real production work.",
                        List.of("Fix bugs and ship small features end to end", "Add tests around legacy code", "Present your work at the end of the internship"), 2),
                    new DemoJob("Senior Platform Engineer", "Remote (India)", R, FT, SENIOR, 30, 45,
                        List.of("Java", "Kafka", "Kubernetes", "AWS", "PostgreSQL"),
                        "Lead the move of our settlement pipeline to event-driven processing.",
                        List.of("Design service boundaries and data flows", "Mentor a team of five engineers", "Own reliability targets for the pipeline"), 6),
                    new DemoJob("QA Automation Engineer", "Pune", H, FT, MID, 10, 16,
                        List.of("Java", "Selenium", "JUnit", "CI/CD"),
                        "Own the automated test suite that guards every release.",
                        List.of("Build and maintain end-to-end test suites", "Integrate tests into CI pipelines", "Work with developers on testability"), 9))),
            new DemoCompany("Kestrel Analytics", "talent@kestrel.example.com", "Rohan Desai",
                "https://kestrel.example.com", "Bengaluru",
                "Kestrel Analytics helps logistics companies forecast demand from their own shipment data. (Demo company for local development.)",
                List.of(
                    new DemoJob("Full Stack Developer", "Bengaluru", H, FT, ENTRY, 9, 14,
                        List.of("React", "TypeScript", "Java", "Spring Boot", "PostgreSQL"),
                        "Build the dashboards our customers open every morning.",
                        List.of("Ship features across a React frontend and a Spring Boot API", "Turn customer feedback into small, testable changes", "Keep pages fast on large datasets"), 0),
                    new DemoJob("Frontend Engineer", "Remote (India)", R, FT, MID, 14, 22,
                        List.of("React", "TypeScript", "CSS", "Testing Library"),
                        "Own the charting and reporting experience in our web app.",
                        List.of("Build accessible, responsive UI components", "Improve rendering performance of large tables", "Write component and integration tests"), 3),
                    new DemoJob("Data Engineering Intern", "Bengaluru", O, IN, INTERN, 2, 3,
                        List.of("Python", "SQL", "Git"),
                        "Help us build reliable data pipelines from customer shipment feeds.",
                        List.of("Write and test ETL jobs", "Document data sources", "Monitor pipeline runs and fix failures"), 4),
                    new DemoJob("Machine Learning Engineer", "Bengaluru", H, FT, MID, 18, 28,
                        List.of("Python", "SQL", "Docker", "AWS"),
                        "Improve the forecasting models behind our core product.",
                        List.of("Train and evaluate demand forecasting models", "Productionise models behind APIs", "Measure model quality over time"), 12),
                    new DemoJob("DevOps Engineer (Contract)", "Remote (India)", R, CT, MID, null, null,
                        List.of("Docker", "Kubernetes", "AWS", "CI/CD"),
                        "A six-month contract to modernise our deployment pipeline.",
                        List.of("Containerise remaining services", "Set up staged deployments", "Write runbooks for the on-call team"), 15))),
            new DemoCompany("Harbor & Pine", "careers@harborpine.example.com", "Ananya Rao",
                "https://harborpine.example.com", "Hyderabad",
                "Harbor & Pine runs a booking platform for independent hotels across India. (Demo company for local development.)",
                List.of(
                    new DemoJob("Software Engineer I - Java", "Hyderabad", H, FT, ENTRY, 7, 11,
                        List.of("Java", "Spring", "MySQL", "REST"),
                        "Work on the booking engine that handles availability and pricing.",
                        List.of("Build APIs used by our partner hotels", "Write unit and integration tests", "Take part in on-call after onboarding"), 1),
                    new DemoJob("Android Developer", "Hyderabad", O, FT, MID, 12, 18,
                        List.of("Kotlin", "Android", "REST", "Git"),
                        "Own the guest app used by thousands of travellers each week.",
                        List.of("Build new booking flows in Kotlin", "Improve app startup time", "Work closely with design on details"), 5),
                    new DemoJob("Product Design Intern", "Hyderabad", H, IN, INTERN, 2, 3,
                        List.of("Figma", "User Research"),
                        "Design and test improvements to the hotel partner dashboard.",
                        List.of("Run short usability tests", "Turn findings into prototypes", "Pair with engineers on implementation"), 7),
                    new DemoJob("Backend Engineer - Payments", "Pune", H, FT, MID, 16, 24,
                        List.of("Java", "Spring Boot", "PostgreSQL", "Kafka", "Docker"),
                        "Build the payments and refunds services for hotel bookings.",
                        List.of("Integrate payment gateways safely and idempotently", "Design for retries and partial failures", "Own the payments data model"), 2),
                    new DemoJob("Technical Support Engineer", "Hyderabad", O, FT, ENTRY, 5, 7,
                        List.of("SQL", "REST", "Linux"),
                        "Be the engineer our hotel partners talk to when something breaks.",
                        List.of("Debug customer issues using logs and SQL", "Write clear bug reports for engineering", "Improve internal tooling"), 20)))
        );
    }
}
