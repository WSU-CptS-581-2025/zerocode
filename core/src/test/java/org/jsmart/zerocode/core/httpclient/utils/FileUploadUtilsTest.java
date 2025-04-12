package org.jsmart.zerocode.core.httpclient.utils;

import org.junit.Test;
import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FileUploadUtilsTest {

    @Test
    public void testGetFileFieldNameValue_parsesJsonToMap() throws IOException {
    String json = "{\"files\":[\"file1:/tmp/test.txt\"],\"name\":\"test\"}";
    Map<String, Object> result = FileUploadUtils.getFileFieldNameValue(json);

    assertEquals("test", result.get("name"));
    assertTrue(result.containsKey("files"));
    }

    @Test
    public void testIsReservedKey_returnsTrueForReservedKeys() {
        assertTrue(FileUploadUtils.isReservedKey("files"));
        assertTrue(FileUploadUtils.isReservedKey("boundary"));
    }

    @Test
    public void testIsReservedKey_returnsFalseForNormalKeys() {
        assertFalse(FileUploadUtils.isReservedKey("username"));
        assertFalse(FileUploadUtils.isReservedKey("email"));
    }
}