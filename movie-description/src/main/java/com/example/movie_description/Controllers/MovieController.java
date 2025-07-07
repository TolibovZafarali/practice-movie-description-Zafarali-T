package com.example.movie_description.Controllers;

import com.example.movie_description.Models.Movie;
import com.example.movie_description.Repositories.MovieRepository;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.apache.http.HttpException;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/movies")
public class MovieController {

    private final MovieRepository movieRepository;

    public MovieController(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    @GetMapping("")
    public String renderMoviesHomePage() {
        List<Movie> allMovies = movieRepository.findAll();
        StringBuilder moviesList = new StringBuilder();
        for (Movie movie : allMovies) {
            moviesList.append("<li><a href='/movies/details/").append(movie.getId()).append("'>").append(movie.getTitle()).append(" - ").append(movie.getDescription()).append("</a></li>");
        }
        return """
                <html>
                <body>
                <h2>MOVIES</h2>
                <ul>
                """ +
                moviesList +
                """
                        </ul>
                        <p><a href='/movies/add'>Add</a> another movie or <a href='/movies/delete'>delete</a> one or more movies.</p>
                        </body>
                        </html>
                        """;
    }

    @GetMapping("/details/{movieId}")
    public String displayMovieDetails(@PathVariable(value = "movieId") int movieId) {
        Movie currentMovie = movieRepository.findById(movieId).orElse(null);
        if (currentMovie != null) {
            return """
                    <html>
                    <body>
                    <h3>Movie Details</h3>
                    """ +
                    "<p><b>ID:</b> " + movieId + "</p>" +
                    "<p><b>Title:</b> " + currentMovie.getTitle() + "</p>" +
                    "<p><b>Description:</b> " + currentMovie.getDescription() + "</p>" +
                    "<p><a href='/movies/update/" + currentMovie.getId() + "'>Update</a></p>" +
                    """
                            </body>
                            </html>
                            """;
        } else {
            return """
                    <html>
                    <body>
                    <h3>Movie Details</h3>
                    <p>Movie not found. <a href='/movies'>Return to list of movies.</a></p>
                    </body>
                    </html>
                    """;
        }
    }

    @GetMapping("/update/{movieId}")
    public String updateMovieDetails(@PathVariable(value = "movieId") int movieId) {
        Movie currentMovie = movieRepository.findById(movieId).orElse(null);
        if (currentMovie != null) {
            return """
                    <html>
                    <body>
                    <h3>Update Movie Details</h3>
                    """ +
                    "<form action='/movies/update/" + currentMovie.getId() + "' method='POST'>" +
                    "<p>Update the details of a movie:</p>" +
                    "<input type='text' name='title' value='" + currentMovie.getTitle() + "' placeholder='Title' />" +
                    "<input type='text' name='description' value='" + currentMovie.getDescription() + "' placeholder='Description' />" +
                    "<button type='submit'>Update</button>" +
                    """
                    </body>
                    </html>
                    """;
        } else {
            return """
                    <html>
                    <body>
                    <h3>Update Movie</h3>
                    <p>Movie not found. <a href='/movies'>Return to list of movies.</a></p>
                    </body>
                    </html>
                    """;
        }
    }

    @PostMapping("/update/{movieId}")
    public String processUpdateMovieDetails(@PathVariable(value = "movieId") int movieId, @RequestParam(value = "title") String title, @RequestParam(value = "description") String description) {
        Movie movieToUpdate = movieRepository.findById(movieId).orElse(null);

        if (movieToUpdate != null) {
            movieToUpdate.setTitle(title);
            movieToUpdate.setDescription(description);

            movieRepository.save(movieToUpdate);

            return """
                <html>
                <body>
                <h3>MOVIE UPDATED</h3>
                """ +
                    "<p>You have successfully updated " + title + " to the collection.</p>" +
                    """
                    <p>View the <a href='/movies'>updated list</a> of movies.</p>
                    </body>
                    </html>
                    """;
        } else {
            return """
                    <html>
                    <body>
                    <h3>Update Movie</h3>
                    <p>Movie not found. <a href='/movies'>Return to list of movies.</a></p>
                    </body>
                    </html>
                    """;
        }
    }

    @GetMapping("/add")
    public String renderAddMovieForm() {
        return """
                <html>
                <body>
                <form action='/movies/add' method='POST'>
                <p>Enter the details of a movie:</p>
                <input type='text' name='title' placeholder='Title' />
                <button type='submit'>Add</button>
                </form>
                </body>
                </html>
                """;
    }

    @PostMapping("/add")
    public String processAddMovieForm(@RequestParam(value="title") String title) throws HttpException, IOException {

        Client client = new Client();

        String query = "Generate a text description of 100 characters for the movie: " + title + ". Search for the movie first and respond with \"Not found\" if the movie doesn't exist" ;

        GenerateContentResponse response = client.models.generateContent("gemini-2.0-flash-001", query, null);

        String description = response.text();

        if (!description.trim().equalsIgnoreCase("not found")){
            Movie newMovie = new Movie(title, description);
            movieRepository.save(newMovie);
            return """
                <html>
                <body>
                <h3>MOVIE ADDED</h3>
                """ +
                    "<p>You have successfully added " + title + " to the collection.</p>" +
                    """
                    <p><a href='/movies/add'>Add</a> another movie or view the <a href='/movies'>updated list</a> of movies.</p>
                    </body>
                    </html>
                    """;
        } else {
            return String.format("""
                                <html>
                                <body>
                                <h3>Movie Not Found</h3>
                                <p>We couldn’t find any info for "<b>%s</b>".</p>
                                <p>Please check the spelling or try another movie title.</p>
                                <p><a href='/movies/add'>Try again</a> or go <a href='/movies'>back to list</a>.</p>
                                </body>
                                </html>
                                """, title);
        }
    }

    @GetMapping("/delete")
    public String renderDeleteMovieForm() {
        List<Movie> allMovies = movieRepository.findAll();
        StringBuilder moviesList = new StringBuilder();
        for (Movie movie : allMovies) {
            int currId = movie.getId();
            moviesList.append("<li><input id='").append(currId).append("' name='movieIds' type='checkbox' value='").append(currId).append("' />").append(movie.getTitle()).append(" - ").append(movie.getDescription()).append("</li>");
        }
        return """
                <html>
                <body>
                <form action='/movies/delete' method='POST'>
                <p>Select which movies you wish to delete:</p>
                <ul>
                """ +
                moviesList +
                """
                </ul>
                <button type='submit'>Submit</button>
                </form>
                </body>
                </html>
                """;
    }

    @PostMapping("/delete")
    public String ProcessDeleteMovieForm(@RequestParam(value="movieIds") int[] movieIds) {
        for (int id : movieIds) {
            Movie currMovie = movieRepository.findById(id).orElse(null);
            if (currMovie != null) {
                movieRepository.deleteById(id);

            }
        }
        String header = movieIds.length > 1 ? "MOVIES " : "MOVIE ";
        return """
                <html>
                <body>
                <h3>
                """ +
                header +
                """
                DELETED</h3>
                <p>Deletion successful.</p>
                <p>View the <a href='/movies'>updated list</a> of movies.</p>
                </body>
                </html>
                """;
    }
}