package org.jsmart.zerocode.core.engine.preprocessor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import org.jsmart.zerocode.TestUtility;
import org.jsmart.zerocode.core.di.main.ApplicationMainModule;
import org.jsmart.zerocode.core.di.provider.JsonPathJacksonProvider;
import org.jsmart.zerocode.core.di.provider.ObjectMapperProvider;
import org.jsmart.zerocode.core.domain.ScenarioSpec;
import org.jsmart.zerocode.core.domain.Step;
import org.jsmart.zerocode.core.engine.assertion.FieldAssertionMatcher;
import org.jsmart.zerocode.core.engine.assertion.JsonAsserter;
import org.jsmart.zerocode.core.engine.tokens.ZeroCodeValueTokens;
import org.jsmart.zerocode.core.utils.SmartUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jayway.jsonpath.JsonPath.read;
import static org.jsmart.zerocode.core.utils.SmartUtils.checkDigNeeded;
import static org.jsmart.zerocode.core.utils.SmartUtils.readJsonAsString;
import static org.jsmart.zerocode.core.utils.TokenUtils.getTestCaseTokens;

@SuppressWarnings("unchecked")
public class ZeroCodeAssertionsProcessorImplTest {
    Injector injector;
    SmartUtils smartUtils;
    ObjectMapper mapper;

    ZeroCodeAssertionsProcessorImpl jsonPreProcessor;

    @Before
    public void setUpStuff() throws Exception {
        String serverEnvFileName = "config_hosts_test.properties";
        injector = Guice.createInjector(new ApplicationMainModule(serverEnvFileName));
        smartUtils = injector.getInstance(SmartUtils.class);
        mapper = new ObjectMapperProvider().get();
        Configuration.setDefaults(new JsonPathJacksonProvider().get());
        jsonPreProcessor =
                new ZeroCodeAssertionsProcessorImpl(smartUtils.getMapper(), serverEnvFileName);
    }

    @Test
    public void willEvaluatePlaceHolder() throws Exception {

        String aString = "Hello_${WORLD}";
        List<String> placeHolders = getTestCaseTokens(aString);
        Assert.assertEquals(1, placeHolders.size());
        Assert.assertEquals("WORLD", placeHolders.get(0));

        aString = "Hello_${$.step_name}";
        placeHolders = getTestCaseTokens(aString);
        Assert.assertEquals(1, placeHolders.size());
        Assert.assertEquals("$.step_name", placeHolders.get(0));
    }

    @Test
    public void willResolveWithParamMap() throws Exception {
        ScenarioSpec scenarioSpec =
                smartUtils.scenarioFileToJava(
                        "unit_test_files/test_engine/01_request_with_place_holders.json", ScenarioSpec.class);
        final String requestJsonAsString = scenarioSpec.getSteps().get(0).getRequest().toString();

        final String resolvedRequestJson =
                jsonPreProcessor.resolveStringJson(requestJsonAsString, requestJsonAsString);

        String lastName = JsonPath.read(resolvedRequestJson, "$.body.Customer.lastName");
        String nickName = JsonPath.read(resolvedRequestJson, "$.body.Customer.nickName");

        Assert.assertNotEquals(lastName, nickName);
    }

    @Test
    public void willCaptureAllPlaceHolders() throws Exception {
        ScenarioSpec scenarioSpec =
                smartUtils.scenarioFileToJava(
                        "unit_test_files/test_engine/01_request_with_place_holders.json", ScenarioSpec.class);
        final String requestJsonAsString = scenarioSpec.getSteps().get(0).getRequest().toString();

        final List<String> placeHolders = getTestCaseTokens(requestJsonAsString);
        Assert.assertEquals(4, placeHolders.size());

        final String resolvedRequestJson =
                jsonPreProcessor.resolveStringJson(requestJsonAsString, requestJsonAsString);
        Assert.assertTrue(resolvedRequestJson.contains("\"staticName\":\"abcde\""));

        String specAsString =
                smartUtils.getJsonDocumentAsString("unit_test_files/test_engine/01_request_with_place_holders.json");
        final String resolvedSpecString =
                jsonPreProcessor.resolveStringJson(specAsString, specAsString);
        Assert.assertTrue(resolvedSpecString.contains("\"url\": \"/persons/abc\""));
    }

    @Test
    public void willResolveJsonPathOfJayWay() throws Exception {
        String specAsString =
                smartUtils.getJsonDocumentAsString("unit_test_files/test_engine/01_request_with_place_holders.json");

        final List<String> jsonPaths = jsonPreProcessor.getAllJsonPathTokens(specAsString);
        Assert.assertEquals(2, jsonPaths.size());

        final String resolvedSpecWithPaths =
                jsonPreProcessor.resolveStringJson(specAsString, specAsString);
        Assert.assertTrue(resolvedSpecWithPaths.contains("\"staticName\": \"abcde\""));

        // final String resolvedSpecResolvedPaths =
        // jsonPreProcessor.resolveJsonPaths(resolvedSpecWithPaths);
        Assert.assertTrue(resolvedSpecWithPaths.contains("\"actualName\": \"${STATIC.ALPHABET:5}\""));
        Assert.assertTrue(resolvedSpecWithPaths.contains("\"actualNameSize\": \"2\""));
    }

    @Test
    public void willResolveJsonPathOfJayWayWith_SuppliedScenarioState() throws Exception {
        String specAsString =
                smartUtils.getJsonDocumentAsString(
                        "unit_test_files/test_engine/02_1_two_requests_with_json_path_assertion.json");

        final List<String> jsonPaths = jsonPreProcessor.getAllJsonPathTokens(specAsString);
        Assert.assertEquals(3, jsonPaths.size());

        String scenarioState =
                "{\n"
                        + "    \"step1\": {\n"
                        + "        \"request\": {\n"
                        + "            \"body\": {\n"
                        + "                \"customer\": {\n"
                        + "                    \"firstName\": \"FIRST_NAME\",\n"
                        + "                    \"staticName\": \"ANOTHER_NAME\",\n"
                        + "                    \"addresses\":[\"office-1\", \"home-2\"]\n"
                        + "                }\n"
                        + "            }\n"
                        + "        },\n"
                        + "        \"response\": {\n"
                        + "            \"id\": 10101\n"
                        + "        }\n"
                        + "    }\n"
                        + "}";
        final String resolvedSpecWithPaths =
                jsonPreProcessor.resolveStringJson(specAsString, scenarioState);
        Assert.assertTrue(resolvedSpecWithPaths.contains("\"staticName\": \"abcde\""));
        Assert.assertTrue(resolvedSpecWithPaths.contains("\"firstName\": \"FIRST_NAME\""));
        Assert.assertTrue(resolvedSpecWithPaths.contains("\"firstName2\": \"FIRST_NAME\""));
        Assert.assertTrue(resolvedSpecWithPaths.contains("\"actualName\": \"ANOTHER_NAME\""));
        Assert.assertTrue(resolvedSpecWithPaths.contains("\"noOfAddresses\": \"2\""));
    }

    @Test
    public void willResolveAndTypeCast_SingleDimensionArrayElements_FromScenarioState() throws Exception {
        String specAsString =
                smartUtils.getJsonDocumentAsString(
                        "unit_test_files/test_engine/02_2_resolve_typecast_in_single_dimention_arraylist_assertion.json");

        final List<String> jsonPaths = jsonPreProcessor.getAllJsonPathTokens(specAsString);
        Assert.assertEquals(6, jsonPaths.size());

        String scenarioState =
                "{\n"
                        + "    \"step1\": {\n"
                        + "        \"request\": {\n"
                        + "            \"body\": {\n"
                        + "                \"customer\": {\n"
                        + "\"ids\": [\n" +
                        "              10101,\n" +
                        "              10102\n" +
                        "            ],"
                        + "                    \"firstName\": \"FIRST_NAME\",\n"
                        + "                    \"staticName\": \"ANOTHER_NAME\",\n"
                        + "                    \"addresses\":[\"office-1\", \"home-2\"]\n"
                        + "                }\n"
                        + "            }\n"
                        + "        },\n"
                        + "        \"response\": {\n"
                        + "            \"id\": 10101\n"
                        + "        }\n"
                        + "    }\n"
                        + "}";
        final String resolvedSpecWithPaths =
                jsonPreProcessor.resolveStringJson(specAsString, scenarioState);
        System.out.println("resolvedSpecWithPaths ==> " + resolvedSpecWithPaths);

        Object jsonPathValue = JsonPath.read(resolvedSpecWithPaths,
                "$.steps[1].request.body.Customer.accounts[0]");

        Assert.assertTrue(jsonPathValue instanceof String);

        Assert.assertTrue(resolvedSpecWithPaths.contains("\"staticName\":\"abcde\""));
       Assert.assertTrue(resolvedSpecWithPaths.contains("\"firstName\":\"FIRST_NAME\""));
       Assert.assertTrue(resolvedSpecWithPaths.contains("\"firstName2\":\"FIRST_NAME\""));
       Assert.assertTrue(resolvedSpecWithPaths.contains("\"actualName\":\"ANOTHER_NAME\""));
        Assert.assertTrue(resolvedSpecWithPaths.contains("\"noOfAddresses\":\"2\""));
        Assert.assertTrue(resolvedSpecWithPaths.contains("\"accounts\":[\"10101\",\"10102\"]"));
    }

