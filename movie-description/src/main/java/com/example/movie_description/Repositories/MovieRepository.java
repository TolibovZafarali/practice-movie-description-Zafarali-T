package com.example.movie_description.Repositories;

import com.example.movie_description.Models.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, Integer> {
}

