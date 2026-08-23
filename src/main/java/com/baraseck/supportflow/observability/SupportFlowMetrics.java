package com.baraseck.supportflow.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class SupportFlowMetrics {
    private final Counter ticketsCreated;
    private final Counter ticketsResolved;
    private final Counter ticketsEscalated;
    private final Counter commentsAdded;

    public SupportFlowMetrics(MeterRegistry registry) {
        ticketsCreated = Counter.builder("supportflow.ticket.creations")
                .description("Tickets créés avec succès").register(registry);
        ticketsResolved = Counter.builder("supportflow.tickets.resolved").description("Tickets réellement résolus").register(registry);
        ticketsEscalated = Counter.builder("supportflow.tickets.escalated").description("Escalades réussies").register(registry);
        commentsAdded = Counter.builder("supportflow.comments.added").description("Commentaires et notes internes ajoutés").register(registry);
    }

    public void ticketCreated() { afterCommit(ticketsCreated::increment); }
    public void ticketResolved() { afterCommit(ticketsResolved::increment); }
    public void ticketEscalated() { afterCommit(ticketsEscalated::increment); }
    public void commentAdded() { afterCommit(commentsAdded::increment); }

    private void afterCommit(Runnable increment) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { increment.run(); }
            });
        } else {
            increment.run();
        }
    }
}
