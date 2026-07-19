package com.example.pmp.question;

import com.example.pmp.category.Category;
import com.example.pmp.category.CategoryRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Builds the Chart category from two sources:
 * 1. Existing imported questions whose text/options/tags mention a PMP chart or diagram.
 * 2. A small idempotent visual question bank, so a fresh database always has chart practice.
 */
@Component
@Order(200)
public class ChartQuestionSeeder implements ApplicationRunner {
    private static final String CHART_CATEGORY_CODE = "TOPIC_CHART";
    private static final String EXTERNAL_ID_PREFIX = "CHART-GUIDE-";

    private final CategoryRepository categoryRepository;
    private final QuestionRepository questionRepository;

    public ChartQuestionSeeder(CategoryRepository categoryRepository, QuestionRepository questionRepository) {
        this.categoryRepository = categoryRepository;
        this.questionRepository = questionRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Category chartCategory = categoryRepository.findByCodeIgnoreCase(CHART_CATEGORY_CODE)
                .orElseThrow(() -> new IllegalStateException("Chart category was not initialized"));

        classifyExistingQuestions(chartCategory);
        upsertVisualQuestionBank(chartCategory);
    }

    private void classifyExistingQuestions(Category chartCategory) {
        List<Question> changed = new ArrayList<>();
        for (Question question : questionRepository.findAll()) {
            if (question.getExternalId() != null && question.getExternalId().startsWith(EXTERNAL_ID_PREFIX)) {
                continue;
            }
            String searchable = searchableText(question);
            String asset = chartAsset(searchable);
            if (asset == null) {
                continue;
            }
            boolean modified = question.getCategories().add(chartCategory);
            if (isBlank(question.getImageUrl())) {
                question.setImageUrl(asset);
                modified = true;
            }
            if (modified) {
                changed.add(question);
            }
        }
        if (!changed.isEmpty()) {
            questionRepository.saveAll(changed);
        }
    }

    private void upsertVisualQuestionBank(Category chartCategory) {
        List<SeedQuestion> seeds = visualQuestionBank();
        for (int i = 0; i < seeds.size(); i++) {
            SeedQuestion seed = seeds.get(i);
            String externalId = EXTERNAL_ID_PREFIX + String.format(Locale.ROOT, "%03d", i + 1);
            Question question = questionRepository.findByExternalId(externalId).orElseGet(Question::new);

            question.setExternalId(externalId);
            question.setExamName("PMP Chart Practice - " + String.format(Locale.ROOT, "%04d", i + 1));
            question.setQuestionType(QuestionType.MCQ);
            question.setQuestionText(seed.question());
            question.setImageUrl(seed.imageUrl());
            question.setPmaExplanation(seed.explanation());
            question.setAiExplanation(null);
            question.setFinalExplanation(seed.explanation());
            question.setFinalExplanationSource(FinalExplanationSource.MANUAL);
            question.setExplanationReviewStatus(ExplanationReviewStatus.REVIEWED);
            question.setExplanationReviewNotes("Curated visual chart question bank.");
            question.setExplanationReviewedAt(Instant.now());
            question.setExplanationStatus(ExplanationStatus.MANUAL);
            question.setExplanationType("manual");
            question.setExplanationPromptVersion(1);
            question.setNumberOfAnswers(1);
            question.setSource(QuestionSource.MANUAL);
            question.setDifficulty(seed.difficulty());
            question.setReference(seed.reference());
            question.setTags("chart,diagram,visual-tool," + seed.slug());

            List<QuestionOption> options = new ArrayList<>();
            for (int optionIndex = 0; optionIndex < seed.options().size(); optionIndex++) {
                QuestionOption option = new QuestionOption();
                option.setOptionKey(String.valueOf((char) ('A' + optionIndex)));
                option.setOptionText(seed.options().get(optionIndex));
                option.setDisplayOrder(optionIndex);
                option.setCorrect(optionIndex == seed.correctIndex());
                options.add(option);
            }
            question.replaceOptions(options);

            question.getCategories().clear();
            question.getCategories().add(chartCategory);
            question.getCategories().addAll(resolveCategories(seed.additionalCategoryCodes()));
            questionRepository.save(question);
        }
    }

