package org.jsmart.zerocode.core.engine.preprocessor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BinaryNode;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.jayway.jsonpath.Configuration;
import org.jsmart.zerocode.core.di.main.ApplicationMainModule;
import org.jsmart.zerocode.core.di.provider.JsonPathJacksonProvider;
import org.jsmart.zerocode.core.di.provider.ObjectMapperProvider;
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

@RunWith(Parameterized.class)
public class ZeroCodeAssertionsProcessorImplConvertJsonTypeToJavaTypeTest {
    @Parameter
    public JsonNode input;
    @Parameter(1)
    public Object output;
    private static Injector injector;
    private static SmartUtils smartUtils;
    private static ObjectMapper mapper;
    private static ZeroCodeAssertionsProcessorImpl jsonPreProcessor;

    @BeforeClass
    public static void setUpStuff() throws Exception {
    }

    @Parameters
    public static Collection<Object[]> data() {
        String serverEnvFileName = "config_hosts_test.properties";
        injector = Guice.createInjector(new ApplicationMainModule(serverEnvFileName));
        smartUtils = injector.getInstance(SmartUtils.class);
        Configuration.setDefaults(new JsonPathJacksonProvider().get());
        mapper = new ObjectMapperProvider().get();
        jsonPreProcessor =
                new ZeroCodeAssertionsProcessorImpl(smartUtils.getMapper(), serverEnvFileName);
        BinaryNode binaryNode = new BinaryNode(new byte[] {'f', 'o', 'r', 't', 'y', ' ', 't', 'w', 'o'});
        return Arrays.asList(
                new Object[][] {
                        {mapper.valueToTree(true), true},
                        {mapper.valueToTree(2147483649L), 2147483649L},
                        {mapper.valueToTree(42), 42},
                        {mapper.valueToTree(42.001D), 42.001D},
                        {mapper.valueToTree(null), null},
                        {mapper.valueToTree(new String[] {"Rough", "Tough"}), null},
                        {binaryNode, null}
                }
        );
    }

    @Test
    public void convertJsonTypeToJavaType() throws Exception {
        if (input.isNull())
            Assert.assertNull(jsonPreProcessor.convertJsonTypeToJavaType(input));
        else if (input.isArray())
            Assert.assertThrows(String.format("Unsupported JSON Type: %s", input.getClass().getName()),
                    RuntimeException.class,
                    () -> jsonPreProcessor.convertJsonTypeToJavaType(input));
        else if (input.isBinary())
            Assert.assertThrows(String.format("Oops! Unsupported JSON primitive to Java : %s by the framework", input.getClass().getName()),
                    RuntimeException.class,
                    () -> jsonPreProcessor.convertJsonTypeToJavaType(input));
        else
            Assert.assertEquals(output, jsonPreProcessor.convertJsonTypeToJavaType(input));
    }
}