    @Test
    public void willResolveJsonPathOfJayWayFor_AssertionSection() throws Exception {
        ScenarioSpec scenarioSpec =
                smartUtils.scenarioFileToJava(
                        "unit_test_files/test_engine/02_1_two_requests_with_json_path_assertion.json", ScenarioSpec.class);

        // Get the 2nd step
        final String assertionsSectionAsString =
                scenarioSpec.getSteps().get(1).getAssertions().toString();
        String scenarioState =
                "{\n"
                        + "    \"step1\": {\n"
                        + "        \"request\": {\n"
                        + "            \"body\": {\n"
                        + "                \"customer\": {\n"
                        + "                    \"firstName\": \"FIRST_NAME\",\n"
                        + "                    \"staticName\": \"ANOTHER_NAME\",\n"
                        + "                    \"addresses\":[\"office-1\", \"home-2\"]\n"
                        + "                }\n"
                        + "            }\n"
                        + "        },\n"
                        + "        \"response\": {\n"
                        + "            \"id\": 10101\n"
                        + "        }\n"
                        + "    }\n"
                        + "}";

        final String resolvedAssertions =
                jsonPreProcessor.resolveStringJson(assertionsSectionAsString, scenarioState);
        Assert.assertTrue(resolvedAssertions.contains("\"actualName\":\"ANOTHER_NAME\""));

        // start assertion
        String sampleExecutionResult =
                smartUtils.getJsonDocumentAsString(
                        "unit_test_files/test_engine/02_2_sample_resolved_execution_response.json");
        List<JsonAsserter> asserters = jsonPreProcessor.createJsonAsserters(resolvedAssertions);
        Assert.assertEquals(17, asserters.size());

        List<FieldAssertionMatcher> failedReports =
                jsonPreProcessor.assertAllAndReturnFailed(asserters, sampleExecutionResult);

        System.out.println("###failedReports : " + failedReports);
        Assert.assertTrue(
                failedReports.toString().contains("did not match the expected value 'NOT NULL'"));
        Assert.assertTrue(
                failedReports.toString()
                        .contains("did not match the expected value 'ANOTHER_NAME'"));
        Assert.assertTrue(failedReports.toString().contains("did not match the expected value 'NULL'"));
        Assert.assertTrue(failedReports.toString().contains("citizenship' with actual value '[{"));
        Assert.assertTrue(failedReports.toString().contains("personalities' with actual value 'null'"));
        Assert.assertTrue(failedReports.toString().contains("did not match the expected value '[]'"));
        Assert.assertFalse(failedReports.toString().contains("pastActivities"));
        Assert.assertTrue(
                failedReports.toString().contains("did not match the expected value 'Array of size 5'"));
        Assert.assertTrue(
                failedReports.toString().contains("did not match the expected value 'Array of size 4'"));
        Assert.assertTrue(
                failedReports.toString().contains("did not match the expected value 'containing sub-string:DaddyWithMac'"));
        Assert.assertTrue(
                failedReports.toString().contains("did not match the expected value 'Greater Than:499'"));
        Assert.assertTrue(
                failedReports.toString()
                        .contains("'null' did not match the expected value 'Greater Than:388'"));
        Assert.assertTrue(
                failedReports.toString()
                        .contains("actual value '1400' did not match the expected value 'Lesser Than:1300'"));
        Assert.assertEquals(11, failedReports.size());
    }

    @Test
    public void willResolveTextNodeFor_Assertion() throws Exception {

        final String assertionsSectionTextNodeAsString = "\"id-generated-0101\"";

        String scenarioState =
                "{\n"
                        + "    \"step1\": {\n"
                        + "        \"request\": {\n"
                        + "            \"body\": {\n"
                        + "                \"customer\": {\n"
                        + "                    \"firstName\": \"FIRST_NAME\",\n"
                        + "                    \"staticName\": \"ANOTHER_NAME\",\n"
                        + "                    \"addresses\":[\"office-1\", \"home-2\"]\n"
                        + "                }\n"
                        + "            }\n"
                        + "        },\n"
                        + "        \"response\": \"id-generated-0101-aga-baga\""
                        + // <--- In this case this is not relevant as this path is not used
                        "    }\n"
                        + "}";

        final String resolvedAssertions =
                jsonPreProcessor.resolveStringJson(assertionsSectionTextNodeAsString, scenarioState);
        Assert.assertTrue(resolvedAssertions.contains("\"id-generated-0101\""));

        // start assertion
        List<JsonAsserter> asserters = jsonPreProcessor.createJsonAsserters(resolvedAssertions);
        Assert.assertEquals(1, asserters.size());

        String sampleExecutionResult = "\"id-generated-0101-XY\"";
        List<FieldAssertionMatcher> failedReports =
                jsonPreProcessor.assertAllAndReturnFailed(asserters, sampleExecutionResult);

        System.out.println("###failedReports : " + failedReports);
        Assert.assertTrue(
                failedReports.toString()
                        .contains(
                        "'$' with actual value 'id-generated-0101-XY' did not match the expected value 'id-generated-0101'"));
        Assert.assertEquals(1, failedReports.size());
    }

    @Test
    public void willResolveIntegerNodeFor_Assertion() throws Exception {

        final Integer assertionsSectionInt = 1099;

        String scenarioState =
                "{\n"
                        + "    \"step1\": {\n"
                        + "        \"request\": {\n"
                        + "            \"body\": {\n"
                        + "                \"customer\": {\n"
                        + "                    \"firstName\": \"FIRST_NAME\",\n"
                        + "                    \"staticName\": \"ANOTHER_NAME\",\n"
                        + "                    \"addresses\":[\"office-1\", \"home-2\"]\n"
                        + "                }\n"
                        + "            }\n"
                        + "        },\n"
                        + "        \"response\": 300000"
                        + // <--- In this case this is not relevant as this path is not used
                        "    }\n"
                        + "}";

        final String resolvedAssertions =
                jsonPreProcessor.resolveStringJson(assertionsSectionInt.toString(), scenarioState);
        Assert.assertTrue(resolvedAssertions.contains("1099"));

        // start assertion
        List<JsonAsserter> asserters = jsonPreProcessor.createJsonAsserters(resolvedAssertions);
        Assert.assertEquals(1, asserters.size());

        Integer sampleExecutionResult = 1077;
        List<FieldAssertionMatcher> failedReports =
                jsonPreProcessor.assertAllAndReturnFailed(asserters, sampleExecutionResult.toString());

        System.out.println("###failedReports : " + failedReports);
        Assert.assertTrue(
                failedReports.toString()
                        .contains("'$' with actual value '1077' did not match the expected value '1099'"));
        Assert.assertEquals(1, failedReports.size());
    }

    @Test
    public void testLocalDate_formatter() throws Exception {
        ScenarioSpec scenarioSpec =
                smartUtils.scenarioFileToJava(
                        "unit_test_files/localdatetime/00_local_date_time_place_holders_unit_test.json", ScenarioSpec.class);

        final String requestJsonAsString = scenarioSpec.getSteps().get(0).getRequest().toString();

        final List<String> placeHolders = getTestCaseTokens(requestJsonAsString);
        Assert.assertEquals(2, placeHolders.size());

        final String resolvedRequestJson =
                jsonPreProcessor.resolveStringJson(requestJsonAsString, requestJsonAsString);
        Assert.assertEquals(-1, resolvedRequestJson.indexOf("LOCAL.DATE.TODAY:"));
        Assert.assertEquals(-1, resolvedRequestJson.indexOf("${LOCAL.DATE.TODAY:yyyy}"));
    }

    @Test
    public void testLocalDateTime_formatter() throws Exception {
        ScenarioSpec scenarioSpec =
                smartUtils.scenarioFileToJava(
                        "unit_test_files/localdatetime/00_local_date_time_place_holders_unit_test.json", ScenarioSpec.class);

        final String requestJsonAsString = scenarioSpec.getSteps().get(0).getRequest().toString();

        final List<String> placeHolders = getTestCaseTokens(requestJsonAsString);
        Assert.assertEquals(2, placeHolders.size());

        final String resolvedRequestJson =
                jsonPreProcessor.resolveStringJson(requestJsonAsString, requestJsonAsString);
        Assert.assertEquals(-1, resolvedRequestJson.indexOf("LOCAL.DATETIME.NOW:"));
    }

