package br.gov.quixada.sports.security.users;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;

public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<User> withFilters(String search, UserStatus status) {
        return (root, query, builder) -> {
            var predicates = new ArrayList<Predicate>();

            if (search != null && !search.isBlank()) {
                var termo = "%" + search.toLowerCase() + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.<String>get("username")), termo),
                        builder.like(builder.lower(root.<String>get("email")), termo)));
            }

            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
