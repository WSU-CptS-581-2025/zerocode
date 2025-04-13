package org.jsmart.zerocode.core.engine.preprocessor;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.jayway.jsonpath.Configuration;
import org.jsmart.zerocode.core.di.main.ApplicationMainModule;
import org.jsmart.zerocode.core.di.provider.JsonPathJacksonProvider;
import org.jsmart.zerocode.core.engine.assertion.array.ArraySizeAsserterImpl;
import org.jsmart.zerocode.core.utils.SmartUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

/**
 * This class tests one class method with all the possible inputs it is expected to respond to
 * in a meaningful way and one that it should return a default of null to.
 */
@RunWith(Parameterized.class)
public class ZeroCodeAssertionsProcessorImplSizeAsserterFinderTest {
    @Parameter
    public Object input;
    private static ZeroCodeAssertionsProcessorImpl jsonPreProcessor;

    @BeforeClass
    public static void setUpStuff() throws Exception {
        String serverEnvFileName = "config_hosts_test.properties";
        Injector injector = Guice.createInjector(new ApplicationMainModule(serverEnvFileName));
        SmartUtils smartUtils = injector.getInstance(SmartUtils.class);
        Configuration.setDefaults(new JsonPathJacksonProvider().get());
        jsonPreProcessor =
                new ZeroCodeAssertionsProcessorImpl(smartUtils.getMapper(), serverEnvFileName);
    }

    @Parameters
    public static Collection<Object> data() {
        return Arrays.asList(
                new Object[] {"42",42,false}
        );
    }

    @Test
    public void testForDateTimeAsserter() {
        if (input instanceof Boolean) {
            Assert.assertThrows(String.format("Oops! Unsupported value for .SIZE: %s", input),
                    RuntimeException.class,
                    () -> jsonPreProcessor.getPathSizeAsserter(".foo", input));
        } else {
            Assert.assertEquals(ArraySizeAsserterImpl.class, jsonPreProcessor
                    .getPathSizeAsserter(".foo", input)
                    .getClass());
        }
    }
}
