package org.jsmart.zerocode.core.engine.preprocessor;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.jayway.jsonpath.Configuration;
import org.jsmart.zerocode.core.di.main.ApplicationMainModule;
import org.jsmart.zerocode.core.di.provider.JsonPathJacksonProvider;
import org.jsmart.zerocode.core.engine.assertion.JsonAsserter;
import org.jsmart.zerocode.core.engine.assertion.field.FieldContainsStringAsserter;
import org.jsmart.zerocode.core.engine.assertion.field.FieldContainsStringIgnoreCaseAsserter;
import org.jsmart.zerocode.core.engine.assertion.field.FieldHasExactValueAsserter;
import org.jsmart.zerocode.core.engine.assertion.field.FieldIsOneOfValueAsserter;
import org.jsmart.zerocode.core.engine.assertion.field.FieldMatchesCustomAsserter;
import org.jsmart.zerocode.core.engine.assertion.field.FieldMatchesRegexPatternAsserter;
import org.jsmart.zerocode.core.engine.tokens.ZeroCodeAssertionTokens;
import org.jsmart.zerocode.core.utils.SmartUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;

/**
 * This class tests one class method with all the possible inputs it is expected to respond to
 * in a meaningful way and one that it should return a default of null to.
 */
@RunWith(Parameterized.class)
public class ZeroCodeAssertionsProcessorImplStringValueAssertersTest {
    @Parameter
    public String input;
    @Parameter(1)
    public Type output;
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
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[][] {
                        { ZeroCodeAssertionTokens.ASSERT_VALUE_NULL, FieldHasExactValueAsserter.class},
                        { ZeroCodeAssertionTokens.ASSERT_VALUE_CONTAINS_STRING, FieldContainsStringAsserter.class },
                        { ZeroCodeAssertionTokens.ASSERT_VALUE_MATCHES_STRING, FieldMatchesRegexPatternAsserter.class },
                        { ZeroCodeAssertionTokens.ASSERT_VALUE_CONTAINS_STRING_IGNORE_CASE, FieldContainsStringIgnoreCaseAsserter.class },
                        { ZeroCodeAssertionTokens.ASSERT_VALUE_IS_ONE_OF, FieldIsOneOfValueAsserter.class },
                        { ZeroCodeAssertionTokens.ASSERT_VALUE_ONE_OF, FieldIsOneOfValueAsserter.class },
                        { ZeroCodeAssertionTokens.ASSERT_VALUE_CUSTOM_ASSERT, FieldMatchesCustomAsserter.class }
                }
        );
    }

    @Test
    public void stringAsserterFinderTest() {
        Object value = input + "\"foo\"";
        JsonAsserter asserter = jsonPreProcessor.getStringAsserter(value, ".bar");
        Assert.assertEquals(asserter.getClass(), output);
    }
}
