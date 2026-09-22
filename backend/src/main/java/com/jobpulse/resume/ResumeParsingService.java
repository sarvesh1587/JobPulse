package com.jobpulse.resume;

import com.jobpulse.entity.Skill;
import com.jobpulse.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Same honest caveat as the job-side keyword tagger: this is regex/keyword
 * matching against a small known vocabulary, not real NLP. It exists so the
 * feature is demonstrable end-to-end. Real resume parsing (arbitrary skill
 * names, actual degree/institution extraction, work-history structuring) is
 * the Python AI service's job — see brief section 24.
 */
@Service
@RequiredArgsConstructor
public class ResumeParsingService {

    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(20\\d{2})\\b");

    private static final List<String> EDUCATION_KEYWORDS = List.of(
            "B.Tech", "B.E.", "Bachelor of Engineering", "Bachelor of Technology",
            "M.Tech", "M.E.", "Master of Engineering", "Master of Technology",
            "B.Sc", "M.Sc", "MBA", "BCA", "MCA", "Ph.D"
    );

    private final SkillRepository skillRepository;

    public ParsedResume parse(String rawText) {
        String text = rawText == null ? "" : rawText;

        Set<String> detectedSkills = new LinkedHashSet<>();
        for (Skill skill : skillRepository.findAll()) {
            Pattern p = Pattern.compile("(?i)\\b" + Pattern.quote(skill.getName()) + "\\b");
            if (p.matcher(text).find()) {
                detectedSkills.add(skill.getName());
            }
        }

        String detectedEducationLevel = EDUCATION_KEYWORDS.stream()
                .filter(kw -> text.toLowerCase(Locale.ROOT).contains(kw.toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElse(null);

        // Best-effort only: takes the latest plausible graduating year
        // (2015-2035) mentioned anywhere in the text. Easy to get wrong on
        // resumes that list past project years, internship years, etc. —
        // it's a starting suggestion for the user to correct, not a fact.
        Integer likelyGraduationYear = null;
        Matcher m = YEAR_PATTERN.matcher(text);
        int latest = -1;
        while (m.find()) {
            int year = Integer.parseInt(m.group(1));
            if (year >= 2015 && year <= 2035 && year > latest) {
                latest = year;
            }
        }
        if (latest > 0) likelyGraduationYear = latest;

        return new ParsedResume(List.copyOf(detectedSkills), detectedEducationLevel, likelyGraduationYear);
    }

    public record ParsedResume(
            List<String> detectedSkills,
            String likelyEducationLevel,
            Integer likelyGraduationYear
    ) {}
}
