package com.example.pmp.category;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Component
public class CategorySeeder implements ApplicationRunner {
    private final CategoryRepository repository;

    public CategorySeeder(CategoryRepository repository) { this.repository = repository; }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Seed> seeds = List.of(
            new Seed("P8_GOVERNANCE", "Governance", "Governance, integration, authorization, change control, compliance and strategic alignment.", Taxonomy.PMBOK8_DOMAIN, 10),
            new Seed("P8_SCOPE", "Scope & Quality", "Business case, requirements, scope, WBS, deliverables, acceptance and quality.", Taxonomy.PMBOK8_DOMAIN, 20),
            new Seed("P8_SCHEDULE", "Schedule", "Activities, dependencies, estimates, iterations, milestones and schedule control.", Taxonomy.PMBOK8_DOMAIN, 30),
            new Seed("P8_FINANCE", "Finance", "Estimating, budgeting, funding, reserves, EVM and financial control.", Taxonomy.PMBOK8_DOMAIN, 40),
            new Seed("P8_STAKEHOLDERS", "Stakeholders", "Identification, analysis, communications, engagement and sponsor/customer relationships.", Taxonomy.PMBOK8_DOMAIN, 50),
            new Seed("P8_RESOURCES", "Resources", "Team, leadership, competencies, training, physical resources and conflict management.", Taxonomy.PMBOK8_DOMAIN, 60),
            new Seed("P8_RISK", "Risk", "Threats, opportunities, uncertainty, analysis, responses and monitoring.", Taxonomy.PMBOK8_DOMAIN, 70),

            new Seed("PMA_INTEGRATION_CHANGE", "Integration & Change", "Charter, project management plan, execution, monitoring, change control and PMO/governance.", Taxonomy.PMA_HANDOUT_TOPIC, 110),
            new Seed("PMA_SCOPE_REQUIREMENTS", "Scope & Requirements", "Requirements, scope statement, WBS, backlog, acceptance and validation.", Taxonomy.PMA_HANDOUT_TOPIC, 120),
            new Seed("PMA_SCHEDULE", "Schedule", "Network diagrams, critical path, Gantt charts, iterations, releases and schedule control.", Taxonomy.PMA_HANDOUT_TOPIC, 130),
            new Seed("PMA_COST_EVM", "Cost & EVM", "Budget, reserves, earned value, CPI/SPI, forecasting and financial decisions.", Taxonomy.PMA_HANDOUT_TOPIC, 140),
            new Seed("PMA_QUALITY", "Quality", "Quality planning, audits, control, defects, testing, continuous improvement and root cause analysis.", Taxonomy.PMA_HANDOUT_TOPIC, 150),
            new Seed("PMA_RESOURCE_TEAM", "Resources, Team & Leadership", "Resource planning, team development, training, leadership, coaching and conflict.", Taxonomy.PMA_HANDOUT_TOPIC, 160),
            new Seed("PMA_COMM_STAKEHOLDER", "Communications & Stakeholders", "Communication methods, stakeholder analysis, engagement, influence and expectations.", Taxonomy.PMA_HANDOUT_TOPIC, 170),
            new Seed("PMA_RISK", "Risk", "Risk planning, identification, qualitative/quantitative analysis, responses and reserves.", Taxonomy.PMA_HANDOUT_TOPIC, 180),
            new Seed("PMA_PROCUREMENT", "Procurement", "SOW, contracts, vendors, source selection, procurement control and claims.", Taxonomy.PMA_HANDOUT_TOPIC, 190),
            new Seed("PMA_AGILE_HYBRID", "Agile & Hybrid", "Scrum, Kanban, XP, backlog, iteration events, adaptive planning and hybrid delivery.", Taxonomy.PMA_HANDOUT_TOPIC, 200),
            new Seed("PMA_BUSINESS_COMPLIANCE", "Business Environment & Compliance", "Business value, benefits, organizational change, regulation, sustainability and compliance.", Taxonomy.PMA_HANDOUT_TOPIC, 210),
            new Seed("PMA_CLOSING_KNOWLEDGE", "Closing & Knowledge", "Lessons learned, knowledge transfer, acceptance, transition and project/phase closure.", Taxonomy.PMA_HANDOUT_TOPIC, 220)
        );
        for (Seed seed : seeds) {
            Category category = repository.findByCodeIgnoreCase(seed.code()).orElseGet(Category::new);
            category.setCode(seed.code());
            category.setName(seed.name());
            category.setDescription(seed.description());
            category.setTaxonomy(seed.taxonomy());
            category.setDisplayOrder(seed.order());
            category.setActive(true);
            repository.save(category);
        }
    }

    private record Seed(String code, String name, String description, Taxonomy taxonomy, int order) {}
}
