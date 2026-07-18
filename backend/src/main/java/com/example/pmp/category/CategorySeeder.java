package com.example.pmp.category;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class CategorySeeder implements ApplicationRunner {
    private final CategoryRepository repository;

    public CategorySeeder(CategoryRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Keep legacy rows for referential integrity, but hide them from the UI.
        repository.findAll().stream()
                .filter(category -> category.getTaxonomy() != Taxonomy.PMP_TOPIC)
                .forEach(category -> category.setActive(false));

        List<Seed> seeds = List.of(
                new Seed("TOPIC_PROJECT_MANAGEMENT", "Project Management",
                        "Project context, project charter, integration, planning, execution, monitoring, value delivery and general management decisions.", 10),
                new Seed("TOPIC_AGILE_HYBRID", "Agile & Hybrid",
                        "Adaptive approaches, Scrum, Kanban, XP, iterations, backlog, product ownership, servant leadership and hybrid delivery.", 20),
                new Seed("TOPIC_RISK", "Risk Management",
                        "Risk planning, identification, qualitative and quantitative analysis, responses, reserves, threats and opportunities.", 30),
                new Seed("TOPIC_SCOPE_REQUIREMENTS", "Scope & Requirements",
                        "Requirements, scope, WBS, backlog, acceptance criteria, validation, deliverables and scope control.", 40),
                new Seed("TOPIC_SCHEDULE", "Schedule Management",
                        "Activities, dependencies, duration, network diagrams, critical path, milestones, velocity and schedule control.", 50),
                new Seed("TOPIC_COST_FINANCE", "Cost & Finance",
                        "Estimating, budgeting, funding, reserves, earned value, CPI/SPI, forecasting and financial control.", 60),
                new Seed("TOPIC_QUALITY", "Quality Management",
                        "Quality planning, assurance, control, defects, testing, audits, continuous improvement and root cause analysis.", 70),
                new Seed("TOPIC_RESOURCE_TEAM", "Resource, Team & Leadership Management",
                        "Resource planning, team development, leadership, coaching, training, competencies, conflict and virtual teams.", 80),
                new Seed("TOPIC_STAKEHOLDER_COMMUNICATION", "Stakeholder & Communication Management",
                        "Stakeholder identification, analysis, engagement, communication methods, influence, feedback and expectations.", 90),
                new Seed("TOPIC_PROCUREMENT", "Procurement Management",
                        "Sourcing, SOW, contracts, vendors, source selection, agile contracting, procurement control and claims.", 100),
                new Seed("TOPIC_GOVERNANCE_CHANGE", "Governance & Change Management",
                        "Governance, PMO, authorization, baselines, change requests, CCB, decision rights and strategic alignment.", 110),
                new Seed("TOPIC_BUSINESS_COMPLIANCE", "Business Environment & Compliance",
                        "Business case, benefits, organizational change, regulation, compliance, sustainability and external environment.", 120),
                new Seed("TOPIC_CLOSING_KNOWLEDGE", "Closing & Knowledge Management",
                        "Final acceptance, transition, closure, lessons learned, knowledge transfer, archiving and benefits follow-up.", 130),
                new Seed("TOPIC_TOOLS_ARTIFACTS", "Tools, Models & Artifacts",
                        "Project documents, registers, plans, charts, diagrams, data analysis, models and decision-making techniques.", 140)
        );

        for (Seed seed : seeds) {
            Category category = repository.findByCodeIgnoreCase(seed.code()).orElseGet(Category::new);
            category.setCode(seed.code());
            category.setName(seed.name());
            category.setDescription(seed.description());
            category.setTaxonomy(Taxonomy.PMP_TOPIC);
            category.setDisplayOrder(seed.order());
            category.setActive(true);
            repository.save(category);
        }
    }

    private record Seed(String code, String name, String description, int order) {}
}
