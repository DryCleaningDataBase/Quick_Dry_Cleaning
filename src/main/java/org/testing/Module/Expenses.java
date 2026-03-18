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
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

public class Expenses {

    public static void allExpensesReportData() {
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
            WebElement expensesButton = driver.findElement(By.xpath("//ul[@class='nav navbar-nav']/child::li[6]/ul/child::li[last()-2]"));
            try {
                accountsButton.click();
                expensesButton.click();
            } catch (Exception e) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", accountsButton);
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", expensesButton);
            }

            WebElement filterButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//div[@class='panel-heading']/h3/div[@id='reportrange']")));
            WebElement filterYear = driver.findElement(By.xpath("//div[@class='daterangepicker dropdown-menu opensleft']/child::div[last()]/ul/child::li[last()-6]"));
            try {
                filterButton.click();
                filterYear.click();
            } catch (Exception e) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", filterButton);
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", filterYear);
            }
            List<Map<String, String>> crParkFinalJsonList = new ArrayList<>();
            List<Map<String, String>> defenceColonyFinalJsonList = new ArrayList<>();
            List<Map<String, String>> greenParkFinalJsonList = new ArrayList<>();
            List<Map<String, String>> saketFinalJsonList = new ArrayList<>();
            WebElement expensesTable = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("ctl00_ContentPlaceHolder1_grdReport")));
            List<WebElement> rowsData = expensesTable.findElements(By.xpath(".//tr[position() > 1]/td[1]/a"));
            String originalTab = driver.getWindowHandle();
            for (WebElement rowData:rowsData)
            {
                String rowName = rowData.getText().trim();
                try {
                    rowData.click();
                } catch (Exception e) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", rowData);
                }
                for (String windowHandle : driver.getWindowHandles()) {
                    if (!originalTab.contentEquals(windowHandle)) {
                        driver.switchTo().window(windowHandle);
                        break;
                    }
                }
                List<String> allowedHeaders = Arrays.asList(
                        "Store Name",
                        "Date",
                        "Narration",
                        "Paid To",
                        "Amount"
                );
                WebElement table = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("ctl00_ContentPlaceHolder1_grdSearchResult")));
                List<WebElement> headerElements = table.findElements(By.xpath(".//tr[1]/th"));
                Map<Integer, String> colMap = new LinkedHashMap<>();
                for (int i = 0; i < headerElements.size(); i++) {
                    String headerText = headerElements.get(i).getText().trim();
                    if (allowedHeaders.contains(headerText)) {
                        colMap.put(i, headerText);
                    }
                }
                List<WebElement> rows = table.findElements(By.xpath(".//tr[position() > 1 and position() < last()]"));
                for (WebElement row: rows)
                {
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
                    if(rowName.equalsIgnoreCase("CR Park"))
                        crParkFinalJsonList.add(jsonObject);
                    else if(rowName.equalsIgnoreCase("Defence Colony"))
                        defenceColonyFinalJsonList.add(jsonObject);
                    else if(rowName.equalsIgnoreCase("Greenpark"))
                        greenParkFinalJsonList.add(jsonObject);
                    else if(rowName.equalsIgnoreCase("Saket"))
                        saketFinalJsonList.add(jsonObject);
                }
                driver.close();
                driver.switchTo().window(originalTab);
            }
            //Logout
            WebElement logoutBtn = driver.findElement(By.id("ctl00_btnLogOut"));
            try {
                logoutBtn.click();
            } catch (Exception e) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", logoutBtn);
            }
            ObjectMapper mapper = new ObjectMapper();

            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("CrParkApi.json", crParkFinalJsonList);
            dataMap.put("DefenceColonyApi.json", defenceColonyFinalJsonList);
            dataMap.put("GreenparkApi.json", greenParkFinalJsonList);
            dataMap.put("SaketApi.json", saketFinalJsonList);

            for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
                String jsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(entry.getValue());
                Path path = Paths.get("src/main/resources/Output/AllExpensesApi/", entry.getKey());

                Files.createDirectories(path.getParent());
                Files.write(path, jsonString.getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING);
            }

        } catch (Exception e) {
            System.out.println("Exception Occurred: "+e);;
        } finally {
            driver.quit();
        }
    }
}