    private Set<Category> resolveCategories(Set<String> codes) {
        Set<Category> categories = new LinkedHashSet<>();
        for (String code : codes) {
            categoryRepository.findByCodeIgnoreCase(code).filter(Category::isActive).ifPresent(categories::add);
        }
        return categories;
    }

    private String searchableText(Question question) {
        StringBuilder text = new StringBuilder();
        append(text, question.getQuestionText());
        append(text, question.getTags());
        append(text, question.getReference());
        append(text, question.getImageUrl());
        for (QuestionOption option : question.getOptions()) {
            append(text, option.getOptionText());
        }
        return text.toString().toLowerCase(Locale.ROOT);
    }

    private void append(StringBuilder target, String value) {
        if (!isBlank(value)) target.append(' ').append(value);
    }

    private String chartAsset(String text) {
        if (containsAny(text, "pareto")) return asset("pareto");
        if (containsAny(text, "histogram")) return asset("histogram");
        if (containsAny(text, "control chart", "control limits", "special cause")) return asset("control-chart");
        if (containsAny(text, "scatter diagram", "scatter plot", "correlation")) return asset("scatter");
        if (containsAny(text, "fishbone", "ishikawa", "cause and effect diagram")) return asset("fishbone");
        if (containsAny(text, "burndown")) return asset("burndown");
        if (containsAny(text, "burnup")) return asset("burnup");
        if (containsAny(text, "cumulative flow")) return asset("cumulative-flow");
        if (containsAny(text, "velocity chart", "team velocity")) return asset("velocity");
        if (containsAny(text, "gantt")) return asset("gantt");
        if (containsAny(text, "milestone chart")) return asset("milestone");
        if (containsAny(text, "network diagram", "precedence diagram", "critical path")) return asset("network");
        if (containsAny(text, "s-curve", "s curve")) return asset("s-curve");
        if (containsAny(text, "tornado diagram", "sensitivity analysis")) return asset("tornado");
        if (containsAny(text, "decision tree", "expected monetary value", " emv ")) return asset("decision-tree");
        if (containsAny(text, "probability impact matrix", "probability and impact matrix")) return asset("probability-impact");
        if (containsAny(text, "power interest grid", "power-interest grid")) return asset("power-interest");
        if (containsAny(text, "raci matrix", "responsible accountable consulted informed")) return asset("raci");
        if (containsAny(text, "task board", "kanban board")) return asset("kanban");
        if (containsAny(text, "flowchart", "flow chart", "process map")) return asset("flowchart");
        if (containsAny(text, "chart", "diagram")) return asset("master-reference");
        return null;
    }

