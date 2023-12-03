// Importing necessary packages
package selenium;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.support.ui.ExpectedConditions;

// Main class for launching Chrome and performing web scraping
public class LaunchChrome {

    // Base URL for the web service
    public static String HostURL = "http://localhost:8080/";

    // Data model class for storing product information
    public static class Product {
        String productName;
        String productSellingPrice;
        String productThumbnail;
        String productComparisonDetails;
        String productURL;
    }

    // Data model class for storing search term information
    public static class SearchTerm {
        public String id;
        public int searchCount;
        public String searchTerm;
    }

    /**
     * Helper method for making GET requests
     *
     * @param urlString The URL for the GET request
     * @return The response from the GET request as a String
     */
    public static String getMethodHelper(String urlString) {
        StringBuilder result = new StringBuilder();

        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                result.append(line);
            }

            reader.close();
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result.toString();
    }

    /**
     * Method for getting website URLs based on nightly run or new search terms
     *
     * @param isNightlyRun Indicates whether it's a nightly run or not
     * @return List of website URLs
     * @throws JsonProcessingException If there is an issue processing JSON
     */
    public static ArrayList<String> getWebsites(boolean isNightlyRun) throws JsonProcessingException {
        ArrayList<String> output = new ArrayList<String>();
        HashSet<String> searchTerms = new HashSet<String>();

        // Fetch search terms based on nightly run or new search terms
        if (isNightlyRun) {
            ObjectMapper objectMapper = new ObjectMapper();
            List<SearchTerm> searchCountsJson = objectMapper.readValue(
                    getMethodHelper(HostURL + "allSearchCounts"),
                    new TypeReference<List<SearchTerm>>() {}
            );
            for (SearchTerm st : searchCountsJson) {
                System.out.println(st.id);
                searchTerms.add(st.searchTerm.toLowerCase().strip());
            }
        } else {
            ObjectMapper objectMapper = new ObjectMapper();
            searchTerms = objectMapper.readValue(
                    getMethodHelper(HostURL + "newSearchTerms"),
                    new TypeReference<HashSet<String>>() {}
            );
        }

        System.out.println(searchTerms);

        if (searchTerms.isEmpty()) {
            return output;
        }

        // Generate website URLs based on search terms and main URLs
        String[] mainURLS = {
                "https://www.nofrills.ca/search?search-bar=",
                "https://www.metro.ca/en/online-grocery/search?filter=",
                "https://www.zehrs.ca/search?search-bar="
        };

        for (String mainURL : mainURLS) {
            for (String searchTerm : searchTerms) {
                output.add(mainURL + searchTerm);
            }
        }
        return output;
    }

    /**
     * Convert product data to JSON format using Gson
     *
     * @param dataBase List of Product objects to convert to JSON
     * @return JSON representation of the product data
     */
    public static String convertDataToJson(ArrayList<Product> dataBase) {
        Gson gson = new Gson();
        return gson.toJson(dataBase);
    }

    /**
     * Convert search term data to JSON format using Gson
     *
     * @param dataBase Search term data to convert to JSON
     * @return JSON representation of the search term data
     */
    public static String convertSearchTermToJson(String dataBase) {
        Gson gson = new Gson();
        return gson.toJson(dataBase);
    }

    /**
     * Send a POST request with JSON data
     *
     * @param endpoint The endpoint for the POST request
     * @param jsonData The JSON data to send in the request
     * @throws Exception If there is an issue with the POST request
     */
    public static void sendPostRequest(String endpoint, String jsonData) throws Exception {
        URL url = new URL(endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            try (OutputStream outputStream = connection.getOutputStream()) {
                byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
                outputStream.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

        } finally {
            connection.disconnect();
        }
    }

    /**
     * Scraping method for the Metro website
     *
     * @param websiteURL The URL of the Metro website to scrape
     * @param driver WebDriver instance for browser automation
     * @return List of Product objects containing scraped data
     * @throws InterruptedException If there is an interruption during execution
     */
    public static ArrayList<Product> scrapeMetro(String websiteURL, WebDriver driver) throws InterruptedException {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        wait.until(ExpectedConditions.jsReturnsValue("return document.readyState==\"complete\";"));
        Thread.sleep(3000);
        slowScrollToBottom(driver);
        ArrayList<Product> dataBase = new ArrayList<Product>();
        driver.get(websiteURL);
        List<WebElement> productDivs = driver.findElements(By.cssSelector(".item-addToCart"));
        for (WebElement productDiv : productDivs) {
            Product p = new Product();
            p.productThumbnail = productDiv.findElement(By.cssSelector(".defaultable-picture > img")).getAttribute("src");
            p.productName = productDiv.findElement(By.cssSelector(".head__title")).getText();
            p.productSellingPrice = productDiv.findElement(By.cssSelector(".pricing__sale-price")).getText();
            p.productComparisonDetails = productDiv.findElement(By.cssSelector(".pricing__secondary-price")).getText();
            p.productURL = websiteURL;
            System.out.println(p.productName);
            dataBase.add(p);
        }
        return dataBase;
    }

    /**
     * Helper method for slow scrolling to the bottom of the page
     *
     * @param driver WebDriver instance for browser automation
     */
    private static void slowScrollToBottom(WebDriver driver) {
        JavascriptExecutor js = (JavascriptExecutor) driver;

        long windowHeight = (long) js.executeScript("return window.innerHeight");
        long pageHeight = (long) js.executeScript("return Math.max(document.body.scrollHeight, document.body.offsetHeight, document.documentElement.clientHeight, document.documentElement.scrollHeight, document.documentElement.offsetHeight)");

        long scrollIncrement = 80; // You can adjust this value to control the scroll speed

        for (long currentScroll = 0; currentScroll < pageHeight; currentScroll += scrollIncrement) {
            js.executeScript("window.scrollTo(0, " + currentScroll + ")");
            try {
                // Add a short delay between scrolls to slow down the scrolling
                Thread.sleep(100); // You can adjust this value to control the delay
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Scraping method for NoFrills and Zehrs websites
     *
     * @param websiteURL The URL of the website to scrape (NoFrills or Zehrs)
     * @param driver WebDriver instance for browser automation
     * @return List of Product objects containing scraped data
     * @throws InterruptedException If there is an interruption during execution
     */
    public static ArrayList<Product> scrapeNoFrillsAndZehrs(String websiteURL, WebDriver driver) throws InterruptedException {
        driver.get(websiteURL);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        wait.until(ExpectedConditions.jsReturnsValue("return document.readyState==\"complete\";"));
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.className("root-spinner-wrapper")));
        Thread.sleep(1000);
        slowScrollToBottom(driver);
        ArrayList<Product> dataBase = new ArrayList<Product>();
        List<WebElement> productsDiv = driver.findElements(By.cssSelector("[class=\"product-tile\"]"));
        List<WebElement> productsImages = driver.findElements(By.className("responsive-image--product-tile-image"));
        System.out.print("Product Div " + productsDiv.size());
        System.out.print("Product IMG " + productsImages.size());

        String productName, sellingPrice, comparisonDetails, productThumbnail;
        for (int i = 4; i < Math.min(40, productsDiv.size()); i++) {
            try {
                Product newProduct = new Product();
                newProduct.productThumbnail = productsDiv.get(i).findElement(By.className("responsive-image--product-tile-image")).getAttribute("src");
                newProduct.productName = productsDiv.get(i).findElement(By.className("product-name--product-tile")).getText();
                newProduct.productSellingPrice = productsDiv.get(i).findElement(By.className("selling-price-list--product-tile")).getText();
                newProduct.productComparisonDetails = productsDiv.get(i).findElement(By.className("comparison-price-list__item__price")).getText();
                newProduct.productURL = websiteURL;
                dataBase.add(newProduct);
                System.out.println(newProduct.productName);

            } catch (Exception e) {
//                System.out.println(e);
            }

        }
        return dataBase;
    }

    /**
     * Main helper method for executing either nightly run or new terms run
     *
     * @param isNightlyRum If true, executes nightly run; otherwise, executes new terms run
     * @throws Exception If there is an issue during execution
     */
    @SuppressWarnings("deprecation")
    public static void mainHelper(boolean isNightlyRum) throws Exception {
        try {


            System.setProperty("webdriver.chrome.driver", "chromedriver.exe");
            ChromeOptions options = new ChromeOptions();
            options.addArguments("headless");
            WebDriver driver = new ChromeDriver(options);

            ArrayList<String> websites = getWebsites(isNightlyRum);
            ArrayList<Product> dataBase = new ArrayList<Product>();
            for (int j = 0; j < websites.size(); j++) {
                String websiteURL = websites.get(j);
                driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
                System.out.println("____________________________________________");
                System.out.println(websiteURL);
                if (websiteURL.toLowerCase().contains("zehrs") || websiteURL.toLowerCase().contains("nofrills")) {
                    dataBase = (scrapeNoFrillsAndZehrs(websiteURL, driver));
                } else {
                    dataBase = (scrapeMetro(websiteURL, driver));
                }

                String jsonString = convertDataToJson(dataBase);
                sendPostRequest(HostURL + "insertdata", jsonString);
            }
            if (!isNightlyRum) {
                getMethodHelper(HostURL + "clearNewSearchTerms");
            }
            driver.close();
        }
        catch (WebDriverException e) {
            // Catch and handle WebDriverException
            System.err.println("WebDriverException: " + e.getMessage());
            // Optionally, you can log the exception or take other actions
        }
        catch (Exception e) {
            // Catch any other exceptions
            e.printStackTrace();
        }
    }

    /**
     * Main method for scheduling and executing nightly and new terms runs
     *
     * @param args Command line arguments (not used)
     * @throws Exception If there is an issue during execution
     */
    public static void main(String[] args) throws Exception {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

//         Schedule the task to run every day once
        scheduler.scheduleAtFixedRate(() -> {
            try {
                System.out.println("Executing Nightly Run...");
                mainHelper(true); // main method
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 24, 24, TimeUnit.HOURS);
//
        scheduler.scheduleAtFixedRate(() -> {
            try {
                System.out.println("Executing New Terms Run...");
                mainHelper(false); // main method
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 10, TimeUnit.SECONDS);
    }
}
