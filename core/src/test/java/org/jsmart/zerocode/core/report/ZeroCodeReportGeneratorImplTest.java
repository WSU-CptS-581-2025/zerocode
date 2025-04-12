package org.jsmart.zerocode.core.report;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsmart.zerocode.core.di.provider.ObjectMapperProvider;
import org.jsmart.zerocode.core.domain.reports.ZeroCodeExecResult;
import org.jsmart.zerocode.core.domain.reports.ZeroCodeReport;
import org.jsmart.zerocode.core.domain.reports.ZeroCodeReportStep;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.jsmart.zerocode.core.constants.ZeroCodeReportConstants.RESULT_FAIL;
import static org.jsmart.zerocode.core.constants.ZeroCodeReportConstants.RESULT_PASS;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ZeroCodeReportGeneratorImplTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();


    private ZeroCodeReportGeneratorImpl zeroCodeReportGenerator;

    ObjectMapper mapper = new ObjectMapperProvider().get();

    @Before
    public void setItUp() throws Exception {

        zeroCodeReportGenerator = new ZeroCodeReportGeneratorImpl(mapper);

    }

    @Test
    public void testReportFolderNotPresentInTarget_validation() throws Exception {
        final String reportsFolder = "/target/helloooo";

        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("Somehow the '/target/helloooo' is not present or has no report JSON files");
        expectedException.expectMessage("1) No tests were activated or made to run via ZeroCode runner.");
        zeroCodeReportGenerator.readZeroCodeReportsByPath(reportsFolder);

    }

    @Ignore("mvn clean install - removes target folder. So this passes when run without 'clean'" +
            "To fix it create a temp folder, assign to reportsFolder variable and run")
    @Test
    public void testReportFolderPresentInTargetNormalFlow() throws Exception {
        final String reportsFolder = "target/zerocode-test-reports";

        zeroCodeReportGenerator.validateReportsFolderAndTheFilesExists(reportsFolder);

    }


    @Test
    public void testAuthorJiraStyle_LEGACYMARKER() throws Exception {
        List<String> authors;

        // OLD - Deprecated
        authors = zeroCodeReportGenerator.optionalAuthors("PayPal One touch payment @@Peter");
        assertThat(authors.get(0), is("@@Peter"));

        authors = zeroCodeReportGenerator.optionalAuthors("PayPal One touch payment");
        assertThat(authors.size(), is(0));

        authors = zeroCodeReportGenerator.optionalAuthors("PayPal One touch @@payment @@Peter");
        assertThat(authors.get(0), is("@@payment"));
        assertThat(authors.get(1), is("@@Peter"));
        assertThat(authors.size(), is(2));

        authors = zeroCodeReportGenerator.optionalAuthors("PayPal One touch payment @@Peter-Gibson");
        assertThat(authors.get(0), is("@@Peter-Gibson"));

        authors = zeroCodeReportGenerator.optionalAuthors("PayPal One touch payment @@Peter Gibson");
        assertThat(authors.get(0), is("@@Peter"));

        authors = zeroCodeReportGenerator.optionalAuthors("@@Peter- PayPal One touch payment ");
        assertThat(authors.get(0), is("@@Peter-"));
    }

    @Test
    public void testAuthorJiraStyle_new() throws Exception {
        List<String> authors;

        // OLD - Deprecated
        authors = zeroCodeReportGenerator.optionalAuthors("PayPal One touch payment @Peter");
        assertThat(authors.get(0), is("@Peter"));

        authors = zeroCodeReportGenerator.optionalAuthors("PayPal One touch payment");
        assertThat(authors.size(), is(0));

        authors = zeroCodeReportGenerator.optionalAuthors("PayPal One touch @payment @Peter");
        assertThat(authors.get(0), is("@payment"));
        assertThat(authors.get(1), is("@Peter"));
        assertThat(authors.size(), is(2));

        authors = zeroCodeReportGenerator.optionalAuthors("PayPal One touch payment @Peter-Gibson");
        assertThat(authors.get(0), is("@Peter-Gibson"));

        authors = zeroCodeReportGenerator.optionalAuthors("PayPal One touch payment @Peter Gibson");
        assertThat(authors.get(0), is("@Peter"));

        authors = zeroCodeReportGenerator.optionalAuthors("@Peter- PayPal One touch payment ");
        assertThat(authors.get(0), is("@Peter-"));

    }

    @Test
    public void testCategoryHashTag() throws Exception {
        List<String> categories;

        categories = zeroCodeReportGenerator.optionalCategories("PayPal One touch payment #Smoke");
        assertThat(categories.get(0), is("#Smoke"));
        assertThat(categories.size(), is(1));

        categories = zeroCodeReportGenerator.optionalCategories("PayPal One touch #Smoke #PDC");
        assertThat(categories.get(0), is("#Smoke"));
        assertThat(categories.get(1), is("#PDC"));
        assertThat(categories.size(), is(2));

        categories = zeroCodeReportGenerator.optionalCategories("PayPal One touch payment #SIT_Smoke");
        assertThat(categories.get(0), is("#SIT_Smoke"));

        categories = zeroCodeReportGenerator.optionalCategories("PayPal One touch payment #PDC-Gibson");
        assertThat(categories.get(0), is("#PDC-Gibson"));

        categories = zeroCodeReportGenerator.optionalCategories("PayPal One touch payment #PDC");
        assertThat(categories.get(0), is("#PDC"));

        categories = zeroCodeReportGenerator.optionalCategories("#PDC, PayPal One touch payment ");
        assertThat(categories.get(0), is("#PDC,"));

        categories = zeroCodeReportGenerator.optionalCategories("PayPal One touch payment");
        assertThat(categories.size(), is(0));

    }

    @Test
    public void testGettingUniqueStepsForMultipleRetries(){
        List<ZeroCodeReportStep> steps = new ArrayList<ZeroCodeReportStep>(){
            {
                add(new ZeroCodeReportStep("testCorrelationId",RESULT_PASS));
                add(new ZeroCodeReportStep("testCorrelationId",RESULT_FAIL));
            }
        };
        List<ZeroCodeReportStep> uniqueSteps = zeroCodeReportGenerator.getUniqueSteps(steps);
        assertEquals(uniqueSteps.size() , 1);
        assertEquals(uniqueSteps.get(0).getResult(),RESULT_PASS);
    }

    @Test
    public void testGettingUniqueStepsForNoRetries(){
        List<ZeroCodeReportStep> steps = new ArrayList<ZeroCodeReportStep>(){
            {
                add(new ZeroCodeReportStep("testCorrelationId1",RESULT_PASS));
                add(new ZeroCodeReportStep("testCorrelationId2",RESULT_FAIL));
                add(new ZeroCodeReportStep("testCorrelationId3",RESULT_FAIL));
            }
        };
        List<ZeroCodeReportStep> uniqueSteps = zeroCodeReportGenerator.getUniqueSteps(steps);
        assertEquals(uniqueSteps.size() , 3);
        assertEquals(uniqueSteps.stream().filter(step->step.getResult().equals(RESULT_PASS)).count(),1);
        assertEquals(uniqueSteps.stream().filter(step->step.getResult().equals(RESULT_FAIL)).count(),2);

        assertThat(uniqueSteps.get(0).getCorrelationId(),is("testCorrelationId1"));
        assertThat(uniqueSteps.get(1).getCorrelationId(),is("testCorrelationId2"));
        assertThat(uniqueSteps.get(2).getCorrelationId(),is("testCorrelationId3"));

    }

    @Test
    public void shouldProcessReportAndAssignMetadata() {
        // Arrange
        ZeroCodeReport mockReport = mock(ZeroCodeReport.class);
        ZeroCodeExecResult mockScenario = mock(ZeroCodeExecResult.class);
        ExtentReports mockExtentReports = mock(ExtentReports.class);
        ExtentTest mockTest = mock(ExtentTest.class);

        when(mockReport.getResults()).thenReturn(Collections.singletonList(mockScenario));
        when(mockScenario.getScenarioName()).thenReturn("Test Scenario");
        when(mockExtentReports.createTest(anyString())).thenReturn(mockTest);

        // Act
        zeroCodeReportGenerator.processReport(mockReport, mockExtentReports);

        // Assert
        verify(mockExtentReports).createTest("Test Scenario");
        verify(mockTest).assignCategory(anyString());
        verify(mockTest).assignAuthor(anyString());
    }

    @Test
    public void shouldAssignDefaultAndOptionalCategories() {
        // Arrange
        ExtentTest mockTest = mock(ExtentTest.class);
        ZeroCodeExecResult mockScenario = mock(ZeroCodeExecResult.class);

        when(mockScenario.getScenarioName()).thenReturn("Test Scenario #Category1 #Category2");

        // Act
        zeroCodeReportGenerator.assignCategory(mockTest, mockScenario);

        // Assert
        verify(mockTest).assignCategory("Regression");
        verify(mockTest).assignCategory("#Category1", "#Category2");
    }

    @Test
    public void shouldAssignDefaultAndOptionalAuthors() {
        // Arrange
        ExtentTest mockTest = mock(ExtentTest.class);
        ZeroCodeExecResult mockScenario = mock(ZeroCodeExecResult.class);

        when(mockScenario.getScenarioName()).thenReturn("Test Scenario @Author1 @Author2");

        // Act
        zeroCodeReportGenerator.assignAuthors(mockTest, mockScenario);

        // Assert
        verify(mockTest).assignAuthor("All");
        verify(mockTest).assignAuthor("@Author1", "@Author2");
    }
}