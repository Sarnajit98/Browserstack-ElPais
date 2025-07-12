package com.browserstack.assignment;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class WebScrapTest {

    private WebDriver driver;

    @Parameters({"browser", "browserVersion", "os", "osVersion", "deviceName"})
    @BeforeMethod
    public void setUp(@Optional("Chrome") String browser,
                      @Optional("") String browserVersion,
                      @Optional("") String os,
                      @Optional("") String osVersion,
                      @Optional("") String deviceName) throws Exception {

        DesiredCapabilities caps = new DesiredCapabilities();
        caps.setCapability("browserName", browser);

        Map<String, Object> bstackOptions = new HashMap<>();

        if (!deviceName.isEmpty()) {
            //Mobile config
            bstackOptions.put("deviceName", deviceName);
            bstackOptions.put("osVersion", osVersion);
        } else {
            // Desktop config
            bstackOptions.put("os", os);
            bstackOptions.put("osVersion", osVersion);
            bstackOptions.put("browserVersion", browserVersion);
        }

        //BrowserStack info
        bstackOptions.put("userName", "sarnajitsantra_lhm2tG");
        bstackOptions.put("accessKey", "Hd39WCAHd1yDPy3ZyDXM");
        bstackOptions.put("projectName", "BrowserStack Sample");
        bstackOptions.put("buildName", "bstack-demo");
        bstackOptions.put("sessionName", "Cross-Browser ElPais Test");

        caps.setCapability("bstack:options", bstackOptions);

        driver = new RemoteWebDriver(new URL("https://hub.browserstack.com/wd/hub"), caps);
    }

    @Test
    public void runWebScraperTest() {
        System.out.println("🔹 Started test on " + ((RemoteWebDriver) driver).getCapabilities().getBrowserName() +
                       " | Thread: " + Thread.currentThread().getId());
        WebScrap.runScraper(driver);
    }

    @AfterMethod
    public void tearDown(ITestResult result) {
        if (driver != null) {
            String status = result.isSuccess() ? "passed" : "failed";
            String reason = result.isSuccess() ? "Test passed" : "Test failed";

            // Reporting status to BrowserStack
            ((JavascriptExecutor) driver).executeScript(
                "browserstack_executor: {\"action\": \"setSessionStatus\", \"arguments\": {\"status\":\"" 
                + status + "\", \"reason\": \"" + reason + "\"}}");

            driver.quit();
        }
    }

}
