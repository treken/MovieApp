package com.kamilismail.movieappandroid.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.JsonObject;
import com.kamilismail.movieappandroid.DTO.BooleanDTO;
import com.kamilismail.movieappandroid.DTO.UserDTO;
import com.kamilismail.movieappandroid.R;
import com.kamilismail.movieappandroid.SessionController;
import com.kamilismail.movieappandroid.connection.ApiUser;
import com.kamilismail.movieappandroid.helpers.RetrofitBuilder;

import org.json.JSONObject;

import java.net.HttpCookie;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class LoginActivity extends AppCompatActivity {

    @BindView(R.id.eLogin)
    EditText _loginText;
    @BindView(R.id.ePassword)
    EditText _passwordText;
    @BindView(R.id.bLogin)
    Button _loginButton;
    @BindView(R.id.tSignUp)
    TextView _signupText;
    @BindView(R.id.mProgressBarProfile)
    ProgressBar progressBar;
    @BindView(R.id.facebook_login)
    LoginButton mFacebookLogin;

    private SessionController sessionController;
    static java.net.CookieManager msCookieManager = new java.net.CookieManager();
    private CallbackManager callbackManager;
    Bundle parameters;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        this.sessionController = new SessionController(LoginActivity.this);
        if (sessionController.isLoggedIn()) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else {
            callbackManager = CallbackManager.Factory.create();
            setContentView(R.layout.activity_login);
            ButterKnife.bind(this);
            ActionBar actionBar = getSupportActionBar();
            actionBar.hide();
            progressBar.setVisibility(View.GONE);
            _loginButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    login(v);
                }
            });
            _signupText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                    startActivity(intent);
                }
            });

            mFacebookLogin.setReadPermissions(Arrays.asList("email", "public_profile"));

            mFacebookLogin.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
                @Override
                public void onSuccess(final LoginResult loginResult) {
                    progressBar.setVisibility(View.VISIBLE);
                    GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(),
                            new GraphRequest.GraphJSONObjectCallback() {
                                @Override
                                public void onCompleted(JSONObject object, GraphResponse response) {
                                    // Application code
                                    String email = object.optString("email");
                                    String name = object.optString("name");
                                    String userId = loginResult.getAccessToken().getUserId();
                                    facebookLogin(email, name, userId);
                                }
                            });
                    parameters = new Bundle();
                    parameters.putString("fields", "id,name,email");
                    request.setParameters(parameters);
                    request.executeAsync();
                }

                @Override
                public void onCancel() {
                }

                @Override
                public void onError(FacebookException exception) {
                }
            });
        }
    }

    private void facebookLogin(String email, String name, String userId) {
        JsonObject obj = new JsonObject();
        obj.addProperty("username", name);
        obj.addProperty("facebookID", userId);
        obj.addProperty("mail", email);
        obj.addProperty("role", "facebook");

        Retrofit retrofit = RetrofitBuilder.createRetrofit(LoginActivity.this);
        ApiUser apiUser = retrofit.create(ApiUser.class);
        Call<UserDTO> call = apiUser.facebookUserLogin(obj);

        call.enqueue(new Callback<UserDTO>() {
            @Override
            public void onResponse(Call<UserDTO> call, Response<UserDTO> response) {
                String cookiesHeader = response.headers().get("Set-Cookie");
                List<HttpCookie> cookies = HttpCookie.parse(cookiesHeader);
                for (HttpCookie cookie : cookies) {
                    msCookieManager.getCookieStore().add(null, cookie);
                }
                String sessionToken = cookies.get(0).toString();
                sessionController.createLoginSession(sessionToken);
                UserDTO userDTO = response.body();
                sessionController.saveUsername(userDTO.getUsername());
                sessionController.saveUserRole("facebook");
                onLoginSuccess();
            }

            @Override
            public void onFailure(Call<UserDTO> call, Throwable t) {
                onLoginFailed();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void login(final View view) {
        if (!validate()) {
            onLoginFailed();
            return;
        }
        _loginButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        String login = _loginText.getText().toString();
        String password = _passwordText.getText().toString();
        final String credentials = "Basic " + Base64.encodeToString((login + ":" + password).getBytes(), Base64.NO_WRAP);
        Retrofit retrofit = RetrofitBuilder.createRetrofit(LoginActivity.this);
        ApiUser apiUser = retrofit.create(ApiUser.class);
        Call<UserDTO> call = apiUser.getUser(credentials);

        call.enqueue(new Callback<UserDTO>() {
            @Override
            public void onResponse(Call<UserDTO> call, Response<UserDTO> response) {
                UserDTO userDTO = response.body();
                if (userDTO != null && !userDTO.getUsername().isEmpty()) {
                    String cookiesHeader = response.headers().get("Set-Cookie");
                    List<HttpCookie> cookies = HttpCookie.parse(cookiesHeader);
                    for (HttpCookie cookie : cookies) {
                        msCookieManager.getCookieStore().add(null, cookie);
                    }
                    String sessionToken = cookies.get(0).toString();
                    sessionController.createLoginSession(sessionToken);
                    sessionController.saveUsername(userDTO.getUsername());
                    sessionController.saveUserRole("user");
                    onLoginSuccess();
                } else
                    onLoginFailed();
            }

            @Override
            public void onFailure(Call<UserDTO> call, Throwable t) {
                onLoginFailed();
            }
        });
    }

    private Boolean validate() {
        String login = _loginText.getText().toString();
        String password = _passwordText.getText().toString();

        if (login.isEmpty()) {
            _loginText.setError("This field cannot be empty");
            return false;
        }
        if (password.isEmpty()) {
            _passwordText.setError("This field cannot be empty");
            return false;
        }
        return true;
    }

    private void onLoginSuccess() { //view
        try {
            sendFirebaseID(); //view
        } catch (Exception e) {
            sessionController.logoutUser();
            LoginManager.getInstance().logOut();
        }
        progressBar.setVisibility(View.GONE);
    }

    //do sprawdzenia
    @SuppressLint("StaticFieldLeak")
    private void sendFirebaseID() throws Exception { //test
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                String token = FirebaseInstanceId.getInstance().getToken();
                // Used to get firebase token until its null so it will save you from null pointer exeption
                while (token == null)
                    token = FirebaseInstanceId.getInstance().getToken();
                sessionController.saveFirebaseToken(token);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                Retrofit retrofit = RetrofitBuilder.createRetrofit(LoginActivity.this);
                ApiUser apiUser = retrofit.create(ApiUser.class);
                String cookie = sessionController.getCookie();
                String token = sessionController.getFirebaseToken();
                Call<BooleanDTO> call = apiUser.setFirebaseID(cookie, token);
                call.enqueue(new Callback<BooleanDTO>() {
                    @Override
                    public void onResponse(Call<BooleanDTO> call, Response<BooleanDTO> response) {
                        BooleanDTO result = response.body();
                        if (result != null && result.getResult()) {
                            progressBar.setVisibility(View.GONE);
                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            finish();
                        } else {
                            onLoginFailed();
                            return;
                        }
                    }

                    @Override
                    public void onFailure(Call<BooleanDTO> call, Throwable t) {
                    }
                });
            }
        }.execute();
    }

    private void onLoginFailed() {
        _loginButton.setEnabled(true);
        progressBar.setVisibility(View.GONE);
        sessionController.logoutUser();
        try {
            LoginManager.getInstance().logOut();
        } catch (Exception e) {}
        Toast.makeText(LoginActivity.this, "No such user", Toast.LENGTH_SHORT).show();
    }
}