    @Test
    public void testRandom_UUID() throws Exception {

        final String requestJsonAsString = "{\n" + "\t\"onlineOrderId\": \"${RANDOM.UUID}\"\n" + "}";

        final List<String> placeHolders = getTestCaseTokens(requestJsonAsString);
        Assert.assertEquals(1, placeHolders.size());

        final String resolvedRequestJson =
                jsonPreProcessor.resolveStringJson(requestJsonAsString, requestJsonAsString);
        Assert.assertEquals(-1, resolvedRequestJson.indexOf("RANDOM.UUID"));

        final HashMap<String, String> hashMap =
                smartUtils.getMapper().readValue(resolvedRequestJson, HashMap.class);

        Assert.assertEquals(36,  // "onlineOrderId": "48c3b4ff-5078-40bb-8d62-11abcbdef5b3"
                hashMap.get("onlineOrderId").length());
    }

    @Test
    public void testRandom_alpha() throws Exception {

        final String requestJsonAsString = "{\n" + "\t\"onlineOrderId\": \"${RANDOM.STRING:2}\"\n" + "}";

        final List<String> placeHolders = getTestCaseTokens(requestJsonAsString);
        Assert.assertEquals(1, placeHolders.size());

        final String resolvedRequestJson =
                jsonPreProcessor.resolveStringJson(requestJsonAsString, requestJsonAsString);
        Assert.assertEquals(-1, resolvedRequestJson.indexOf("RANDOM.STRING:"));

        final HashMap<String, String> hashMap =
                smartUtils.getMapper().readValue(resolvedRequestJson, HashMap.class);

        Assert.assertEquals(2, hashMap.get("onlineOrderId").length());
    }

    @Test
    public void testRandom_alphanumeric() throws Exception {

        final String requestJsonAsString = "{\n" + "\t\"onlineOrderId\": \"${RANDOM.ALPHANUMERIC:2}\"\n" + "}";

        final List<String> placeHolders = getTestCaseTokens(requestJsonAsString);
        Assert.assertEquals(1, placeHolders.size());

        final String resolvedRequestJson =
                jsonPreProcessor.resolveStringJson(requestJsonAsString, requestJsonAsString);
        Assert.assertEquals(-1, resolvedRequestJson.indexOf("RANDOM.ALPHANUMERIC:"));

        final HashMap<String, String> hashMap =
                smartUtils.getMapper().readValue(resolvedRequestJson, HashMap.class);

        Assert.assertEquals(2, hashMap.get("onlineOrderId").length());
    }

    @Test
    public void testIgnoreCaseWith_containsNoMatch() throws Exception {
        ScenarioSpec scenarioSpec =
                smartUtils.scenarioFileToJava(
                        "unit_test_files/ignore_case/test_string_match_withIgnoring_case.json", ScenarioSpec.class);

        final String assertionsSectionAsString =
                scenarioSpec.getSteps().get(0).getAssertions().toString();
        String mockScenarioState = "{}";

        final String resolvedAssertions =
                jsonPreProcessor.resolveStringJson(assertionsSectionAsString, mockScenarioState);
        Assert.assertTrue(
                resolvedAssertions.contains("{\"name\":\"$CONTAINS.STRING.IGNORECASE:CReASY\"}}"));

        List<JsonAsserter> asserters = jsonPreProcessor.createJsonAsserters(resolvedAssertions);
        Assert.assertEquals(2, asserters.size());

        String mockTestResponse =
                "{\n"
                        + "  \"status\": 201,\n"
                        + "  \"body\": {\n"
                        + "    \"name\": \"Hello CreXasy\"\n"
                        + "  }\n"
                        + "}";
        List<FieldAssertionMatcher> failedReports =
                jsonPreProcessor.assertAllAndReturnFailed(asserters, mockTestResponse);

        Assert.assertEquals(1, failedReports.size());
        Assert.assertTrue(
                failedReports.toString()
                        .contains(
                        "did not match the expected value 'containing sub-string with ignoring case:"));
    }

    @Test
    public void testIgnoreCaseWith_containsMatch() throws Exception {
        ScenarioSpec scenarioSpec =
                smartUtils.scenarioFileToJava(
                        "unit_test_files/ignore_case/test_string_match_withIgnoring_case.json", ScenarioSpec.class);

        final String assertionsSectionAsString =
                scenarioSpec.getSteps().get(0).getAssertions().toString();
        String mockScenarioState = "{}";

        final String resolvedAssertions =
                jsonPreProcessor.resolveStringJson(assertionsSectionAsString, mockScenarioState);
        Assert.assertTrue(
                resolvedAssertions.contains("{\"name\":\"$CONTAINS.STRING.IGNORECASE:CReASY\"}}"));

        List<JsonAsserter> asserters = jsonPreProcessor.createJsonAsserters(resolvedAssertions);
        Assert.assertEquals(2, asserters.size());

        String mockTestResponse =
                "{\n"
                        + "  \"status\": 201,\n"
                        + "  \"body\": {\n"
                        + "    \"name\": \"Hello Creasy\"\n"
                        + "  }\n"
                        + "}";
        List<FieldAssertionMatcher> failedReports =
                jsonPreProcessor.assertAllAndReturnFailed(asserters, mockTestResponse);

        Assert.assertEquals(0, failedReports.size());
    }

    @Test
    public void testString_regexMatch() throws Exception {
        ScenarioSpec scenarioSpec =
                smartUtils.scenarioFileToJava("unit_test_files/regex_match/string_matches_regex_test.json", ScenarioSpec.class);

        final String assertionsSectionAsString =
                scenarioSpec.getSteps().get(0).getAssertions().toString();
        String mockScenarioState = "{}";

        final String resolvedAssertions =
                jsonPreProcessor.resolveStringJson(assertionsSectionAsString,
                        mockScenarioState);
        Assert.assertTrue(resolvedAssertions
                .contains("{\"dob\":\"$MATCHES.STRING:\\\\d{4}-\\\\d{2}-\\\\d{2}\"}}"));

        List<JsonAsserter> asserters = jsonPreProcessor.createJsonAsserters(resolvedAssertions);
        Assert.assertEquals(2, asserters.size());

        String mockTestResponse =
                "{\n"
                        + "  \"status\": 201,\n"
                        + "  \"body\": {\n"
                        + "    \"dob\": \"2018-06-26\"\n"
                        + "  }\n"
                        + "}";
        List<FieldAssertionMatcher> failedReports =
                jsonPreProcessor.assertAllAndReturnFailed(asserters, mockTestResponse);

        Assert.assertEquals(0, failedReports.size());
    }

    @Test
    public void testArraySize_numberOnly() throws Exception {
        ScenarioSpec scenarioSpec =
                smartUtils.scenarioFileToJava(
                        "unit_test_files/array_size/array_size_number_only_test.json", ScenarioSpec.class);

        final String assertionsSectionAsString =
                scenarioSpec.getSteps().get(0).getAssertions().toString();
        String mockScenarioState = "{}";

        final String resolvedAssertions =
                jsonPreProcessor.resolveStringJson(assertionsSectionAsString, mockScenarioState);
        Assert.assertTrue(resolvedAssertions.contains("{\"persons.SIZE\":2}"));

        List<JsonAsserter> asserters = jsonPreProcessor.createJsonAsserters(resolvedAssertions);
        Assert.assertEquals(2, asserters.size());

        String mockTestResponse =
                "{\n"
                        + "    \"status\": 201,\n"
                        + "    \"body\": {\n"
                        + "        \"persons\": [\n"
                        + "            {\n"
                        + "                \"name\": \"Tom\"\n"
                        + "            },\n"
                        + "            {\n"
                        + "                \"name\": \"Mady\"\n"
                        + "            }\n"
                        + "        ]\n"
                        + "    }\n"
                        + "}";
        List<FieldAssertionMatcher> failedReports =
                jsonPreProcessor.assertAllAndReturnFailed(asserters, mockTestResponse);

        Assert.assertEquals(0, failedReports.size());
    }

