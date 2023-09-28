package selenium;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.Keys;

public class LaunchChrome {

	
    @SuppressWarnings("deprecation")
	public static void main(String[] args) {
    	System.setProperty("webdriver.chrome.driver", "C:/Users/haydencordeiro/Pictures/chromedriver.exe");
        WebDriver driver = new ChromeDriver();

//        driver.get("https://www.zehrs.ca/");
        driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
//        WebDriverWait.until(By.id("search-form-input"));

        
//        WebElement searchBox = driver.findElement(By.cssSelector(".search-input__input"));
//        searchBox.sendKeys("apples");
//
//        
//        searchBox.sendKeys(Keys.RETURN);
        driver.get("https://www.zehrs.ca/search?search-bar=pears");

        List<WebElement> productsDiv = driver.findElements(By.cssSelector("[class=\"product-tile\"]"));
        String productName, sellingPrice, comparisionDetails;
        for( int i =0 ; i < productsDiv.size(); i++) {
        	productName = productsDiv.get(i).findElement(By.className("product-name--product-tile")).getText();
        	sellingPrice = productsDiv.get(i).findElement(By.className("selling-price-list--product-tile")).getText();
        	comparisionDetails = productsDiv.get(i).findElement(By.className("comparison-price-list__item__price")).getText();
        	System.out.println("Product Name: "+ productName);
        	System.out.println("Product Price: "+ sellingPrice + " Description: "+ comparisionDetails);
        }
        
        driver.quit();
    }
    }

