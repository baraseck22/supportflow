package com.baraseck.supportflow.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;

class SupportFlowMetricsTest {
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final SupportFlowMetrics metrics = new SupportFlowMetrics(registry);

    @AfterEach void cleanSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(false);
        registry.close();
    }

    @Test void exposesExpectedCounters() {
        metrics.ticketCreated(); metrics.ticketResolved(); metrics.ticketEscalated(); metrics.commentAdded();
        assertThat(count("supportflow.ticket.creations")).isEqualTo(1);
        assertThat(count("supportflow.tickets.resolved")).isEqualTo(1);
        assertThat(count("supportflow.tickets.escalated")).isEqualTo(1);
        assertThat(count("supportflow.comments.added")).isEqualTo(1);
    }

    @Test void incrementsOnlyAfterCommit() {
        begin(); metrics.ticketCreated();
        assertThat(count("supportflow.ticket.creations")).isZero();
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        assertThat(count("supportflow.ticket.creations")).isEqualTo(1);
    }

    @Test void rollbackDoesNotIncrement() {
        begin(); metrics.ticketCreated();
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        assertThat(count("supportflow.ticket.creations")).isZero();
    }

    private void begin() { TransactionSynchronizationManager.setActualTransactionActive(true); TransactionSynchronizationManager.initSynchronization(); }
    private double count(String name) { return registry.get(name).counter().count(); }
}
