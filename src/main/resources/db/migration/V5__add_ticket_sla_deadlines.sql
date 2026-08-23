ALTER TABLE ticket
    ADD COLUMN first_response_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN response_due_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN resolution_due_at TIMESTAMP WITH TIME ZONE;

UPDATE ticket
SET response_due_at = created_at + CASE priority
        WHEN 'CRITICAL' THEN INTERVAL '15 minutes'
        WHEN 'HIGH' THEN INTERVAL '30 minutes'
        WHEN 'MEDIUM' THEN INTERVAL '2 hours'
        WHEN 'LOW' THEN INTERVAL '4 hours'
    END,
    resolution_due_at = created_at + CASE priority
        WHEN 'CRITICAL' THEN INTERVAL '2 hours'
        WHEN 'HIGH' THEN INTERVAL '4 hours'
        WHEN 'MEDIUM' THEN INTERVAL '8 hours'
        WHEN 'LOW' THEN INTERVAL '24 hours'
    END;

ALTER TABLE ticket
    ALTER COLUMN response_due_at SET NOT NULL,
    ALTER COLUMN resolution_due_at SET NOT NULL;

CREATE INDEX idx_ticket_response_due_at ON ticket (response_due_at);
CREATE INDEX idx_ticket_resolution_due_at ON ticket (resolution_due_at);
