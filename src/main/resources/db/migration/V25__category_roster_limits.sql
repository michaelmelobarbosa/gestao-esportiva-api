-- =============================================================================
-- V25__category_roster_limits.sql
--
-- Adds optional minimum and maximum athlete roster limits to categories.
-- =============================================================================

alter table db_category
    add column min_athletes integer null,
    add column max_athletes integer null;