    @Test
    public void testArraySize_numberOnlyNegative() throws Exception {
        ScenarioSpec scenarioSpec =
                smartUtils.scenarioFileToJava(
                        "unit_test_files/array_size/array_size_number_only_test.json", ScenarioSpec.class);

        final String assertionsSectionAsString =
                scenarioSpec.getSteps().get(0).getAssertions().toString();
        String mockScenarioState = "{}";

        final String resolvedAssertions =
                jsonPreProcessor.resolveStringJson(assertionsSectionAsString, mockScenarioState);
        Assert.assertTrue(resolvedAssertions.contains("{\"persons.SIZE\":2}"));

        List<JsonAsserter> asserters = jsonPreProcessor.createJsonAsserters(resolvedAssertions);
        Assert.assertEquals(2, asserters.size());

        String mockTestResponse =
                "{\n"
                        + "    \"status\": 201,\n"
                        + "    \"body\": {\n"
                        + "        \"persons\": [\n"
                        + "            {\n"
                        + "                \"name\": \"Tom\"\n"
                        + "            }\n"
                        + "        ]\n"
                        + "    }\n"
                        + "}";
        List<FieldAssertionMatcher> failedReports =
                jsonPreProcessor.assertAllAndReturnFailed(asserters, mockTestResponse);

        Assert.assertEquals(1, failedReports.size());
    }

    @Test
    public void testArraySize_expressionGT() throws Exception {
        ScenarioSpec scenarioSpec =
                smartUtils.scenarioFileToJava(
                        "unit_test_files/array_size/array_size_expresssion_test_GT.json", ScenarioSpec.class);

        final String assertionsSectionAsString =
                scenarioSpec.getSteps().get(0).getAssertions().toString();
        String mockScenarioState = "{}";

        final String resolvedAssertions =
                jsonPreProcessor.resolveStringJson(assertionsSectionAsString, mockScenarioState);
        Assert.assertTrue(resolvedAssertions.contains("{\"persons.SIZE\":\"$GT.1\"}"));

        List<JsonAsserter> asserters = jsonPreProcessor.createJsonAsserters(resolvedAssertions);
        Assert.assertEquals(2, asserters.size());

        String mockTestResponse =
                "{\n"
                        + "    \"status\": 201,\n"
                        + "    \"body\": {\n"
                        + "        \"persons\": [\n"
                        + "            {\n"
                        + "                \"name\": \"Tom\"\n"
                        + "            },\n"
                        + "            {\n"
                        + "                \"name\": \"Mady\"\n"
                        + "            }\n"
                        + "        ]\n"
                        + "    }\n"
                        + "}";
        List<FieldAssertionMatcher> failedReports =
                jsonPreProcessor.assertAllAndReturnFailed(asserters, mockTestResponse);

        Assert.assertEquals(0, failedReports.size());
    }

    @Test
    public void testArraySize_expressionFailTest() throws Exception {
        ScenarioSpec scenarioSpec =
                smartUtils.scenarioFileToJava(
                        "unit_test_files/array_size/array_size_expresssion_fail_test_GT.json", ScenarioSpec.class);

        final String assertionsSectionAsString =
                scenarioSpec.getSteps().get(0).getAssertions().toString();
        String mockScenarioState = "{}";

        final String resolvedAssertions =
                jsonPreProcessor.resolveStringJson(assertionsSectionAsString, mockScenarioState);
        Assert.assertTrue(resolvedAssertions.contains("{\"persons.SIZE\":\"$GT.5\"}"));

        List<JsonAsserter> asserters = jsonPreProcessor.createJsonAsserters(resolvedAssertions);
        Assert.assertEquals(2, asserters.size());

        String mockTestResponse =
                "{\n"
                        + "    \"status\": 201,\n"
                        + "    \"body\": {\n"
                        + "        \"persons\": [\n"
                        + "            {\n"
                        + "                \"name\": \"Tom\"\n"
                        + "            },\n"
                        + "            {\n"
                        + "                \"name\": \"Mady\"\n"
                        + "            }\n"
                        + "        ]\n"
                        + "    }\n"
                        + "}";
        List<FieldAssertionMatcher> failedReports =
                jsonPreProcessor.assertAllAndReturnFailed(asserters, mockTestResponse);

        Assert.assertEquals(1, failedReports.size());
        Assert.assertEquals("Assertion jsonPath '$.body.persons' with actual value '2' did not match the expected value 'Array of size $GT.5'",
                failedReports.get(0).toString());
    }

    @Test
    public void testArraySize_expressionLT() throws Exception {
        ScenarioSpec scenarioSpec =
                smartUtils.scenarioFileToJava(
                        "unit_test_files/array_size/array_size_expresssion_test_LT.json", ScenarioSpec.class);

        final String assertionsSectionAsString =
                scenarioSpec.getSteps().get(0).getAssertions().toString();
        String mockScenarioState = "{}";

        final String resolvedAssertions =
                jsonPreProcessor.resolveStringJson(assertionsSectionAsString, mockScenarioState);
        Assert.assertTrue(resolvedAssertions.contains("{\"persons.SIZE\":\"$LT.3\"}"));

        List<JsonAsserter> asserters = jsonPreProcessor.createJsonAsserters(resolvedAssertions);
        Assert.assertEquals(2, asserters.size());

        String mockTestResponse =
                "{\n"
                        + "    \"status\": 201,\n"
                        + "    \"body\": {\n"
                        + "        \"persons\": [\n"
                        + "            {\n"
                        + "                \"name\": \"Tom\"\n"
                        + "            },\n"
                        + "            {\n"
                        + "                \"name\": \"Mady\"\n"
                        + "            }\n"
                        + "        ]\n"
                        + "    }\n"
                        + "}";
        List<FieldAssertionMatcher> failedReports =
                jsonPreProcessor.assertAllAndReturnFailed(asserters, mockTestResponse);

        Assert.assertEquals(0, failedReports.size());
    }

    @Test
    public void testArraySize_expressionFailLT() throws Exception {
        ScenarioSpec scenarioSpec =
                smartUtils.scenarioFileToJava(
                        "unit_test_files/array_size/array_size_expresssion_test_fail_LT.json", ScenarioSpec.class);

        final String assertionsSectionAsString =
                scenarioSpec.getSteps().get(0).getAssertions().toString();
        String mockScenarioState = "{}";

        final String resolvedAssertions =
                jsonPreProcessor.resolveStringJson(assertionsSectionAsString, mockScenarioState);
        Assert.assertTrue(resolvedAssertions.contains("{\"persons.SIZE\":\"$LT.1\"}"));

        List<JsonAsserter> asserters = jsonPreProcessor.createJsonAsserters(resolvedAssertions);
        Assert.assertEquals(2, asserters.size());

        String mockTestResponse =
                "{\n"
                        + "    \"status\": 201,\n"
                        + "    \"body\": {\n"
                        + "        \"persons\": [\n"
                        + "            {\n"
                        + "                \"name\": \"Tom\"\n"
                        + "            },\n"
                        + "            {\n"
                        + "                \"name\": \"Mady\"\n"
                        + "            }\n"
                        + "        ]\n"
                        + "    }\n"
                        + "}";
        List<FieldAssertionMatcher> failedReports =
                jsonPreProcessor.assertAllAndReturnFailed(asserters, mockTestResponse);

        Assert.assertEquals(1, failedReports.size());
        Assert.assertEquals("Assertion jsonPath '$.body.persons' with actual value '2' did not match the expected value 'Array of size $LT.1'",
                failedReports.get(0).toString());
    }

    @Test
    public void testArraySize_expressionEQ() throws Exception {
        ScenarioSpec scenarioSpec =
                smartUtils.scenarioFileToJava(
                        "unit_test_files/array_size/array_size_expresssion_test_EQ.json", ScenarioSpec.class);

        final String assertionsSectionAsString =
                scenarioSpec.getSteps().get(0).getAssertions().toString();
        String mockScenarioState = "{}";

        final String resolvedAssertions =
                jsonPreProcessor.resolveStringJson(assertionsSectionAsString, mockScenarioState);
        Assert.assertTrue(resolvedAssertions.contains("{\"persons.SIZE\":\"$EQ.2\"}"));

        List<JsonAsserter> asserters = jsonPreProcessor.createJsonAsserters(resolvedAssertions);
        Assert.assertEquals(2, asserters.size());

        String mockTestResponse =
                "{\n"
                        + "    \"status\": 201,\n"
                        + "    \"body\": {\n"
                        + "        \"persons\": [\n"
                        + "            {\n"
                        + "                \"name\": \"Tom\"\n"
                        + "            },\n"
                        + "            {\n"
                        + "                \"name\": \"Mady\"\n"
                        + "            }\n"
                        + "        ]\n"
                        + "    }\n"
                        + "}";
        List<FieldAssertionMatcher> failedReports =
                jsonPreProcessor.assertAllAndReturnFailed(asserters, mockTestResponse);

        Assert.assertEquals(0, failedReports.size());
    }

