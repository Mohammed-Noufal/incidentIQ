package com.example.incidentagent;

import org.springframework.test.context.TestContextManager;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

public class TestRunner {
    public static void main(String[] args) throws Exception {
        String[] classNames = new String[] {
            "com.example.incidentagent.persistence.PersistenceMappingTest",
            "com.example.incidentagent.catalog.ServiceCatalogControllerTest",
            "com.example.incidentagent.common.exception.GlobalExceptionHandlerTest",
            "com.example.incidentagent.health.HealthControllerTest",
            "com.example.incidentagent.health.HealthKeysConfiguredTest",
            "com.example.incidentagent.memory.MemoryDocumentFormatterTest",
            "com.example.incidentagent.seed.SeedGuardTest",
            "com.example.incidentagent.briefing.BriefingValidatorTest",
            "com.example.incidentagent.alert.AlertControllerTest",
            "com.example.incidentagent.incident.IncidentLifecycleControllerTest",
            "com.example.incidentagent.briefing.BriefingControllerTest",
            "com.example.incidentagent.seed.AdminSeedControllerTest",
            "com.example.incidentagent.demo.InvestigationControllerTest"
        };
        int passed = 0;
        int failed = 0;
        for (String className : classNames) {
            Class<?> clazz = Class.forName(className);
            System.out.println("Running " + clazz.getSimpleName() + "...");
            TestContextManager tcm = null;
            boolean isSpringTest = clazz.isAnnotationPresent(org.springframework.boot.test.context.SpringBootTest.class)
                    || clazz.isAnnotationPresent(org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest.class);
            if (isSpringTest) {
                tcm = new TestContextManager(clazz);
                tcm.beforeTestClass();
            }
            Constructor<?> ctor = clazz.getDeclaredConstructor();
            ctor.setAccessible(true);
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.isAnnotationPresent(Test.class)) {
                    m.setAccessible(true);
                    Object testInstance = ctor.newInstance();
                    try {
                        if (tcm != null) {
                            tcm.prepareTestInstance(testInstance);
                            tcm.beforeTestMethod(testInstance, m);
                        }
                        for (Method bm : clazz.getDeclaredMethods()) {
                            if (bm.isAnnotationPresent(BeforeEach.class)) {
                                bm.setAccessible(true);
                                bm.invoke(testInstance);
                            }
                        }
                        if (tcm != null) {
                            tcm.beforeTestExecution(testInstance, m);
                        }
                        m.invoke(testInstance);
                        if (tcm != null) {
                            tcm.afterTestExecution(testInstance, m, null);
                            tcm.afterTestMethod(testInstance, m, null);
                        }
                        System.out.println("  PASS: " + m.getName());
                        passed++;
                    } catch (Throwable t) {
                        Throwable cause = t.getCause() != null ? t.getCause() : t;
                        System.out.println("  FAIL: " + m.getName() + " -> " + cause);
                        cause.printStackTrace(System.out);
                        if (tcm != null) {
                            try {
                                tcm.afterTestExecution(testInstance, m, cause);
                                tcm.afterTestMethod(testInstance, m, cause);
                            } catch (Throwable ignored) {}
                        }
                        failed++;
                    }
                }
            }
            if (tcm != null) {
                tcm.afterTestClass();
            }
        }
        System.out.println("\n==========================================");
        System.out.println("TEST RESULTS: " + passed + " passed, " + failed + " failed.");
        System.out.println("==========================================");
        if (failed > 0) System.exit(1);
    }
}
