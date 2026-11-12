package br.gov.quixada.sports.athlete.exception;

/**
 * Physical athlete deletion blocked: the record must be INACTIVE and must not
 * have registrations that still occupy a slot (PENDING/APPROVED/INACTIVE).
 */
public class AthleteNotDeletableException extends RuntimeException {
    public AthleteNotDeletableException(String message) {
        super(message);
    }
}
