-- Migration: V19__Fix_peer_reviews_constraints.sql
-- Description: Fix peer_reviews status/review_type/priority_suggestion constraints to match
-- the PeerReview.ReviewStatus/ReviewType/PrioritySuggestion enum values (same lowercase-vs-uppercase
-- mismatch already fixed for team_members in V16/V17 and task_assignments in V18)

-- ===============================================
-- 1. BACKUP CURRENT PEER REVIEWS DATA
-- ===============================================

CREATE TABLE IF NOT EXISTS peer_reviews_backup AS SELECT * FROM peer_reviews;

-- ===============================================
-- 2. DROP EXISTING CONSTRAINTS
-- ===============================================

ALTER TABLE peer_reviews DROP CONSTRAINT IF EXISTS peer_reviews_status_check;
ALTER TABLE peer_reviews DROP CONSTRAINT IF EXISTS peer_reviews_review_type_check;
ALTER TABLE peer_reviews DROP CONSTRAINT IF EXISTS peer_reviews_priority_suggestion_check;

-- ===============================================
-- 3. CLEAN UP ANY INVALID / LOWERCASE VALUES
-- ===============================================

UPDATE peer_reviews SET status = 'PENDING'
WHERE status IS NULL OR status = '' OR status = 'null';

UPDATE peer_reviews SET status = CASE
    WHEN UPPER(status) = 'PENDING' THEN 'PENDING'
    WHEN UPPER(status) = 'IN_PROGRESS' THEN 'IN_PROGRESS'
    WHEN UPPER(status) = 'APPROVED' THEN 'APPROVED'
    WHEN UPPER(status) = 'REJECTED' THEN 'REJECTED'
    WHEN UPPER(status) = 'REVISION_REQUESTED' THEN 'REVISION_REQUESTED'
    ELSE 'PENDING'
END
WHERE status NOT IN ('PENDING', 'IN_PROGRESS', 'APPROVED', 'REJECTED', 'REVISION_REQUESTED');

UPDATE peer_reviews SET review_type = 'CODE'
WHERE review_type IS NULL OR review_type = '' OR review_type = 'null';

UPDATE peer_reviews SET review_type = CASE
    WHEN UPPER(review_type) = 'CODE' THEN 'CODE'
    WHEN UPPER(review_type) = 'DESIGN' THEN 'DESIGN'
    WHEN UPPER(review_type) = 'DOCUMENTATION' THEN 'DOCUMENTATION'
    WHEN UPPER(review_type) = 'PROCESS' THEN 'PROCESS'
    WHEN UPPER(review_type) = 'REQUIREMENTS' THEN 'REQUIREMENTS'
    ELSE 'CODE'
END
WHERE review_type NOT IN ('CODE', 'DESIGN', 'DOCUMENTATION', 'PROCESS', 'REQUIREMENTS');

UPDATE peer_reviews SET priority_suggestion = CASE
    WHEN UPPER(priority_suggestion) = 'MAINTAIN' THEN 'MAINTAIN'
    WHEN UPPER(priority_suggestion) = 'INCREASE' THEN 'INCREASE'
    WHEN UPPER(priority_suggestion) = 'DECREASE' THEN 'DECREASE'
    ELSE NULL
END
WHERE priority_suggestion IS NOT NULL
  AND priority_suggestion NOT IN ('MAINTAIN', 'INCREASE', 'DECREASE');

-- ===============================================
-- 4. ADD NEW CONSTRAINTS WITH MATCHING ENUM VALUES
-- ===============================================

ALTER TABLE peer_reviews ADD CONSTRAINT peer_reviews_status_check
CHECK (status IN ('PENDING', 'IN_PROGRESS', 'APPROVED', 'REJECTED', 'REVISION_REQUESTED'));

ALTER TABLE peer_reviews ADD CONSTRAINT peer_reviews_review_type_check
CHECK (review_type IN ('CODE', 'DESIGN', 'DOCUMENTATION', 'PROCESS', 'REQUIREMENTS'));

ALTER TABLE peer_reviews ADD CONSTRAINT peer_reviews_priority_suggestion_check
CHECK (priority_suggestion IS NULL OR priority_suggestion IN ('MAINTAIN', 'INCREASE', 'DECREASE'));

-- ===============================================
-- 5. FIX COLUMN DEFAULT VALUES
-- ===============================================

ALTER TABLE peer_reviews ALTER COLUMN status SET DEFAULT 'PENDING';
ALTER TABLE peer_reviews ALTER COLUMN review_type SET DEFAULT 'CODE';

-- ===============================================
-- 6. VERIFICATION AND AUDIT
-- ===============================================

DO $$
DECLARE
    review_count INTEGER;
    invalid_statuses INTEGER;
    invalid_types INTEGER;
    invalid_suggestions INTEGER;
    backup_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO review_count FROM peer_reviews;
    SELECT COUNT(*) INTO backup_count FROM peer_reviews_backup;

    SELECT COUNT(*) INTO invalid_statuses FROM peer_reviews
    WHERE status NOT IN ('PENDING', 'IN_PROGRESS', 'APPROVED', 'REJECTED', 'REVISION_REQUESTED');

    SELECT COUNT(*) INTO invalid_types FROM peer_reviews
    WHERE review_type NOT IN ('CODE', 'DESIGN', 'DOCUMENTATION', 'PROCESS', 'REQUIREMENTS');

    SELECT COUNT(*) INTO invalid_suggestions FROM peer_reviews
    WHERE priority_suggestion IS NOT NULL
      AND priority_suggestion NOT IN ('MAINTAIN', 'INCREASE', 'DECREASE');

    RAISE NOTICE 'Peer reviews constraint fix completed:';
    RAISE NOTICE 'Total reviews: %', review_count;
    RAISE NOTICE 'Backup created: % records', backup_count;
    RAISE NOTICE 'Invalid statuses after fix: %', invalid_statuses;
    RAISE NOTICE 'Invalid review types after fix: %', invalid_types;
    RAISE NOTICE 'Invalid priority suggestions after fix: %', invalid_suggestions;

    IF invalid_statuses > 0 THEN
        RAISE EXCEPTION 'Found % peer reviews with invalid statuses after constraint update', invalid_statuses;
    END IF;

    IF invalid_types > 0 THEN
        RAISE EXCEPTION 'Found % peer reviews with invalid review types after constraint update', invalid_types;
    END IF;

    IF invalid_suggestions > 0 THEN
        RAISE EXCEPTION 'Found % peer reviews with invalid priority suggestions after constraint update', invalid_suggestions;
    END IF;
END $$;

-- ===============================================
-- 7. LOG THE FIX
-- ===============================================

INSERT INTO audit_logs (user_id, action, resource, resource_id, details, severity, created_at)
VALUES (
    NULL,
    'CONSTRAINT_FIX',
    'PEER_REVIEWS',
    'V019_CONSTRAINT_VIOLATION',
    jsonb_build_object(
        'migration_version', 'V019',
        'description', 'Fixed peer_reviews status/review_type/priority_suggestion constraint violations',
        'constraint_updated', true,
        'statuses_allowed', ARRAY['PENDING', 'IN_PROGRESS', 'APPROVED', 'REJECTED', 'REVISION_REQUESTED'],
        'review_types_allowed', ARRAY['CODE', 'DESIGN', 'DOCUMENTATION', 'PROCESS', 'REQUIREMENTS'],
        'priority_suggestions_allowed', ARRAY['MAINTAIN', 'INCREASE', 'DECREASE'],
        'timestamp', NOW()
    ),
    'INFO',
    NOW()
);
