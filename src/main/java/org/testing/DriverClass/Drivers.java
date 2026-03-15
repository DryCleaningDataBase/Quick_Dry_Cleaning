package org.testing.DriverClass;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import java.util.Collections; // Recommended for cleaner option management

public class Drivers {
    
    public static WebDriver openChromeBrowser() {
        ChromeOptions options = new ChromeOptions();
        
        // Essential Headless & CI Flags
        options.addArguments("--headless=new");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--disable-gpu");
        options.addArguments("--ignore-certificate-errors");
        
        // This flag helps bypass "Bot Detection" which might be causing your redirect to fail
        options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);

        return new ChromeDriver(options);
    }
}
