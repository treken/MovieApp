package com.kamilismail.movieappandroid.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.baoyz.widget.PullRefreshLayout;
import com.kamilismail.movieappandroid.DTO.BooleanDTO;
import com.kamilismail.movieappandroid.DTO.search_movies.GetMovieDTO;
import com.kamilismail.movieappandroid.R;
import com.kamilismail.movieappandroid.SessionController;
import com.kamilismail.movieappandroid.adapters.MovieCommentsRecyclerViewAdapter;
import com.kamilismail.movieappandroid.connection.ApiFavourites;
import com.kamilismail.movieappandroid.connection.ApiMovieComments;
import com.kamilismail.movieappandroid.connection.ApiRatings;
import com.kamilismail.movieappandroid.connection.ApiReminders;
import com.kamilismail.movieappandroid.connection.ApiSearch;
import com.kamilismail.movieappandroid.connection.ApiWantToWatch;
import com.kamilismail.movieappandroid.helpers.RetrofitBuilder;
import com.kamilismail.movieappandroid.helpers.UnitConversionHelper;
import com.squareup.picasso.Picasso;
import com.willy.ratingbar.ScaleRatingBar;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import ru.tinkoff.scrollingpagerindicator.ScrollingPagerIndicator;

public class MovieDetailsFragment extends Fragment {

    public interface SendArgumentsAndLaunchFragment {
        void logoutUser();
    }

    private SendArgumentsAndLaunchFragment mCallback;

    public static String TAG = "MovieDetailsFragment";
    private SessionController sessionController;

    private String id;
    private String title;

    private Boolean favBool;
    private Boolean reminderBool;
    private Boolean wantBool;

    @BindView(R.id.title)
    TextView mTitle;
    @BindView(R.id.poster)
    ImageView mPoster;
    @BindView(R.id.avarageRating)
    TextView mAvarageRating;
    @BindView(R.id.ratingBar)
    ScaleRatingBar mRatingBar;
    @BindView(R.id.addReminder)
    Button mReminder;
    @BindView(R.id.addFav)
    Button mAddFav;
    @BindView(R.id.addWantToWatch)
    Button mWantToWatch;
    @BindView(R.id.tvChanel)
    TextView mTVChanel;
    @BindView(R.id.tvlogo)
    ImageView mTVLogo;
    @BindView(R.id.release)
    TextView mRelease;
    @BindView(R.id.description)
    TextView mDescription;
    @BindView(R.id.tvDate)
    TextView mTVDate;
    @BindView(R.id.sendRating)
    Button mSendRating;
    @BindView(R.id.userRating)
    TextView tUserRating;
    @BindView(R.id.onTV)
    TextView tOnTV;
    @BindView(R.id.chanelInfo)
    TextView tChanelInfo;
    @BindView(R.id.swipeRefreshLayout)
    PullRefreshLayout pullRefreshLayout;
    @BindView(R.id.mProgressBarProfile)
    ProgressBar mProgressBar;
    @BindView(R.id.shareButton)
    ImageButton mShareButton;
    @BindView(R.id.backdrop)
    ImageView mBackdrop;
    @BindView(R.id.img_send)
    ImageView mImgSend;
    @BindView(R.id.img_reminder)
    ImageView mImgReminder;
    @BindView(R.id.img_want_watch)
    ImageView mImgWantToWatch;
    @BindView(R.id.img_fav)
    ImageView mImgFav;
    @BindView(R.id.relativeLayout)
    RelativeLayout relativeLayout;
    @BindView(R.id.nocomments)
    TextView nocomments;
    @BindView(R.id.addComment)
    Button addComment;

