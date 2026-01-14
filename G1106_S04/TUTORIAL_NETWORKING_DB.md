# Tutorial: Fetching Movies from Network & Saving to DB
## "The Clean Architecture Route"

You chose the **Network Manager** approach. This is excellent because it separates "Connecting to the Internet" from "Managing the UI".

Here is your step-by-step implementation plan.

---

## 🟢 Step 1: Create the `HttpManager`
**Location**: `ro.ase.ie.g1106_s04.networking`

This class has **one job**: Take a URL string, go to the internet, and bring back the raw text (JSON). It doesn't care about Movies or Databases.

**The Code to Write:**
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
            // 1. Open the Connection
            URL url = new URL(urlAddress);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect(); // Handshake with server

            // 2. Open the Stream (The Pipe)
            InputStream inputStream = connection.getInputStream();
            
            // 3. Wrap it (The Filter) - allows reading line-by-line instead of byte-by-byte
            reader = new BufferedReader(new InputStreamReader(inputStream));

            // 4. Read Loop
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 5. Cleanup - Close everything to prevent memory leaks
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

---

## 🟢 Step 2: Prepare the Database to READ
**Location**: `ro.ase.ie.g1106_s04.database.MovieDAO`

Your database can currently write (`insert`), but it cannot read. Let's fix that.

**The Code to Add:**
```java
@Query("SELECT * FROM MovieTable")
List<Movie> getAllMovies();
```

---

## 🟢 Step 3: Connect Everything in `MainActivity`

This is the brain. We need to do two things here:
1.  **Load Data on Start**: When the app opens, read from the DB.
2.  **Fetch Data on Command**: When user asks, download -> parse -> save -> reload.

### 3.1 Add the Parsing Method
Add this helper method to `MainActivity` to convert the raw JSON string into your Movie objects.

```java
private List<Movie> parseMovies(String jsonResult) {
    List<Movie> list = new ArrayList<>();
    try {
        // Assume API returns a generic object wrapping the list, e.g. { "movies": [...] }
        // OR if it returns an array directly: new JSONArray(jsonResult)
        
        // Example for Array: [{"title":"Dune", ...}, {...}]
        JSONArray array = new JSONArray(jsonResult);

        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);
            
            // EXTRACT DATA - CAREFUL WITH KEY NAMES!
            // The strings "title", "budget" must match the JSON EXACTLY.
            String title = obj.getString("title"); // e.g. "Dune: Part Two"
            
            // Handle Budget (might be null or missing)
            Double budget = obj.optDouble("budget", 0.0);
            
            // Handle Date (Usually a Long timestamp or a String)
            // If Long: new Date(obj.getLong("releaseDate"));
             Date release = new Date(); // Mock date if missing
            
            // Create the object
            Movie movie = new Movie(title, budget, release, 120, GenreEnum.Action, ParentalGuidanceEnum.PG13, 5.0f, false, "https://...");
            list.add(movie);
        }
    } catch (JSONException e) {
        e.printStackTrace();
    }
    return list;
}
```

### 3.2 Add the Network Method
This uses the "Executor + Handler" pattern (The replacement for AsyncTask).

```java
private void fetchMovies() {
    // 1. Create a background thread
    ExecutorService executor = Executors.newSingleThreadExecutor();
    
    // 2. Create a handler to talk back to the UI
    Handler handler = new Handler(Looper.getMainLooper());

    executor.execute(() -> {
        // --- BACKGROUND THREAD START ---
        
        // A. Network Call
        HttpManager manager = new HttpManager("https://jsonkeeper.com/b/YOUR_ID");
        String result = manager.process();
        
        // B. Parsing
        List<Movie> newMovies = parseMovies(result);
        
        // C. Database Save (Still background!)
        for(Movie m : newMovies) {
             databaseManager.getMovieDao().insertMovie(m);
        }
        
        // D. Database Read (Get the FULL list, including old ones)
        List<Movie> allMovies = databaseManager.getMovieDao().getAllMovies();
        
        // --- BACKGROUND THREAD END ---

        handler.post(() -> {
            // --- UI THREAD START ---
            movieList.clear();
            movieList.addAll(allMovies);
            movieAdapter.notifyDataSetChanged();
            Toast.makeText(this, "Updated!", Toast.LENGTH_SHORT).show();
            // --- UI THREAD END ---
        });
    });
}
```

---

## 🟢 Step 4: Call it!

1.  In `onCreate`:
    ```java
    // Load existing data immediately
    List<Movie> dbMovies = databaseManager.getMovieDao().getAllMovies();
    movieList.addAll(dbMovies);
    ```
    *(Note: For the exam, they might let you do this on Main Thread. If not, wrap it in executor).*

2.  In `onCreateOptionsMenu`, add a button "Load from Network" that calls `fetchMovies()`.

---

## 🧠 Why this is "Cleaner"

1.  **Reusability**: You can use `HttpManager` to fetch *anything* (Movies, Weather, News), not just Movies.
2.  **Readability**: Your `MainActivity` doesn't have 50 lines of `HttpURLConnection` boilerplate cluttering it up.
3.  **Safety**: Separation of threads is clear. Background does the work, Handler touches the Views.
