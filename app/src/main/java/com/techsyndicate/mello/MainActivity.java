package com.techsyndicate.mello;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.accounts.Account;
import android.app.Activity;
import android.app.Person;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Contacts;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import org.mortbay.jetty.Main;
import org.mortbay.util.IO;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    GoogleSignInClient mGoogleSignInClient;
    SignInButton signinbutton;
    String query = "";
    Gmail service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAuth = FirebaseAuth.getInstance();
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        signinbutton = findViewById(R.id.signinbutton);
        signinbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, 101);
            }
        });
        Scope SCOPE_GMAIL_READ =
                new Scope("https://www.googleapis.com/auth/gmail.readonly");
        Scope SCOPE_EMAIL = new Scope(Scopes.EMAIL);

        if (!GoogleSignIn.hasPermissions(
                GoogleSignIn.getLastSignedInAccount(this),
                SCOPE_GMAIL_READ,
                SCOPE_EMAIL)) {
            GoogleSignIn.requestPermissions(MainActivity.this,
                    100,
                    GoogleSignIn.getLastSignedInAccount(this),
                    SCOPE_GMAIL_READ,
                    SCOPE_EMAIL);
        } else {
            try {
                messageList("me", query);
            } catch (IOException e) {
                Toast.makeText(this, "Can't fetch", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(MainActivity.this, HomeActivity.class));
        }
    }

    public List<Message> messageList(String userId, String query) throws IOException {
        ListMessagesResponse response = service.users().messages().list(userId).setQ(query).execute();
        List<Message> messages = new ArrayList<Message>();
        while (response.getMessages() != null) {
            messages.addAll(response.getMessages());
            if (response.getNextPageToken() != null) {
                String pageToken = response.getNextPageToken();
                response = service.users().messages().list(userId).setQ(query)
                        .setPageToken(pageToken).execute();
            } else {
                Toast.makeText(MainActivity.this, "Messages not fetched.", Toast.LENGTH_SHORT).show();
                break;
            }
        }

        for (Message message : messages) {
            System.out.println(message.toPrettyString());
        }

        return messages;
    }


    /** Global instance of the HTTP transport. */
    private static HttpTransport HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport();
    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private class GetContactsTask extends AsyncTask<Void, Void, List<Person>> {

        Account mAccount;
        public GetContactsTask(Account account) {
            mAccount = account;
        }

        @Override
        protected List<Person> doInBackground(Void... params) {
            List<Person> result = null;
            try {
                GoogleAccountCredential credential =
                        GoogleAccountCredential.usingOAuth2(
                                MainActivity.this,
                                Collections.singleton(
                                        "https://www.googleapis.com/auth/gmail.readonly")
                        );
                credential.setSelectedAccount(mAccount);
                Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                        .setApplicationName("mello")
                        .build();
                String user = "me";
                ListLabelsResponse listResponse = service.users().labels().list(user).execute();
                List<Label> labels = listResponse.getLabels();
                if (labels.isEmpty()) {
                    System.out.println("No labels found.");
                } else {
                    System.out.println("Labels:");
                    for (Label label : labels) {
                        System.out.printf("- %s\n", label.getName());
                    }
                }
            } catch (UserRecoverableAuthIOException userRecoverableException) {
                Toast.makeText(MainActivity.this, "Please give permission", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
            }

            return result;
        }

        @Override
        protected void onCancelled() {
        }

        @Override
        protected void onPostExecute(List<Person> connections) {
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
            if (requestCode == 101) {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                try {
                    // Google Sign In was successful, authenticate with Firebase
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    firebaseAuthWithGoogle(account);
                } catch (ApiException e) {
                    // Google Sign In failed, update UI appropriately
                    Toast.makeText(this, "Google Sign-in failed.", Toast.LENGTH_SHORT).show();
                    // ...
                }
            }
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == 100
                || requestCode == 101) {
            if (resultCode == RESULT_OK) {
                try {
                    messageList("me",query);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = mAuth.getCurrentUser();
                            startActivity(new Intent(MainActivity.this,HomeActivity.class));
                            Toast.makeText(MainActivity.this, "Sign in successful.", Toast.LENGTH_SHORT).show();
                        } else {
                            // If sign in fails, display a message to the user.
                            String exc = task.getException().toString();
                            String[] split = exc.split(":",2);
                            String exception = split[1].trim();
                            Toast.makeText(MainActivity.this, exception, Toast.LENGTH_SHORT).show();
                        }

                        // ...
                    }
                });
    }

}
