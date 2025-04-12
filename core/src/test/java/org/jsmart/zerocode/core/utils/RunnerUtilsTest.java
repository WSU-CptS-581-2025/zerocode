package org.jsmart.zerocode.core.utils;

import org.junit.Test;

import com.google.inject.Injector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.jsmart.zerocode.core.utils.RunnerUtils.getFullyQualifiedUrl;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.jsmart.zerocode.core.domain.TargetEnv;
import org.jsmart.zerocode.core.domain.UseHttpClient;
import org.jsmart.zerocode.core.domain.UseKafkaClient;
import org.jsmart.zerocode.core.httpclient.BasicHttpClient;
import org.jsmart.zerocode.core.httpclient.ssl.SslTrustHttpClient;
import org.jsmart.zerocode.core.kafka.client.BasicKafkaClient;
import org.jsmart.zerocode.core.kafka.client.ZerocodeCustomKafkaClient;

public class RunnerUtilsTest {
    @UseHttpClient(BasicHttpClient.class)
    static class TestClassWithAnnotation {}
    
    static class TestClassWithoutAnnotation {}

    @UseKafkaClient(ZerocodeCustomKafkaClient.class)
    static class TestClassWithKafkaAnnotation {}

    static class TestClassWithoutKafkaAnnotation {}

    @UseHttpClient(BasicHttpClient.class)
    static class TestClassWithHttpClientAnnotation {}

    static class TestClassWithoutHttpClientAnnotation {}
    
    @UseKafkaClient(BasicKafkaClient.class)
    static class TestClassWithKafkaClientAnnotation {}

    static class TestClassWithoutKafkaClientAnnotation {}

    @TargetEnv("test_env.properties")
    static class TestClassWithTargetEnv {}

    static class TestClassWithoutTargetEnv {}
    
    @Test
    public void testSuffixEnvValue() throws Exception {

        assertThat(RunnerUtils.suffixEnvValue("abcd.properties", "_ci"), is("abcd_ci.properties"));

        assertThat(RunnerUtils.suffixEnvValue("my_conf.properties", "_ci"), is("my_conf_ci.properties"));

        assertThat(RunnerUtils.suffixEnvValue("myfolder/myapp.properties", "_ci"), is("myfolder/myapp_ci.properties"));

        assertThat(RunnerUtils.suffixEnvValue("/myfolder/myapp.properties", "_ci"), is("/myfolder/myapp_ci.properties"));

        assertThat(RunnerUtils.suffixEnvValue("/myfolder/myapp.properties", ""), is("/myfolder/myapp.properties"));

    }

    @Test
    public void test_Fqdn() {
        String fullyQualifiedUrl = getFullyQualifiedUrl("/abc",
                "http://aws-host.com",
                "8080",
                "/acontext");
        assertThat(fullyQualifiedUrl, is("http://aws-host.com:8080/acontext/abc"));

        // Host having http/https
        fullyQualifiedUrl = getFullyQualifiedUrl("http://aws-host.com:8080/acontext/abc",
                "asdf",
                "9090",
                "asdf");
        assertThat(fullyQualifiedUrl, is("http://aws-host.com:8080/acontext/abc"));

        // Without port -http
        fullyQualifiedUrl = getFullyQualifiedUrl("/abc",
                "http://aws-host.com",
                "",
                "/acontext");
        assertThat(fullyQualifiedUrl, is("http://aws-host.com/acontext/abc"));

        // Without port -https
        fullyQualifiedUrl = getFullyQualifiedUrl("/abc",
                "https://aws-host.com",
                "",
                "/acontext");
        assertThat(fullyQualifiedUrl, is("https://aws-host.com/acontext/abc"));

        // Without context
        fullyQualifiedUrl = getFullyQualifiedUrl("/abc",
                "https://aws-host.com",
                "",
                "");
        assertThat(fullyQualifiedUrl, is("https://aws-host.com/abc"));

    }

    @Test
    public void testGetUseHttpClient_withAnnotation() {
        UseHttpClient annotation = RunnerUtils.getUseHttpClient(TestClassWithAnnotation.class);
        assertThat(annotation.value(), is((Object) BasicHttpClient.class));
    }
    
    @Test
    public void testGetUseHttpClient_withoutAnnotation() {
        UseHttpClient annotation = RunnerUtils.getUseHttpClient(TestClassWithoutAnnotation.class);
        assertThat(annotation, is((UseHttpClient) null));
    }

    @Test
    public void testGetUseKafkaClient_withAnnotation() {
        UseKafkaClient annotation = RunnerUtils.getUseKafkaClient(TestClassWithKafkaAnnotation.class);
        assertThat(annotation.value(), is((Object) ZerocodeCustomKafkaClient.class));
    }

    @Test
    public void testGetUseKafkaClient_withoutAnnotation() {
        UseKafkaClient annotation = RunnerUtils.getUseKafkaClient(TestClassWithoutKafkaAnnotation.class);
        assertThat(annotation, is((UseKafkaClient) null));
    }
    
    @Test
    public void testCreateCustomHttpClientOrDefault_withAnnotation() {
        Class<? extends BasicHttpClient> httpClientClass = RunnerUtils.createCustomHttpClientOrDefault(TestClassWithHttpClientAnnotation.class);
        assertThat(httpClientClass, is((Object) BasicHttpClient.class));
    }

    @Test
    public void testCreateCustomHttpClientOrDefault_withoutAnnotation() {
        Class<? extends BasicHttpClient> httpClientClass = RunnerUtils.createCustomHttpClientOrDefault(TestClassWithoutHttpClientAnnotation.class);
        assertThat(httpClientClass.getName(), is(SslTrustHttpClient.class.getName())); // Default value
    }

    @Test
    public void testCreateCustomKafkaClientOrDefault_withAnnotation() {
        Class<? extends BasicKafkaClient> kafkaClientClass = RunnerUtils.createCustomKafkaClientOrDefault(TestClassWithKafkaClientAnnotation.class);
        assertThat(kafkaClientClass, is((Object) BasicKafkaClient.class));
    }

    @Test
    public void testCreateCustomKafkaClientOrDefault_withoutAnnotation() {
        Class<? extends BasicKafkaClient> kafkaClientClass = RunnerUtils.createCustomKafkaClientOrDefault(TestClassWithoutKafkaClientAnnotation.class);
        assertThat(kafkaClientClass, is((Object) ZerocodeCustomKafkaClient.class)); // Default value
    }

    @Test
    public void testGetMainModuleInjector_withoutTargetEnvAnnotation() {
        // Arrange
        Class<?> testClass = TestClassWithoutTargetEnv.class;

        // Act
        Injector injector = RunnerUtils.getMainModuleInjector(testClass);

        // Assert
        assertNotNull(injector);
        // Verify that the default environment file is used
    }
}