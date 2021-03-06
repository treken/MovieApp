package com.KamilIsmail.MovieApp.service;

import com.KamilIsmail.MovieApp.Constants;
import com.KamilIsmail.MovieApp.DAO.FavouriteDao;
import com.KamilIsmail.MovieApp.DTO.BooleanDTO;
import com.KamilIsmail.MovieApp.DTO.DiscoverMovieDTO;
import com.KamilIsmail.MovieApp.entities.FavouritesEntity;
import com.KamilIsmail.MovieApp.entities.MoviesEntity;
import com.KamilIsmail.MovieApp.repository.FavouriteRepository;
import com.KamilIsmail.MovieApp.repository.MovieRepository;
import info.movito.themoviedbapi.TmdbApi;
import info.movito.themoviedbapi.model.MovieDb;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Math.toIntExact;

/**
 * @author kamilismail
 * Serwis wywoływany z kontrolera.
 */
@Service
public class FavouritesServiceImpl implements FavouritesService {
    @Autowired
    FavouriteRepository favsRepository;

    @Autowired
    @SuppressWarnings("SpringJavaAutowiringInspection")
    FavouriteDao favsDao;

    @Autowired
    MovieRepository movieRepository;

    /**
     * Metoda zwracająca listę pozycji ulubionych przez użytkownika.
     * @param userid
     * @return
     */
    @Override
    public List<DiscoverMovieDTO> getFavourites(int userid) {
        return favsRepository.findFavouritesEntityByUserId(userid).stream()
                .map(p -> new DiscoverMovieDTO(p.getMoviesByMovieId().getMediaType(), Integer.toString(p.getMoviesByMovieId().getTmdbId()),
                        p.getMoviesByMovieId().getMovieName(), Constants.getPosterPath() + p.getMoviesByMovieId().getBackdropPath(),
                        p.getMoviesByMovieId().getReleaseDate(), String.valueOf(p.getMoviesByMovieId().getAvarageRating())))
                .collect(Collectors.toList());
    }

    /**
     * Metoda dodająca daną pozycję do listy ulubionych.
     * @param userid
     * @param movieId
     * @return
     */
    @Override
    public BooleanDTO addFavourite(int userid, int movieId) {
        try {
            MoviesEntity moviesEntity = movieRepository.findByTmdbId(movieId);
            if (moviesEntity == null)
                return favsDao.addFavourite(userid, movieId);
            if (favsRepository.findFavouritesEntityByUserIdAndMovieId(userid, moviesEntity.getMovieId()) != null)
                return new BooleanDTO(false);
            else
                return favsDao.addFavourite(userid, movieId);
        } catch (Exception e) {
            return new BooleanDTO(false);
        }
    }

    /**
     * Metoda usuwająca daną pozycję z listy ulubionych.
     * @param userid
     * @param movieId
     * @return
     */
    @Override
    public BooleanDTO deleteFavourite(int userid, int movieId) {
        return favsDao.deleteFavourite(userid, movieId);
    }

}
