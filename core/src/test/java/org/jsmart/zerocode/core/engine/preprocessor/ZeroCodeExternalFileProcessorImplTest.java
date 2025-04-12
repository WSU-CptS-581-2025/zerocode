package org.jsmart.zerocode.core.engine.preprocessor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsmart.zerocode.core.di.provider.ObjectMapperProvider;
import org.jsmart.zerocode.core.domain.Step;
import org.jsmart.zerocode.core.engine.tokens.ZeroCodeValueTokens;
import org.junit.Assert;
import org.junit.Test;


import java.io.IOException;
import java.util.Map;

import static com.jayway.jsonpath.JsonPath.read;
import static org.jsmart.zerocode.core.utils.SmartUtils.checkDigNeeded;
import static org.jsmart.zerocode.core.utils.SmartUtils.readJsonAsString;

public class ZeroCodeExternalFileProcessorImplTest {
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private final ZeroCodeExternalFileProcessorImpl externalFileProcessor = new ZeroCodeExternalFileProcessorImpl(objectMapper);

    @Test
    public void test_wrongFileException() throws IOException {
        String jsonAsString = readJsonAsString("unit_test_files/filebody_unit_test/json_step_test_wrong_file_ref.json");
        Map<String, Object> map = objectMapper.readValue(jsonAsString, new TypeReference<Map<String, Object>>() {});

        Assert.assertThrows(RuntimeException.class, () -> externalFileProcessor.digReplaceContent(map));
    }

    @Test
    public void test_deepHashMapTraverse() throws IOException {
        String jsonAsString = readJsonAsString("unit_test_files/filebody_unit_test/json_step_test_file.json");
        Map<String, Object> map = objectMapper.readValue(jsonAsString, new TypeReference<Map<String, Object>>() {});

        externalFileProcessor.digReplaceContent(map);
        String resultJson = objectMapper.writeValueAsString(map);

        Assert.assertEquals("Emp-No-${RANDOM.NUMBER}", read(resultJson, "$.request.body.id"));
        Assert.assertEquals("passwwrd", read(resultJson, "$.request.headers.secret"));
        Assert.assertEquals(16, (int)read(resultJson, "$.assertions.body.age"));
    }

    @Test
    public void test_deepRecursiveFile() throws IOException {
        String jsonAsString = readJsonAsString("unit_test_files/filebody_unit_test/json_step_test_file_recursive.json");
        Map<String, Object> map = objectMapper.readValue(jsonAsString, new TypeReference<Map<String, Object>>() {});

        externalFileProcessor.digReplaceContent(map);
        String resultJson = objectMapper.writeValueAsString(map);

        Assert.assertEquals("corp-office", read(resultJson, "$.request.body.addresses[0].type"));
        Assert.assertEquals("hr-office", read(resultJson, "$.request.body.addresses[1].type"));
    }

    @Test
    public void test_addressArray() throws IOException {
        String jsonAsString = readJsonAsString("unit_test_files/filebody_unit_test/json_step_test_address_array.json");
        Map<String, Object> map = objectMapper.readValue(jsonAsString, new TypeReference<Map<String, Object>>() {});

        externalFileProcessor.digReplaceContent(map);
        String resultJson = objectMapper.writeValueAsString(map);

        Assert.assertEquals("corp-office", read(resultJson, "$.request.body.addresses[0].type"));
        Assert.assertEquals("hr-office", read(resultJson, "$.request.body.addresses[1].type"));
    }

    @Test
    public void test_NoExtJsonFile() throws IOException {
        String jsonAsString = readJsonAsString("unit_test_files/filebody_unit_test/json_step_no_ext_json_test_file.json");
        Map<String, Object> map = objectMapper.readValue(jsonAsString, new TypeReference<Map<String, Object>>() {});

        externalFileProcessor.digReplaceContent(map);
        String resultJson = objectMapper.writeValueAsString(map);

        Assert.assertEquals("Emma", read(resultJson, "$.request.body.name"));
        Assert.assertEquals(100, (int)read(resultJson, "$.assertions.body.id"));
    }

    @Test
    public void test_NoExtFileCheckDigNeeded() throws IOException {
        String jsonAsString = readJsonAsString("unit_test_files/filebody_unit_test/json_step_no_ext_json_test_file.json");
        Step step = objectMapper.readValue(jsonAsString, Step.class);
        Assert.assertFalse(checkDigNeeded(objectMapper, step, ZeroCodeValueTokens.JSON_PAYLOAD_FILE, ZeroCodeValueTokens.YAML_PAYLOAD_FILE));

        jsonAsString = readJsonAsString("unit_test_files/filebody_unit_test/json_step_text_node_ext_json_file_test.json");
        step = objectMapper.readValue(jsonAsString, Step.class);
        Assert.assertTrue(checkDigNeeded(objectMapper, step, ZeroCodeValueTokens.JSON_PAYLOAD_FILE, ZeroCodeValueTokens.YAML_PAYLOAD_FILE));
    }

    @Test
    public void test_textNode() throws IOException {
        String jsonAsString = readJsonAsString("unit_test_files/filebody_unit_test/json_step_text_request.json");
        Step step = objectMapper.readValue(jsonAsString, Step.class);

        externalFileProcessor.resolveExtJsonFile(step);
        String resultJsonStep = objectMapper.writeValueAsString(step);

        Assert.assertEquals("I am a simple text", read(resultJsonStep, "$.request"));
    }

    @Test
    public void test_textNodeExtFile() throws IOException {
        String jsonAsString = readJsonAsString("unit_test_files/filebody_unit_test/json_step_text_node_ext_json_file_test.json");
        Step step = objectMapper.readValue(jsonAsString, Step.class);

        Step effectiveStep = externalFileProcessor.resolveExtJsonFile(step);
        String resultJsonStep = objectMapper.writeValueAsString(effectiveStep);

        Assert.assertEquals("Emp-No-${RANDOM.NUMBER}", read(resultJsonStep, "$.request.body.id"));
        Assert.assertEquals("hello key", read(resultJsonStep, "$.request.headers.api_key"));
    }
}