    @Test
    public void testArraySize_expressionFailEQ() throws Exception {
        ScenarioSpec scenarioSpec =
                smartUtils.scenarioFileToJava(
                        "unit_test_files/array_size/array_size_expresssion_test_fail_EQ.json", ScenarioSpec.class);

        final String assertionsSectionAsString =
                scenarioSpec.getSteps().get(0).getAssertions().toString();
        String mockScenarioState = "{}";

        final String resolvedAssertions =
                jsonPreProcessor.resolveStringJson(assertionsSectionAsString, mockScenarioState);
        Assert.assertTrue(resolvedAssertions.contains("{\"persons.SIZE\":\"$EQ.3\"}"));

        List<JsonAsserter> asserters = jsonPreProcessor.createJsonAsserters(resolvedAssertions);
        Assert.assertEquals(2, asserters.size());

        String mockTestResponse =
                "{\n"
                        + "    \"status\": 201,\n"
                        + "    \"body\": {\n"
                        + "        \"persons\": [\n"
                        + "            {\n"
                        + "                \"name\": \"Tom\"\n"
                        + "            },\n"
                        + "            {\n"
                        + "                \"name\": \"Mady\"\n"
                        + "            }\n"
                        + "        ]\n"
                        + "    }\n"
                        + "}";
        List<FieldAssertionMatcher> failedReports =
                jsonPreProcessor.assertAllAndReturnFailed(asserters, mockTestResponse);

        Assert.assertEquals(1, failedReports.size());
        Assert.assertEquals("Assertion jsonPath '$.body.persons' with actual value '2' did not match the expected value 'Array of size $EQ.3'",
                failedReports.get(0).toString());
    }

    @Test
    public void testArraySize_expressionNotEQ() throws Exception {
        ScenarioSpec scenarioSpec =
                smartUtils.scenarioFileToJava(
                        "unit_test_files/array_size/array_size_expresssion_test_NotEQ.json", ScenarioSpec.class);

        final String assertionsSectionAsString =
                scenarioSpec.getSteps().get(0).getAssertions().toString();
        String mockScenarioState = "{}";

        final String resolvedAssertions =
                jsonPreProcessor.resolveStringJson(assertionsSectionAsString, mockScenarioState);
        Assert.assertTrue(resolvedAssertions.contains("{\"persons.SIZE\":\"$NOT.EQ.3\"}"));

        List<JsonAsserter> asserters = jsonPreProcessor.createJsonAsserters(resolvedAssertions);
        Assert.assertEquals(2, asserters.size());

        String mockTestResponse =
                "{\n"
                        + "    \"status\": 201,\n"
                        + "    \"body\": {\n"
                        + "        \"persons\": [\n"
                        + "            {\n"
                        + "                \"name\": \"Tom\"\n"
                        + "            },\n"
                        + "            {\n"
                        + "                \"name\": \"Mady\"\n"
                        + "            }\n"
                        + "        ]\n"
                        + "    }\n"
                        + "}";
        List<FieldAssertionMatcher> failedReports =
                jsonPreProcessor.assertAllAndReturnFailed(asserters, mockTestResponse);

        Assert.assertEquals(0, failedReports.size());
    }

    @Test
    public void testArraySize_expressionFailNotEQ() throws Exception {
        ScenarioSpec scenarioSpec =
                smartUtils.scenarioFileToJava(
                        "unit_test_files/array_size/array_size_expresssion_test_fail_NotEQ.json", ScenarioSpec.class);

        final String assertionsSectionAsString =
                scenarioSpec.getSteps().get(0).getAssertions().toString();
        String mockScenarioState = "{}";

        final String resolvedAssertions =
                jsonPreProcessor.resolveStringJson(assertionsSectionAsString, mockScenarioState);
        Assert.assertTrue(resolvedAssertions.contains("{\"persons.SIZE\":\"$NOT.EQ.2\"}"));

        List<JsonAsserter> asserters = jsonPreProcessor.createJsonAsserters(resolvedAssertions);
        Assert.assertEquals(2, asserters.size());

        String mockTestResponse =
                "{\n"
                        + "    \"status\": 201,\n"
                        + "    \"body\": {\n"
                        + "        \"persons\": [\n"
                        + "            {\n"
                        + "                \"name\": \"Tom\"\n"
                        + "            },\n"
                        + "            {\n"
                        + "                \"name\": \"Mady\"\n"
                        + "            }\n"
                        + "        ]\n"
                        + "    }\n"
                        + "}";
        List<FieldAssertionMatcher> failedReports =
                jsonPreProcessor.assertAllAndReturnFailed(asserters, mockTestResponse);

        Assert.assertEquals(1, failedReports.size());
        Assert.assertEquals("Assertion jsonPath '$.body.persons' with actual value '2' did not match the expected value 'Array of size $NOT.EQ.2'",
                failedReports.get(0).toString());
    }

    @Test
    public void testDateAfterBefore_both() throws Exception {
        ScenarioSpec scenarioSpec =
                smartUtils.scenarioFileToJava(
                        "unit_test_files/date_after_before/dateAfterBefore_test_both.json", ScenarioSpec.class);

        final String assertionsSectionAsString =
                scenarioSpec.getSteps().get(0).getAssertions().toString();
        String mockScenarioState = "{}";

        final String resolvedAssertions =
                jsonPreProcessor.resolveStringJson(assertionsSectionAsString, mockScenarioState);
        Assert.assertTrue(resolvedAssertions
                .contains("\"startDateTime\":\"$LOCAL.DATETIME.BEFORE:2015-09-14T09:49:34.000Z\","));
        Assert.assertTrue(resolvedAssertions
                .contains("\"endDateTime\":\"$LOCAL.DATETIME.AFTER:2015-09-14T09:49:34.000Z\""));

        List<JsonAsserter> asserters = jsonPreProcessor.createJsonAsserters(resolvedAssertions);
        Assert.assertEquals(3, asserters.size());

        String mockTestResponse =
                "{\n"
                        + "	\"status\": 200,\n"
                        + "	\"body\": {\n"
                        + "	    \"projectDetails\": {\n"
                        + "            \"startDateTime\": \"2014-09-14T09:49:34.000Z\",\n"
                        + "            \"endDateTime\": \"2016-09-14T09:49:34.000Z\"\n"
                        + "        }\n"
                        + "    }\n"
                        + "}";

        List<FieldAssertionMatcher> failedReports =
                jsonPreProcessor.assertAllAndReturnFailed(asserters, mockTestResponse);
        Assert.assertEquals(0, failedReports.size());
    }

    @Test
    public void testDateAfterBefore_fail_both() throws Exception {
        ScenarioSpec scenarioSpec =
                smartUtils.scenarioFileToJava(
                        "unit_test_files/date_after_before/dateAfterBefore_test_fail_both.json", ScenarioSpec.class);

        final String assertionsSectionAsString =
                scenarioSpec.getSteps().get(0).getAssertions().toString();
        String mockScenarioState = "{}";

        final String resolvedAssertions =
                jsonPreProcessor.resolveStringJson(assertionsSectionAsString, mockScenarioState);
        Assert.assertTrue(resolvedAssertions
                .contains("\"startDateTime\":\"$LOCAL.DATETIME.BEFORE:2016-09-14T09:49:34.000Z\","));
        Assert.assertTrue(resolvedAssertions
                .contains("\"endDateTime\":\"$LOCAL.DATETIME.AFTER:2019-09-14T09:49:34.000Z\""));

        List<JsonAsserter> asserters = jsonPreProcessor.createJsonAsserters(resolvedAssertions);
        Assert.assertEquals(3, asserters.size());

        String mockTestResponse =
                "{\n"
                        + "	\"status\": 200,\n"
                        + "	\"body\": {\n"
                        + "	    \"projectDetails\": {\n"
                        + "	            \"startDateTime\": \"2017-04-14T11:49:56.000Z\",\n"
                        + "	            \"endDateTime\": \"2018-11-12T09:39:34.000Z\"\n"
                        + "        }\n"
                        + "    }\n"
                        + "}";

        List<FieldAssertionMatcher> failedReports =
                jsonPreProcessor.assertAllAndReturnFailed(asserters, mockTestResponse);

        Assert.assertEquals(2, failedReports.size());
        Assert.assertEquals("Assertion jsonPath '$.body.projectDetails.startDateTime' with actual value '2017-04-14T11:49:56.000Z' "
                    + "did not match the expected value 'Date Before:2016-09-14T09:49:34'",
                failedReports.get(0).toString());
        Assert.assertEquals("Assertion jsonPath '$.body.projectDetails.endDateTime' with actual value '2018-11-12T09:39:34.000Z' "
                    + "did not match the expected value 'Date After:2019-09-14T09:49:34'",
                failedReports.get(1).toString());
    }

