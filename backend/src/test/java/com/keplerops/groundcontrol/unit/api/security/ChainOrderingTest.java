package com.keplerops.groundcontrol.unit.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.shared.security.ApiSecurityConfig;
import com.keplerops.groundcontrol.shared.security.BrowserSecurityConfig;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;

/**
 * Pins the ADR-037 invariant that the bearer (API) chain is ordered ahead of the browser
 * chain. The chain wiring itself is verified end-to-end by {@code BrowserSessionIntegrationTest};
 * this test catches the common regression — an inadvertently removed {@code @Order} or a
 * swapped pair — at the @Configuration class level without paying for a Spring context.
 *
 * <p>Reading the annotations directly off the bean methods keeps the assertion close to the
 * source of truth: if anyone changes the priority, they have to touch a {@code @Order(N)}
 * literal on the chain method itself, and the test fires.
 */
class ChainOrderingTest {

    @Test
    void apiChainBeanHasOrderOne() {
        Order order = AnnotationUtils.findAnnotation(
                beanMethod(ApiSecurityConfig.class, "apiSecurityFilterChain"), Order.class);
        assertThat(order)
                .as("ApiSecurityConfig#apiSecurityFilterChain must declare @Order")
                .isNotNull();
        assertThat(order.value()).isEqualTo(1);
    }

    @Test
    void browserChainBeanHasOrderTwo() {
        Order order = AnnotationUtils.findAnnotation(
                beanMethod(BrowserSecurityConfig.class, "browserSecurityFilterChain"), Order.class);
        assertThat(order)
                .as("BrowserSecurityConfig#browserSecurityFilterChain must declare @Order")
                .isNotNull();
        assertThat(order.value()).isEqualTo(2);
    }

    @Test
    void apiChainPriorityIsAheadOfBrowserChain() {
        int apiOrder = AnnotationUtils.findAnnotation(
                        beanMethod(ApiSecurityConfig.class, "apiSecurityFilterChain"), Order.class)
                .value();
        int browserOrder = AnnotationUtils.findAnnotation(
                        beanMethod(BrowserSecurityConfig.class, "browserSecurityFilterChain"), Order.class)
                .value();
        assertThat(apiOrder).isLessThan(browserOrder);
    }

    private static Method beanMethod(Class<?> configClass, String name) {
        for (Method method : configClass.getDeclaredMethods()) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        throw new AssertionError("Bean method " + name + " not found on " + configClass.getName());
    }
}
