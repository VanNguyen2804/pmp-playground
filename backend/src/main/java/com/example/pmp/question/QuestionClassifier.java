package com.example.pmp.question;

import com.example.pmp.category.Category;
import com.example.pmp.category.CategoryRepository;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class QuestionClassifier {
    private final CategoryRepository categoryRepository;

    private static final Map<String, List<String>> TOPIC_KEYWORDS = new LinkedHashMap<>();

    static {
        TOPIC_KEYWORDS.put("TOPIC_AGILE_HYBRID", List.of(
                "agile", "scrum", "kanban", "extreme programming", " xp ", "hybrid", "adaptive",
                "iteration", "sprint", "backlog", "product owner", "scrum master", "velocity",
                "burndown", "burnup", "retrospective", "daily standup", "increment", "user story",
                "story point", "servant leader", "self-organizing", "self-managing", "timebox"));
        TOPIC_KEYWORDS.put("TOPIC_RISK", List.of(
                "risk", "threat", "opportunity", "uncertainty", "probability", "impact", "contingency",
                "risk threshold", "risk appetite", "risk tolerance", "risk register", "risk report",
                "tornado", "decision tree", "expected monetary value", " emv ", "fallback", "reserve analysis"));
        TOPIC_KEYWORDS.put("TOPIC_SCOPE_REQUIREMENTS", List.of(
                "scope", "requirement", "wbs", "work breakdown structure", "deliverable", "acceptance criteria",
                "validate scope", "scope creep", "gold plating", "requirements traceability", "prototype",
                "product scope", "collect requirements", "scope statement"));
        TOPIC_KEYWORDS.put("TOPIC_SCHEDULE", List.of(
                "schedule", "activity", "critical path", "float", "duration", "gantt", "network diagram",
                "milestone", "fast tracking", "fast-track", "crash", "lead", "lag", "spi", "release date"));
        TOPIC_KEYWORDS.put("TOPIC_COST_FINANCE", List.of(
                "cost", "budget", "cpi", "eac", "bac", "funding", "management reserve", "contingency reserve",
                "estimate to complete", " etc ", "earned value", "profit", "financial", "cost baseline",
                "cost-benefit", "benefit-cost", "present value", "roi"));
        TOPIC_KEYWORDS.put("TOPIC_QUALITY", List.of(
                "quality", "defect", "testing", "test", "quality audit", "iso", "six sigma", "ishikawa",
                "fishbone", "pareto", "root cause", "control quality", "manage quality", "continuous improvement",
                "grade", "tolerance", "quality metric", "inspection"));
        TOPIC_KEYWORDS.put("TOPIC_RESOURCE_TEAM", List.of(
                "team", "resource", "leadership", "leader", "training", "competency", "skill gap", "conflict",
                "mentor", "coach", "recognition", "reward", "virtual team", "tuckman", "developer",
                "team charter", "ground rules", "emotional intelligence", "power", "motivation", "resource leveling"));
        TOPIC_KEYWORDS.put("TOPIC_STAKEHOLDER_COMMUNICATION", List.of(
                "stakeholder", "communication", "customer", "client", "engagement", "influence", "supportive",
                "resistant", "expectation", "feedback", "sponsor", "communication channel", "information radiator",
                "kick-off", "kickoff", "presentation", "reporting"));
        TOPIC_KEYWORDS.put("TOPIC_PROCUREMENT", List.of(
                "vendor", "seller", "buyer", "procurement", "contract", "statement of work", " sow ", "bid",
                "rfp", "rfq", "outsource", "claim", "pre-bid", "source selection", "agreement"));
        TOPIC_KEYWORDS.put("TOPIC_GOVERNANCE_CHANGE", List.of(
                "governance", "pmo", "portfolio", "program manager", "change request", "change control", "ccb",
                "baseline", "project charter", "authority", "approval", "escalation", "decision rights",
                "project management plan", "integrate", "monitor and control"));
        TOPIC_KEYWORDS.put("TOPIC_BUSINESS_COMPLIANCE", List.of(
                "business case", "benefit", "business value", "compliance", "regulatory", "regulation",
                "sustainability", "organizational change", "market", "strategy", "legal", "audit", "government"));
        TOPIC_KEYWORDS.put("TOPIC_CLOSING_KNOWLEDGE", List.of(
                "close project", "close the project", "closure", "final report", "lessons learned", "knowledge",
                "transition", "handover", "final acceptance", "archive", "release the team", "administrative closure",
                "procurement closure", "benefits realization"));
        TOPIC_KEYWORDS.put("TOPIC_TOOLS_ARTIFACTS", List.of(
                "diagram", "matrix", "register", "chart", "analysis", "artifact", "document", "model",
                "checklist", "brainstorm", "facilitation", "expert judgment", "data representation"));
        TOPIC_KEYWORDS.put("TOPIC_PROJECT_MANAGEMENT", List.of(
                "project manager", "project management", "project plan", "project objective", "project life cycle",
                "project phase", "project work", "performing organization", "organizational process assets", " opa ",
                "enterprise environmental factors", " eef ", "tailoring"));
    }

    public QuestionClassifier(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public Set<Category> classify(String questionAndOptions) {
        String text = normalize(questionAndOptions);
        Map<String, Integer> scores = new LinkedHashMap<>();
        TOPIC_KEYWORDS.forEach((code, words) -> scores.put(code,
                words.stream().mapToInt(word -> occurrences(text, word) * weight(word)).sum()));

        int top = scores.values().stream().max(Integer::compareTo).orElse(0);
        LinkedHashSet<String> selectedCodes = new LinkedHashSet<>();
        if (top == 0) {
            selectedCodes.add("TOPIC_PROJECT_MANAGEMENT");
        } else {
            scores.entrySet().stream()
                    .filter(entry -> entry.getValue() == top || (entry.getValue() >= 3 && entry.getValue() >= top - 2))
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(3)
                    .forEach(entry -> selectedCodes.add(entry.getKey()));
        }

        Set<Category> categories = new LinkedHashSet<>();
        selectedCodes.forEach(code -> categoryRepository.findByCodeIgnoreCase(code)
                .filter(Category::isActive)
                .ifPresent(categories::add));
        return categories;
    }

    private String normalize(String value) {
        String raw = value == null ? "" : value.toLowerCase(Locale.ROOT);
        return " " + raw.replaceAll("[^a-z0-9]+", " ").replaceAll("\\s+", " ").trim() + " ";
    }

    private int weight(String keyword) {
        String trimmed = keyword.trim();
        if (trimmed.contains(" ")) return 3;
        return trimmed.length() >= 8 ? 2 : 1;
    }

    private int occurrences(String text, String needle) {
        String normalizedKeyword = needle.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ").replaceAll("\\s+", " ").trim();
        String normalizedNeedle = " " + normalizedKeyword + " ";
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(normalizedNeedle, index)) >= 0) {
            count++;
            index += normalizedNeedle.length();
        }
        return count;
    }
}
