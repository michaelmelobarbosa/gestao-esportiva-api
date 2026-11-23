-- =============================================================================
-- V23__conditional_unique_athlete_registration.sql
--
-- Scopes athlete registration uniqueness (jersey number and athlete per team
-- registration) to slot-occupying statuses (PENDING, APPROVED, INACTIVE) using
-- MySQL 8.4 functional unique indexes so CANCELED/REJECTED records free slots.
-- =============================================================================

create index idx_athlete_registration_team_reg
    on db_athlete_registration (team_registration_id);

alter table db_athlete_registration
    drop index uk_athlete_registration_jersey,
    drop index uk_athlete_registration_athlete;

create unique index uk_athlete_registration_jersey_active
    on db_athlete_registration (
        team_registration_id,
        jersey_number,
        (case when status in ('PENDING', 'APPROVED', 'INACTIVE') then 1 else null end)
    );

create unique index uk_athlete_registration_athlete_active
    on db_athlete_registration (
        team_registration_id,
        athlete_id,
        (case when status in ('PENDING', 'APPROVED', 'INACTIVE') then 1 else null end)
    );
