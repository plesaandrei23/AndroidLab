# Android Exam Code Bible

This document contains the exact code snippets you will likely need for your exam. **Memorize the patterns**, but you can copy-paste these structures into your `MainActivity` or helper classes during practice.

---

## 1. Networking (HTTP & JSON) 🌐
**Scenario**: "Fetch a list of movies from a URL like `https://api.example.com/movies.json`."

### A. Helper Class (`HttpManager.java`)
Create a simple helper class to handle the connection. **Do not run this on the main thread.**

```java
package ro.ase.ie.g1106_s04.networking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpManager {
    private String urlAddress;

    public HttpManager(String urlAddress) {
        this.urlAddress = urlAddress;
    }

    public String process() {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        StringBuilder result = new StringBuilder();

        try {
            URL url = new URL(urlAddress);
            connection = (HttpURLConnection) url.openConnection();
            // Optional: connection.setRequestMethod("GET");
            connection.connect();

            InputStream inputStream = connection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // ALWAYS CLOSE STREAMS
            if (connection != null) connection.disconnect();
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result.toString();
    }
}
```

### B. Calling it & Parsing JSON (In `MainActivity`)
Use `ExecutorService` (Background) and `Handler` (UI) to fetch and parse.

```java
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;

// ... inside MainActivity ...

private void loadMoviesFromNetwork(String url) {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Handler handler = new Handler(Looper.getMainLooper());

    executor.execute(() -> {
        // 1. BACKGROUND: Fetch data
        HttpManager manager = new HttpManager(url);
        String jsonResult = manager.process();

        handler.post(() -> {
            // 2. UI THREAD: Parse and Update
            try {
                // Assuming the response is a JSON Array: [...]
                JSONArray jsonArray = new JSONArray(jsonResult);
                
                movieList.clear(); // Clear old data if needed
                
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject object = jsonArray.getJSONObject(i);
                    
                    // Parse fields (MATCH KEYS TO JSON!)
                    String title = object.getString("title");
                    double budget = object.getDouble("budget");
                    // ... etc ...
                    
                    // Handle Date (often a String or Long in JSON)
                    // Date release = new Date(object.getLong("release")); 
                    
                    // Create object
                    Movie movie = new Movie(title, budget, ...); 
                    movieList.add(movie);
                }
                
                // NOTIFY ADAPTER
                movieAdapter.notifyDataSetChanged();
                
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error parsing JSON", Toast.LENGTH_SHORT).show();
            }
        });
    });
}
```

---

## 2. Persistence: Local Files 📂
**Scenario**: "Save the current list of movies to a text file and read it back."

### A. Write to File (Internal Storage)
```java
import android.content.Context;
import java.io.OutputStreamWriter;

private void saveToFile(String fileName) {
    try {
        // openFileOutput creates/opens a private file for the app
        OutputStreamWriter writer = new OutputStreamWriter(openFileOutput(fileName, Context.MODE_PRIVATE));
        
        // Format your data as a generic String (CSV, JSON, etc.)
        for (Movie m : movieList) {
            writer.write(m.getTitle() + "," + m.getBudget() + "\n");
        }
        
        writer.close();
        Toast.makeText(this, "Saved to " + fileName, Toast.LENGTH_SHORT).show();
    } catch (IOException e) {
        e.printStackTrace();
    }
}
```

### B. Read from File
```java
import java.io.BufferedReader;
import java.io.InputStreamReader;

private void readFromFile(String fileName) {
    try {
        BufferedReader reader = new BufferedReader(new InputStreamReader(openFileInput(fileName)));
        String line;
        
        while ((line = reader.readLine()) != null) {
            // Assuming CSV format: "Title,Budget"
            String[] tokens = line.split(",");
            if(tokens.length >= 2) {
                String title = tokens[0];
                Double budget = Double.parseDouble(tokens[1]);
                
                // Add to list...
            }
        }
        reader.close();
        movieAdapter.notifyDataSetChanged();
        
    } catch (IOException e) {
        e.printStackTrace();
        Toast.makeText(this, "File not found!", Toast.LENGTH_SHORT).show();
    }
}
```

---

## 3. Persistence: Shared Preferences ⚙️
**Scenario**: "Save the user's username or the 'Last Opened' time."

```java
import android.content.SharedPreferences;

// 1. SAVE
private void savePreference(String key, String value) {
    // "settings" is the name of the preference file
    SharedPreferences pfs = getSharedPreferences("settings", MODE_PRIVATE);
    SharedPreferences.Editor editor = pfs.edit();
    editor.putString(key, value);
    editor.apply(); // Async save
}

// 2. READ
private String getPreference(String key) {
    SharedPreferences pfs = getSharedPreferences("settings", MODE_PRIVATE);
    return pfs.getString(key, "default_value"); // Returns default if key missing
}
```

---

## 4. Database: Room (Completing the Missing Pieces) 🗄️
Your `MovieDAO` is missing the SELECT query.

### A. Update `MovieDAO.java`
```java
@Dao
public interface MovieDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertMovie(Movie movie);

    @Delete
    int deleteMovie(Movie movie);

    // ADD THIS!
    @Query("SELECT * FROM MovieTable")
    List<Movie> getAllMovies();
    
    // Optional: Delete all
    @Query("DELETE FROM MovieTable")
    void deleteAll();
}
```

### B. Using it in MainActivity (Async!)
Even if `allowMainThreadQueries` is on, using threads is better for the exam.

```java
// Loading data on startup (in onCreate)
ExecutorService executor = Executors.newSingleThreadExecutor();
Handler handler = new Handler(Looper.getMainLooper());

executor.execute(() -> {
    // Background Read
    List<Movie> fromDb = databaseManager.getMovieDao().getAllMovies();
    
    handler.post(() -> {
        // UI Update
        movieList.clear();
        movieList.addAll(fromDb);
        movieAdapter.notifyDataSetChanged();
    });
});
```

---

## 5. XML Parsing (DOM Parser) 📄
**Scenario**: "The data comes in XML format, not JSON."

```java
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.io.ByteArrayInputStream;

private void parseXML(String xmlData) {
    try {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        
        // Convert String -> InputStream
        InputStream is = new ByteArrayInputStream(xmlData.getBytes());
        Document doc = builder.parse(is);
        doc.getDocumentElement().normalize();

        // Assuming XML: <movies><movie><title>...</title></movie></movies>
        NodeList nodeList = doc.getElementsByTagName("movie");

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;

                // Helper method to get value of a tag
                String title = element.getElementsByTagName("title").item(0).getTextContent();
                String budgetStr = element.getElementsByTagName("budget").item(0).getTextContent();
                
                // Create Movie & Add
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```

---

## 6. Image Loading (Using your `DownloadTask`) 🖼️
**Scenario**: "Each movie in the list has an image URL. Display it."

In `MovieAdapter.java` -> `onBindViewHolder` (or `MovieHolder`):

```java
// Inside onBindViewHolder
if (currentMovie.getPosterUrl() != null && !currentMovie.getPosterUrl().isEmpty()) {
    // Use the Thread/Runnable pattern (or your DownloadTask class if you want to reuse it)
    DownloadTask task = new DownloadTask(currentMovie.getPosterUrl(), holder.imageViewPoster);
    new Thread(task).start();
}
```
*Note: Depending on implementation, `DownloadTask` should handle connection logic similar to `HttpManager` but with `BitmapFactory.decodeStream`.*
