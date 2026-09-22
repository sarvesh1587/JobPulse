package com.jobpulse.resume;

import com.jobpulse.dto.ResumeAnalysisResponse;
import com.jobpulse.entity.CandidateProfile;
import com.jobpulse.entity.CandidateSkill;
import com.jobpulse.entity.Resume;
import com.jobpulse.entity.Skill;
import com.jobpulse.exception.ResourceNotFoundException;
import com.jobpulse.repository.CandidateProfileRepository;
import com.jobpulse.repository.CandidateSkillRepository;
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
 * Handles the whole "upload a resume" flow, honoring the brief's explicit
 * privacy requirement: nothing gets written to disk or the database unless
 * the caller passes consentToStore=true. Without consent, this still runs
 * extraction and returns the results — the person can see what would be
 * detected before deciding whether to let JobPulse keep it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final List<TextExtractor> extractors;
    private final ResumeParsingService parsingService;
    private final ResumeProperties resumeProperties;
    private final CandidateProfileRepository candidateProfileRepository;
    private final CandidateSkillRepository candidateSkillRepository;
    private final SkillRepository skillRepository;
    private final ResumeRepository resumeRepository;

    @Transactional
    public ResumeAnalysisResponse analyze(UUID userId, MultipartFile file, boolean consentToStore) {
        validate(file);

        TextExtractor extractor = extractors.stream()
                .filter(e -> e.supports(file.getContentType()))
                .findFirst()
                .orElseThrow(() -> new InvalidResumeException(
                        "Unsupported file type. Please upload a PDF or DOCX resume."));

        String text;
        try {
            text = extractor.extractText(file);
        } catch (IOException e) {
            log.warn("Resume text extraction failed: {}", e.getMessage());
            throw new InvalidResumeException(
                    "Couldn't read this file — it may be corrupted, empty, or password-protected.");
        }

        if (text == null || text.isBlank()) {
            throw new InvalidResumeException(
                    "No readable text found in this file — is it a scanned image rather than a text-based document?");
        }

        var parsed = parsingService.parse(text);
        String preview = text.length() > 500 ? text.substring(0, 500) + "…" : text;

        if (!consentToStore) {
            return new ResumeAnalysisResponse(
                    parsed.detectedSkills(), parsed.likelyEducationLevel(), parsed.likelyGraduationYear(),
                    preview, false, null);
        }

        CandidateProfile profile = candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Complete your profile before uploading a resume"));

        String storedPath = storeFile(userId, file);

        Resume resume = Resume.builder()
                .candidateProfile(profile)
                .fileName(sanitizeFileName(file.getOriginalFilename()))
                .storagePath(storedPath)
                .extractedText(text)
                .parsedAt(Instant.now())
                .build();
        resume = resumeRepository.save(resume);

        attachDetectedSkills(profile, parsed.detectedSkills());

        return new ResumeAnalysisResponse(
                parsed.detectedSkills(), parsed.likelyEducationLevel(), parsed.likelyGraduationYear(),
                preview, true, resume.getId());
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidResumeException("Please attach a resume file.");
        }
        if (file.getSize() > resumeProperties.maxSizeBytes()) {
            long maxMb = resumeProperties.maxSizeBytes() / (1024 * 1024);
            throw new InvalidResumeException("File is too large — the limit is " + maxMb + " MB.");
        }
        if (file.getContentType() == null || !ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new InvalidResumeException("Unsupported file type. Please upload a PDF or DOCX resume.");
        }
    }

    private String storeFile(UUID userId, MultipartFile file) {
        try {
            Path userDir = Paths.get(resumeProperties.storageDir(), userId.toString());
            Files.createDirectories(userDir);

            String safeName = UUID.randomUUID() + "-" + sanitizeFileName(file.getOriginalFilename());
            Path target = userDir.resolve(safeName).normalize();

            if (!target.startsWith(userDir)) {
                // Defends against a crafted filename trying to escape the
                // user's directory via "../" segments.
                throw new InvalidResumeException("Invalid file name.");
            }

            file.transferTo(target);
            return target.toString();
        } catch (IOException e) {
            log.error("Failed to store resume file for user {}: {}", userId, e.getMessage());
            throw new InvalidResumeException("Couldn't save the file — please try again.");
        }
    }

    private String sanitizeFileName(String originalName) {
        String base = originalName == null ? "resume" : Paths.get(originalName).getFileName().toString();
        return base.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void attachDetectedSkills(CandidateProfile profile, List<String> detectedSkillNames) {
        Set<String> existing = candidateSkillRepository.findByCandidateProfileId(profile.getId()).stream()
                .map(cs -> cs.getSkill().getName().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        for (String name : detectedSkillNames) {
            if (existing.contains(name.toLowerCase(Locale.ROOT))) continue;

            Skill skill = skillRepository.findByNameIgnoreCase(name).orElse(null);
            if (skill == null) continue; // only attach known, catalogued skills

            candidateSkillRepository.save(CandidateSkill.builder()
                    .candidateProfile(profile)
                    .skill(skill)
                    .proficiency("INTERMEDIATE")
                    .source("RESUME")
                    .build());
        }
    }
}
