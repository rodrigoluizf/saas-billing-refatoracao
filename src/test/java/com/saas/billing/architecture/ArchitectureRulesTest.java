package com.saas.billing.architecture;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;

/**
 * Fitness functions da arquitetura hexagonal: o núcleo (serviço, domínio e portas) não pode
 * voltar a depender de I/O concreto, de relógio estático nem engolir exceções genéricas.
 * Se alguém reintroduzir o débito técnico, o CI quebra.
 */
public class ArchitectureRulesTest {

    private static final Path BASE = Paths.get("src", "main", "java", "com", "saas", "billing");

    private static final List<String> FORBIDDEN_IN_CORE = List.of(
            "import java.sql",
            "import org.apache.commons.mail",
            "import com.saas.billing.adapter",
            "import com.saas.billing.config",
            "DriverManager",
            "LocalDate.now()",
            "LocalDateTime.now()",
            "System.currentTimeMillis()",
            "catch (Exception",
            "catch (Throwable");

    private static List<Path> coreSources() throws IOException {
        List<Path> files = new ArrayList<>();
        files.add(BASE.resolve("SubscriptionBillingService.java"));
        for (String pkg : List.of("domain", "port")) {
            try (Stream<Path> walk = Files.walk(BASE.resolve(pkg))) {
                files.addAll(walk.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList()));
            }
        }
        return files;
    }

    @Test
    public void nucleoNaoDependeDeInfraestruturaNemDeRelogioEstatico() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path file : coreSources()) {
            String source = Files.readString(file, StandardCharsets.UTF_8);
            for (String forbidden : FORBIDDEN_IN_CORE) {
                if (source.contains(forbidden)) {
                    violations.add(file + " contém '" + forbidden + "'");
                }
            }
        }
        assertTrue("Violações de arquitetura:\n" + String.join("\n", violations), violations.isEmpty());
    }

    @Test
    public void nenhumCodigoDeProducaoTemCredenciaisFixas() throws IOException {
        try (Stream<Path> walk = Files.walk(BASE)) {
            List<Path> offenders = walk.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> {
                        try {
                            return Files.readString(p, StandardCharsets.UTF_8).contains("admin123");
                        } catch (IOException e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .collect(Collectors.toList());
            assertTrue("Senha fixa encontrada em: " + offenders, offenders.isEmpty());
        }
    }
}
