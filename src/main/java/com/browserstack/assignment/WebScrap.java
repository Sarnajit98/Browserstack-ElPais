package com.browserstack.assignment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WebScrap {

    public static void main(String[] args) {
        // For local test using ChromeDriver
        System.setProperty("webdriver.chrome.driver", "chromedriver.exe");

        WebDriver driver = new ChromeDriver();
        runScraper(driver);
        driver.quit();
    }

    // Main method to run the web scraper
    public static void runScraper(WebDriver driver) {
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.manage().window().maximize();

        List<String> translatedTitlesList = new ArrayList<>();

        try {
            driver.get("https://elpais.com/opinion/");

            String lang = driver.findElement(By.tagName("html")).getAttribute("lang");
            if (!"es-ES".equalsIgnoreCase(lang)) {
                System.out.println("Warning: Site is not displayed in Spanish!");
            } else {
                System.out.println("Site is displayed in Spanish.");
            }

            try {
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
                WebElement acceptBtn = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//button[.//span[normalize-space()='Accept']]")));
                acceptBtn.click();
                Thread.sleep(1000);
                System.out.println("Closed cookie popup.");
            } catch (Exception e) {
                System.out.println("No popup appeared.");
            }

            List<WebElement> articleElements = driver.findElements(By.xpath("//article"));
            List<String> articleUrls = new ArrayList<>();

            // Iterate through the article elements and extract URLs
            for (WebElement article : articleElements) {
                try {
                    WebElement link = article.findElement(By.xpath(".//a"));
                    String href = link.getAttribute("href");

                    if (isValidArticle(href)) {
                        articleUrls.add(href);
                    }

                    if (articleUrls.size() == 5) break;
                } catch (Exception e) {
                    System.out.println("Failed to extract URL from article block: " + e.getMessage());
                }
            }

            int count = 1;

            // Iterate through the collected article URLs
            for (String articleUrl : articleUrls) {
                try {
                    driver.get(articleUrl);

                    try {
                        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
                        WebElement acceptBtn = wait.until(ExpectedConditions.elementToBeClickable(
                                By.xpath("//button[.//span[normalize-space()='Accept']]")));
                        acceptBtn.click();
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        System.out.println("No popup on article page.");
                    }

                    WebElement titleElement;
                    try {
                        titleElement = driver.findElement(By.xpath("//header//h1"));
                    } catch (Exception e) {
                        System.out.println("Failed to get title: " + articleUrl);
                        continue;
                    }

                    String title = titleElement.getText();
                    String translatedTitle = translateWithGoogle(title);
                    System.out.println("Translated Title: " + translatedTitle);
                    translatedTitlesList.add(translatedTitle);

                    List<WebElement> paragraphs = driver.findElements(By.xpath("//article//p"));
                    if (paragraphs.isEmpty()) {
                        paragraphs = driver.findElements(By.xpath("//div[contains(@class,'article_body')]//p"));
                    }
                    if (paragraphs.isEmpty()) {
                        paragraphs = driver.findElements(By.xpath("//div[contains(@class,'a_c')]//p"));
                    }
                    if (paragraphs.isEmpty()) {
                        paragraphs = driver.findElements(By.xpath("//section[contains(@class,'a_c')]//p"));
                    }

                    if (paragraphs.isEmpty()) {
                        System.out.println("No content found in article: " + articleUrl);
                        continue;
                    }

                    StringBuilder contentBuilder = new StringBuilder();
                    for (WebElement p : paragraphs) {
                        contentBuilder.append(p.getText()).append("\n");
                    }

                    String content = contentBuilder.toString();

                    try {
                        WebElement imageElement = driver.findElement(By.xpath("//figure//img | //img[contains(@class, 'foto')]"));
                        String imageUrl = imageElement.getAttribute("src");

                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            downloadImage(imageUrl, "article" + count + ".jpg");
                        } else {
                            System.out.println("No image URL found for article " + count);
                        }
                    } catch (Exception e) {
                        System.out.println("Failed to extract image for article " + count + ": " + e.getMessage());
                    }

                    System.out.println("Article " + count);
                    System.out.println("Title: " + title);
                    System.out.println("Content (first 200 chars): " + content.substring(0, Math.min(200, content.length())) + "...");
                    System.out.println("--------------------------------------------------");

                    count++;

                } catch (Exception e) {
                    System.out.println("Skipping article due to error: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.out.println("Unhandled exception in runScraper: " + e.getMessage());
        } finally {
            System.out.println("\n Analyzing Translated Headers for Repeated Words:");
            analyzeRepeatedWords(translatedTitlesList);
        }
    }

    // Checks if the article URL is valid
    public static boolean isValidArticle(String url) {
        return url.matches(".*/\\d{4}-\\d{2}-\\d{2}/.*");
    }

    // Downloads an image from the given URL and saves it to the "images" directory
    public static void downloadImage(String imageUrl, String fileName) {
        try {
            File dir = new File("images");
            if (!dir.exists()) {
                dir.mkdir();
            }

            InputStream in = new URL(imageUrl).openStream();
            Files.copy(in, Paths.get("images/" + fileName), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Saved cover image: " + fileName);
        } catch (Exception e) {
            System.out.println("Failed to download image: " + e.getMessage());
        }
    }

    // Translates a given text from Spanish to English using Google Translate API
    public static String translateWithGoogle(String text) throws IOException {
        OkHttpClient client = new OkHttpClient();
        String url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=es&tl=en&dt=t&q="
                + URLEncoder.encode(text, StandardCharsets.UTF_8);

        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String body = response.body().string();
                JSONArray arr = new JSONArray(body);
                return arr.getJSONArray(0).getJSONArray(0).getString(0);
            } else {
                return "Translation failed: " + response.code();
            }
        }
    }

    // Analyzes the translated titles for repeated words
    public static void analyzeRepeatedWords(List<String> titles) {
        Map<String, Integer> wordCount = new HashMap<>();

        for (String title : titles) {
            String[] words = title.toLowerCase().split("\\W+");
            for (String word : words) {
                if (word.isBlank()) continue;
                wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
            }
        }

        boolean found = false;
        for (Map.Entry<String, Integer> entry : wordCount.entrySet()) {
            if (entry.getValue() > 2) {
                System.out.println("Repetitive Word: '" + entry.getKey() + "' -> " + entry.getValue() + " times");
                found = true;
            }
        }

        if (!found) {
            System.out.println("No words repeated more than twice.");
        }
    }
}
