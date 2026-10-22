package com.jobpulse.service;

import com.jobpulse.entity.Skill;
import com.jobpulse.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Turns free-text skill names from forms into Skill rows. Matching is
 * case-insensitive ("spring boot" reuses "Spring Boot"), and names not in
 * the catalogue yet are added, so candidates and employers aren't limited to
 * the seed list.
 */
@Service
@RequiredArgsConstructor
public class SkillCatalogService {

    private final SkillRepository skillRepository;

    /** Returns one Skill per distinct name, in the order given, creating any that don't exist. */
    @Transactional
    public List<Skill> resolve(List<String> rawNames) {
        Map<String, String> wanted = new LinkedHashMap<>();   // lower-case key -> display name
        for (String raw : rawNames) {
            String name = TextNormalizer.clean(raw);
            if (name != null) wanted.putIfAbsent(name.toLowerCase(Locale.ROOT), name);
        }
        if (wanted.isEmpty()) return List.of();

        Map<String, Skill> existing = new LinkedHashMap<>();
        skillRepository.findByLowerNames(wanted.keySet())
                .forEach(skill -> existing.put(skill.getName().toLowerCase(Locale.ROOT), skill));

        List<Skill> result = new ArrayList<>();
        wanted.forEach((key, displayName) -> result.add(
                existing.computeIfAbsent(key, k -> skillRepository.save(new Skill(displayName)))));
        return result;
    }

    @Transactional(readOnly = true)
    public List<String> suggest(String fragment) {
        String q = TextNormalizer.clean(fragment);
        if (q == null) return List.of();
        return skillRepository.findTop15ByNameContainingIgnoreCaseOrderByNameAsc(q).stream()
                .map(Skill::getName)
                .toList();
    }
}
