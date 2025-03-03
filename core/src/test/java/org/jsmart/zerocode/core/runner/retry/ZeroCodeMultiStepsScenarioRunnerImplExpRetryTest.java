package org.jsmart.zerocode.core.runner;

import org.jsmart.zerocode.core.domain.Retry;
import org.jsmart.zerocode.core.domain.ScenarioSpec;
import org.jsmart.zerocode.core.domain.Step;
import org.jsmart.zerocode.core.engine.preprocessor.ScenarioExecutionState;
import org.jsmart.zerocode.core.engine.preprocessor.ZeroCodeAssertionsProcessor;
import org.jsmart.zerocode.core.engine.preprocessor.ZeroCodeExternalFileProcessor;
import org.jsmart.zerocode.core.engine.preprocessor.ZeroCodeParameterizedProcessor;
import org.jsmart.zerocode.core.engine.sorter.ZeroCodeSorter;
import org.jsmart.zerocode.core.engine.validators.ZeroCodeValidator;
import org.jsmart.zerocode.core.logbuilder.ZerocodeCorrelationshipLogger;
import org.jsmart.zerocode.core.runner.ZeroCodeMultiStepsScenarioRunnerImpl;
import org.jsmart.zerocode.core.utils.ApiTypeUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import static org.mockito.Mockito.*;

public class ZeroCodeMultiStepsScenarioRunnerImplExpRetryTest {

    @Mock
    private ZeroCodeAssertionsProcessor zeroCodeAssertionsProcessor;

    @Mock
    private ZeroCodeExternalFileProcessor extFileProcessor;

    @Mock
    private ZeroCodeParameterizedProcessor parameterizedProcessor;

    @Mock
    private ZeroCodeSorter sorter;

    @Mock
    private ApiTypeUtils apiTypeUtils;

    @Mock
    private ZeroCodeValidator validator;

    @Mock
    private ZerocodeCorrelationshipLogger correlLogger;

    @InjectMocks
    private ZeroCodeMultiStepsScenarioRunnerImpl runner;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testExponentialBackoff() throws Exception {
        // Arrange
        ScenarioSpec scenarioSpec = mock(ScenarioSpec.class);
        Step step = mock(Step.class);
        when(step.getRetry()).thenReturn(new Retry(3, 1000, "exponential", Collections.emptyList()));
        when(step.getAssertions()).thenReturn(null);
        when(step.getIgnoreStep()).thenReturn(false);
        when(scenarioSpec.getSteps()).thenReturn(Collections.singletonList(step));
        when(parameterizedProcessor.resolveParameterized(any(), anyInt())).thenReturn(scenarioSpec);
        when(zeroCodeAssertionsProcessor.resolveJsonContent(any(), any())).thenReturn(step);
        when(extFileProcessor.resolveExtJsonFile(any())).thenReturn(step);

        ScenarioExecutionState scenarioExecutionState = new ScenarioExecutionState();
        RunNotifier notifier = new RunNotifier();
        Description description = Description.createTestDescription("test", "testExponentialBackoff");

        // Act
        runner.runScenario(scenarioSpec, notifier, description);

        // Assert
        verify(runner, times(3)).waitForDelay(anyInt());
    }
}