package br.gov.quixada.sports.security.users;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "api.audit")
public record UserAuditRetentionProperties(
        @DefaultValue("365") @Min(0) int retentionDays,
        @DefaultValue("0 0 3 * * *") @NotBlank String purgeCron,
        @DefaultValue("10000") @Positive int exportMaxRows
) {
    public static UserAuditRetentionProperties defaults() {
        return new UserAuditRetentionProperties(365, "0 0 3 * * *", 10_000);
    }
}