    @Test
    public void testDateAfterBefore_fail_afterSameDate() throws Exception {
        ScenarioSpec scenarioSpec =
                smartUtils.scenarioFileToJava(
                        "unit_test_files/date_after_before/dateAfterBefore_test_fail_afterSameDate.json", ScenarioSpec.class);

        final String assertionsSectionAsString =
                scenarioSpec.getSteps().get(0).getAssertions().toString();
        String mockScenarioState = "{}";

        final String resolvedAssertions =
                jsonPreProcessor.resolveStringJson(assertionsSectionAsString, mockScenarioState);
        Assert.assertTrue(resolvedAssertions
                .contains("\"startDateTime\":\"$LOCAL.DATETIME.AFTER:2015-09-14T09:49:34.000Z\""));

        List<JsonAsserter> asserters = jsonPreProcessor.createJsonAsserters(resolvedAssertions);
        Assert.assertEquals(2, asserters.size());

        String mockTestResponse =
                "{\n"
                        + "	\"status\": 200,\n"
                        + "	\"body\": {\n"
                        + "	    \"projectDetails\": {\n"
                        + "	            \"startDateTime\": \"2015-09-14T09:49:34.000Z\"\n"
                        + "        }\n"
                        + "    }\n"
                        + "}";

        List<FieldAssertionMatcher> failedReports =
                jsonPreProcessor.assertAllAndReturnFailed(asserters, mockTestResponse);

        Assert.assertEquals(1, failedReports.size());
        Assert.assertEquals("Assertion jsonPath '$.body.projectDetails.startDateTime' with actual value '2015-09-14T09:49:34.000Z' "
                        + "did not match the expected value 'Date After:2015-09-14T09:49:34'",
                failedReports.get(0).toString());
    }

    @Test
    public void testDateAfterBefore_fail_beforeSameDate() throws Exception {
        ScenarioSpec scenarioSpec =
                smartUtils.scenarioFileToJava(
                        "unit_test_files/date_after_before/dateAfterBefore_test_fail_beforeSameDate.json", ScenarioSpec.class);

        final String assertionsSectionAsString =
                scenarioSpec.getSteps().get(0).getAssertions().toString();
        String mockScenarioState = "{}";

        final String resolvedAssertions =
                jsonPreProcessor.resolveStringJson(assertionsSectionAsString, mockScenarioState);
        Assert.assertTrue(resolvedAssertions
                .contains("\"startDateTime\":\"$LOCAL.DATETIME.BEFORE:2015-09-14T09:49:34.000Z\""));

        List<JsonAsserter> asserters = jsonPreProcessor.createJsonAsserters(resolvedAssertions);
        Assert.assertEquals(2, asserters.size());

        String mockTestResponse =
                "{\n"
                        + "	\"status\": 200,\n"
                        + "	\"body\": {\n"
                        + " 		\"projectDetails\": {\n"
                        + "			\"startDateTime\": \"2015-09-14T09:49:34.000Z\"\n"
                        + "		}\n"
                        + "	}\n"
                        + "}";

        List<FieldAssertionMatcher> failedReports =
                jsonPreProcessor.assertAllAndReturnFailed(asserters, mockTestResponse);

        Assert.assertEquals(1, failedReports.size());
        Assert.assertEquals("Assertion jsonPath '$.body.projectDetails.startDateTime' with actual value '2015-09-14T09:49:34.000Z' "
                + "did not match the expected value 'Date Before:2015-09-14T09:49:34'",
                failedReports.get(0).toString());
    }

    @Test
    public void testValueOneOf_ValuePresent() throws Exception {
        ScenarioSpec scenarioSpec =
                smartUtils.scenarioFileToJava("unit_test_files/one_of/oneOf_test_currentStatus.json", ScenarioSpec.class);

        final String assertionsSectionAsString =
                scenarioSpec.getSteps().get(0).getAssertions().toString();
        String mockScenarioState = "{}";

        final String resolvedAssertions =
                jsonPreProcessor.resolveStringJson(assertionsSectionAsString, mockScenarioState);
        Assert.assertTrue(resolvedAssertions
                .contains("\"currentStatus\":\"$ONE.OF:[Found, Searching, Not Looking]\""));

        List<JsonAsserter> asserters = jsonPreProcessor.createJsonAsserters(resolvedAssertions);
        Assert.assertEquals(2, asserters.size());

        String mockTestResponse =
                "{\n"
                        + "    \"status\": 200,\n"
                        + "    \"body\": {\n"
                        + "        \"currentStatus\": \"Searching\"\n"
                        + "    }\n"
                        + "}";
        List<FieldAssertionMatcher> failedReports =
                jsonPreProcessor.assertAllAndReturnFailed(asserters, mockTestResponse);

        Assert.assertEquals(0, failedReports.size());
    }

    @Test
    public void testValueOneOf_ValueNotPresent() throws Exception {
        ScenarioSpec scenarioSpec =
                smartUtils.scenarioFileToJava("unit_test_files/one_of/oneOf_test_currentStatus.json", ScenarioSpec.class);

        final String assertionsSectionAsString =
                scenarioSpec.getSteps().get(0).getAssertions().toString();
        String mockScenarioState = "{}";

        final String resolvedAssertions =
                jsonPreProcessor.resolveStringJson(assertionsSectionAsString, mockScenarioState);
        Assert.assertTrue(resolvedAssertions
                .contains("\"currentStatus\":\"$ONE.OF:[Found, Searching, Not Looking]\""));

        List<JsonAsserter> asserters = jsonPreProcessor.createJsonAsserters(resolvedAssertions);
        Assert.assertEquals(2, asserters.size());

        String mockTestResponse =
                "{\n"
                        + "    \"status\": 200,\n"
                        + "    \"body\": {\n"
                        + "        \"currentStatus\": \"Quit\"\n"
                        + "    }\n"
                        + "}";
        List<FieldAssertionMatcher> failedReports =
                jsonPreProcessor.assertAllAndReturnFailed(asserters, mockTestResponse);

        Assert.assertEquals(1, failedReports.size());
    }

    @Test
    public void testValueOneOf_ActualResultNull() throws Exception {
        ScenarioSpec scenarioSpec =
                smartUtils.scenarioFileToJava("unit_test_files/one_of/oneOf_test_currentStatus.json", ScenarioSpec.class);

        final String assertionsSectionAsString =
                scenarioSpec.getSteps().get(0).getAssertions().toString();
        String mockScenarioState = "{}";

        final String resolvedAssertions =
                jsonPreProcessor.resolveStringJson(assertionsSectionAsString, mockScenarioState);
        Assert.assertTrue(resolvedAssertions
                .contains("\"currentStatus\":\"$ONE.OF:[Found, Searching, Not Looking]\""));

        List<JsonAsserter> asserters = jsonPreProcessor.createJsonAsserters(resolvedAssertions);
        Assert.assertEquals(2, asserters.size());

        String mockTestResponse =
                "{\n" + "    \"status\": 200,\n" + "    \"body\": {\n" + "    }\n" + "}";
        List<FieldAssertionMatcher> failedReports =
                jsonPreProcessor.assertAllAndReturnFailed(asserters, mockTestResponse);

        Assert.assertEquals(1, failedReports.size());
    }

    @Test
    public void testValueOneOf_MatchEmptyString() throws Exception {
        ScenarioSpec scenarioSpec =
                smartUtils.scenarioFileToJava("unit_test_files/one_of/oneOf_test_emptyString.json", ScenarioSpec.class);

        final String assertionsSectionAsString =
                scenarioSpec.getSteps().get(0).getAssertions().toString();
        String mockScenarioState = "{}";

        final String resolvedAssertions =
                jsonPreProcessor.resolveStringJson(assertionsSectionAsString, mockScenarioState);
        Assert.assertTrue(resolvedAssertions
                .contains("\"currentStatus\":\"$ONE.OF:[Found, Searching,, Not Looking]\""));

        List<JsonAsserter> asserters = jsonPreProcessor.createJsonAsserters(resolvedAssertions);
        Assert.assertEquals(2, asserters.size());

        String mockTestResponse =
                "{\n"
                        + "    \"status\": 200,\n"
                        + "    \"body\": {\n"
                        + "        \"currentStatus\": \"\"\n"
                        + "    }\n"
                        + "}";
        List<FieldAssertionMatcher> failedReports =
                jsonPreProcessor.assertAllAndReturnFailed(asserters, mockTestResponse);

        Assert.assertEquals(0, failedReports.size());
    }

