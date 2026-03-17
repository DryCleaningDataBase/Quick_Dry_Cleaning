package org.testing.Module;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testing.DriverClass.Drivers;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.*;

public class Payments {

    public static void allPaymentsReceived() {
        WebDriver driver = Drivers.openChromeBrowser();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(50));

        try {
            driver.get("https://subs3.quickdrycleaning.com/superadmin/Login");
            String userId = System.getenv("USER_ID");
            String userPass = System.getenv("USER_PASS");

            WebElement userField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("txtUserId")));
            userField.clear();
            userField.sendKeys(userId);

            WebElement passField = driver.findElement(By.id("txtPassword"));
            passField.clear();
            passField.sendKeys(userPass);

            WebElement loginBtn = driver.findElement(By.id("btnLogin"));
            try {
                loginBtn.click();
            } catch (Exception e) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", loginBtn);
            }

            wait.until(ExpectedConditions.not(ExpectedConditions.urlContains("Login")));

            WebElement accountsButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//ul[@class='nav navbar-nav']/child::li[6]")));
            WebElement paymentTypeButton = driver.findElement(By.xpath("//ul[@class='nav navbar-nav']/child::li[6]/ul/child::li[1]"));
            try {
                accountsButton.click();
                paymentTypeButton.click();
            } catch (Exception e) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", accountsButton);
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", paymentTypeButton);
            }

            WebElement filterButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//div[@class='panel-heading']/h3/div[@id='reportrange']")));
            WebElement filterYear = driver.findElement(By.xpath("//div[@class='daterangepicker dropdown-menu opensleft']/child::div[last()]/ul/child::li[last()-2]"));
            try {
                filterButton.click();
                filterYear.click();
            } catch (Exception e) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", filterButton);
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", filterYear);
            }

            HashMap<String, Map<String, Double>> allLocationPaymentMap = new LinkedHashMap<>();
            List<String> locations = Arrays.asList("CR Park", "Defence Colony", "Greenpark", "Saket");

            for (String loc : locations) {
                Map<String, Double> paymentMap = new LinkedHashMap<>();
                paymentMap.put("Advance - Cash", 0.0);
                paymentMap.put("Advance - Cheque/Bank", 0.0);
                paymentMap.put("Advance - Credit Card/Debit Card", 0.0);
                paymentMap.put("Advance - UPI", 0.0);
                paymentMap.put("Delivery - Cash", 0.0);
                paymentMap.put("Delivery - Cheque/Bank", 0.0);
                paymentMap.put("Delivery - Credit Card/Debit Card", 0.0);
                paymentMap.put("Delivery - UPI", 0.0);
                paymentMap.put("Package - Sale", 0.0);
                paymentMap.put("Customer Advance", 0.0);
                allLocationPaymentMap.put(loc, paymentMap);
            }

            WebElement paymentTable = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("grdPaymentType")));
            List<WebElement> rowsData = paymentTable.findElements(By.xpath(".//tr[position() > 1 and position() < last()]"));

            for (WebElement rowData : rowsData) {
                String locationKey = rowData.findElement(By.xpath(".//td[1]")).getText().trim();

                if (allLocationPaymentMap.containsKey(locationKey)) {
                    String paymentMode = rowData.findElement(By.xpath(".//td[2]/a")).getText().trim();
                    String textAmount = rowData.findElement(By.xpath(".//td[last()]")).getText().trim().replace(",", "");

                    double amount = textAmount.isEmpty() ? 0.0 : Double.parseDouble(textAmount);

                    Map<String, Double> currentLocMap = allLocationPaymentMap.get(locationKey);
                    if (currentLocMap != null && currentLocMap.containsKey(paymentMode)) {
                        currentLocMap.put(paymentMode, currentLocMap.get(paymentMode) + amount);
                    }
                }
            }

            List<Map<String, Object>> finalJsonList = new ArrayList<>();
            for (Map.Entry<String, Map<String, Double>> entry : allLocationPaymentMap.entrySet()) {
                Map<String, Object> locationData = new LinkedHashMap<>();
                locationData.put("Location", entry.getKey());
                locationData.putAll(entry.getValue());
                finalJsonList.add(locationData);
            }

            ObjectMapper mapper = new ObjectMapper();
            String jsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(finalJsonList);

            Path path = Paths.get("src/main/resources/Output/AllPaymentDataApi.json");
            Files.createDirectories(path.getParent());
            Files.write(path, jsonString.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);

            WebElement logoutBtn = driver.findElement(By.id("ctl00_btnLogOut"));
            try {
                logoutBtn.click();
            } catch (Exception e) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", logoutBtn);
            }

        } catch (Exception e) {
            System.out.println("Runtime error Occurred"+e);
        } finally {
            driver.quit();
        }
    }
}