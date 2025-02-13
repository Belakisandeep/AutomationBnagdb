import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v85.network.Network;
import org.openqa.selenium.devtools.v85.network.model.RequestId;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.Optional;

public class SignUpTest {
    WebDriver driver;
    WebDriverWait wait;
    DevTools devTools;
    boolean apiCallPassed = false;  // Track API success

    @BeforeClass
    public void setUpClass() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeMethod
    public void setUp() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");  // Run Chrome in headless mode (important for Jenkins)
        options.addArguments("--no-sandbox");  // Required when running as root or Jenkins
        options.addArguments("--disable-dev-shm-usage");  // Prevents memory issues in Docker/low-memory environments
        options.addArguments("--user-data-dir=/tmp/chrome-profile-" + System.currentTimeMillis());  // Unique profile directory
        options.addArguments("--remote-allow-origins=*");  // Allow cross-origin requests (if needed)

        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        driver.get("https://testappstore.bangdb.com/detail/bugtapp");

        wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        // Setup DevTools for intercepting network calls
        devTools = ((ChromeDriver) driver).getDevTools();
        devTools.createSession();
        devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));
    }

    @Test
    public void fillSignUpFormAndVerifyAPIResponse() {
        // Intercept network responses
        devTools.addListener(Network.responseReceived(), response -> {
            String url = response.getResponse().getUrl();
            int status = response.getResponse().getStatus();
            RequestId requestId = response.getRequestId();

            // Check for user creation API call
            if (url.contains("/user/create") && status == 200) {
                // Get response body using request ID
                Network.GetResponseBodyResponse bodyResponse = devTools.send(Network.getResponseBody(requestId));
                String responseBody = bodyResponse.getBody();

                System.out.println("API Response Body: " + responseBody);

                // Check if the response contains the success message
                if (responseBody.contains("\"errcode\":0") && responseBody.contains("user creation successful!")) {
                    apiCallPassed = true;  // Mark as passed
                    System.out.println("User creation successful API response detected!");
                }
            }
        });

        // Fill the signup form
        WebElement install_btn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[.='Install']")));
        install_btn.click();

        WebElement firstname = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//input[@name='first_name']")));
        WebElement lastname = driver.findElement(By.xpath("//input[@name='last_name']"));
        WebElement email = driver.findElement(By.xpath("//input[@name='email']"));
        WebElement phone = driver.findElement(By.xpath("//input[@name='phone']"));
        WebElement checkbox = driver.findElement(By.xpath("//input[@name='remember-me']"));
        WebElement submitButton = driver.findElement(By.xpath("//button[@type='submit']"));

        // Use dynamic email for unique submission
        String dynamicEmail = "johndoe" + System.currentTimeMillis() + "@example.com";

        firstname.sendKeys("John");
        lastname.sendKeys("Doe");
        email.sendKeys(dynamicEmail);
        phone.sendKeys("1234567890");
        checkbox.click();
        submitButton.click();

        // Allow time for network response capture
        try {
            Thread.sleep(5000);  // Adjust if needed
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Assert if API call was successful
        Assert.assertTrue(apiCallPassed, "API call for user creation failed!");
    }

    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
