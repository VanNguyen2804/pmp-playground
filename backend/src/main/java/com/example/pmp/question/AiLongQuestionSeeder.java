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
 * Adds practical PMBOK 8 AI questions and two shared long-case question sets.
 * The external IDs are stable, so rerunning the application updates the curated
 * questions instead of inserting duplicates or losing answer history.
 */
@Component
@Order(220)
public class AiLongQuestionSeeder implements ApplicationRunner {
    private static final String AI_CATEGORY_CODE = "TOPIC_AI";
    private static final String LONG_CATEGORY_CODE = "TOPIC_LONG_QUESTION";
    private static final String AI_PREFIX = "AI-PRACTICE-";
    private static final String PMBOK8_AI_PREFIX = "AI-PMBOK8-";
    private static final String LONG_PREFIX = "LONG-CASE-";
    private static final int LONG_QUESTION_MINIMUM_CHARACTERS = 700;

    private final CategoryRepository categoryRepository;
    private final QuestionRepository questionRepository;

    public AiLongQuestionSeeder(CategoryRepository categoryRepository, QuestionRepository questionRepository) {
        this.categoryRepository = categoryRepository;
        this.questionRepository = questionRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Category aiCategory = requiredCategory(AI_CATEGORY_CODE);
        Category longCategory = requiredCategory(LONG_CATEGORY_CODE);

        classifyExistingQuestions(aiCategory, longCategory);
        upsertQuestions(standaloneAiQuestions(), AI_PREFIX, "PMP AI Practice", aiCategory, null);
        upsertQuestions(additionalPmbok8AiQuestions(), PMBOK8_AI_PREFIX, "PMBOK 8 AI Practice", aiCategory, null);
        upsertQuestions(longAiCaseQuestions(), LONG_PREFIX + "AI-", "PMP Long Case AI", aiCategory, longCategory);
        upsertQuestions(longGeneralCaseQuestions(), LONG_PREFIX + "GENERAL-", "PMP Long Case General", null, longCategory);
    }

