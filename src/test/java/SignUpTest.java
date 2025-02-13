import io.github.bonigarcia.wdm.WebDriverManager;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v85.network.Network;
import org.openqa.selenium.devtools.v85.network.model.RequestId;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.*;

import java.time.Duration;
import java.util.Optional;

public class SignUpTest {
    WebDriver driver;
    WebDriverWait wait;
    DevTools devTools;
    boolean apiCallPassed = false;

    @BeforeClass
    public void setUpClass() {
        // Setup WebDriver manager
        WebDriverManager.chromedriver().setup();
    }

    @BeforeMethod
    public void setUp() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--user-data-dir=/tmp/chrome-profile-" + System.currentTimeMillis());
        options.addArguments("--remote-allow-origins=*");

        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        driver.get("https://testappstore.bangdb.com/detail/bugtapp");

        wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        // Setup DevTools
        devTools = ((ChromeDriver) driver).getDevTools();
        devTools.createSession();
        devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));
    }

    @Test
    @Feature("User Sign Up")
    @Description("Test for filling out the sign-up form and verifying API response.")
    public void fillSignUpFormAndVerifyAPIResponse() {
        // Intercept network responses
        devTools.addListener(Network.responseReceived(), response -> {
            String url = response.getResponse().getUrl();
            int status = response.getResponse().getStatus();
            RequestId requestId = response.getRequestId();

            if (url.contains("/user/create") && status == 200) {
                Network.GetResponseBodyResponse bodyResponse = devTools.send(Network.getResponseBody(requestId));
                String responseBody = bodyResponse.getBody();

                System.out.println("API Response Body: " + responseBody);

                if (responseBody.contains("\"errcode\":0") && responseBody.contains("user creation successful!")) {
                    apiCallPassed = true;
                    System.out.println("User creation successful API response detected!");
                }
            }
        });

        // Filling form
        fillForm("John", "Doe", "johndoe" + System.currentTimeMillis() + "@example.com");

        // Submit form
        WebElement submitButton = driver.findElement(By.xpath("//button[@type='submit']"));
        submitButton.click();

        // Wait for API response
        wait.until(driver -> apiCallPassed);

        // Assertion
        Assert.assertTrue(apiCallPassed, "API call for user creation failed!");
    }

    @Step("Filling the sign-up form with name {0} {1} and email {2}")
    public void fillForm(String firstName, String lastName, String email) {
        WebElement firstname = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//input[@name='first_name']")));
        firstname.sendKeys(firstName);
        WebElement lastname = driver.findElement(By.xpath("//input[@name='last_name']"));
        lastname.sendKeys(lastName);
        WebElement emailElement = driver.findElement(By.xpath("//input[@name='email']"));
        emailElement.sendKeys(email);
    }

    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
