import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testng.Assert;
import org.testng.annotations.Test;

public class N8nTest extends BaseTest {

    private void login(String user, String pass) {

        driver.get(baseUrl + "/signin");

        driver.findElement(By.cssSelector("input[type='email']"))
                .sendKeys(user);

        driver.findElement(By.cssSelector("input[type='password']"))
                .sendKeys(pass);

        driver.findElement(By.xpath(
                "//button[contains(translate(.,'SIGN','sign'),'sign in')]"))
                .click();
    }

    private void capture(String name) {

        File src = ((TakesScreenshot) driver)
                .getScreenshotAs(OutputType.FILE);

        try {
            FileUtils.copyFile(src,
                    new File("screenshots/" + name + ".png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // =====================================================
    // TC01
    // =====================================================

    @Test(priority = 1)
    public void TC01_LoginSuccess() {

        login(email, password);

        wait.until(d ->
                d.getCurrentUrl().contains("/home")
                        || d.getCurrentUrl().contains("/workflows"));

        Assert.assertTrue(
                driver.getCurrentUrl().contains("/home")
                        || driver.getCurrentUrl().contains("/workflows"));

        capture("TC01_LoginSuccess");
    }

    // =====================================================
    // TC02
    // =====================================================

    @Test(priority = 2)
    public void TC02_InvalidLogin() {

        login(
                "invalid.user.never.exists@example.com",
                "DefinitelyWrongPassword123!");

        WebElement errorToast =
                wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//*[contains(text(),'Wrong username or password')]")));

        Assert.assertTrue(errorToast.isDisplayed());

        capture("TC02_InvalidLogin");
    }

    // =====================================================
    // TC03
    // =====================================================

    @Test(priority = 3)
    public void TC03_VerifyWorkflowLoads() {

        login(email, password);

        driver.get(baseUrl + "/workflow/" + workflowId);

        wait.until(ExpectedConditions.urlContains("/workflow/" + workflowId));

        WebElement aiAgent =
                wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//*[contains(text(),'AI Agent')]")));

        Assert.assertTrue(aiAgent.isDisplayed());

        capture("TC03_VerifyWorkflowLoads");
    }

    // =====================================================
    // TC04
    // =====================================================

    @Test(priority = 4)
    public void TC04_VerifyNodeIssues() {

        login(email, password);

        driver.get(baseUrl + "/workflow/" + workflowId);

        var issues =
                driver.findElements(By.cssSelector("[data-test-id='node-issues']"));

        System.out.println("Issue Count = " + issues.size());

        Assert.assertTrue(
                issues.size() > 0,
                "Expected at least one node issue");

        capture("TC04_VerifyNodeIssues");
    }

    // =====================================================
    // TC05
    // =====================================================

    @Test(priority = 5)
    public void TC05_ExecuteWorkflowFailure() {

    login(email, password);

    driver.get(baseUrl + "/workflow/" + workflowId);

    wait.until(ExpectedConditions.elementToBeClickable(
            By.cssSelector("[data-test-id='workflow-chat-button']")))
            .click();

    WebElement chatInput =
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("[data-test-id='chat-input']")));

    chatInput.sendKeys("hi");

    wait.until(ExpectedConditions.elementToBeClickable(
            By.cssSelector(".chat-input-send-button")))
            .click();

    WebElement errorNotification =
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector(".el-notification--error")));

    Assert.assertTrue(errorNotification.isDisplayed());

    capture("TC05_ExecuteWorkflowFailure");
    }

    // =====================================================
    // TC06
    // =====================================================

    @Test(priority = 6)
    public void TC06_VerifyExecutionHistory() {

        login(email, password);

        driver.get(baseUrl + "/home/executions");

        wait.until(ExpectedConditions.urlContains("/executions"));

        WebElement latestStatus =
                wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("(//tbody/tr)[1]//*[contains(text(),'Error')]")
                ));

        String statusText = latestStatus.getText().trim();

        System.out.println("Latest execution status = " + statusText);

        Assert.assertEquals(
                statusText,
                "Error",
                "Latest execution is not in Error state");

        capture("TC06_VerifyExecutionHistory");
    }
}