    private Category requiredCategory(String code) {
        return categoryRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new IllegalStateException("Category was not initialized: " + code));
    }

    private void classifyExistingQuestions(Category aiCategory, Category longCategory) {
        List<Question> changed = new ArrayList<>();
        for (Question question : questionRepository.findAll()) {
            if (isCurated(question)) {
                continue;
            }
            boolean modified = false;
            String text = searchableText(question);
            if (isAiQuestion(text)) {
                modified |= question.getCategories().add(aiCategory);
            }
            if (question.getQuestionText() != null
                    && question.getQuestionText().length() >= LONG_QUESTION_MINIMUM_CHARACTERS) {
                modified |= question.getCategories().add(longCategory);
            }
            if (modified) {
                changed.add(question);
            }
        }
        if (!changed.isEmpty()) {
            questionRepository.saveAll(changed);
        }
    }

    private boolean isCurated(Question question) {
        String id = question.getExternalId();
        return id != null && (id.startsWith(AI_PREFIX)
                || id.startsWith(PMBOK8_AI_PREFIX)
                || id.startsWith(LONG_PREFIX));
    }

    private String searchableText(Question question) {
        StringBuilder value = new StringBuilder();
        append(value, question.getQuestionText());
        append(value, question.getTags());
        append(value, question.getReference());
        for (QuestionOption option : question.getOptions()) {
            append(value, option.getOptionText());
        }
        return value.toString().toLowerCase(Locale.ROOT);
    }

    private void append(StringBuilder builder, String value) {
        if (value != null && !value.isBlank()) {
            builder.append(' ').append(value);
        }
    }

    private boolean isAiQuestion(String text) {
        return containsAny(text,
                "artificial intelligence",
                "generative ai",
                "genai",
                "ai-enabled",
                "ai enabled",
                "ai tool",
                "ai system",
                "ai assistant",
                "ai model",
                "machine learning",
                "predictive analytics",
                "chatbot");
    }

    private boolean containsAny(String text, String... phrases) {
        for (String phrase : phrases) {
            if (text.contains(phrase)) {
                return true;
            }
        }
        return false;
    }

    private void upsertQuestions(
            List<SeedQuestion> seeds,
            String prefix,
            String examName,
            Category mandatoryCategory,
            Category secondMandatoryCategory
    ) {
        for (int i = 0; i < seeds.size(); i++) {
            SeedQuestion seed = seeds.get(i);
            String number = String.format(Locale.ROOT, "%04d", i + 1);
            String externalId = prefix + number;
            Question question = questionRepository.findByExternalId(externalId).orElseGet(Question::new);

            question.setExternalId(externalId);
            question.setExamName(examName + " - " + number);
            question.setQuestionType(QuestionType.MCQ);
            question.setQuestionText(seed.question());
            question.setImageUrl(null);
            question.setPmaExplanation(null);
            question.setAiExplanation(null);
            question.setFinalExplanation(seed.explanation());
            question.setFinalExplanationSource(FinalExplanationSource.MANUAL);
            question.setExplanationReviewStatus(ExplanationReviewStatus.REVIEWED);
            question.setExplanationReviewNotes("Curated PMBOK 8 scenario practice question.");
            question.setExplanationReviewedAt(Instant.now());
            question.setExplanationStatus(ExplanationStatus.MANUAL);
            question.setExplanationType("manual");
            question.setExplanationPromptVersion(1);
            question.setNumberOfAnswers(1);
            question.setSource(QuestionSource.MANUAL);
            question.setDifficulty(seed.difficulty());
            question.setReference(seed.reference());
            question.setTags(seed.tags());

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
            if (mandatoryCategory != null) {
                question.getCategories().add(mandatoryCategory);
            }
            if (secondMandatoryCategory != null) {
                question.getCategories().add(secondMandatoryCategory);
            }
            question.getCategories().addAll(resolveCategories(seed.categoryCodes()));
            questionRepository.save(question);
        }
    }

    private Set<Category> resolveCategories(Set<String> codes) {
        Set<Category> categories = new LinkedHashSet<>();
        for (String code : codes) {
            categoryRepository.findByCodeIgnoreCase(code)
                    .filter(Category::isActive)
                    .ifPresent(categories::add);
        }
        return categories;
    }

    private List<SeedQuestion> standaloneAiQuestions() {
        String responsibleAi = "PMBOK Guide 8th Edition, Appendix X3: Artificial Intelligence, Responsible Use and Ethical Concerns";
        String aiUseCases = "PMBOK Guide 8th Edition, Appendix X3, Table X3-1: Primary AI Use Cases in Project Management";
        String pmaAi = "PMA Handout, The AI Revolution in Project Management: PRIME, 3 A's, and Responsible Use";

        return List.of(
                q("A project manager asks a generative AI tool to draft a risk register from historical project data. The draft contains plausible risks and proposed owners. What should the project manager do next?",
                        List.of(
                                "Publish the risk register because the AI used historical data",
                                "Validate and refine the draft with the project team and relevant subject matter experts",
                                "Ask the AI to approve its own output",
                                "Replace the normal risk management process with the AI draft"),
                        1,
                        "This is an assistance use case. AI can accelerate creation of a draft, but the project team and subject matter experts must validate completeness, relevance, ownership, and responses before the artifact is used.",
                        Difficulty.EASY, aiUseCases, "ai,assistance,risk-register,human-validation", "TOPIC_RISK"),

                q("A PMO configures an AI service to compile approved project data into a standard weekly status report and distribute it to a predefined audience. The format and rules are stable. Which AI adoption strategy best describes this use?",
                        List.of("Automation", "Assistance", "Augmentation", "Replacement of accountability"),
                        0,
                        "Routine report generation with stable rules and little human intervention is automation. The PMO should still monitor the process and ensure the inputs and distribution rules remain accurate.",
                        Difficulty.EASY, aiUseCases, "ai,automation,status-report", "TOPIC_GOVERNANCE_CHANGE"),

                q("Senior leaders use AI to compare portfolio scenarios involving strategic alignment, risk exposure, resource limits, and expected return on investment. Leaders iteratively challenge the recommendations before selecting a scenario. Which strategy is being used?",
                        List.of("Automation", "Assistance", "Augmentation", "Data deletion"),
                        2,
                        "Portfolio balancing is a strategic and complex task. Using AI as a thinking partner while leaders explore and refine alternatives is augmentation; the leaders retain judgment and decision accountability.",
                        Difficulty.MEDIUM, aiUseCases, "ai,augmentation,portfolio,decision-making", "TOPIC_GOVERNANCE_CHANGE", "TOPIC_COST_FINANCE"),

                q("A team member proposes uploading a customer file containing names, medical information, and contact details to a public generative AI service to summarize stakeholder feedback. What should the project manager do first?",
                        List.of(
                                "Upload the file because the output will only be used internally",
                                "Remove a few names and proceed immediately",
                                "Stop the upload and verify privacy, security, consent, data-handling policy, and use of an approved tool",
                                "Ask the AI provider to accept accountability for the project"),
                        2,
                        "Sensitive and regulated information requires privacy and security controls. The project manager should prevent unapproved disclosure and follow organizational policy, consent requirements, applicable law, and approved-tool guidance before processing the data.",
                        Difficulty.MEDIUM, responsibleAi, "ai,privacy,security,sensitive-data", "TOPIC_BUSINESS_COMPLIANCE", "TOPIC_STAKEHOLDER_COMMUNICATION"),

                q("An AI model trained on historical staffing decisions repeatedly recommends fewer leadership opportunities for one demographic group. What is the most appropriate response?",
                        List.of(
                                "Use the recommendations because historical data is objective",
                                "Hide the demographic field but keep the same model without testing",
                                "Test for bias, investigate the data and model, diversify the data and review team, and require human oversight",
                                "Allow the model to make decisions until a complaint is received"),
                        2,
                        "Historical data can encode past bias. Responsible use requires testing for bias, examining data and algorithms, using diverse data and perspectives, and maintaining human review before decisions affect people.",
                        Difficulty.MEDIUM, responsibleAi, "ai,bias,fairness,human-oversight", "TOPIC_RESOURCE_TEAM", "TOPIC_BUSINESS_COMPLIANCE"),

                q("A generative AI assistant states that a newly adopted regulation does not apply to the project, but it provides no authoritative source. What should the project manager do?",
                        List.of(
                                "Accept the answer because the assistant was trained on large data sets",
                                "Verify the claim with current authoritative legal or compliance sources and qualified experts",
                                "Remove the regulation from the risk register",
                                "Update the project management plan based only on the AI response"),
                        1,
                        "AI output may be incorrect, outdated, or unsupported. Regulatory applicability should be validated using authoritative current sources and qualified compliance or legal expertise before project decisions are made.",
                        Difficulty.EASY, responsibleAi, "ai,reliability,compliance,verification", "TOPIC_BUSINESS_COMPLIANCE", "TOPIC_RISK"),

                q("An AI tool scores vendor proposals and recommends a supplier. The procurement manager asks who is accountable if the recommendation causes a major loss. What is the best response?",
                        List.of(
                                "The AI vendor is automatically accountable for the procurement decision",
                                "No person is accountable because the decision was data-driven",
                                "A clearly designated human decision-maker remains accountable and must validate the criteria, data, and recommendation",
                                "The lowest-cost supplier must be selected regardless of the AI score"),
                        2,
                        "AI can assist analysis, but human accountability must be clearly defined. The responsible decision-maker should validate the source-selection criteria, data quality, assumptions, conflicts, and recommendation.",
                        Difficulty.MEDIUM, responsibleAi, "ai,accountability,procurement,vendor-selection", "TOPIC_PROCUREMENT", "TOPIC_GOVERNANCE_CHANGE"),

                q("A project manager submits the prompt, 'Plan my project,' and receives a generic response. What is the best next action?",
                        List.of(
                                "Use the generic response without modification",
                                "Provide context, the task, constraints, relevant data, desired format, and a request for sources or validation",
                                "Repeat exactly the same prompt until the answer changes",
                                "Ask the AI to make all decisions independently"),
                        1,
                        "A useful prompt should provide sufficient context and a clear task, constraints, examples or desired format, and a reliability check. The user can then interact and refine the output rather than expecting a perfect first response.",
                        Difficulty.EASY, pmaAi, "ai,prompt,prime,context,reliability"),

                q("Predictive analytics identifies a possible schedule bottleneck even though the current critical path report shows no delay. What should the project manager do first?",
                        List.of(
                                "Ignore the signal because the current schedule report is still green",
                                "Immediately rebaseline the schedule",
                                "Validate the early-warning signal, examine the assumptions and data, and assess the risk with the team",
                                "Crash all remaining critical activities"),
                        2,
                        "AI pattern recognition can provide an early warning. The project manager should validate the signal and underlying data, assess probability and impact, and then update risks or plans as appropriate rather than acting blindly.",
                        Difficulty.MEDIUM, aiUseCases, "ai,predictive-analytics,early-warning,schedule", "TOPIC_SCHEDULE", "TOPIC_RISK"),

                q("An AI service transcribes a meeting and produces minutes containing decisions and action owners. What should occur before the minutes are distributed as the official record?",
                        List.of(
                                "The AI should sign the minutes",
                                "A responsible person should verify the decisions, owners, dates, confidentiality, and context",
                                "The raw transcript should always be deleted without review",
                                "The minutes should be distributed automatically to every stakeholder"),
                        1,
                        "Automated minutes are useful, but the official record should be checked for accuracy, missing context, confidentiality, decisions, owners, and deadlines before distribution.",
                        Difficulty.EASY, aiUseCases, "ai,automation,meeting-minutes,verification", "TOPIC_STAKEHOLDER_COMMUNICATION"),

                q("A project team deploys an AI chatbot that answers routine questions using approved project information, such as milestone dates and published status. Which use case does this best represent?",
                        List.of("Automation of routine queries", "Augmentation of portfolio strategy", "Replacement of the sponsor", "Transfer of project accountability to the chatbot"),
                        0,
                        "A chatbot handling routine, approved, repeatable queries is an automation use case. Access control, data freshness, escalation paths, and monitoring are still required.",
                        Difficulty.EASY, aiUseCases, "ai,chatbot,automation,project-status", "TOPIC_STAKEHOLDER_COMMUNICATION"),

                q("A team uses generative AI to create text and images for a confidential product concept. Before publishing the content externally, what should the project manager ensure?",
                        List.of(
                                "AI-generated content has no legal or ownership concerns",
                                "The content is reviewed for intellectual property, confidentiality, licensing, accuracy, organizational policy, and required human approval",
                                "The prompt is deleted, which resolves every concern",
                                "The AI model is named as the project sponsor"),
                        1,
                        "AI-generated content can create copyright, licensing, confidentiality, and ownership questions. The team should follow policy and legal guidance, verify accuracy, protect confidential information, and obtain required approval.",
                        Difficulty.MEDIUM, responsibleAi, "ai,copyright,intellectual-property,confidentiality", "TOPIC_BUSINESS_COMPLIANCE")
        );
    }

    private List<SeedQuestion> additionalPmbok8AiQuestions() {
        String useCases = "PMBOK Guide 8th Edition, Appendix X3, Table X3-1: Primary AI Use Cases in Project Management";
        String responsible = "PMBOK Guide 8th Edition, Appendix X3.3: Responsible Use and Ethical Concerns";
        String adoption = "PMBOK Guide 8th Edition, Appendix X3.1: Strategies for AI Adoption";

        return List.of(
                q("A PMO uses AI to continuously compare actual progress with approved baselines and alert the project manager when deviations cross predefined thresholds. Which use of AI from PMBOK 8 is being applied?",
                        List.of(
                                "Real-time monitoring through automation",
                                "Portfolio balancing through augmentation",
                                "Risk register creation without human review",
                                "Replacement of the project governance structure"),
                        0,
                        "PMBOK 8 identifies real-time monitoring as an automation use case. AI can continuously compare progress with baselines and generate alerts, while the project manager remains responsible for interpreting the alert and selecting an action.",
                        Difficulty.EASY, useCases, "ai,pmbok8,automation,real-time-monitoring", "TOPIC_GOVERNANCE_CHANGE", "TOPIC_PROJECT_MANAGEMENT"),

                q("Critical path analysis and the burndown chart appear normal, but an AI tool detects a pattern that preceded delays on similar projects. What should the project manager do next?",
                        List.of(
                                "Ignore the signal because the standard charts are green",
                                "Validate the signal, investigate the assumptions and leading indicators, and assess whether preventive action is warranted",
                                "Immediately replace the schedule baseline",
                                "Allow the AI tool to authorize overtime"),
                        1,
                        "PMBOK 8 describes early warning signaling as an assistance use case. The signal should trigger human investigation and validation; it is not automatic evidence that a baseline change or corrective action is already approved.",
                        Difficulty.MEDIUM, useCases, "ai,pmbok8,assistance,early-warning,schedule", "TOPIC_SCHEDULE", "TOPIC_RISK"),

                q("A project team asks AI to explore trade-offs among scope, schedule, cost, and value before proposing a new baseline. How should the AI output be used?",
                        List.of(
                                "As an augmentation input to human trade-off analysis and governance approval",
                                "As an automatically approved baseline",
                                "As a replacement for stakeholder consultation",
                                "As evidence that change control is unnecessary"),
                        0,
                        "Baseline optimization is an augmentation use case. AI can expand trade-off analysis, but accountable stakeholders must evaluate value, assumptions, constraints, and risks and follow the established change-governance process.",
                        Difficulty.MEDIUM, useCases, "ai,pmbok8,augmentation,baseline-optimization", "TOPIC_GOVERNANCE_CHANGE", "TOPIC_COST_FINANCE", "TOPIC_SCOPE_REQUIREMENTS", "TOPIC_SCHEDULE"),

                q("An internal AI chatbot answers routine questions about approved project status, milestones, and task ownership. What is the most important project management control?",
                        List.of(
                                "Give the chatbot access to every project document",
                                "Use approved data sources, access controls, current information, and escalation to a human for uncertain or sensitive requests",
                                "Let the chatbot create commitments on behalf of the sponsor",
                                "Remove normal communication channels"),
                        1,
                        "AI chatbots can automate routine queries, but the implementation needs trusted sources, authorization boundaries, privacy controls, currency checks, and a human escalation path.",
                        Difficulty.MEDIUM, useCases, "ai,pmbok8,automation,chatbot,privacy", "TOPIC_STAKEHOLDER_COMMUNICATION", "TOPIC_GOVERNANCE_CHANGE"),

                q("An AI assistant transcribes a steering committee meeting and drafts the decisions and action items. Before the minutes become the official record, what should occur?",
                        List.of(
                                "An accountable person verifies accuracy, context, owners, sensitive content, and the approved audience",
                                "The transcript is permanently published without review",
                                "The AI tool asks itself whether the minutes are correct",
                                "Every informal comment is converted into a project decision"),
                        0,
                        "Automated meeting minutes are a PMBOK 8 automation use case. Human verification is still needed because transcription can lose context, misstate decisions, expose sensitive data, or assign the wrong owner.",
                        Difficulty.EASY, useCases, "ai,pmbok8,automation,meeting-minutes,human-review", "TOPIC_STAKEHOLDER_COMMUNICATION"),

                q("A project manager uses AI to analyze historical project data and industry benchmarks to identify possible threats and estimate their probability and impact. Which statement is most accurate?",
                        List.of(
                                "This is an augmentation use case that enriches risk identification and assessment but still requires expert review",
                                "The AI output becomes the approved risk register automatically",
                                "Only risks found by AI should be retained",
                                "The project manager no longer needs a risk owner"),
                        0,
                        "PMBOK 8 lists AI-supported risk identification and assessment as augmentation. The output broadens professional analysis; the team still validates relevance, probability, impact, ownership, response, and data limitations.",
                        Difficulty.MEDIUM, useCases, "ai,pmbok8,augmentation,risk-identification", "TOPIC_RISK"),

                q("A company allows AI to trigger a predefined low-impact risk response when an approved threshold is crossed. What is the best governance design?",
                        List.of(
                                "Define rules, authority limits, monitoring, auditability, exception handling, and human escalation",
                                "Permit the AI to take any action it considers useful",
                                "Remove the risk owner because the response is automated",
                                "Hide the automation from affected stakeholders"),
                        0,
                        "Automated mitigation can be suitable for bounded, predefined actions. Governance should define permitted actions, thresholds, owners, logs, monitoring, exception handling, and conditions requiring human decision-making.",
                        Difficulty.HARD, useCases, "ai,pmbok8,automation,risk-mitigation,governance", "TOPIC_RISK", "TOPIC_GOVERNANCE_CHANGE"),

                q("A team member plans to upload customer records and confidential contract details to a free public generative AI tool to prepare a summary. What should the project manager do first?",
                        List.of(
                                "Proceed because summaries are low complexity",
                                "Stop the upload and verify organizational AI policy, data classification, consent, privacy, security, and intellectual-property controls",
                                "Remove customer names only and upload everything else",
                                "Ask the AI provider to delete the data after generation"),
                        1,
                        "PMBOK 8 emphasizes privacy and the way AI providers handle user data. Sensitive or regulated information should not be submitted until approved policy, security, consent, retention, and intellectual-property controls are confirmed.",
                        Difficulty.MEDIUM, responsible, "ai,pmbok8,privacy,confidentiality", "TOPIC_BUSINESS_COMPLIANCE", "TOPIC_PROCUREMENT"),

                q("An AI model consistently recommends lower resource allocations for projects serving a particular geographic group. What is the best response?",
                        List.of(
                                "Accept the recommendation because the model is consistent",
                                "Investigate bias, diversify relevant data, perform focused periodic tests, involve diverse teams, and correct and retest the model",
                                "Exclude that geographic group from future analysis",
                                "Lower the decision threshold for every project"),
                        1,
                        "PMBOK 8 recommends mitigating bias through diverse training data, periodic bias-focused testing, and involvement of different teams. The affected use should be controlled until the weakness is understood and corrected.",
                        Difficulty.MEDIUM, responsible, "ai,pmbok8,bias,fairness", "TOPIC_RESOURCE_TEAM", "TOPIC_BUSINESS_COMPLIANCE", "TOPIC_QUALITY"),

                q("An AI system ranks vendors, and the project team selects the top-ranked vendor. Who is ultimately accountable for the procurement decision?",
                        List.of(
                                "The AI model vendor",
                                "The human decision-maker identified by the organization's governance and procurement authority",
                                "No one, because the ranking was data driven",
                                "The lowest-ranked bidder"),
                        1,
                        "PMBOK 8 states that a human should ultimately be accountable for decisions and that accountability must be clearly defined. AI can contribute analysis but cannot absorb organizational accountability.",
                        Difficulty.EASY, responsible, "ai,pmbok8,accountability,vendor-selection", "TOPIC_PROCUREMENT", "TOPIC_GOVERNANCE_CHANGE"),

                q("A generative AI tool produces different regulatory answers when the same question is phrased differently. What should the project manager do?",
                        List.of(
                                "Select the answer that best supports the current plan",
                                "Treat the output as unreliable until it is validated against authoritative sources and reviewed by appropriate experts",
                                "Average the answers",
                                "Use the longest answer as the official interpretation"),
                        1,
                        "AI-generated information is not infallible. Reliability requires validation and cross-checking with trusted sources, especially for legal, regulatory, safety, or other high-impact decisions.",
                        Difficulty.MEDIUM, responsible, "ai,pmbok8,reliability,regulation,validation", "TOPIC_BUSINESS_COMPLIANCE", "TOPIC_QUALITY"),

                q("Stakeholders ask how an AI recommendation affected the project's resource-allocation decision. Which principle should guide the response?",
                        List.of(
                                "Transparency about relevant data, the role of the model, limitations, and the human decision process",
                                "Refusal to explain because AI models are always confidential",
                                "Disclosure of all personal data used by the system",
                                "A statement that the AI made the final decision"),
                        0,
                        "Responsible AI includes transparency. Affected parties should receive appropriate information about data use, the AI contribution, limitations, and accountable human decision-making without exposing protected information.",
                        Difficulty.MEDIUM, responsible, "ai,pmbok8,transparency,stakeholder", "TOPIC_STAKEHOLDER_COMMUNICATION", "TOPIC_GOVERNANCE_CHANGE"),

                q("An AI scheduling recommendation would reduce duration but weakens a mandatory safety verification. What should the project manager do?",
                        List.of(
                                "Accept the recommendation because schedule value is measurable",
                                "Reject or constrain the recommendation, involve safety and compliance experts, and preserve mandatory controls unless formally changed by authorized governance",
                                "Ask AI to accept the safety risk",
                                "Remove the safety criterion from the definition of done"),
                        1,
                        "AI use must be safe and governed. Mandatory safety and compliance controls cannot be traded away by an optimization tool; experts and authorized decision-makers must evaluate any proposed change.",
                        Difficulty.HARD, responsible, "ai,pmbok8,safety,compliance,schedule", "TOPIC_BUSINESS_COMPLIANCE", "TOPIC_SCHEDULE", "TOPIC_QUALITY"),

                q("A marketing project uses generative AI to draft campaign artwork and text. Before publishing the material, what should the team review?",
                        List.of(
                                "Only whether the content is visually attractive",
                                "Copyright, ownership, training/output restrictions, licenses, attribution requirements, and the amount of human elaboration",
                                "Whether the AI generated the content quickly",
                                "Only the project's cost baseline"),
                        1,
                        "PMBOK 8 identifies copyright and ownership as responsible-AI considerations. The team should verify applicable law, licenses, organizational policy, provider terms, provenance, and required human contribution before use.",
                        Difficulty.MEDIUM, responsible, "ai,pmbok8,copyright,ownership,marketing", "TOPIC_BUSINESS_COMPLIANCE", "TOPIC_GOVERNANCE_CHANGE"),

                q("A project team submits thousands of unnecessary AI prompts for simple calculations that existing tools already perform accurately. Which PMBOK 8 consideration is most relevant?",
                        List.of(
                                "Sustainability and proportional use of computing resources",
                                "Critical path compression",
                                "Stakeholder salience",
                                "Cost-reimbursable contracting"),
                        0,
                        "PMBOK 8 notes the energy and water resources consumed by AI systems. Teams should use AI where it provides sufficient value and avoid unnecessary use when simpler, reliable tools are adequate.",
                        Difficulty.EASY, responsible, "ai,pmbok8,sustainability,resource-use", "TOPIC_BUSINESS_COMPLIANCE"),

                q("A project manager is choosing between a public free AI service and an enterprise paid service for confidential project analysis. What difference should receive particular attention?",
                        List.of(
                                "How each service uses submitted data for model training, retention, privacy, and intellectual-property protection",
                                "The color of the user interface",
                                "The number of emojis produced",
                                "Whether the service can replace project sponsorship"),
                        0,
                        "PMBOK 8 highlights that AI services can differ materially in their handling of user data. Enterprise controls may restrict retraining and improve privacy and intellectual-property protection, but the actual terms and organizational approval must be verified.",
                        Difficulty.EASY, adoption, "ai,pmbok8,privacy,market,enterprise-tool", "TOPIC_PROCUREMENT", "TOPIC_BUSINESS_COMPLIANCE"),

                q("An AI tool predicts project timelines, resource needs, and possible bottlenecks from historical and current project data. The project manager reviews and refines the output before planning. Which adoption level best describes this use?",
                        List.of("Automation", "Assistance", "Augmentation", "Autonomous governance"),
                        1,
                        "Predictive analytics for planning is listed as assistance. AI complements analysis and creates an input that a project professional must review for accuracy, completeness, assumptions, and context.",
                        Difficulty.EASY, useCases, "ai,pmbok8,assistance,predictive-analytics,planning", "TOPIC_SCHEDULE", "TOPIC_RESOURCE_TEAM"),

                q("An AI-enhanced collaboration platform groups ideas from a large workshop and highlights recurring themes. What should the facilitator do next?",
                        List.of(
                                "Treat the highest-ranked theme as an approved decision",
                                "Use the organized output to support inclusive discussion, validate context, and let authorized participants make the decision",
                                "Delete ideas that the model ranks low",
                                "End the workshop because AI has completed stakeholder engagement"),
                        1,
                        "AI collaborative platforms can assist by organizing contributions in real time. The facilitator should ensure contributions are represented fairly, validate context, and preserve human participation and decision authority.",
                        Difficulty.MEDIUM, useCases, "ai,pmbok8,assistance,collaboration,brainstorming", "TOPIC_STAKEHOLDER_COMMUNICATION", "TOPIC_RESOURCE_TEAM")
        );
    }

    private List<SeedQuestion> longAiCaseQuestions() {
        String caseContext = """
                CASE 1 — AI-enabled public health platform:
                A government agency is delivering a multilingual public health platform across six regions. The program uses a hybrid approach: infrastructure, security certification, and regulatory approvals follow predictive plans, while citizen-facing features are delivered iteratively. A technology vendor proposes several AI capabilities: summarizing feedback submitted by citizens, forecasting schedule and resource bottlenecks, drafting risks and responses, generating meeting minutes, prioritizing service requests, and analyzing stakeholder sentiment. Some source data includes health information, language, location, age, and disability status. The sponsor wants rapid adoption because the public launch date is politically visible. Regional laws differ, historical data is incomplete, and early tests show that recommendations are less accurate for two minority-language groups. The governance board states that AI may support work, but named humans remain accountable for decisions and all sensitive data must use approved environments.
                """.strip();
        String ref = "PMBOK Guide 8th Edition, Appendix X3: Artificial Intelligence; PMA Handout, The AI Revolution in Project Management";

        return List.of(
                q(caseContext + "\n\nBefore the vendor processes citizen feedback containing health and location data, what should the project manager do first?",
                        List.of(
                                "Approve the upload because the launch is politically visible",
                                "Verify applicable privacy requirements, consent, data minimization, security controls, and the approved processing environment",
                                "Ask the AI to remove any data it considers sensitive",
                                "Transfer accountability to the technology vendor"),
                        1,
                        "The immediate decision trigger is sensitive regulated data. The project manager should confirm legal and organizational requirements, consent, minimization, access, security, retention, and approved tooling before processing begins.",
                        Difficulty.HARD, ref, "ai,long-question,case-1,privacy", "TOPIC_BUSINESS_COMPLIANCE", "TOPIC_STAKEHOLDER_COMMUNICATION"),

                q(caseContext + "\n\nTesting shows that AI recommendations are consistently less accurate for two minority-language groups. What is the best next action?",
                        List.of(
                                "Continue because overall accuracy is acceptable",
                                "Suspend or limit the affected use, investigate the data and model, involve diverse experts, test for bias, and correct the weakness before relying on the output",
                                "Exclude those language groups from the platform",
                                "Publish the recommendations with a disclaimer only"),
                        1,
                        "The case presents an observed fairness and quality problem. Responsible action is to control the affected use, investigate bias and data quality, involve relevant diverse expertise, improve and retest the solution, and retain human review.",
                        Difficulty.HARD, ref, "ai,long-question,case-1,bias", "TOPIC_QUALITY", "TOPIC_RESOURCE_TEAM", "TOPIC_BUSINESS_COMPLIANCE"),

                q(caseContext + "\n\nThe forecasting model recommends delaying security testing to protect the public launch date. What should the project manager do?",
                        List.of(
                                "Implement the recommendation because AI can optimize schedules",
                                "Ask the model to accept the security risk",
                                "Validate the assumptions and impacts with security, compliance, and delivery experts, then use the established governance process for any decision",
                                "Remove security testing from the project scope"),
                        2,
                        "AI may support trade-off analysis, but it does not replace expert judgment, mandatory controls, or governance. The named human decision-makers should validate assumptions and impacts and follow the approved decision process.",
                        Difficulty.HARD, ref, "ai,long-question,case-1,accountability,schedule", "TOPIC_SCHEDULE", "TOPIC_GOVERNANCE_CHANGE", "TOPIC_BUSINESS_COMPLIANCE"),

                q(caseContext + "\n\nThe vendor uses AI to prepare an initial risk register and proposed responses for review by the project team. Which AI adoption strategy best describes this activity?",
                        List.of("Automation", "Assistance", "Augmentation", "Delegation of accountability"),
                        1,
                        "Creating a draft risk register that requires project-team analysis, refinement, and validation is assistance. It complements professional work but is not a final autonomous decision.",
                        Difficulty.MEDIUM, ref, "ai,long-question,case-1,assistance,risk", "TOPIC_RISK"),

                q(caseContext + "\n\nThe project uses AI to transcribe recurring meetings and draft decisions and action items. Which control is most important before those minutes become the official record?",
                        List.of(
                                "Allow the tool to send the minutes to every citizen automatically",
                                "Have an accountable person verify accuracy, context, decisions, owners, privacy, and distribution",
                                "Remove all human review to save time",
                                "Store every transcript permanently"),
                        1,
                        "Meeting-minute automation should retain human verification. The reviewer checks meaning, decisions, action ownership, dates, sensitive information, access, and the intended audience before release.",
                        Difficulty.MEDIUM, ref, "ai,long-question,case-1,automation,communication", "TOPIC_STAKEHOLDER_COMMUNICATION"),

                q(caseContext + "\n\nThe governance board asks the project manager to use stakeholder sentiment analysis to explore alternative engagement strategies and refine them through several workshops. Which AI strategy is most appropriate?",
                        List.of("Automation", "Assistance", "Augmentation", "Unsupervised authorization"),
                        2,
                        "Exploring a complex stakeholder strategy through iterative human-AI brainstorming is augmentation. The project team uses AI to expand thinking while humans interpret context and choose actions.",
                        Difficulty.MEDIUM, ref, "ai,long-question,case-1,augmentation,stakeholder", "TOPIC_STAKEHOLDER_COMMUNICATION", "TOPIC_GOVERNANCE_CHANGE")
        );
    }

    private List<SeedQuestion> longGeneralCaseQuestions() {
        String caseContext = """
                CASE 2 — Global ERP and operating-model transformation:
                A manufacturer is replacing legacy finance, procurement, inventory, and human-resource systems in four regions. The regulatory go-live date is fixed because new financial-reporting rules become effective at the end of the year. The core platform and data migration are managed predictively, while regional user workflows are configured by agile teams in three-week iterations. A fixed-price vendor owns the integration layer. The sponsor is highly influential and requests additional analytics features, regional managers are concerned about job changes, and the operations group has limited capacity for training and transition. A known regulatory risk has now occurred, one vendor interface activity is delaying a near-critical path, and the latest earned value report shows CPI 0.88 and SPI 1.10. The teams have completed several product increments, but users in one region continue to reject them because local acceptance expectations were not clearly understood.
                """.strip();
        String ref = "PMBOK Guide 8th Edition, Performance Domains and Project Management Focus Areas";

        return List.of(
                q(caseContext + "\n\nThe new financial-reporting regulation has now been formally adopted. It was already documented as a risk. What should the project manager do first?",
                        List.of(
                                "Review the risk register, confirm the trigger and applicability, and reassess or activate the planned response",
                                "Immediately revise every baseline",
                                "Close the risk without further analysis",
                                "Reduce product scope to create schedule reserve"),
                        0,
                        "Because the identified risk event has occurred, the project manager first reviews the documented trigger, assumptions, impact, ownership, and planned response. Issue and change records are then updated as appropriate, and a change request is submitted if baselines are affected.",
                        Difficulty.HARD, ref, "long-question,case-2,risk,regulation", "TOPIC_RISK", "TOPIC_BUSINESS_COMPLIANCE", "TOPIC_GOVERNANCE_CHANGE"),

                q(caseContext + "\n\nRegional managers are resisting the transformation because they fear job losses. What should the project manager do next?",
                        List.of(
                                "Escalate every resistant manager to the sponsor",
                                "Assess current and desired engagement, understand the causes and information needs, and tailor the engagement strategy",
                                "Send the same technical report to all managers",
                                "Remove the managers from the stakeholder register"),
                        1,
                        "The project manager should understand the engagement gap and underlying concerns, then tailor communication and involvement. Escalation or one-way generic communication is unlikely to address resistance.",
                        Difficulty.MEDIUM, ref, "long-question,case-2,stakeholder,resistance", "TOPIC_STAKEHOLDER_COMMUNICATION", "TOPIC_RESOURCE_TEAM"),

                q(caseContext + "\n\nThe vendor interface activity is delaying a near-critical path. What should the project manager do first?",
                        List.of(
                                "Terminate the vendor immediately",
                                "Analyze the network and schedule impact, review the contract and performance evidence, and then discuss recovery options with the vendor",
                                "Crash every activity in the project",
                                "Move the regulatory date without approval"),
                        1,
                        "The project manager needs facts before action: verify the network impact and available float, review contractual commitments and evidence, then collaborate on recovery options. Corrective or contractual action follows the analysis.",
                        Difficulty.HARD, ref, "long-question,case-2,schedule,vendor", "TOPIC_SCHEDULE", "TOPIC_PROCUREMENT"),

                q(caseContext + "\n\nDuring an iteration, the sponsor asks the agile team to add a new analytics feature immediately. What is the best response?",
                        List.of(
                                "Add the feature directly to the current iteration without team discussion",
                                "Ask the Product Owner to add and prioritize the feature in the product backlog and let the team plan it according to capacity and the framework",
                                "Reject the feature because the sponsor cannot provide input",
                                "Update the predictive scope baseline only and bypass the backlog"),
                        1,
                        "New agile product work is captured and prioritized through the product backlog by the Product Owner. The team then selects work based on capacity and the applicable iteration-change rules; the sponsor does not directly assign work mid-iteration.",
                        Difficulty.MEDIUM, ref, "long-question,case-2,agile,backlog", "TOPIC_AGILE_HYBRID", "TOPIC_SCOPE_REQUIREMENTS", "TOPIC_STAKEHOLDER_COMMUNICATION"),

                q(caseContext + "\n\nThe earned value report shows CPI 0.88 and SPI 1.10. What should the project manager do next?",
                        List.of(
                                "Crash the schedule because SPI is above 1.0",
                                "Remove team members immediately",
                                "Investigate the cost variance and its relationship to work performed ahead of schedule, update forecasts, and evaluate corrective options",
                                "Reduce required features without change control"),
                        2,
                        "CPI below 1.0 indicates unfavorable cost performance while SPI above 1.0 indicates the project is ahead of schedule. The project manager should isolate the cost variance, determine whether accelerated work explains part of it, assess trend and forecasts, and only then select corrective action.",
                        Difficulty.MEDIUM, ref, "long-question,case-2,cpi,spi,cost", "TOPIC_COST_FINANCE", "TOPIC_SCHEDULE"),

                q(caseContext + "\n\nThe solution has passed technical testing, but the operations group is not ready to support users after go-live. What should the project manager do before closing the project or phase?",
                        List.of(
                                "Close immediately because technical testing is complete",
                                "Confirm operational readiness, training, support ownership, transition criteria, knowledge transfer, and formal acceptance",
                                "Transfer all unresolved work to the issue log and close without owners",
                                "Ask the agile teams to operate the system permanently"),
                        1,
                        "Technical completion alone is insufficient. The team should satisfy transition and acceptance criteria, prepare operations and support, transfer knowledge and ownership, resolve or hand over open items, and then complete closure activities.",
                        Difficulty.MEDIUM, ref, "long-question,case-2,transition,closing", "TOPIC_CLOSING_KNOWLEDGE", "TOPIC_RESOURCE_TEAM", "TOPIC_QUALITY")
        );
    }

    private SeedQuestion q(
            String question,
            List<String> options,
            int correctIndex,
            String explanation,
            Difficulty difficulty,
            String reference,
            String tags,
            String... categoryCodes
    ) {
        return new SeedQuestion(
                question,
                options,
                correctIndex,
                explanation,
                difficulty,
                reference,
                tags,
                new LinkedHashSet<>(List.of(categoryCodes))
        );
    }

    private record SeedQuestion(
            String question,
            List<String> options,
            int correctIndex,
            String explanation,
            Difficulty difficulty,
            String reference,
            String tags,
            Set<String> categoryCodes
    ) {}
}