    @Test
    public void testValueOneOf_MatchWhiteSpace() throws Exception {
        ScenarioSpec scenarioSpec =
                smartUtils.scenarioFileToJava("unit_test_files/one_of/oneOf_test_whiteSpace.json", ScenarioSpec.class);

        final String assertionsSectionAsString =
                scenarioSpec.getSteps().get(0).getAssertions().toString();
        String mockScenarioState = "{}";

        final String resolvedAssertions =
                jsonPreProcessor.resolveStringJson(assertionsSectionAsString, mockScenarioState);
        Assert.assertTrue(resolvedAssertions
                .contains("\"currentStatus\":\"$ONE.OF:[Found, Searching, , Not Looking]\""));

        List<JsonAsserter> asserters = jsonPreProcessor.createJsonAsserters(resolvedAssertions);
        Assert.assertEquals(2, asserters.size());

        String mockTestResponse =
                "{\n"
                        + "    \"status\": 200,\n"
                        + "    \"body\": {\n"
                        + "        \"currentStatus\": \" \"\n"
                        + "    }\n"
                        + "}";
        List<FieldAssertionMatcher> failedReports =
                jsonPreProcessor.assertAllAndReturnFailed(asserters, mockTestResponse);

        Assert.assertEquals(0, failedReports.size());
    }

    @Test
    public void testValueOneOf_ExpectedArrayEmpty() throws Exception {
        ScenarioSpec scenarioSpec =
                smartUtils.scenarioFileToJava("unit_test_files/one_of/oneOf_test_expectedArrayEmpty.json", ScenarioSpec.class);

        final String assertionsSectionAsString =
                scenarioSpec.getSteps().get(0).getAssertions().toString();
        String mockScenarioState = "{}";

        final String resolvedAssertions =
                jsonPreProcessor.resolveStringJson(assertionsSectionAsString, mockScenarioState);
        Assert.assertTrue(resolvedAssertions.contains("\"currentStatus\":\"$ONE.OF:[]\""));

        List<JsonAsserter> asserters = jsonPreProcessor.createJsonAsserters(resolvedAssertions);
        Assert.assertEquals(2, asserters.size());

        String mockTestResponse =
                "{\n"
                        + "    \"status\": 200,\n"
                        + "    \"body\": {\n"
                        + "        \"currentStatus\": \"Searching\"\n"
                        + "    }\n"
                        + "}";
        List<FieldAssertionMatcher> failedReports =
                jsonPreProcessor.assertAllAndReturnFailed(asserters, mockTestResponse);

        Assert.assertEquals(1, failedReports.size());
    }

    @Test
    public void testJsonPathValue_isArray() throws Exception {
        String scenarioStateJson =
                "{\n"
                        + "    \"type\": \"fuzzy\",\n"
                        + "    \"results\": [\n"
                        + "        {\n"
                        + "            \"id\": \"id-001\",\n"
                        + "            \"name\": \"Emma\"\n"
                        + "        },\n"
                        + "        {\n"
                        + "            \"id\": \"id-002\",\n"
                        + "            \"name\": \"Nikhi\"\n"
                        + "        }\n"
                        + "    ]\n"
                        + "}";
        Object jsonPathValue = JsonPath.read(scenarioStateJson, "$.results");
        Assert.assertEquals("[{\"id\":\"id-001\",\"name\":\"Emma\"},{\"id\":\"id-002\",\"name\":\"Nikhi\"}]",
                mapper.writeValueAsString(jsonPathValue));
        Assert.assertTrue(jsonPreProcessor.isPathValueJson(jsonPathValue));
    }

    @Test
    public void testJsonPathValue_isObject() throws Exception {
        String scenarioStateJson =
                "{\n"
                        + "    \"type\": \"fuzzy\",\n"
                        + "    \"results\": [\n"
                        + "        {\n"
                        + "            \"id\": \"id-001\",\n"
                        + "            \"name\": \"Emma\"\n"
                        + "        },\n"
                        + "        {\n"
                        + "            \"id\": \"id-002\",\n"
                        + "            \"name\": \"Nikhi\"\n"
                        + "        }\n"
                        + "    ]\n"
                        + "}";
        Object jsonPathValue = JsonPath.read(scenarioStateJson, "$.results[0]");
        Assert.assertEquals("{\"id\":\"id-001\",\"name\":\"Emma\"}",
                mapper.writeValueAsString(jsonPathValue));
        Assert.assertTrue(jsonPreProcessor.isPathValueJson(jsonPathValue));
    }

    @Test
    public void testJsonPathValue_isSingleField() {
        String scenarioStateJson =
                "{\n"
                        + "    \"type\": \"fuzzy\",\n"
                        + "    \"results\": [\n"
                        + "        {\n"
                        + "            \"id\": \"id-001\",\n"
                        + "            \"name\": \"Emma\"\n"
                        + "        },\n"
                        + "        {\n"
                        + "            \"id\": \"id-002\",\n"
                        + "            \"name\": \"Nikhi\"\n"
                        + "        }\n"
                        + "    ]\n"
                        + "}";
        Object jsonPathValue = JsonPath.read(scenarioStateJson, "$.type");
        Assert.assertEquals("fuzzy", jsonPathValue + "");
        Assert.assertFalse(jsonPreProcessor.isPathValueJson(jsonPathValue));
    }

    @Test
    public void testLeafValuesArray_getIndexedElement() {

        Map<String, String> paramMap = new HashMap<>();
        String scenarioState = "{\n" +
                "    \"store\": {\n" +
                "        \"book\": [\n" +
                "            {\n" +
                "                \"category\": \"reference\",\n" +
                "                \"author\": \"Nigel Rees\",\n" +
                "                \"title\": \"Sayings of the Century\",\n" +
                "                \"price\": 8.95\n" +
                "            },\n" +
                "            {\n" +
                "                \"category\": \"fiction\",\n" +
                "                \"author\": \"Evelyn Waugh\",\n" +
                "                \"title\": \"Sword of Honour\",\n" +
                "                \"price\": 12.99\n" +
                "            }\n" +
                "        ]\n" +
                "    }\n" +
                "}";


        String thisPath;
        thisPath = "$..author.$VALUE[0]";
        jsonPreProcessor.resolveLeafOnlyNodeValue(scenarioState, paramMap, thisPath);
        Assert.assertEquals("Nigel Rees", paramMap.get(thisPath));

        thisPath = "$..author.$VALUE[1]";
        jsonPreProcessor.resolveLeafOnlyNodeValue(scenarioState, paramMap, thisPath);
        Assert.assertEquals("Evelyn Waugh", paramMap.get(thisPath));
    }

    @Test
    public void testLeafValuesArray_VALUE() {

        Map<String, String> paramMap = new HashMap<>();
        String scenarioState = "{\n" +
                "    \"store\": {\n" +
                "        \"book\": [\n" +
                "            {\n" +
                "                \"category\": \"reference\",\n" +
                "                \"author\": \"Nigel Rees\",\n" +
                "                \"title\": \"Sayings of the Century\",\n" +
                "                \"price\": 8.95\n" +
                "            },\n" +
                "            {\n" +
                "                \"category\": \"fiction\",\n" +
                "                \"author\": \"Evelyn Waugh\",\n" +
                "                \"title\": \"Sword of Honour\",\n" +
                "                \"price\": 12.99\n" +
                "            }\n" +
                "        ]\n" +
                "    }\n" +
                "}";

        String thisPath;
        thisPath = "$..author.$VALUE";
        jsonPreProcessor.resolveLeafOnlyNodeValue(scenarioState, paramMap, thisPath);
        Assert.assertEquals("Nigel Rees", paramMap.get(thisPath));
    }

    @Test
    public void testLeafSingleValueArray_VALUE() {

        Map<String, String> paramMap = new HashMap<>();
        String scenarioState = "{\n" +
                "    \"store\": {\n" +
                "        \"book\": [\n" +
                "            {\n" +
                "                \"category\": \"reference\",\n" +
                "                \"author\": \"Nigel Rees\",\n" +
                "                \"title\": \"Sayings of the Century\",\n" +
                "                \"price\": 8.95\n" +
                "            }" +
                "        ]\n" +
                "    }\n" +
                "}";

        String thisPath;
        thisPath = "$..author.$VALUE";
        jsonPreProcessor.resolveLeafOnlyNodeValue(scenarioState, paramMap, thisPath);
        Assert.assertEquals("Nigel Rees", paramMap.get(thisPath));
    }

    @Test
    public void testLeafValuesArray_badIndex() {

        Map<String, String> paramMap = new HashMap<>();
        String scenarioState = "{\n" +
                "    \"store\": {\n" +
                "        \"book\": [\n" +
                "            {\n" +
                "                \"category\": \"reference\",\n" +
                "                \"author\": \"Nigel Rees\",\n" +
                "                \"title\": \"Sayings of the Century\",\n" +
                "                \"price\": 8.95\n" +
                "            },\n" +
                "            {\n" +
                "                \"category\": \"fiction\",\n" +
                "                \"author\": \"Evelyn Waugh\",\n" +
                "                \"title\": \"Sword of Honour\",\n" +
                "                \"price\": 12.99\n" +
                "            }\n" +
                "        ]\n" +
                "    }\n" +
                "}";

        String thisPath;
        thisPath = "$..author.$VALUE[3]";

        Assert.assertThrows("Index: 3, Size: 2",
                IndexOutOfBoundsException.class,
                () -> jsonPreProcessor.resolveLeafOnlyNodeValue(scenarioState, paramMap, thisPath));
    }