    private boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) return true;
        }
        return false;
    }

    private String asset(String slug) {
        return "/chart-guides/" + slug + ".svg";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private List<SeedQuestion> visualQuestionBank() {
        String toolsRef = "PMBOK Guide 8th Edition, Section 5 Tools and Techniques";
        return List.of(
                q("pareto", "A quality team wants to identify the small number of defect causes responsible for most defects. Which chart should it use?",
                        List.of("Histogram", "Pareto chart", "Scatter diagram", "Control chart"), 1,
                        "A Pareto chart ranks causes from most frequent to least frequent and commonly adds a cumulative line. It supports the 80/20 principle so the team can focus improvement on the vital few causes.", Difficulty.EASY, toolsRef, "TOPIC_QUALITY"),
                q("histogram", "The project manager wants to see the frequency distribution and spread of cycle-time measurements. Which chart is most appropriate?",
                        List.of("Histogram", "Fishbone diagram", "Milestone chart", "RACI matrix"), 0,
                        "A histogram groups numerical observations into ranges and shows their frequencies. It helps reveal center, spread, skewness, and unusual patterns in process data.", Difficulty.EASY, toolsRef, "TOPIC_QUALITY"),
                q("control-chart", "A process measurement is plotted over time with a center line and upper and lower control limits. What is the primary purpose of this chart?",
                        List.of("Display the project critical path", "Determine whether the process is stable", "Assign stakeholder responsibilities", "Rank risks by exposure"), 1,
                        "A control chart distinguishes common-cause variation from special-cause variation and helps determine whether a process is stable and predictable. Control limits are not the same as customer specification limits.", Difficulty.MEDIUM, toolsRef, "TOPIC_QUALITY"),
                q("scatter", "A team plots training hours against defect counts to explore whether the variables move together. Which tool is being used?",
                        List.of("Scatter diagram", "Pareto chart", "Gantt chart", "Decision tree"), 0,
                        "A scatter diagram plots paired numerical observations to reveal the direction and strength of a possible relationship. Correlation alone does not prove causation.", Difficulty.EASY, toolsRef, "TOPIC_QUALITY"),
                q("fishbone", "During root cause analysis, the team groups possible causes under people, process, equipment, materials, measurement, and environment. Which diagram is this?",
                        List.of("Cause-and-effect diagram", "Burnup chart", "Tornado diagram", "Power-interest grid"), 0,
                        "The cause-and-effect diagram, also called the Ishikawa or fishbone diagram, organizes potential causes around a defined problem before the team validates the actual root cause with evidence.", Difficulty.EASY, toolsRef, "TOPIC_QUALITY"),
                q("gantt", "Which chart shows project activities as horizontal bars positioned against a calendar timeline?",
                        List.of("Gantt chart", "Histogram", "Probability-impact matrix", "Scatter diagram"), 0,
                        "A Gantt chart displays activity timing, duration, overlap, and often progress against a calendar. It is useful for communicating the schedule but does not replace network analysis.", Difficulty.EASY, toolsRef, "TOPIC_SCHEDULE"),
                q("milestone", "A sponsor needs a high-level view of key approval dates and major delivery events without detailed activity durations. Which chart is best?",
                        List.of("Milestone chart", "Control chart", "Cumulative flow diagram", "Fishbone diagram"), 0,
                        "A milestone chart highlights significant zero-duration events or decision points. It is concise and appropriate for executive-level schedule communication.", Difficulty.EASY, toolsRef, "TOPIC_SCHEDULE"),
                q("network", "The project manager needs to analyze dependencies, calculate float, and identify the critical path. Which diagram should be used?",
                        List.of("Project schedule network diagram", "Pareto chart", "Power-interest grid", "Histogram"), 0,
                        "A project schedule network diagram represents logical relationships among activities. It supports forward and backward passes, float calculation, and critical-path identification.", Difficulty.MEDIUM, toolsRef, "TOPIC_SCHEDULE"),
                q("burndown", "During an iteration, the team wants to compare remaining work with the time left. Which chart should it inspect?",
                        List.of("Burndown chart", "Burnup chart", "S-curve", "Tornado diagram"), 0,
                        "A burndown chart shows remaining work decreasing over time. It helps the team see whether the iteration is likely to finish the committed work within the timebox.", Difficulty.EASY, toolsRef, "TOPIC_AGILE_HYBRID"),
                q("burnup", "The product owner wants a chart that shows completed work and also makes scope growth visible. Which chart is most useful?",
                        List.of("Burnup chart", "Control chart", "Histogram", "RACI matrix"), 0,
                        "A burnup chart shows completed work rising toward a total-scope line. When the scope line changes, the chart makes scope growth or reduction visible.", Difficulty.MEDIUM, toolsRef, "TOPIC_AGILE_HYBRID"),
                q("cumulative-flow", "Which chart helps an agile team detect a workflow bottleneck when the band for work in progress becomes wider over time?",
                        List.of("Cumulative flow diagram", "Milestone chart", "Scatter diagram", "Decision tree"), 0,
                        "A cumulative flow diagram shows work items in each workflow state over time. A widening in-progress band indicates accumulating WIP and a likely bottleneck.", Difficulty.MEDIUM, toolsRef, "TOPIC_AGILE_HYBRID"),
                q("velocity", "What is the appropriate use of a team velocity chart?",
                        List.of("Compare individual developer productivity", "Forecast how much work the same team may complete", "Set a contractual performance penalty", "Determine process control limits"), 1,
                        "Velocity is a planning aid based on the amount of work completed by the same team in prior iterations. It should not be used to compare teams or evaluate individuals.", Difficulty.MEDIUM, toolsRef, "TOPIC_AGILE_HYBRID"),
                q("s-curve", "A report plots cumulative planned value, earned value, and actual cost over the project timeline. What type of visual is commonly used?",
                        List.of("S-curve", "Fishbone diagram", "Stakeholder cube", "Check sheet"), 0,
                        "An S-curve displays cumulative values over time. Comparing planned value, earned value, and actual cost supports performance and trend analysis.", Difficulty.MEDIUM, toolsRef, "TOPIC_COST_FINANCE"),
                q("tornado", "A quantitative risk analysis shows which uncertain input has the greatest effect on project outcome by using ranked horizontal bars. Which diagram is this?",
                        List.of("Tornado diagram", "Gantt chart", "Control chart", "Kanban board"), 0,
                        "A tornado diagram presents sensitivity analysis. The longest bar represents the variable with the greatest influence on the selected outcome.", Difficulty.MEDIUM, toolsRef, "TOPIC_RISK"),
                q("probability-impact", "The team needs to prioritize identified risks by combining each risk's probability and impact. Which tool should it use?",
                        List.of("Probability-impact matrix", "Histogram", "RACI matrix", "Burndown chart"), 0,
                        "A probability-impact matrix maps probability and impact to an exposure or priority level. It supports qualitative risk analysis and response prioritization.", Difficulty.EASY, toolsRef, "TOPIC_RISK"),
                q("decision-tree", "Two project alternatives have uncertain outcomes with different probabilities and payoffs. Which visual tool supports comparison using expected monetary value?",
                        List.of("Decision tree", "Milestone chart", "Scatter diagram", "Cumulative flow diagram"), 0,
                        "A decision tree models decision points, chance events, probabilities, and monetary outcomes. Expected monetary value can be calculated along each branch to compare alternatives.", Difficulty.MEDIUM, toolsRef, "TOPIC_RISK"),
                q("power-interest", "The project manager classifies stakeholders into manage closely, keep satisfied, keep informed, and monitor groups. Which matrix is being used?",
                        List.of("Power-interest grid", "Probability-impact matrix", "RACI matrix", "Requirements traceability matrix"), 0,
                        "The power-interest grid classifies stakeholders by their authority or influence and their level of concern about the project, guiding engagement effort.", Difficulty.EASY, toolsRef, "TOPIC_STAKEHOLDER_COMMUNICATION"),
                q("raci", "Which matrix clarifies who is Responsible, Accountable, Consulted, and Informed for project activities?",
                        List.of("RACI matrix", "Risk matrix", "Scatter matrix", "Decision matrix"), 0,
                        "A RACI matrix maps activities or deliverables to stakeholder roles. It helps avoid role ambiguity and should normally identify one accountable owner for each activity.", Difficulty.EASY, toolsRef, "TOPIC_RESOURCE_TEAM"),
                q("kanban", "Which visual board organizes work items by workflow state and can display work-in-progress limits?",
                        List.of("Kanban or task board", "Tornado diagram", "Histogram", "Milestone chart"), 0,
                        "A Kanban or task board visualizes workflow, such as To Do, In Progress, and Done. WIP limits support flow and expose bottlenecks.", Difficulty.EASY, toolsRef, "TOPIC_AGILE_HYBRID"),
                q("flowchart", "A quality team needs to understand decision points, rework loops, and handoffs in an existing process. Which visual tool should it create first?",
                        List.of("Flowchart or process map", "Velocity chart", "Power-interest grid", "Tornado diagram"), 0,
                        "A flowchart or process map represents the sequence of steps and decisions. It helps the team see handoffs, delays, rework loops, and improvement opportunities.", Difficulty.EASY, toolsRef, "TOPIC_QUALITY")
        );
    }

    private SeedQuestion q(String slug, String question, List<String> options, int correctIndex,
                           String explanation, Difficulty difficulty, String reference, String... categories) {
        return new SeedQuestion(slug, question, asset(slug), options, correctIndex, explanation,
                difficulty, reference, new LinkedHashSet<>(List.of(categories)));
    }

    private record SeedQuestion(
            String slug,
            String question,
            String imageUrl,
            List<String> options,
            int correctIndex,
            String explanation,
            Difficulty difficulty,
            String reference,
            Set<String> additionalCategoryCodes
    ) {}
}
