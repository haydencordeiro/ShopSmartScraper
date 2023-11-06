package selenium;

import com.google.gson.Gson;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.ArrayList; // import the ArrayList class
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openqa.selenium.JavascriptExecutor;

public class LaunchChrome {
    public static class Product{
        String productName;
        String productSellingPrice;
        String productThumbnail;
        String productComparisonDetails;
        String productURL;

    }
    public static ArrayList<String> getWebsites(){
    	ArrayList<String> output = new ArrayList<String>();
    	String[] searchTerms = { "Eggs", "Apples", "Orange Juices", "Vegetable Oil", "Peanut Butter", "Instant Noodles", "Milk"};
        String[] mainURLS = {"https://www.zehrs.ca/search?search-bar=","https://www.nofrills.ca/search?search-bar="};
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
    
    @SuppressWarnings("deprecation")
	public static void main(String[] args) throws Exception {
    	System.setProperty("webdriver.chrome.driver", "chromedriver.exe");
        WebDriver driver = new ChromeDriver();

        driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);

        ArrayList<String> websites = getWebsites();

//        HashMap<String, HashMap<String,String>> dataBase = new HashMap<String, HashMap<String,String>>();
        ArrayList<Product> dataBase = new ArrayList<Product>();
        for(int j=0;j<websites.size();j++)
        {
        driver.get(websites.get(j));
        driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
        List<WebElement> productsDiv = driver.findElements(By.cssSelector("[class=\"product-tile\"]"));
        String productName, sellingPrice, comparisionDetails,productThumbnail;
        for( int i =4 ; i < 10; i++) {
            Thread.sleep(3000);
            if (i == 4) ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight/5)");
            Product newProduct = new Product();
            newProduct.productThumbnail = productsDiv.get(i).findElement(By.className("responsive-image--product-tile-image")).getAttribute("src");
            newProduct.productName = productsDiv.get(i).findElement(By.className("product-name--product-tile")).getText();
            newProduct.productSellingPrice = productsDiv.get(i).findElement(By.className("selling-price-list--product-tile")).getText();
            newProduct.productComparisonDetails = productsDiv.get(i).findElement(By.className("comparison-price-list__item__price")).getText();
            newProduct.productURL = websites.get(j);
            dataBase.add(newProduct);
        }
        }
        System.out.println(dataBase);
        driver.close();

        String jsonString = convertDataToJson(dataBase);
        System.out.println(jsonString);

        sendPostRequest("http://localhost:8080/insertdata", jsonString);
    }
    }

