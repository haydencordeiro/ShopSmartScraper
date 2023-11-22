package selenium;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.gson.Gson;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList; // import the ArrayList class
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LaunchChrome {
    public static class Product{
        String productName;
        String productSellingPrice;
        String productThumbnail;
        String productComparisonDetails;
        String productURL;

    }

    public static class SearchTerm{
        public String id;
        public int searchCount;
        public String searchTerm;
    }
    public static String fetchSearchTerms(String urlString) {
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
    public static ArrayList<String> getWebsites() throws JsonProcessingException {
    	ArrayList<String> output = new ArrayList<String>();
        ObjectMapper objectMapper = new ObjectMapper();
        List<SearchTerm> searchCountsJson = objectMapper.readValue(fetchSearchTerms("http://localhost:8080/allSearchCounts"),  new TypeReference<List<SearchTerm>>() {});


//        ArrayList<String> modifiedList = new ArrayList<>();
//        for (String str : searchCountsJson) {
//            String modifiedStr = str.toLowerCase().trim();
//            modifiedList.add(modifiedStr);
//        }
//
//        // Convert the modified ArrayList to a Set
//        HashSet<String> resultSet = new HashSet<>(modifiedList);



        HashSet<String> searchTerms = new HashSet<String>();
//        searchTerms.add("Eggs");
//        searchTerms.add("Milk");
//        searchTerms.add("Instant Noodles");
//        searchTerms.add("Orange Juice");


        for(SearchTerm st :searchCountsJson){
            searchTerms.add(st.searchTerm.toLowerCase().strip());
        }
        System.out.println(searchTerms);

        String[] mainURLS = {"https://www.nofrills.ca/search?search-bar=", "https://www.metro.ca/en/online-grocery/search?filter=", "https://www.zehrs.ca/search?search-bar="};

        for(String mainURL: mainURLS) {
        	for(String searchTerm: searchTerms) {
        		output.add(mainURL+searchTerm);
        	}
        }
        return output;
    }

    public static String convertDataToJson(ArrayList<Product> dataBase ){
        Gson gson = new Gson();
        return gson.toJson(dataBase);
    }

    public static String convertSearchTermToJson(String dataBase ){
        Gson gson = new Gson();
        return gson.toJson(dataBase);
    }


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

    public static ArrayList<Product>  scrapeMetro(String websiteURL, WebDriver driver) throws InterruptedException {
        WebDriverWait wait = new WebDriverWait(driver,  Duration.ofSeconds(30));
        wait.until(ExpectedConditions.jsReturnsValue("return document.readyState==\"complete\";"));
//        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.className("root-spinner-wrapper")));
        Thread.sleep(1000);
        slowScrollToBottom(driver);
        ArrayList<Product> dataBase = new ArrayList<Product>();
        driver.get(websiteURL);
        List<WebElement> productDivs = driver.findElements(By.cssSelector(".item-addToCart"));
        for(WebElement productDiv : productDivs){
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


    public static ArrayList<Product> scrapeNoFrillsAndZehrs( String websiteURL, WebDriver driver ) throws InterruptedException {
        driver.get(websiteURL);
        WebDriverWait wait = new WebDriverWait(driver,  Duration.ofSeconds(30));
        wait.until(ExpectedConditions.jsReturnsValue("return document.readyState==\"complete\";"));
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.className("root-spinner-wrapper")));
        Thread.sleep(1000);

        slowScrollToBottom(driver);
//        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight)");
//       Thread.sleep(10000);




        ArrayList<Product> dataBase = new ArrayList<Product>();
        List<WebElement> productsDiv = driver.findElements(By.cssSelector("[class=\"product-tile\"]"));
        List<WebElement> productsImages = driver.findElements(By.className("responsive-image--product-tile-image"));
        System.out.print("Product Div " +productsDiv.size());
        System.out.print("Product IMG " +productsImages.size());

        String productName, sellingPrice, comparisionDetails,productThumbnail;
        for( int i =4 ; i < Math.min(40,productsDiv.size()); i++) {
            try{
                Product newProduct = new Product();
                newProduct.productThumbnail = productsDiv.get(i).findElement(By.className("responsive-image--product-tile-image")).getAttribute("src");
                newProduct.productName = productsDiv.get(i).findElement(By.className("product-name--product-tile")).getText();
                newProduct.productSellingPrice = productsDiv.get(i).findElement(By.className("selling-price-list--product-tile")).getText();
                newProduct.productComparisonDetails = productsDiv.get(i).findElement(By.className("comparison-price-list__item__price")).getText();
                newProduct.productURL = websiteURL;
                dataBase.add(newProduct);
                System.out.println(newProduct.productName);

            }catch (Exception e){
//                System.out.println(e);
            }

        }
        return dataBase;
    }
    @SuppressWarnings("deprecation")
	public static void mainHelper() throws Exception {
    	System.setProperty("webdriver.chrome.driver", "chromedriver.exe");
        ChromeOptions options = new ChromeOptions();
        options.addArguments("headless");
        WebDriver driver = new ChromeDriver(options);


        ArrayList<String> websites = getWebsites();

//        HashMap<String, HashMap<String,String>> dataBase = new HashMap<String, HashMap<String,String>>();
        ArrayList<Product> dataBase = new ArrayList<Product>();
        for(int j=0;j<websites.size();j++)
        {
        String websiteURL = websites.get(j);
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
//        Thread.sleep(10000);
            if(websiteURL.toLowerCase().contains("zehrs") || websiteURL.toLowerCase().contains("nofrills")) {
                dataBase.addAll(scrapeNoFrillsAndZehrs(websiteURL, driver));
            }
            else{
                dataBase.addAll(scrapeMetro(websiteURL, driver));
            }
        }
        System.out.println(dataBase);

        driver.close();

        String jsonString = convertDataToJson(dataBase);
        sendPostRequest("http://localhost:8080/insertdata", jsonString);
    }


    public static void main(String[] args) throws Exception {

        mainHelper(); // main method

//        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
//
//        // Schedule the task to run every 60 minutes
//        scheduler.scheduleAtFixedRate(() -> {
//            try {
//                System.out.println("Executing task...");
//                mainHelper(); // main metho
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }, 0, 60, TimeUnit.MINUTES);
    }
}

