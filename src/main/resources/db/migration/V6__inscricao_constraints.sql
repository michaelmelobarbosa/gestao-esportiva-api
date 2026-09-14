-- =============================================================================
-- V6__inscricao_constraints.sql
-- Garante no banco as invariantes de unicidade da inscrição de atleta:
--   - um número de camisa por inscrição de equipe;
--   - um atleta por inscrição de equipe.
-- A regra "um atleta por competição" depende de join (InscricaoEquipe ->
-- Competicao) e é validada no InscricaoAtletaService, não por constraint.
-- =============================================================================

alter table db_inscricao_atleta
    add constraint uk_inscricao_atleta_numero unique (id_inscricao_equipe, numero_camisa);

alter table db_inscricao_atleta
    add constraint uk_inscricao_atleta_atleta unique (id_inscricao_equipe, id_atleta);
