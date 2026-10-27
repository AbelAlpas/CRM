package com.example.CRM;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.CRM.activity.entity.Activity;
import com.example.CRM.activity.entity.ActivityPriority;
import com.example.CRM.activity.entity.ActivityStatus;
import com.example.CRM.activity.entity.ActivityType;
import com.example.CRM.activity.repository.ActivityRepository;
import com.example.CRM.client.entity.Client;
import com.example.CRM.client.entity.ClientType;
import com.example.CRM.lead.entity.Lead;
import com.example.CRM.opportunity.entity.Opportunity;
import com.example.CRM.opportunity.entity.PipelineStage;
import com.example.CRM.user.entity.User;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest(properties = {
    "spring.autoconfigure.exclude=",
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class ActivityRepositoryTests {
    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");
    private static final Instant DAY_START = Instant.parse("2026-01-15T00:00:00Z");
    private static final Instant DAY_END = Instant.parse("2026-01-16T00:00:00Z");

    @Autowired
    private ActivityRepository activities;

    @Autowired
    private TestEntityManager entityManager;

    private User seller;
    private User otherSeller;
    private Lead lead;
    private Client client;
    private Opportunity opportunity;
    private Activity overdue;
    private Activity upcoming;
    private Activity completed;
    private Activity general;

    @BeforeEach
    void setUp() {
        seller = entityManager.persist(new User("Ana", "Vega", "ana@example.com", "hash"));
        otherSeller = entityManager.persist(new User("Luis", "Rojas", "luis@example.com", "hash"));

        lead = new Lead();
        lead.setFirstName("Carlos");
        lead.setLastName("Quispe");
        lead.setCompanyName("Ferreteria Central");
        lead.setAssignedTo(seller);
        lead.setCreatedBy(seller);
        entityManager.persist(lead);

        client = new Client();
        client.setClientType(ClientType.COMPANY);
        client.setCompanyName("Aceros Mantaro");
        client.setAssignedTo(otherSeller);
        client.setCreatedBy(otherSeller);
        entityManager.persist(client);

        PipelineStage stage = BeanUtils.instantiateClass(PipelineStage.class);
        stage.setCode("PROPOSAL");
        stage.setName("Propuesta");
        stage.setDisplayOrder((short) 1);
        stage.setActive(true);
        ReflectionTestUtils.setField(stage, "createdAt", OffsetDateTime.now());
        ReflectionTestUtils.setField(stage, "updatedAt", OffsetDateTime.now());
        entityManager.persist(stage);

        opportunity = new Opportunity();
        opportunity.setName("Renovacion del pabellon norte");
        opportunity.setClient(client);
        opportunity.setOwner(seller);
        opportunity.setCreatedBy(seller);
        opportunity.setStage(stage);
        opportunity.setAmount(new BigDecimal("25000.00"));
        opportunity.setProbability((short) 60);
        entityManager.persist(opportunity);

        overdue = activity("Llamar por cotizacion", "Consulta sobre cemento", ActivityType.CALL,
            ActivityStatus.PENDING, ActivityPriority.HIGH, seller);
        overdue.setDueAt(NOW.minusSeconds(7200));
        overdue.setLead(lead);
        entityManager.persist(overdue);

        upcoming = activity("Visita al almacen", null, ActivityType.MEETING,
            ActivityStatus.IN_PROGRESS, ActivityPriority.MEDIUM, otherSeller);
        upcoming.setScheduledAt(NOW.plusSeconds(3600));
        upcoming.setDueAt(NOW.plusSeconds(7200));
        upcoming.setClient(client);
        entityManager.persist(upcoming);

        completed = activity("Enviar propuesta", null, ActivityType.TASK,
            ActivityStatus.COMPLETED, ActivityPriority.LOW, seller);
        completed.setCompletedAt(NOW.minusSeconds(1800));
        completed.setOpportunity(opportunity);
        entityManager.persist(completed);

        general = activity("Seguimiento general", null, ActivityType.FOLLOW_UP,
            ActivityStatus.PENDING, ActivityPriority.MEDIUM, seller);
        general.setDueAt(DAY_END.plusSeconds(3600));
        entityManager.persist(general);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void searchMatchesActivityAndRelatedEntityText() {
        assertThat(search("cemento", null, null, null, null, null, null, null,
            true, false, false, false, PageRequest.of(0, 10)).getContent())
            .extracting(Activity::getId).containsExactly(overdue.getId());
        assertThat(search("Aceros Mantaro", null, null, null, null, null, null, null,
            true, false, false, false, PageRequest.of(0, 10)).getContent())
            .extracting(Activity::getId).containsExactly(upcoming.getId());
        assertThat(search("pabellon norte", null, null, null, null, null, null, null,
            true, false, false, false, PageRequest.of(0, 10)).getContent())
            .extracting(Activity::getId).containsExactly(completed.getId());
    }

    @Test
    void filtersByTypeStatusPriorityOwnerAndEveryRelation() {
        assertSingle(search(null, ActivityType.CALL, null, null, null, null, null, null,
            true, false, false, false, PageRequest.of(0, 10)), overdue);
        assertSingle(search(null, null, ActivityStatus.IN_PROGRESS, null, null, null, null, null,
            true, false, false, false, PageRequest.of(0, 10)), upcoming);
        assertSingle(search(null, null, null, ActivityPriority.LOW, null, null, null, null,
            true, false, false, false, PageRequest.of(0, 10)), completed);
        assertThat(search(null, null, null, null, seller.getId(), null, null, null,
            true, false, false, false, PageRequest.of(0, 10)).getTotalElements()).isEqualTo(3);
        assertSingle(search(null, null, null, null, null, lead.getId(), null, null,
            true, false, false, false, PageRequest.of(0, 10)), overdue);
        assertSingle(search(null, null, null, null, null, null, client.getId(), null,
            true, false, false, false, PageRequest.of(0, 10)), upcoming);
        assertSingle(search(null, null, null, null, null, null, null, opportunity.getId(),
            true, false, false, false, PageRequest.of(0, 10)), completed);
    }

    @Test
    void filtersTodayUpcomingAndOverdue() {
        assertThat(search(null, null, null, null, null, null, null, null,
            false, true, false, false, PageRequest.of(0, 10)).getContent())
            .extracting(Activity::getId).containsExactlyInAnyOrder(overdue.getId(), upcoming.getId());
        assertThat(search(null, null, null, null, null, null, null, null,
            false, false, true, false, PageRequest.of(0, 10)).getContent())
            .extracting(Activity::getId).containsExactly(upcoming.getId(), general.getId());
        assertSingle(search(null, null, null, null, null, null, null, null,
            false, false, false, true, PageRequest.of(0, 10)), overdue);
    }

    @Test
    void paginatesWithoutLoadingTheWholeTable() {
        Page<Activity> firstPage = search(null, null, null, null, null, null, null, null,
            true, false, false, false, PageRequest.of(0, 2));

        assertThat(firstPage.getNumberOfElements()).isEqualTo(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(4);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
    }

    private Activity activity(String subject, String description, ActivityType type,
                              ActivityStatus status, ActivityPriority priority, User owner) {
        Activity activity = new Activity();
        activity.setSubject(subject);
        activity.setDescription(description);
        activity.setActivityType(type);
        activity.setStatus(status);
        activity.setPriority(priority);
        activity.setOwner(owner);
        activity.setCreatedBy(owner);
        return activity;
    }

    private Page<Activity> search(String term, ActivityType type, ActivityStatus status,
                                  ActivityPriority priority, Long ownerId, Long leadId,
                                  Long clientId, Long opportunityId, boolean allScope,
                                  boolean todayScope, boolean upcomingScope, boolean overdueScope,
                                  PageRequest pageable) {
        return activities.search(term, type, status, priority, ownerId, leadId, clientId,
            opportunityId, allScope, todayScope, upcomingScope, overdueScope,
            NOW, DAY_START, DAY_END, pageable);
    }

    private void assertSingle(Page<Activity> page, Activity expected) {
        assertThat(page.getContent()).extracting(Activity::getId).containsExactly(expected.getId());
    }
}
