
## 🎯 High-Probability Exam Scenarios (Solutions)

Your colleague's list is **excellent**. It is highly accurate to what is typically asked in these exams.
Based on the "Topics" image you shared (`async`, `http`, `sqlite`, `files`, `json`), here are the **top 3 exercises** you should master, with their solutions.

### 1. The "Fetch & Save" (Combines N1 + P2) 🏆 **MOST LIKELY**
**Task**: "Download a list of movies from a URL (JSON) and save them into the Database. Then display them."

**Step 1: The Network Call (GET)**
*Where: Inside `MainActivity.java` or a `NetworkManager` class.*
```java
public void fetchAndSaveMovies(String urlAddress) {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Handler handler = new Handler(Looper.getMainLooper());

    executor.execute(() -> {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(urlAddress);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            InputStream is = connection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder buffer = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
            String jsonResult = buffer.toString();

            // PARSING
            JSONArray jsonArray = new JSONArray(jsonResult);
            List<Movie> validMovies = new ArrayList<>();
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                // Validation logic can go here
                Movie movie = new Movie();
                movie.setTitle(obj.getString("title"));
                // ... set other fields
                validMovies.add(movie);
                
                // SAVE TO DB IMMEDIATELY (Background thread!)
                databaseManager.getMovieDao().insertMovie(movie); 
            }

            handler.post(() -> {
                // UPDATE UI
                movieList.clear();
                movieList.addAll(validMovies);
                movieAdapter.notifyDataSetChanged();
                Toast.makeText(this, "Imported " + validMovies.size() + " movies!", Toast.LENGTH_SHORT).show();
            });

        } catch (Exception e) {
            e.printStackTrace();
            handler.post(() -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
        } finally {
            if(connection != null) connection.disconnect();
            // close reader...
        }
    });
}
```

### 2. The "Export to File" (Exercise F1) 📂
**Task**: "Create a menu option to save the current movie list to a `movies.json` file in internal storage."

**Step 1: The Writer**
*Where: Inside `MainActivity.java` (e.g., in `onOptionsItemSelected`)*
```java
private void exportToInternalStorage() {
    try {
        // Convert List -> JSONArray
        JSONArray array = new JSONArray();
        for(Movie m : movieList) {
            JSONObject obj = new JSONObject();
            obj.put("title", m.getTitle());
            obj.put("budget", m.getBudget());
            // ... add others
            array.put(obj);
        }

        // Write to file
        FileOutputStream fos = openFileOutput("movies_backup.json", MODE_PRIVATE);
        fos.write(array.toString().getBytes());
        fos.close();

        Toast.makeText(this, "Backup saved to internal storage!", Toast.LENGTH_LONG).show();

    } catch (Exception e) {
        e.printStackTrace();
    }
}
```

### 3. The "Preferences Filter" (Exercise P1) ⚙️
**Task**: "Save a 'min_budget' filter in settings and apply it when the app opens."

**Step 1: Save (e.g., in a Dialog or SettingsActivity)**
```java
SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
prefs.edit().putFloat("min_budget", 50000.0f).apply();
```

**Step 2: Load & Filter (in `MainActivity.onCreate` or `onResume`)**
```java
SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
float minBudget = prefs.getFloat("min_budget", 0.0f); // Default 0

// Filter the list
List<Movie> filteredList = new ArrayList<>();
for(Movie m : movieList) {
    if(m.getBudget() >= minBudget) {
        filteredList.add(m);
    }
}
movieAdapter.setMovies(filteredList); // You might need to add this method to Adapter
movieAdapter.notifyDataSetChanged();
```

---
## 🧪 Recommended Order of Practice
1.  **Start with P2 (Fix Room)**: You can't save anything if the DB doesn't work. Add `getAllMovies()` to `MovieDAO`.
2.  **Do N1 (Networking)**: This is the hardest part to type from memory. Practice fetching JSON and parsing it.
3.  **Do F1 (Files)**: It's easiest but usually required. "Export to JSON" is a classic requirement.
