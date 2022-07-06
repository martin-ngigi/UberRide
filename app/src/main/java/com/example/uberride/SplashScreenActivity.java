package com.example.uberride;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.uberride.Model.DriverInfoModel;
import com.firebase.ui.auth.AuthMethodPickerLayout;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;

public class SplashScreenActivity extends AppCompatActivity {

    private final static int LOGIN_REQUEST_CODE = 7171;
    private List<AuthUI.IdpConfig> providers;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener listener;

    @BindView(R.id.progress_bar)
    ProgressBar progress_bar;

    FirebaseDatabase database;
    DatabaseReference driverInfoReference;

    @Override
    protected void onStart() {
        super.onStart();
        displaySplashScreen();

    }

    @Override
    protected void onStop() {

        if (firebaseAuth != null && listener != null){
            firebaseAuth.removeAuthStateListener(listener);
        }
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        init();
    }

    private void init() {
        ButterKnife.bind(this);

        database = FirebaseDatabase.getInstance();
        driverInfoReference = database.getReference(Common.DRIVER_INFO_REFERENCE);

        providers = Arrays.asList(
                new AuthUI.IdpConfig.PhoneBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());

        firebaseAuth = FirebaseAuth.getInstance();
        listener = myFirebaseAuth ->{
            FirebaseUser user = myFirebaseAuth.getCurrentUser();
            if (user != null){
                checkUserFromFirebase();

            }
            else {
                showLoginLayout();
            }
        };
    }

    private void checkUserFromFirebase() {
        driverInfoReference.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull  DataSnapshot snapshot) {
                        if (snapshot.exists()){
                            Toast.makeText(SplashScreenActivity.this, "User already Registered", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            showRegisterLayout();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(SplashScreenActivity.this, ""+error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String firstName,lastName,phone;


    private void showRegisterLayout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DialogTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.layout_register, null);


        TextInputEditText edt_first_name = (TextInputEditText)view.findViewById(R.id.edt_first_name);
        TextInputEditText edt_last_name = (TextInputEditText)view.findViewById(R.id.edt_last_name);
        TextInputEditText edt_phone_number = (TextInputEditText)view.findViewById(R.id.edt_phone_number);

        Button registerBtn = (Button) view.findViewById(R.id.registerBtn);

        //setData
        if ( (FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber() != null) && ( ! TextUtils.isEmpty(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber()) ) ){
            edt_phone_number.setText(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber());
        }

        //set view
        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.show();


        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firstName = edt_first_name.getText().toString();
                lastName = edt_last_name.getText().toString();
                phone = edt_phone_number.getText().toString();

                //validate that data has inputted,
                if (firstName.isEmpty()){
                    Toast.makeText(SplashScreenActivity.this, "Please enter first name ", Toast.LENGTH_SHORT).show();
                    edt_first_name.setFocusable(true);
                    return; //don't proceed
                }

                if (lastName.isEmpty()){
                    Toast.makeText(SplashScreenActivity.this, "Please enter last name ", Toast.LENGTH_SHORT).show();
                    edt_last_name.setFocusable(true);
                    return; //don't proceed
                }

                if (phone.isEmpty()){
                    Toast.makeText(SplashScreenActivity.this, "Please enter Phone number  ", Toast.LENGTH_SHORT).show();
                    edt_phone_number.setFocusable(true);
                    return; //don't proceed
                }

                //else do the following
                DriverInfoModel model = new DriverInfoModel(firstName, lastName, phone, 0.0);

                //save data to db
                driverInfoReference.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .setValue(model)
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                dialog.dismiss();
                                Toast.makeText(SplashScreenActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                dialog.dismiss();
                                Toast.makeText(SplashScreenActivity.this, "Registration Successful", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });


    }

    private void showLoginLayout() {
        AuthMethodPickerLayout authMethodPickerLayout = new AuthMethodPickerLayout
                .Builder(R.layout.layout_sing_in)
                .setPhoneButtonId(R.id.btn_phone_sign_in)
                .setGoogleButtonId(R.id.btn_google_sign_in)
                .build();

        startActivityForResult(AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAuthMethodPickerLayout(authMethodPickerLayout)
                .setIsSmartLockEnabled(false)
                .setTheme(R.style.LoginTheme)
                .setAvailableProviders(providers)
                .build(), LOGIN_REQUEST_CODE
        );
    }

    private void displaySplashScreen() {

        progress_bar.setVisibility(View.VISIBLE);

        Completable.timer(3, TimeUnit.SECONDS,
                AndroidSchedulers.mainThread())
                .subscribe(() ->
                        firebaseAuth.addAuthStateListener(listener)
                );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOGIN_REQUEST_CODE){
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK){
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            }
            else {
                Toast.makeText(this, "[ERROR...]"+response.getError().getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}