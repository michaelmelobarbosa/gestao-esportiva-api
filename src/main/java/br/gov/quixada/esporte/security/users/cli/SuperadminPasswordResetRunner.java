package br.gov.quixada.esporte.security.users.cli;

import br.gov.quixada.esporte.security.users.SuperadminPasswordResetService;
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
        int exitCode = executar(args);
        int code = SpringApplication.exit(applicationContext, () -> exitCode);
        System.exit(code);
    }

    private int executar(ApplicationArguments args) {
        String email = resolverEmail(args);
        char[] password = resolverSenha();

        if (password == null || password.length == 0) {
            System.err.println("Senha não informada. Use o console interativo ou a variável " + PASSWORD_ENV + ".");
            return 1;
        }

        try {
            service.reset(email, password);
            System.out.println("Senha do SUPERADMIN '" + email + "' redefinida com sucesso.");
            return 0;
        } catch (RuntimeException e) {
            System.err.println("Falha ao redefinir a senha: " + e.getMessage());
            return 1;
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private String resolverEmail(ApplicationArguments args) {
        var valores = args.getOptionValues(EMAIL_PROPERTY);
        if (valores != null && !valores.isEmpty() && !valores.get(0).isBlank()) {
            return valores.get(0);
        }

        var env = System.getenv(EMAIL_ENV);
        if (env != null && !env.isBlank()) {
            return env;
        }

        return DEFAULT_EMAIL;
    }

    private char[] resolverSenha() {
        Console console = System.console();
        if (console != null) {
            char[] senha = console.readPassword("Nova senha do SUPERADMIN: ");
            char[] confirmacao = console.readPassword("Confirme a nova senha: ");
            if (!Arrays.equals(senha, confirmacao)) {
                System.err.println("As senhas não conferem.");
                return new char[0];
            }
            return senha;
        }

        String senha = System.getenv(PASSWORD_ENV);
        return senha == null ? null : senha.toCharArray();
    }
}
