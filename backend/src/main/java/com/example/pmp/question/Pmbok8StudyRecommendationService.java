package com.example.pmp.question;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class Pmbok8StudyRecommendationService {
    private static final Map<String, RecommendationTemplate> TEMPLATES = templates();

    public List<CategoryStudySuggestion> suggestionsFor(List<PracticeSessionCategoryResult> results) {
        List<PracticeSessionCategoryResult> wrongCategories = results.stream()
                .filter(result -> result.incorrectAnswers() > 0)
                .limit(5)
                .toList();

        return java.util.stream.IntStream.range(0, wrongCategories.size())
                .mapToObj(index -> toSuggestion(wrongCategories.get(index), index))
                .toList();
    }

    private CategoryStudySuggestion toSuggestion(PracticeSessionCategoryResult result, int rank) {
        RecommendationTemplate template = TEMPLATES.getOrDefault(result.categoryCode(), genericTemplate());
        String priority;
        if (rank == 0 || result.accuracyPercentage() < 50.0) {
            priority = "HIGH";
        } else if (result.incorrectAnswers() >= 2 || result.accuracyPercentage() < 75.0) {
            priority = "MEDIUM";
        } else {
            priority = "REVIEW";
        }
        return new CategoryStudySuggestion(
                result.categoryCode(),
                result.categoryName(),
                priority,
                template.reference(),
                template.focusAreas(),
                template.decisionRule(),
                template.practice()
        );
    }

    private static Map<String, RecommendationTemplate> templates() {
        Map<String, RecommendationTemplate> values = new LinkedHashMap<>();
        values.put("TOPIC_AI", new RecommendationTemplate(
                "PMBOK 8 — Appendix X3, Sections X3.1–X3.3 and Table X3-1",
                List.of(
                        "Phân biệt Automation, Assistance và Augmentation theo độ phức tạp và mức human intervention.",
                        "Luôn kiểm tra AI output về bias, privacy, reliability, safety, transparency và copyright.",
                        "Xác định rõ human accountability; AI hỗ trợ quyết định nhưng không thay thế trách nhiệm con người."
                ),
                "AI tạo insight hoặc bản nháp → con người validate, áp dụng governance và chịu trách nhiệm cuối cùng.",
                "Làm lại câu AI theo 3 nhóm: use case, mức human oversight và responsible AI risk."
        ));
        values.put("TOPIC_PROJECT_MANAGEMENT", new RecommendationTemplate(
                "PMBOK 8 — The Standard, Sections 2–4; Governance Performance Domain, Section 2.1",
                List.of(
                        "Liên kết quyết định với value delivery, governance và project objectives.",
                        "Nhận diện đúng Focus Area: Initiating, Planning, Executing, Monitoring and Controlling, Closing.",
                        "Phân biệt assess/analyze với act/implement; không nhảy thẳng tới giải pháp."
                ),
                "Context → facts → artifact/plan → collaborate → act hoặc escalate cuối cùng.",
                "Ôn lại các câu FIRST/NEXT và ghi keyword chỉ ra bước đang thực hiện trong project life cycle."
        ));
        values.put("TOPIC_AGILE_HYBRID", new RecommendationTemplate(
                "PMBOK 8 — The Standard, Section 4.2 Adaptive and Hybrid Approaches; Section 4.4 Delivery Cadence",
                List.of(
                        "Value, feedback và incremental delivery dẫn dắt quyết định.",
                        "Product Owner ưu tiên backlog; team tự tổ chức và chọn work theo capacity.",
                        "Servant leadership: coach, facilitate và remove impediments thay vì command-and-control."
                ),
                "Agile issue → minh bạch, collaborate, inspect/adapt; không tự ý đổi iteration scope hoặc ra lệnh cho team.",
                "Luyện theo cặp khái niệm: iteration review vs retrospective, backlog vs issue log, velocity vs EVM."
        ));
        values.put("TOPIC_RISK", new RecommendationTemplate(
                "PMBOK 8 — Risk Performance Domain, Sections 2.7.1–2.7.5",
                List.of(
                        "Phân biệt uncertain future event (risk) với current condition (issue).",
                        "Đi theo chuỗi Identify → Analyze → Plan Response → Implement → Monitor.",
                        "Kiểm tra trigger, owner, response, residual/secondary risks và reserve phù hợp."
                ),
                "Risk chưa xảy ra → risk register; risk đã xảy ra → activate response và quản lý như issue/change khi cần.",
                "Làm lại các câu về response strategy, contingency/fallback, risk owner và escalation threshold."
        ));
        values.put("TOPIC_SCOPE_REQUIREMENTS", new RecommendationTemplate(
                "PMBOK 8 — Scope Performance Domain, Sections 2.2.1–2.2.5",
                List.of(
                        "Requirements phải traceable tới business need, value và acceptance criteria.",
                        "WBS/scope baseline quản lý required work; product backlog quản lý adaptive product work.",
                        "Validate Scope là formal acceptance; Control Quality xác minh correctness trước acceptance."
                ),
                "Không thêm/bớt scope tùy ý; phân tích impact và đi đúng change/backlog process.",
                "Ôn artifact mapping: requirements documentation, RTM, scope statement, WBS và acceptance criteria."
        ));
        values.put("TOPIC_SCHEDULE", new RecommendationTemplate(
                "PMBOK 8 — Schedule Performance Domain, Sections 2.3.1–2.3.5; Tools and Techniques, Critical Path Method",
                List.of(
                        "Xác định dependencies, path duration, critical path và total float trước khi hành động.",
                        "Crashing thường tăng cost; fast tracking thường tăng risk/rework.",
                        "SPI phản ánh schedule efficiency nhưng cần phân tích network và trend để chọn corrective action."
                ),
                "Schedule variance → tìm path/root cause trước; chỉ compress khi biết activity phù hợp và impact.",
                "Luyện network diagram, float, lead/lag, critical path và schedule compression bằng hình minh họa."
        ));
        values.put("TOPIC_COST_FINANCE", new RecommendationTemplate(
                "PMBOK 8 — Finance Performance Domain, Sections 2.4.1–2.4.5; Table 5-1 Earned Value Calculations",
                List.of(
                        "Nhớ EV, PV, AC và ý nghĩa CPI/SPI, CV/SV, EAC/ETC/TCPI.",
                        "Phân tích variance và trend trước khi cắt scope, resource hoặc budget.",
                        "Chọn forecast formula theo giả định hiệu suất tương lai."
                ),
                "CPI hoặc SPI xấu → isolate root cause, forecast, evaluate alternatives rồi mới corrective action/change request.",
                "Làm một bộ 10 câu tính EVM và giải thích bằng lời mỗi chỉ số trước khi chọn đáp án."
        ));
        values.put("TOPIC_QUALITY", new RecommendationTemplate(
                "PMBOK 8 — Embed Quality Into Processes and Deliverables; Quality tools in Section 5",
                List.of(
                        "Build quality in, không chỉ inspect ở cuối.",
                        "Phân biệt quality assurance/process improvement với quality control/deliverable inspection.",
                        "Dùng root cause analysis và đúng chart: Pareto, control chart, histogram, fishbone, scatter."
                ),
                "Defect → contain/verify facts → root cause → corrective action → prevent recurrence.",
                "Ôn Cost of Quality, acceptance vs conformance và lựa chọn seven basic quality tools."
        ));
        values.put("TOPIC_RESOURCE_TEAM", new RecommendationTemplate(
                "PMBOK 8 — Resources Performance Domain, Sections 2.6.1–2.6.5; Principle: Build an Empowered Culture",
                List.of(
                        "Hiểu nguyên nhân và xử lý conflict trực tiếp, riêng tư, collaborative khi phù hợp.",
                        "Coach và empower team; escalation hoặc disciplinary action chỉ sau khi các bước phù hợp thất bại.",
                        "Phân biệt leadership styles, emotional intelligence, team norms và ground rules."
                ),
                "People issue → listen/assess → collaborate/coach → document/escalate last.",
                "Luyện câu về conflict, virtual team, motivation, coaching, team charter và leadership recognition keywords."
        ));
        values.put("TOPIC_STAKEHOLDER_COMMUNICATION", new RecommendationTemplate(
                "PMBOK 8 — Stakeholders Performance Domain, Sections 2.5.1–2.5.5",
                List.of(
                        "Identify và analyze power, interest, influence, impact, needs và expectations.",
                        "So sánh current/desired engagement và tailor message, channel, timing, frequency, detail.",
                        "Manage engagement là two-way collaboration; communication chỉ gửi thông tin là chưa đủ."
                ),
                "Đúng stakeholder + đúng thông tin + đúng kênh + đúng thời điểm; confirm understanding bằng feedback.",
                "Lập lại power-interest grid và stakeholder engagement assessment matrix cho các câu đã sai."
        ));
        values.put("TOPIC_PROCUREMENT", new RecommendationTemplate(
                "PMBOK 8 — Appendix X4 Procurement, Sections X4.1–X4.9",
                List.of(
                        "Vendor problem → review contract, SOW, SLA, warranty, remedies và claim process trước.",
                        "Hiểu risk allocation của fixed-price, cost-reimbursable và time-and-materials.",
                        "Procurement decision cần phối hợp procurement/legal theo authority và governance."
                ),
                "Contract first, then evidence, conversation, remedy hoặc change.",
                "Ôn contract types, source selection, claims, closure và procurement documents."
        ));
        values.put("TOPIC_GOVERNANCE_CHANGE", new RecommendationTemplate(
                "PMBOK 8 — Governance Performance Domain, Assess and Implement Changes, Section 2.1",
                List.of(
                        "Phân tích impact trước khi approve/reject/implement change.",
                        "Không tự sửa baseline hoặc project management plan trước approval.",
                        "Dùng đúng decision rights, change authority, CCB và traceability."
                ),
                "Request → impact analysis → decision → update plans/baselines → communicate → implement.",
                "Làm lại các câu có từ change, baseline, governance, sponsor authority và compliance."
        ));
        values.put("TOPIC_BUSINESS_COMPLIANCE", new RecommendationTemplate(
                "PMBOK 8 — Governance Performance Domain; The Standard, Project Environment and Value Delivery",
                List.of(
                        "Regulation/compliance là mandatory constraint, không thể trade off tùy ý.",
                        "Liên kết project outcomes với strategy, business case, benefits và external environment.",
                        "Xác định applicability, effective date, ownership và governance response."
                ),
                "Compliance change → assess applicability/impact ngay, engage experts, then follow governance/change control.",
                "Ôn business case, benefits, regulatory events, organizational change và sustainability."
        ));
        values.put("TOPIC_CLOSING_KNOWLEDGE", new RecommendationTemplate(
                "PMBOK 8 — Closing Focus Area; Governance Performance Domain, Close Project or Phase",
                List.of(
                        "Formal acceptance và transition readiness trước administrative closure.",
                        "Transfer ownership, support, knowledge, documents và unresolved items có owner.",
                        "Capture lessons throughout project và archive final records/contracts."
                ),
                "Accept → transition → close contracts/records → release resources → lessons and benefits follow-up.",
                "Luyện phân biệt validate/accept, transition, administrative closure và benefits realization."
        ));
        values.put("TOPIC_TOOLS_ARTIFACTS", new RecommendationTemplate(
                "PMBOK 8 — Sections 4 Inputs and Outputs and 5 Tools and Techniques",
                List.of(
                        "Bắt đầu từ câu hỏi: cần thông tin gì và artifact nào chứa thông tin đó?",
                        "Phân biệt plan, baseline, register, log, report, matrix và visual chart.",
                        "Chọn tool theo mục đích: analyze, represent, decide, communicate hoặc control."
                ),
                "Information need → đúng artifact/tool; không chọn tài liệu chỉ vì tên nghe liên quan.",
                "Tạo flashcards Artifact → purpose → updated when → owner cho các câu đã sai."
        ));
        return Map.copyOf(values);
    }

    private static RecommendationTemplate genericTemplate() {
        return new RecommendationTemplate(
                "PMBOK 8 — Relevant Performance Domain and Tailoring guidance",
                List.of(
                        "Xác định context, development approach và stakeholder/value impact.",
                        "Phân tích facts và root cause trước khi hành động.",
                        "Chọn đúng artifact, process và authority cho bước tiếp theo."
                ),
                "Assess first, collaborate with the right people, then act within governance.",
                "Xem lại explanation của từng câu sai, ghi keyword và làm lại sau 24–48 giờ."
        );
    }

    private record RecommendationTemplate(
            String reference,
            List<String> focusAreas,
            String decisionRule,
            String practice
    ) {}
}
