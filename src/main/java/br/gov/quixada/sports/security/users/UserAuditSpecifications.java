package br.gov.quixada.sports.security.users;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;

public final class UserAuditSpecifications {

    private UserAuditSpecifications() {
    }

    public static Specification<UserAudit> withFilters(String actorEmail, String targetEmail, UserAuditAction action, LocalDateTime from, LocalDateTime to) {
        return (root, query, builder) -> {
            var predicates = new ArrayList<Predicate>();

            if (actorEmail != null && !actorEmail.isBlank()) {
                var term = "%" + actorEmail.toLowerCase() + "%";
                predicates.add(builder.like(builder.lower(root.get("actorEmail")), term));
            }

            if (targetEmail != null && !targetEmail.isBlank()) {
                var term = "%" + targetEmail.toLowerCase() + "%";
                predicates.add(builder.like(builder.lower(root.get("targetEmail")), term));
            }

            if (action != null) {
                predicates.add(builder.equal(root.get("action"), action));
            }

            if (from != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("occurredAt"), from));
            }

            if (to != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("occurredAt"), to));
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
