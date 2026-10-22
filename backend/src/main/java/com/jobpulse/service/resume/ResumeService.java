package com.jobpulse.service.resume;

import com.jobpulse.dto.profile.ResumeAnalysisResponse;
import com.jobpulse.entity.CandidateProfile;
import com.jobpulse.entity.CandidateSkill;
import com.jobpulse.entity.Resume;
import com.jobpulse.entity.Skill;
import com.jobpulse.exception.ResourceNotFoundException;
import com.jobpulse.repository.CandidateProfileRepository;
import com.jobpulse.repository.ResumeRepository;
import com.jobpulse.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Resume upload. Privacy rule: nothing is written to disk or the database
 * unless the caller passes consentToStore=true. Without consent, extraction
 * still runs and the results come back, so the candidate can see what would
 * be detected before deciding whether JobPulse may keep the file.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );
    private static final int PREVIEW_LENGTH = 500;

    private final List<TextExtractor> extractors;
    private final ResumeParsingService parsingService;
    private final ResumeProperties resumeProperties;
    private final CandidateProfileRepository candidateProfileRepository;
    private final SkillRepository skillRepository;
    private final ResumeRepository resumeRepository;

    @Transactional
    public ResumeAnalysisResponse analyze(UUID userId, MultipartFile file, boolean consentToStore) {
        validate(file);
        String text = extractText(file);
        var parsed = parsingService.parse(text);
        String preview = text.length() > PREVIEW_LENGTH ? text.substring(0, PREVIEW_LENGTH) + "…" : text;

        if (!consentToStore) {
            return new ResumeAnalysisResponse(parsed.detectedSkills(), parsed.likelyEducationLevel(),
                    parsed.likelyGraduationYear(), preview, false, null, List.of());
        }

        CandidateProfile profile = candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No candidate profile found for this account."));

        String storedPath = storeFile(userId, file);
        Resume resume = resumeRepository.save(new Resume(
                profile, sanitizeFileName(file.getOriginalFilename()), storedPath, text, Instant.now()));

        List<String> added = attachDetectedSkills(profile, parsed.detectedSkills());
        log.info("Stored resume {} for user {} ({} new skills added)", resume.getId(), userId, added.size());

        return new ResumeAnalysisResponse(parsed.detectedSkills(), parsed.likelyEducationLevel(),
                parsed.likelyGraduationYear(), preview, true, resume.getId(), added);
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidResumeException("Please attach a resume file.");
        }
        if (file.getSize() > resumeProperties.maxSizeBytes()) {
            long maxMb = resumeProperties.maxSizeBytes() / (1024 * 1024);
            throw new InvalidResumeException("That file is too large. The limit is " + maxMb + " MB.");
        }
        if (file.getContentType() == null || !ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new InvalidResumeException("Unsupported file type. Upload a PDF or DOCX resume.");
        }
    }

    private String extractText(MultipartFile file) {
        TextExtractor extractor = extractors.stream()
                .filter(e -> e.supports(file.getContentType()))
                .findFirst()
                .orElseThrow(() -> new InvalidResumeException("Unsupported file type. Upload a PDF or DOCX resume."));
        String text;
        try {
            text = extractor.extractText(file);
        } catch (IOException e) {
            log.warn("Resume text extraction failed: {}", e.getMessage());
            throw new InvalidResumeException("We couldn't read this file. It may be corrupted, empty, or password-protected.");
        }
        if (text == null || text.isBlank()) {
            throw new InvalidResumeException("No readable text found. Is this a scanned image rather than a text document?");
        }
        return text;
    }

    private String storeFile(UUID userId, MultipartFile file) {
        Path userDir = Paths.get(resumeProperties.storageDir(), userId.toString()).toAbsolutePath().normalize();
        Path target = userDir.resolve(UUID.randomUUID() + "-" + sanitizeFileName(file.getOriginalFilename())).normalize();
        // Defence in depth: the sanitized name can't contain "..", but never
        // write outside the user's own directory even if that changes.
        if (!target.startsWith(userDir)) {
            throw new InvalidResumeException("Invalid file name.");
        }
        try {
            Files.createDirectories(userDir);
            file.transferTo(target);
            return target.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Could not store resume file for user " + userId, e);
        }
    }

    private String sanitizeFileName(String originalName) {
        String base = originalName == null ? "resume" : Paths.get(originalName).getFileName().toString();
        return base.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /** Adds catalogued skills found in the resume that the profile doesn't have yet. */
    private List<String> attachDetectedSkills(CandidateProfile profile, List<String> detectedSkillNames) {
        Set<String> existing = profile.getSkills().stream()
                .map(cs -> cs.getSkill().getName().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        Set<String> wanted = detectedSkillNames.stream()
                .map(n -> n.toLowerCase(Locale.ROOT))
                .filter(n -> !existing.contains(n))
                .collect(Collectors.toSet());
        if (wanted.isEmpty()) return List.of();

        List<Skill> skills = skillRepository.findByLowerNames(wanted);
        skills.forEach(skill -> profile.getSkills().add(
                new CandidateSkill(profile, skill, CandidateSkill.SOURCE_RESUME)));
        return skills.stream().map(Skill::getName).sorted().toList();
    }
}