    @Test(expected = RuntimeException.class)
    public void test_wrongJsonPathBy_JSONCONTENT_Exception() throws JsonProcessingException {
        String jsonAsString = readJsonAsString("unit_test_files/json_content_unit_test/json_step_test_wrong_json_path.json");
        Map<String, Object> map = mapper.readValue(jsonAsString, new TypeReference<Map<String, Object>>() {});

        jsonPreProcessor.digReplaceContent(map, new ScenarioExecutionState());
    }

    @Test
    public void test_JSONCONTENT_leafNode() throws IOException {
        ScenarioExecutionState scenarioExecutionState = new ScenarioExecutionState();

        final StepExecutionState step1 =  createStepWithRequestAndResponse("create_emp", "\"body\" : {\n    \"id\" : 39001,\n    \"ldapId\" : \"emmanorton\"\n  }\n}\n  }");
        scenarioExecutionState.addStepState(step1);

        ScenarioSpec scenarioSpec =
            smartUtils.scenarioFileToJava(
                "unit_test_files/json_content_unit_test/json_step_test_json_content.json", ScenarioSpec.class);
        Step thisStep = scenarioSpec.getSteps().get(1);
        JsonNode stepNode = mapper.convertValue(thisStep, JsonNode.class);
        Map<String, Object> map = mapper.readValue(stepNode.toString(), new TypeReference<Map<String, Object>>() {});

        jsonPreProcessor.digReplaceContent(map, scenarioExecutionState);

        String jsonResult = mapper.writeValueAsString(map);

        Assert.assertEquals(39001, (int)JsonPath.read(jsonResult, "$.request.body.addressId"));
    }


    @Test
    public void test_JSONCONTENT_stringArray() throws IOException {
        ScenarioExecutionState scenarioExecutionState = new ScenarioExecutionState();

        final StepExecutionState step1 =  createStepWithRequestAndResponse("create_emp", "\"body\": {\"id\": 38001,\n     \"names\": [\"test1\", \"test2\"]\n}");
        scenarioExecutionState.addStepState(step1);

        ScenarioSpec scenarioSpec =
            smartUtils.scenarioFileToJava(
                "unit_test_files/json_content_unit_test/json_step_test_json_content_array.json", ScenarioSpec.class);
        Step thisStep = scenarioSpec.getSteps().get(1);
        JsonNode stepNode = mapper.convertValue(thisStep, JsonNode.class);
        Map<String, Object> map = mapper.readValue(stepNode.toString(), new TypeReference<Map<String, Object>>() {});

        jsonPreProcessor.digReplaceContent(map, scenarioExecutionState);

        String jsonResult = mapper.writeValueAsString(map);

        String result = "[\"test1\",\"test2\"]";

        Object jsonPathValue = JsonPath.parse(jsonResult).read("$.request.body.names");

        Assert.assertEquals(result, this.mapper.writeValueAsString(jsonPathValue));
    }

    @Test
    public void test_JSONCONTENT_objectArray() throws IOException {
        ScenarioExecutionState scenarioExecutionState = new ScenarioExecutionState();
        /*
         * {
         *     "id": 38001,
         *     "allAddresses": [
         *         {
         *             "type": "Home",
         *             "line1": "North Lon",
         *             "id": 43
         *         },
         *         {
         *             "type": "Office",
         *             "line1": "Central Lon"
         *         }
         *     ]
         * }
         */
        final StepExecutionState step1 =  createStepWithRequestAndResponse("create_emp",
                "\"body\": {\"id\": 38001, \"allAddresses\": [{\"type\": \"Home\", \"line1\": \"North Lon\", \"id\": 47}, {\"type\": \"Office\", \"line1\": \"Central Lon\"}]}");
        scenarioExecutionState.addStepState(step1);

        ScenarioSpec scenarioSpec =
            smartUtils.scenarioFileToJava(
                "unit_test_files/json_content_unit_test/json_step_test_json_content_objectarray.json", ScenarioSpec.class);

        Step thisStep = scenarioSpec.getSteps().get(1);
        JsonNode stepNode = mapper.convertValue(thisStep, JsonNode.class);
        Map<String, Object> map = mapper.readValue(stepNode.toString(), new TypeReference<Map<String, Object>>() {});

        jsonPreProcessor.digReplaceContent(map, scenarioExecutionState);

        String jsonResult = mapper.writeValueAsString(map);

        Assert.assertEquals(47, (int)JsonPath.read(jsonResult, "$.request.body.allAddresses[0].id"));
        Assert.assertEquals("Home", JsonPath.read(jsonResult, "$.request.body.allAddresses[0].type"));
        Assert.assertEquals("Office", JsonPath.read(jsonResult, "$.request.body.allAddresses[1].type"));
        Assert.assertEquals("North Lon", JsonPath.read(jsonResult, "$.request.body.allAddresses[0].line1"));
        Assert.assertEquals("Central Lon", JsonPath.read(jsonResult, "$.request.body.allAddresses[1].line1"));
    }

    @Test
    public void test_JSONCONTENT_jsonBlock() throws IOException {
        ScenarioExecutionState scenarioExecutionState = new ScenarioExecutionState();

        final StepExecutionState step1 =  createStepWithRequestAndResponse("create_emp",
                "\"body\": {\n" +
                        "    \"id\": 38001,\n" +
                        "    \"address\": {\n" +
                        "        \"type\": \"Home\",\n" +
                        "        \"line1\": \"River Side\"\n" +
                        "    }\n" +
                        "}");
        scenarioExecutionState.addStepState(step1);

        ScenarioSpec scenarioSpec =
            smartUtils.scenarioFileToJava(
                "unit_test_files/json_content_unit_test/json_step_test_json_content_block.json", ScenarioSpec.class);
        Step thisStep = scenarioSpec.getSteps().get(1);
        JsonNode stepNode = mapper.convertValue(thisStep, JsonNode.class);
        Map<String, Object> map = mapper.readValue(stepNode.toString(), new TypeReference<Map<String, Object>>() {});

        jsonPreProcessor.digReplaceContent(map, scenarioExecutionState);

        String jsonResult = mapper.writeValueAsString(map);

        Assert.assertEquals("Home", JsonPath.read(jsonResult, "$.request.body.address.type"));
        Assert.assertEquals("River Side", JsonPath.read(jsonResult, "$.request.body.address.line1"));
    }

    @Test
    public void test_NoJSONContentCheckDigNeeded() throws IOException {
        String jsonAsString = readJsonAsString("unit_test_files/json_content_unit_test/json_step_no_json_content_test.json");
        Step step = mapper.readValue(jsonAsString, Step.class);
        Assert.assertFalse(checkDigNeeded(mapper, step, ZeroCodeValueTokens.JSON_CONTENT));


        ScenarioSpec scenarioSpec =
            smartUtils.scenarioFileToJava(
                "unit_test_files/json_content_unit_test/json_step_test_json_content.json", ScenarioSpec.class);
        step = scenarioSpec.getSteps().get(1);
        Assert.assertTrue(checkDigNeeded(mapper, step, ZeroCodeValueTokens.JSON_CONTENT));
    }

    @Test
    public void test_textNode() throws IOException {
        String jsonAsString = readJsonAsString("unit_test_files/filebody_unit_test/json_step_text_request.json");
        Step step = mapper.readValue(jsonAsString, Step.class);

        jsonPreProcessor.resolveJsonContent(step, new ScenarioExecutionState());
        String resultJsonStep = mapper.writeValueAsString(step);

        Assert.assertEquals("I am a simple text", read(resultJsonStep, "$.request"));
    }


    @SuppressWarnings("SameParameterValue")
    protected StepExecutionState createStepWithRequestAndResponse(String stepName, String body) {
        StepExecutionState stepExecutionState = new StepExecutionState();
        stepExecutionState.addStep(TestUtility.createDummyStep(stepName));
        stepExecutionState.addRequest("{\n" +
                "    \"customer\": {\n" +
                "        \"firstName\": \"FIRST_NAME\"\n" +
                "    }\n" +
                "}");
        stepExecutionState.addResponse("{\n" +
                body +
                "}");
        return stepExecutionState;
    }

    @Test
    public void getPathSizeAsserterInvalidTypeTest() {
        Object value = LocalDateTime.now();
        String path = "$.foo.SIZE";
        Assert.assertThrows(String.format("Oops! Unsupported value for .SIZE: %s", value),
                RuntimeException.class,
                () -> jsonPreProcessor.getPathSizeAsserter(path, value));
    }
}
