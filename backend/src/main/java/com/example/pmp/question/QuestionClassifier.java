package com.example.pmp.question;

import com.example.pmp.category.Category;
import com.example.pmp.category.CategoryRepository;
import com.example.pmp.category.Taxonomy;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class QuestionClassifier {
    private final CategoryRepository categoryRepository;

    private static final Map<String, List<String>> PMBOK_KEYWORDS = Map.ofEntries(
        Map.entry("P8_GOVERNANCE", List.of("charter", "business case", "benefit", "governance", "pmo", "portfolio", "program manager", "change request", "change control", "ccb", "organizational process assets", "opa", "compliance", "audit", "project management plan", "sponsor")),
        Map.entry("P8_SCOPE", List.of("scope", "requirement", "wbs", "deliverable", "acceptance", "quality", "defect", "test", "verification", "validation", "backlog", "user story", "prototype", "gold plating")),
        Map.entry("P8_SCHEDULE", List.of("schedule", "activity", "critical path", "float", "duration", "gantt", "network diagram", "milestone", "iteration length", "release date", "spi", "velocity", "sprint goal")),
        Map.entry("P8_FINANCE", List.of("cost", "budget", "cpi", "eac", "bac", "funding", "reserve", "estimate to complete", "etc", "earned value", "emv", "profit", "financial")),
        Map.entry("P8_STAKEHOLDERS", List.of("stakeholder", "customer", "client", "communication", "engagement", "influence", "supportive", "resistant", "expectation", "feedback", "sponsor")),
        Map.entry("P8_RESOURCES", List.of("team", "resource", "leadership", "leader", "training", "competency", "skill", "conflict", "mentor", "coach", "recognition", "reward", "virtual", "tuckman", "developer", "scrum master")),
        Map.entry("P8_RISK", List.of("risk", "threat", "opportunity", "uncertainty", "probability", "impact", "contingency", "risk threshold", "risk appetite", "tornado", "decision tree", "spike"))
    );

    private static final Map<String, List<String>> PMA_KEYWORDS = Map.ofEntries(
        Map.entry("PMA_INTEGRATION_CHANGE", List.of("charter", "project management plan", "change request", "change control", "ccb", "direct and manage", "monitor and control", "pmo", "portfolio", "program manager", "organizational process assets", "opa")),
        Map.entry("PMA_SCOPE_REQUIREMENTS", List.of("scope", "requirement", "wbs", "deliverable", "acceptance", "backlog", "user story", "product owner", "validate scope", "gold plating")),
        Map.entry("PMA_SCHEDULE", List.of("schedule", "critical path", "float", "duration", "gantt", "network", "milestone", "iteration length", "release date", "spi", "velocity")),
        Map.entry("PMA_COST_EVM", List.of("cost", "budget", "cpi", "eac", "bac", "reserve", "earned value", "emv", "profit", "estimate to complete", "funding")),
        Map.entry("PMA_QUALITY", List.of("quality", "defect", "test", "audit", "iso", "six sigma", "ishikawa", "fishbone", "pareto", "root cause", "control quality", "continuous improvement")),
        Map.entry("PMA_RESOURCE_TEAM", List.of("team", "resource", "training", "competency", "skill", "conflict", "leader", "leadership", "coach", "mentor", "recognition", "reward", "virtual team", "tuckman", "ground rules", "team charter")),
        Map.entry("PMA_COMM_STAKEHOLDER", List.of("stakeholder", "communication", "customer", "client", "engagement", "influence", "supportive", "resistant", "feedback", "social media", "communication channel")),
        Map.entry("PMA_RISK", List.of("risk", "threat", "opportunity", "uncertainty", "probability", "impact", "contingency", "risk threshold", "risk appetite", "tornado", "decision tree", "spike")),
        Map.entry("PMA_PROCUREMENT", List.of("vendor", "seller", "buyer", "procurement", "contract", "sow", "statement of work", "bid", "rfp", "rfq", "outsourc", "claim")),
        Map.entry("PMA_AGILE_HYBRID", List.of("agile", "scrum", "kanban", "xp", "iteration", "sprint", "backlog", "product owner", "scrum master", "velocity", "burndown", "burnup", "retrospective", "daily standup", "hybrid", "adaptive", "increment")),
        Map.entry("PMA_BUSINESS_COMPLIANCE", List.of("business case", "benefit", "business value", "compliance", "regulatory", "regulation", "sustainability", "organizational change", "market", "strategy")),
        Map.entry("PMA_CLOSING_KNOWLEDGE", List.of("close project", "closure", "final report", "lessons learned", "knowledge", "transition", "handover", "final acceptance", "archive", "release the team"))
    );

    public QuestionClassifier(CategoryRepository categoryRepository) { this.categoryRepository = categoryRepository; }

    public Set<Category> classify(String questionAndOptions) {
        String text = questionAndOptions == null ? "" : questionAndOptions.toLowerCase(Locale.ROOT);
        Set<Category> result = new LinkedHashSet<>();
        result.addAll(bestMatches(text, PMBOK_KEYWORDS, Taxonomy.PMBOK8_DOMAIN));
        result.addAll(bestMatches(text, PMA_KEYWORDS, Taxonomy.PMA_HANDOUT_TOPIC));
        return result;
    }

    private Set<Category> bestMatches(String text, Map<String, List<String>> keywords, Taxonomy taxonomy) {
        Map<String, Integer> scores = new HashMap<>();
        keywords.forEach((code, words) -> {
            int score = words.stream().mapToInt(word -> occurrences(text, word) * (word.contains(" ") ? 2 : 1)).sum();
            scores.put(code, score);
        });
        int top = scores.values().stream().max(Integer::compareTo).orElse(0);
        String fallback = taxonomy == Taxonomy.PMBOK8_DOMAIN ? "P8_GOVERNANCE" : "PMA_INTEGRATION_CHANGE";
        Set<String> selected = new LinkedHashSet<>();
        if (top == 0) selected.add(fallback);
        else scores.entrySet().stream()
                .filter(e -> e.getValue() == top || (e.getValue() >= 3 && e.getValue() >= top - 1))
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(3)
                .forEach(e -> selected.add(e.getKey()));
        Set<Category> categories = new LinkedHashSet<>();
        selected.forEach(code -> categoryRepository.findByCodeIgnoreCase(code).ifPresent(categories::add));
        return categories;
    }

    private int occurrences(String text, String needle) {
        int count = 0, index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) { count++; index += needle.length(); }
        return count;
    }
}
