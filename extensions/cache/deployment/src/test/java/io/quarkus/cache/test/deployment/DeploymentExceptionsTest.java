package io.quarkus.cache.test.deployment;

import static java.util.Arrays.stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CacheResult;
import io.quarkus.cache.deployment.exception.ClassTargetException;
import io.quarkus.cache.deployment.exception.PrivateMethodTargetException;
import io.quarkus.cache.deployment.exception.VoidReturnTypeTargetException;
import io.quarkus.test.QuarkusUnitTest;

/**
 * This class tests many kinds of {@link DeploymentException} causes related to caching annotations.
 */
public class DeploymentExceptionsTest {

    private static final String UNKNOWN_CACHE_1 = "unknown-cache-1";
    private static final String UNKNOWN_CACHE_2 = "unknown-cache-2";
    private static final String UNKNOWN_CACHE_3 = "unknown-cache-3";

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(TestResource.class, TestBean.class))
            .assertException(t -> {
                assertEquals(DeploymentException.class, t.getClass());
                assertEquals(7, t.getSuppressed().length);
                assertPrivateMethodTargetException(t, "shouldThrowPrivateMethodTargetException", 1);
                assertPrivateMethodTargetException(t, "shouldAlsoThrowPrivateMethodTargetException", 2);
                assertVoidReturnTypeTargetException(t, "showThrowVoidReturnTypeTargetException");
                assertClassTargetException(t, TestResource.class, 1);
                assertClassTargetException(t, TestBean.class, 2);
            });

    private static void assertPrivateMethodTargetException(Throwable t, String expectedMethodName, long expectedCount) {
        assertEquals(expectedCount, filterSuppressed(t, PrivateMethodTargetException.class)
                .filter(s -> expectedMethodName.equals(s.getMethodInfo().name())).count());
    }

    private static void assertVoidReturnTypeTargetException(Throwable t, String expectedMethodName) {
        assertEquals(1, filterSuppressed(t, VoidReturnTypeTargetException.class)
                .filter(s -> expectedMethodName.equals(s.getMethodInfo().name())).count());
    }

    private static void assertClassTargetException(Throwable t, Class<?> expectedClassName, long expectedCount) {
        assertEquals(expectedCount, filterSuppressed(t, ClassTargetException.class)
                .filter(s -> expectedClassName.getName().equals(s.getClassName().toString())).count());
    }

    private static <T extends RuntimeException> Stream<T> filterSuppressed(Throwable t, Class<T> filterClass) {
        return stream(t.getSuppressed()).filter(filterClass::isInstance).map(filterClass::cast);
    }

    @Test
    public void shouldNotBeInvoked() {
        fail("This method should not be invoked");
    }

    @Path("/test")
    // Single annotation test.
    @CacheInvalidate(cacheName = "should-throw-class-target-exception")
    static class TestResource {

        @GET
        // Single annotation test.
        @CacheInvalidateAll(cacheName = "should-throw-private-method-target-exception")
        private void shouldThrowPrivateMethodTargetException() {
        }

        @GET
        // Repeated annotations test.
        @CacheInvalidate(cacheName = "should-throw-private-method-target-exception")
        @CacheInvalidate(cacheName = "should-throw-private-method-target-exception")
        private void shouldAlsoThrowPrivateMethodTargetException() {
        }

        @GET
        @CacheResult(cacheName = "should-throw-void-return-type-target-exception")
        public void showThrowVoidReturnTypeTargetException(String key) {
        }
    }

    @ApplicationScoped
    // Repeated annotations test.
    @CacheInvalidateAll(cacheName = "should-throw-class-target-exception")
    @CacheInvalidateAll(cacheName = "should-throw-class-target-exception")
    static class TestBean {

        @CacheName(UNKNOWN_CACHE_1)
        Cache cache;

        public TestBean(@CacheName(UNKNOWN_CACHE_2) Cache cache) {
        }

        public void setCache(@CacheName(UNKNOWN_CACHE_3) Cache cache) {
        }
    }
}
