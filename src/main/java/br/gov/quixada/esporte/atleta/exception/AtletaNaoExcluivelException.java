package br.gov.quixada.esporte.atleta.exception;

/**
 * Exclusão física de atleta bloqueada: o registro precisa estar INATIVO e não
 * pode possuir inscrições que ainda ocupam vaga (PENDENTE/APROVADA/INATIVA).
 */
public class AtletaNaoExcluivelException extends RuntimeException {
    public AtletaNaoExcluivelException(String message) {
        super(message);
    }
}