    public MovieDetailsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_movie_details, container, false);
        ButterKnife.bind(this, view);
        this.sessionController = new SessionController(getContext());
        Bundle args = this.getArguments();
        this.id = args.getString("id");
        this.title = args.getString("title");
        mTitle.setText(this.title);
        setVisibility(View.GONE);
        nocomments.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);
        pullRefreshLayout.setRefreshStyle(PullRefreshLayout.STYLE_MATERIAL);
        pullRefreshLayout.setOnRefreshListener(new PullRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getData(view);
            }
        });
        getData(view);

        mShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                String shareBody = "Check out this movie:\n" + mTitle.getText().toString()
                        + ", " + mAvarageRating.getText().toString() + ".";
                if (mRatingBar.getRating() > 0)
                    shareBody += " I've rated it for : " + mRatingBar.getRating() + "/10.";
                if (!mTVDate.getText().toString().equals("No tv emission info"))
                    shareBody += " It's on tv today at " + mTVDate.getText().toString() + " on "
                            + mTVChanel.getText().toString();
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
                startActivity(Intent.createChooser(shareIntent, "Share via"));
            }
        });

        mAddFav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setMessage("Add this title to favourites?")
                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                favClicked(view);
                            }
                        })
                        .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User cancelled the dialog
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });
        mWantToWatch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setMessage("Add this title to want to watch?")
                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                wantsClicked(view);
                            }
                        })
                        .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User cancelled the dialog
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });
        mReminder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setMessage("Set up reminder for this title?")
                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                reminderClicked(view);
                            }
                        })
                        .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User cancelled the dialog
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });

        mSendRating.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setMessage("Set rating " + mRatingBar.getRating() + "/10 for this movie?")
                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                sendRatingClicked(view);
                            }
                        })
                        .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User cancelled the dialog
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });

        addComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (addComment.getText().toString().toLowerCase().contains("delete")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setMessage("Delete Your review?")
                            .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    deleteComment(view);
                                }
                            })
                            .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // User cancelled the dialog
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                } else {
                    AlertDialog.Builder alert = new AlertDialog.Builder(v.getContext());
                    alert.setTitle("Add review");
                    alert.setMessage("When ready just click 'SEND'");
                    final EditText input = new EditText(v.getContext());
                    alert.setView(input);

                    alert.setPositiveButton("SEND", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            addComment(view, input.getText().toString());
                        }
                    });

                    alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Canceled.
                        }
                    });

                    alert.show();
                }
            }
        });

        return view;
    }

    private void setVisibility(int visibility) {
        mPoster.setVisibility(visibility);
        mAvarageRating.setVisibility(visibility);
        mRatingBar.setVisibility(visibility);
        mReminder.setVisibility(visibility);
        mAddFav.setVisibility(visibility);
        mWantToWatch.setVisibility(visibility);
        mTVChanel.setVisibility(visibility);
        mTVLogo.setVisibility(visibility);
        mRelease.setVisibility(visibility);
        mDescription.setVisibility(visibility);
        mTVDate.setVisibility(visibility);
        mSendRating.setVisibility(visibility);
        tUserRating.setVisibility(visibility);
        tOnTV.setVisibility(visibility);
        tChanelInfo.setVisibility(visibility);
        mShareButton.setVisibility(visibility);
        mBackdrop.setVisibility(visibility);
        mImgFav.setVisibility(visibility);
        mImgReminder.setVisibility(visibility);
        mImgSend.setVisibility(visibility);
        mImgWantToWatch.setVisibility(visibility);
        relativeLayout.setVisibility(visibility);
        addComment.setVisibility(visibility);
    }

    private void getData(final View view) {
        Retrofit retrofit = RetrofitBuilder.createRetrofit(view.getContext());

        ApiSearch apiSearch = retrofit.create(ApiSearch.class);

        String cookie = sessionController.getCookie();
        Call<GetMovieDTO> call = apiSearch.getMovie(cookie, this.id);
        call.enqueue(new Callback<GetMovieDTO>() {
            @Override
            public void onResponse(Call<GetMovieDTO> call, Response<GetMovieDTO> response) {
                GetMovieDTO result = response.body();
                if (result == null) {
                    mCallback.logoutUser();
                } else {
                    onSuccess(result, view);
                    pullRefreshLayout.setRefreshing(false);
                }
            }

            @Override
            public void onFailure(Call<GetMovieDTO> call, Throwable t) {
                onFailed();
            }
        });
    }

    private void onSuccess(final GetMovieDTO result, final View view) {
        mAvarageRating.setText("Rating: " + result.getAvarageRating());
        if (!result.getUserRating().isEmpty())
            mRatingBar.setRating(Float.valueOf(result.getUserRating()));
        if (result.getUserFav())
            mAddFav.setText("Delete from favourites");
        else
            mAddFav.setText("Add to favourites");
        if (result.getUserWantToWatch())
            mWantToWatch.setText("Delete from want to watch");
        else
            mWantToWatch.setText("want to watch");

        if (result.getUserReminder())
            mReminder.setText("Delete reminder");
        else
            mReminder.setText("Add reminder");

        if (result.getUserComment())
            addComment.setText("Delete review");
        else
            addComment.setText("Add review");

        mRelease.setText("Release date: " + result.getReleaseDate());
        mDescription.setText("Description:\n\t" + result.getOverview());
        mTVChanel.setText(result.getChanel());
        if (!result.getHour().isEmpty())
            mTVDate.setText(result.getHour());
        else {
            mTVDate.setText("No tv emission info");
            mTVChanel.setText("No tv emission info");
        }
        favBool = result.getUserFav();
        reminderBool = result.getUserReminder();
        wantBool = result.getUserWantToWatch();
        UnitConversionHelper unitConversionHelper = new UnitConversionHelper();
        Picasso.get().load("https://image.tmdb.org/t/p/w500/" + result.getPosterPath())
                .resize((int) unitConversionHelper.convertDpToPixel(140, view.getContext()),
                        (int) unitConversionHelper.convertDpToPixel(200, view.getContext()))
                .into(mPoster, new com.squareup.picasso.Callback() {
                    @Override
                    public void onSuccess() {
                        mProgressBar.setVisibility(View.GONE);
                        setVisibility(View.VISIBLE);
                        if (result.getMovieCommentsDTOS().isEmpty())
                            nocomments.setVisibility(View.VISIBLE);
                        else
                            nocomments.setVisibility(View.GONE);
                    }

                    @Override
                    public void onError(Exception e) {
                    }
                });
        Picasso.get().load(result.getLogoPath())
                .resize((int) unitConversionHelper.convertDpToPixel(47, view.getContext())
                        , (int) unitConversionHelper.convertDpToPixel(30, view.getContext()))
                .into(mTVLogo);

        Picasso.get().load("https://image.tmdb.org/t/p/w780/" + result.getBackdropPath())
                .into(mBackdrop);

        try {
            RecyclerView recyclerView = setAdapters(R.id.comments, view);
            recyclerView.setAdapter(new MovieCommentsRecyclerViewAdapter(result.getMovieCommentsDTOS(), recyclerView, view));
            ScrollingPagerIndicator recyclerIndicator = view.findViewById(R.id.indicator);
            recyclerIndicator.attachToRecyclerView(recyclerView);
        } catch (Exception e) {
            mCallback.logoutUser();
        }
    }

    private void onFailed() {
        Toast.makeText(getContext(), "Server error", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mCallback = (SendArgumentsAndLaunchFragment) context;
        } catch (ClassCastException e) {
        }
    }

    private void favClicked(final View view) {
        Retrofit retrofit = RetrofitBuilder.createRetrofit(view.getContext());

        ApiFavourites apiFavourites = retrofit.create(ApiFavourites.class);

        String cookie = sessionController.getCookie();
        Call<BooleanDTO> call;
        if (!favBool)
            call = apiFavourites.addFavourite(cookie, this.id);
        else
            call = apiFavourites.deleteFavourite(cookie, this.id);

        call.enqueue(new Callback<BooleanDTO>() {
            @Override
            public void onResponse(Call<BooleanDTO> call, Response<BooleanDTO> response) {
                BooleanDTO result = response.body();
                favBool = !favBool;
                if (result == null) {
                    mCallback.logoutUser();
                } else {
                    if (favBool)
                        mAddFav.setText("Delete from favourites");
                    else
                        mAddFav.setText("Add to favourites");
                }
            }

            @Override
            public void onFailure(Call<BooleanDTO> call, Throwable t) {
            }
        });
    }

    private void wantsClicked(final View view) {
        Retrofit retrofit = RetrofitBuilder.createRetrofit(view.getContext());

        ApiWantToWatch apiWantToWatch = retrofit.create(ApiWantToWatch.class);

        String cookie = sessionController.getCookie();
        Call<BooleanDTO> call;
        if (!wantBool)
            call = apiWantToWatch.addWant(cookie, this.id);
        else
            call = apiWantToWatch.deleteWant(cookie, this.id);

        call.enqueue(new Callback<BooleanDTO>() {
            @Override
            public void onResponse(Call<BooleanDTO> call, Response<BooleanDTO> response) {
                BooleanDTO result = response.body();
                wantBool = !wantBool;
                if (result == null) {
                    mCallback.logoutUser();
                } else {
                    if (wantBool)
                        mWantToWatch.setText("Delete from want to watch");
                    else
                        mWantToWatch.setText("want to watch");
                }
            }

            @Override
            public void onFailure(Call<BooleanDTO> call, Throwable t) {
            }
        });
    }

    private void reminderClicked(final View view) {
        Retrofit retrofit = RetrofitBuilder.createRetrofit(view.getContext());

        ApiReminders apiReminders = retrofit.create(ApiReminders.class);

        String cookie = sessionController.getCookie();
        Call<BooleanDTO> call;
        if (!reminderBool)
            call = apiReminders.addReminder(cookie, this.id);
        else
            call = apiReminders.deleteReminder(cookie, this.id);

        call.enqueue(new Callback<BooleanDTO>() {
            @Override
            public void onResponse(Call<BooleanDTO> call, Response<BooleanDTO> response) {
                BooleanDTO result = response.body();
                reminderBool = !reminderBool;
                if (result == null) {
                    mCallback.logoutUser();
                } else {
                    if (reminderBool)
                        mReminder.setText("Delete reminder");
                    else
                        mReminder.setText("Add reminder");
                }
            }

            @Override
            public void onFailure(Call<BooleanDTO> call, Throwable t) {
            }
        });
    }

    private RecyclerView setAdapters(Integer recyclerId, final View view) {
        RecyclerView recyclerView = view.findViewById(recyclerId);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext(), LinearLayoutManager.HORIZONTAL, false));
        PagerSnapHelper snapHelper = new PagerSnapHelper();
        recyclerView.setOnFlingListener(null);
        snapHelper.attachToRecyclerView(recyclerView);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        return recyclerView;
    }

    private void sendRatingClicked(final View view) {
        Retrofit retrofit = RetrofitBuilder.createRetrofit(view.getContext());

        ApiRatings apiRatings = retrofit.create(ApiRatings.class);

        String cookie = sessionController.getCookie();
        Call<BooleanDTO> call;
        call = apiRatings.setRating(cookie, this.id, String.valueOf(Math.round(mRatingBar.getRating())));

        call.enqueue(new Callback<BooleanDTO>() {
            @Override
            public void onResponse(Call<BooleanDTO> call, Response<BooleanDTO> response) {
                BooleanDTO result = response.body();
            }

            @Override
            public void onFailure(Call<BooleanDTO> call, Throwable t) {
            }
        });
    }

    private void deleteComment(final View view) {
        mProgressBar.setVisibility(View.VISIBLE);
        Retrofit retrofit = RetrofitBuilder.createRetrofit(view.getContext());

        ApiMovieComments apiMovieComments = retrofit.create(ApiMovieComments.class);

        String cookie = sessionController.getCookie();
        Call<BooleanDTO> call;
        call = apiMovieComments.deleteComment(cookie, this.id);

        call.enqueue(new Callback<BooleanDTO>() {
            @Override
            public void onResponse(Call<BooleanDTO> call, Response<BooleanDTO> response) {
                BooleanDTO result = response.body();
                if (result.getResult())
                    getData(view);
            }

            @Override
            public void onFailure(Call<BooleanDTO> call, Throwable t) {
            }
        });
    }

    private void addComment(final View view, String comment) {
        mProgressBar.setVisibility(View.VISIBLE);
        Retrofit retrofit = RetrofitBuilder.createRetrofit(view.getContext());

        ApiMovieComments apiMovieComments = retrofit.create(ApiMovieComments.class);

        String cookie = sessionController.getCookie();
        Call<BooleanDTO> call;
        call = apiMovieComments.addComment(cookie, this.id, comment.replaceAll(" ", "_"));

        call.enqueue(new Callback<BooleanDTO>() {
            @Override
            public void onResponse(Call<BooleanDTO> call, Response<BooleanDTO> response) {
                BooleanDTO result = response.body();
                if (result.getResult())
                    getData(view);
            }

            @Override
            public void onFailure(Call<BooleanDTO> call, Throwable t) {
            }
        });
    }
}
