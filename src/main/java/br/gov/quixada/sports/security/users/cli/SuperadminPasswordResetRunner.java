package br.gov.quixada.sports.security.users.cli;

import br.gov.quixada.sports.security.users.SuperadminPasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.Console;
import java.util.Arrays;

@Component
@Profile("superadmin-reset")
@RequiredArgsConstructor
public class SuperadminPasswordResetRunner implements ApplicationRunner {

    static final String EMAIL_PROPERTY = "reset.email";
    static final String EMAIL_ENV = "SUPERADMIN_EMAIL";
    static final String PASSWORD_ENV = "SUPERADMIN_NEW_PASSWORD";
    static final String DEFAULT_EMAIL = "admin@quixada.gov.br";

    private final SuperadminPasswordResetService service;
    private final ApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) {
        int exitCode = execute(args);
        int code = SpringApplication.exit(applicationContext, () -> exitCode);
        System.exit(code);
    }

    private int execute(ApplicationArguments args) {
        String email = resolveEmail(args);
        char[] password = resolvePassword();

        if (password == null || password.length == 0) {
            System.err.println("Password not provided. Use the interactive console or the " + PASSWORD_ENV + ".");
            return 1;
        }

        try {
            service.reset(email, password);
            System.out.println("SUPERADMIN password '" + email + "' reset successfully.");
            return 0;
        } catch (RuntimeException e) {
            System.err.println("Failed to reset password: " + e.getMessage());
            return 1;
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private String resolveEmail(ApplicationArguments args) {
        var values = args.getOptionValues(EMAIL_PROPERTY);
        if (values != null && !values.isEmpty() && !values.get(0).isBlank()) {
            return values.get(0);
        }

        var env = System.getenv(EMAIL_ENV);
        if (env != null && !env.isBlank()) {
            return env;
        }

        return DEFAULT_EMAIL;
    }

    private char[] resolvePassword() {
        Console console = System.console();
        if (console != null) {
            char[] password = console.readPassword("New SUPERADMIN password: ");
            char[] confirmation = console.readPassword("Confirm the new password: ");
            if (!Arrays.equals(password, confirmation)) {
                System.err.println("Passwords do not match.");
                return new char[0];
            }
            return password;
        }

        String password = System.getenv(PASSWORD_ENV);
        return password == null ? null : password.toCharArray();
    }
}
