package ro.ase.ie.g1106_s04.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ro.ase.ie.g1106_s04.R;
import ro.ase.ie.g1106_s04.adapters.MovieAdapter;
import ro.ase.ie.g1106_s04.database.DatabaseManager;
import ro.ase.ie.g1106_s04.database.MovieDAO;
import ro.ase.ie.g1106_s04.model.GenreEnum;
import ro.ase.ie.g1106_s04.model.Movie;
import ro.ase.ie.g1106_s04.model.ParentalGuidanceEnum;
import ro.ase.ie.g1106_s04.networking.HttpManager;

public class MainActivity extends AppCompatActivity implements IMovieEventListener{

    private static final int ADD_MOVIE = 100;
    private static final int UPDATE_MOVIE = 200;
    private ActivityResultLauncher<Intent> launcher;
    private final ArrayList<Movie> movieList = new ArrayList<>();
    private MovieAdapter movieAdapter;
    private RecyclerView recyclerView;
    private DatabaseManager databaseManager;
    private MovieDAO movieTable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        movieAdapter=new MovieAdapter(this,movieList);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(movieAdapter);
        databaseManager = DatabaseManager.getInstance(getApplicationContext());
        movieTable = databaseManager.getMovieDao();
        List<Movie> allMovies = databaseManager.getMovieDao().getAllMovies();
        movieList.addAll(allMovies);
        movieAdapter.notifyDataSetChanged();
        fetchMovies();
        launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult o) {
                        if(o.getResultCode() == RESULT_OK)
                        {

                            Intent data = o.getData();
                            Movie movie = data.getParcelableExtra("movie");
                            if(!movieList.contains(movie)){
                                movieList.add(movie);
                            }
                            else{
                                int position=movieList.indexOf(movie);
                                movieList.set(position, movie);
                            }
                            movieTable.insertMovie(movie);
                            Log.d("MainActivityTag", movie.toString());
                            movieAdapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if(item.getItemId() == R.id.add_movie_menu_item)
        {
            //add a new movie instance
            Intent intent = new Intent(MainActivity.this, MovieActivity.class);
            intent.putExtra("action_code", ADD_MOVIE);
            launcher.launch(intent);
        }
        else if(item.getItemId() == R.id.about_menu_item)
        {
            Toast.makeText(MainActivity.this,
                    "DMA2025 - G1106!",
                    Toast.LENGTH_LONG).show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMovieClick(int position) {
        Movie currentMovie = movieList.get(position);
        Intent intent = new Intent(MainActivity.this, MovieActivity.class);
        intent.putExtra("action_code", UPDATE_MOVIE);
        intent.putExtra("movie", currentMovie);
        launcher.launch(intent);
    }

    @Override
    public void onMovieDelete(int position) {
        movieTable.deleteMovie(movieList.get(position));
        movieList.remove(position);
        movieAdapter.notifyDataSetChanged();
    }

    private List<Movie> parseMovies(String jsonResult){
        List<Movie> list = new ArrayList<>();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);

        try {
            JSONArray array = new JSONArray(jsonResult);
            for(int i = 0; i < array.length(); i++){
                JSONObject obj = array.getJSONObject(i);

                String title = obj.getString("title");
                double budget = obj.getDouble("budget");
                // Correct Date Parsing
                String releaseStr = obj.getString("release");
                Date release = null;
                try {
                    release = sdf.parse(releaseStr);
                } catch (java.text.ParseException e) {
                    e.printStackTrace();
                    release = new Date(); // Fallback to current date or handle appropriate error
                }

                double rating = obj.getDouble("rating");
                String poster = obj.getString("poster");
                int duration = obj.getInt("duration");
                String genre = obj.getString("genre");
                boolean watched = obj.getBoolean("watched");
                String guidance = obj.getString("guidance");

                Movie movie = new Movie();
                movie.setTitle(title);
                movie.setBudget(budget);
                movie.setRelease(release);
                movie.setRating((float) rating);
                movie.setPosterUrl(poster);
                movie.setDuration(duration);
                movie.setGenre(GenreEnum.valueOf(genre));
                movie.setWatched(watched);
                movie.setpGuidance(ParentalGuidanceEnum.valueOf(guidance));

                list.add(movie);

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }

    private void fetchMovies(){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute( () -> {

            HttpManager manager = new HttpManager("https://jsonkeeper.com/b/FLBCO");
            String result = manager.process();
            List<Movie> newMovies = parseMovies(result);
            
            // SAVE TO DATABASE (Background)
            for(Movie m : newMovies){
                databaseManager.getMovieDao().insertMovie(m);
            }
            
            // READ FROM DATABASE (Background) - Get the fresh list including what we just saved
            List<Movie> allMovies = databaseManager.getMovieDao().getAllMovies();

            handler.post( ()-> {
                // UPDATE UI (Foreground)
                movieList.clear();
                movieList.addAll(allMovies);
                movieAdapter.notifyDataSetChanged();
                Toast.makeText(this, "Updated!", Toast.LENGTH_SHORT).show();
            });

        });
    }


}


