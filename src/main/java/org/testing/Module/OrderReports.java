package org.testing.Module;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testing.DriverClass.Drivers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.*;

public class OrderReports {

    public static void allOrderReports()
    {
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

            WebElement reportsButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//ul[@class='nav navbar-nav']/child::li[7]")));
            WebElement orderButton = driver.findElement(By.xpath("//ul[@class='nav navbar-nav']/child::li[7]/ul/child::li"));
            try {
                reportsButton.click();
                orderButton.click();
            } catch (Exception e) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", reportsButton);
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", orderButton);
            }

            WebElement filterButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//div[@class='panel-heading']/h3/div[@id='reportrange']")));
            WebElement filterMonth = driver.findElement(By.xpath("//div[@class='daterangepicker dropdown-menu opensleft']/child::div[last()]/ul/child::li[last()-6]"));
            try {
                filterButton.click();
                filterMonth.click();
            } catch (Exception e) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", filterButton);
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", filterMonth);
            }
            WebElement expandButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//span[@id='achrExpand']")));
            try {
                expandButton.click();
            } catch (Exception e) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", expandButton);
            }

            List<String> allowedHeaders = Arrays.asList(
                    "Store Name", "Order Date / Time", "Order No.", "Name", "Address",
                    "Phone", "Preference", "Due Date", "Delivered On", "Pcs.",
                    "Weight", "Gross Amount", "Discount", "Advance", "Paid",
                    "Adjustment", "Balance", "Advance Received", "Advance Used",
                    "Booked By", "WorkShop Note", "Order Note", "Home Delivery",
                    "Area Location", "Garments inspected by", "Customer GSTIN",
                    "Registration Source", "Order from POS", "Order Status", "Customer Code"
            );

            List<Map<String, String>> finalJsonList = new ArrayList<>();
            int currentPage = 1;
            boolean keepGoing = true;

            WebElement firstTable = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("grdReport")));
            List<WebElement> headerElements = firstTable.findElements(By.xpath(".//tr[1]/th"));
            Map<Integer, String> colMap = new LinkedHashMap<>();
            for (int i = 0; i < headerElements.size(); i++) {
                String headerText = headerElements.get(i).getText().trim();
                if (allowedHeaders.contains(headerText)) {
                    colMap.put(i, headerText);
                }
            }

            while (keepGoing) {
                WebElement table = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("grdReport")));
                List<WebElement> rows = table.findElements(By.xpath(".//tr[position() > 1 and not(contains(@class, 'Pager'))]"));

                for (WebElement row : rows) {
                    List<WebElement> cells = row.findElements(By.tagName("td"));
                    if (cells.size() < colMap.size()) continue;

                    Map<String, String> jsonObject = new LinkedHashMap<>();
                    for (Map.Entry<Integer, String> entry : colMap.entrySet()) {
                        int colIndex = entry.getKey();
                        if (colIndex < cells.size()) {
                            String headerName = entry.getValue();
                            String cellValue = cells.get(colIndex).getAttribute("textContent").replaceAll("\\u00A0", " ").trim();
                            jsonObject.put(headerName, cellValue);
                        }
                    }
                    finalJsonList.add(jsonObject);
                }

                try {
                    String nextPageNumber = String.valueOf(currentPage + 1);
                    By nextSelector = By.xpath("//table[@id='grdReport']//a[text()='" + nextPageNumber + "']");
                    List<WebElement> nextLinks = driver.findElements(nextSelector);

                    if (!nextLinks.isEmpty()) {
                        WebElement nextButton = nextLinks.get(0);
                        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", nextButton);
                        currentPage++;
                        wait.until(ExpectedConditions.stalenessOf(table));
                    } else {
                        By ellipsisSelector = By.xpath("//table[@id='grdReport']//a[text()='...']");
                        List<WebElement> ellipsis = driver.findElements(ellipsisSelector);
                        if (!ellipsis.isEmpty()) {
                            WebElement lastEllipsis = ellipsis.get(ellipsis.size() - 1);
                            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", lastEllipsis);
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
            WebElement logoutBtn = driver.findElement(By.id("ctl00_btnLogOut"));
            try {
                logoutBtn.click();
            } catch (Exception e) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", logoutBtn);
            }
            ObjectMapper mapper = new ObjectMapper();
            String jsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(finalJsonList);
            String jsContent = "const OrderData = " + jsonString + ";";

            Path path = Paths.get("src/main/resources/Output/OrderReportApi.js");
            Files.createDirectories(path.getParent());
            Files.write(path, jsContent.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        } catch (Exception e) {
            System.out.println("Exception Occurred: "+e);;
        } finally {
            driver.quit();
        }
    }
}
