package org.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testing.DriverClass.Drivers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class Main {

    public static void main(String[] args) {
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
            
            WebElement reportsButton = driver.findElement(By.xpath("//ul[@class='nav navbar-nav']/child::li[7]"));
            WebElement orderButton = driver.findElement(By.xpath("//ul[@class='nav navbar-nav']/child::li[7]/ul/child::li"));
            try {
                reportsButton.click();
                orderButton.click();
            } catch (Exception e) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", reportsButton);
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", orderButton);
            }
            
            WebElement filterButton = driver.findElement(By.xpath("//div[@class='panel-heading']/h3/div[@id='reportrange']"));
            WebElement filterMonth = driver.findElement(By.xpath("//div[@class='daterangepicker dropdown-menu opensleft']/child::div[last()]/ul/child::li[5]"));
            try {
                filterButton.click();
                filterMonth.click();
            } catch (Exception e) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", filterButton);
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", filterMonth);
            }

            List<String> allowedHeaders = Arrays.asList(
                    "Store Name", "Order Date / Time", "Order No.", "Name",
                    "Address", "Phone", "Due Date", "Pcs.", "Weight",
                    "Gross Amount", "Discount", "Advance", "Paid", "Balance"
            );

            List<Map<String, String>> finalJsonList = new ArrayList<>();
            int currentPage = 1;
            boolean keepGoing = true;

            while (keepGoing) {
                WebElement table = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("grdReport")));
                
                List<WebElement> headerElements = table.findElements(By.xpath(".//tr[1]/th"));
                Map<Integer, String> colMap = new LinkedHashMap<>();
                for (int i = 0; i < headerElements.size(); i++) {
                    String headerText = headerElements.get(i).getText().trim();
                    if (allowedHeaders.contains(headerText)) {
                        colMap.put(i, headerText);
                    }
                }

                List<WebElement> rows = table.findElements(By.xpath(".//tr[position() > 1 and not(contains(@class, 'Pager'))]"));
                for (WebElement row : rows) {
                    List<WebElement> cells = row.findElements(By.tagName("td"));
                    if (cells.size() < colMap.size()) continue;
                    
                    Map<String, String> jsonObject = new LinkedHashMap<>();
                    for (Map.Entry<Integer, String> entry : colMap.entrySet()) {
                        int colIndex = entry.getKey();
                        String headerName = entry.getValue();
                        String cellValue = cells.get(colIndex).getAttribute("textContent").trim();
                        jsonObject.put(headerName, cellValue);
                    }
                    finalJsonList.add(jsonObject);
                }

                try {
                    String nextPageNumber = String.valueOf(currentPage + 1);
                    By nextSelector = By.xpath("//table[@id='grdReport']//a[text()='" + nextPageNumber + "']");
                    List<WebElement> nextLinks = driver.findElements(nextSelector);

                    if (!nextLinks.isEmpty()) {
                        WebElement nextButton = nextLinks.get(0);
                        try {
                            nextButton.click();
                        } catch (Exception e) {
                            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", nextButton);
                        }
                        currentPage++;
                        wait.until(ExpectedConditions.stalenessOf(table));
                    } else {
                        By ellipsisSelector = By.xpath("//table[@id='grdReport']//a[text()='...']");
                        List<WebElement> ellipsis = driver.findElements(ellipsisSelector);
                        if (!ellipsis.isEmpty()) {
                            WebElement lastEllipsis = ellipsis.get(ellipsis.size() - 1);
                            try {
                                lastEllipsis.click();
                            } catch (Exception e) {
                                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", lastEllipsis);
                            }
                            currentPage++;
                            wait.until(ExpectedConditions.stalenessOf(table));
                        } else {
                            keepGoing = false;
                        }
                    }
                } catch (Exception e) {
                    keepGoing = false;
                }
            }

            ObjectMapper mapper = new ObjectMapper();
            String jsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(finalJsonList);
            String jsContent = "const OrderData = " + jsonString + ";";

            Path path = Paths.get("src/main/resources/output/data.js");
            Files.createDirectories(path.getParent());
            Files.write(path, jsContent.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }
}
