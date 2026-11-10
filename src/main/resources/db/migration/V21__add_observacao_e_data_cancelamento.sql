-- =============================================================================
-- V21__add_observacao_e_data_cancelamento.sql
-- Campos de anotação livre (observacao) e de registro do momento do
-- cancelamento (data_cancelamento) nos modelos com status CANCELADA(O):
-- competição, inscrição de equipe, inscrição de atleta e partida.
--
--   - observacao: texto livre, opcional, sem vínculo com status. Não é
--     obrigatória nem disparada por nenhuma transição; serve para comunicar
--     qualquer informação relevante (motivo de cancelamento, prorrogação de
--     datas, etc.).
--   - data_cancelamento: informativa, preenchida apenas na transição para
--     CANCELADA. Não existe regra de negócio condicionada a ela.
-- =============================================================================

alter table db_competicao
    add column observacao varchar(500) null,
    add column data_cancelamento datetime(6) null;

alter table db_inscricao_equipe
    add column observacao varchar(500) null,
    add column data_cancelamento datetime(6) null;

alter table db_inscricao_atleta
    add column observacao varchar(500) null,
    add column data_cancelamento datetime(6) null;

alter table db_partida
    add column observacao varchar(500) null,
    add column data_cancelamento datetime(6) null;
