package com.bornbytes.walkietalkie;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Random;

/**
 * Our WalkieTalkie Activity. This Activity has 4 {@link State}s.
 *
 * <p>{@link State#UNKNOWN}: We cannot do anything while we're in this state. The app is likely in
 * the background.
 *
 * <p>{@link State#DISCOVERING}: Our default state (after we've connected). We constantly listen for
 * a device to advertise near us.
 *
 * <p>{@link State#ADVERTISING}: If a user shakes their device, they enter this state. We advertise
 * our device so that others nearby can discover us.
 *
 * <p>{@link State#CONNECTED}: We've connected to another device. We can now talk to them by holding
 * down the volume keys and speaking into the phone. We'll continue to advertise (if we were already
 * advertising) so that more people can connect to us.
 */
public class HomeActivity extends ConnectionsActivity {

    private static final Strategy STRATEGY = Strategy.P2P_POINT_TO_POINT;

    private static final String SERVICE_ID = "com.bornbytes.sharefile.SERVICE_ID";

    /**
     * The state of the app. As the app changes states, the UI will update and advertising/discovery
     * will start/stop.
     */
    private State mState = State.UNKNOWN;

    /**
     * A random UID used as this device's endpoint name.
     */
    private String mName;

    private TextView hintTv, idTv;

    private ImageView retryImageView;

    Uri file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        // getSupportActionBar().setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.actionBar));

        hintTv = findViewById(R.id.state_tv);
        idTv = findViewById(R.id.id_tv);

        retryImageView = findViewById(R.id.retry_iv);
        retryImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                retryImageView.setVisibility(View.GONE);
                setState(State.DISCOVERING);
            }
        });

        mName = generateRandomName();

        file = getIntent().getParcelableExtra("file");

        hintTv.setText(isAdvertise() ? "Ready to Discover" : "Discovering Advertiser");

        idTv.setText(mName);
    }

    public boolean isAdvertise() {
        return file != null && !TextUtils.isEmpty(file.toString());
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!isAdvertise()) {
            setState(State.DISCOVERING);
        } else {
            setState(State.ADVERTISING);
        }
    }

    @Override
    protected void onStop() {

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        setState(State.UNKNOWN);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
       /* if (getState() == State.CONNECTED || getState() == State.ADVERTISING) {
            setState(State.DISCOVERING);
            return;
        }*/
        super.onBackPressed();
    }

    @Override
    protected void onEndpointDiscovered(Endpoint endpoint) {
        if (!isConnecting()) {
            connectToEndpoint(endpoint);
        }
    }

    @Override
    protected void onConnectionInitiated(final Endpoint endpoint, ConnectionInfo connectionInfo) {
        if (!isAdvertise()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(connectionInfo.getEndpointName() + " wants to connect")
                    .setMessage("\nConfirm the code : " + connectionInfo.getAuthenticationToken())
                    .setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            acceptConnection(endpoint);
                        }
                    })
                    .setNegativeButton("Decline", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            rejectConnection(endpoint);
                        }
                    }).show();

        } else {
            acceptConnection(endpoint);
        }
    }

    @Override
    protected void onEndpointConnected(Endpoint endpoint) {
        Toast.makeText(this, getString(R.string.toast_connected, endpoint.getName()), Toast.LENGTH_SHORT).show();
        setState(State.CONNECTED);
        if (isAdvertise()) {
            sendFiles();
        }
    }

    private void sendFiles() {
        // The URI of the file selected by the user.
        // Uri uri = Uri.parse("file://" + file);

        Payload filePayload;
        try {
            // Open the ParcelFileDescriptor for this URI with read access.
            ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(file, "r");
            filePayload = Payload.fromFile(pfd);
        } catch (FileNotFoundException e) {
            Log.e("MyApp", "File not found", e);
            return;
        }

        // Construct a simple message mapping the ID of the file payload to the desired filename.
        String filenameMessage = filePayload.getId() + ":" + file.getLastPathSegment();

        // Send the filename message as a bytes payload.
        Payload filenameBytesPayload = Payload.fromBytes(filenameMessage.getBytes(StandardCharsets.UTF_8));

        send(filenameBytesPayload);

        send(filePayload);
    }

    @Override
    protected void onEndpointDisconnected(Endpoint endpoint) {
        Toast.makeText(this, getString(R.string.toast_disconnected, endpoint.getName()), Toast.LENGTH_SHORT).show();

        onBackPressed();
        // If we lost all our endpoints, then we should reset the state of our app and go back
        // to our initial state (discovering).
        /*if (getConnectedEndpoints().isEmpty()) {
            setState(State.DISCOVERING);
        }*/
    }

    @Override
    protected void onConnectionFailed(Endpoint endpoint) {
        // Let's try someone else.
        if (getState() == State.DISCOVERING && !getDiscoveredEndpoints().isEmpty()) {
            connectToEndpoint(pickRandomElem(getDiscoveredEndpoints()));
        }
    }

    /**
     * The state has changed. I wonder what we'll be doing now.
     *
     * @param state The new state.
     */
    private void setState(State state) {
        if (mState == state) {
            logW("State set to " + state + " but already in that state");
            //return;
        }

        logD("State set to " + state);
        State oldState = mState;
        mState = state;
        onStateChanged(oldState, state);
    }

    /**
     * @return The current state.
     */
    private State getState() {
        return mState;
    }

    /**
     * State has changed.
     *
     * @param oldState The previous state we were in. Clean up anything related to this state.
     * @param newState The new state we're now in. Prepare the UI for this state.
     */
    private void onStateChanged(State oldState, State newState) {

        // Update Nearby Connections to the new state.
        switch (newState) {
            case DISCOVERING:
                if (isAdvertising()) {
                    stopAdvertising();
                }
                disconnectFromAllEndpoints();
                startDiscovering();
                break;
            case ADVERTISING:
                if (isDiscovering()) {
                    stopDiscovering();
                }
                disconnectFromAllEndpoints();
                startAdvertising();
                break;
            case CONNECTED:
                if (isDiscovering()) {
                    stopDiscovering();
                } else if (isAdvertising()) {
                    // Continue to advertise, so others can still connect,
                    // but clear the discover runnable.
                }
                break;
            case UNKNOWN:
                stopAllEndpoints();
                break;
            default:
                // no-op
                break;
        }

    }


    /**
     * {@see ConnectionsActivity#onReceive(Endpoint, Payload)}
     */
    @Override
    protected void onReceive(Endpoint endpoint, Payload payload) {
        if (payload.getType() == Payload.Type.STREAM) {

            // Todo: write files here
        }
    }


    @Override
    protected String[] getRequiredPermissions() {
        return join(super.getRequiredPermissions(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    /**
     * Joins 2 arrays together.
     */
    private static String[] join(String[] a, String... b) {
        String[] join = new String[a.length + b.length];
        System.arraycopy(a, 0, join, 0, a.length);
        System.arraycopy(b, 0, join, a.length, b.length);
        return join;
    }

    /**
     * Queries the phone's contacts for their own profile, and returns their name. Used when
     * connecting to another device.
     */
    @Override
    protected String getName() {
        return mName;
    }

    /**
     * {@see ConnectionsActivity#getServiceId()}
     */
    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

    /**
     * {@see ConnectionsActivity#getStrategy()}
     */
    @Override
    public Strategy getStrategy() {
        return STRATEGY;
    }


    private static CharSequence toColor(String msg, int color) {
        SpannableString spannable = new SpannableString(msg);
        spannable.setSpan(new ForegroundColorSpan(color), 0, msg.length(), 0);
        return spannable;
    }

    private static String generateRandomName() {
        String name = "";
        Random random = new Random();
        for (int i = 0; i < 4; i++) {
            name += random.nextInt(10);
        }
        return name;
    }

    @SuppressWarnings("unchecked")
    private static <T> T pickRandomElem(Collection<T> collection) {
        return (T) collection.toArray()[new Random().nextInt(collection.size())];
    }

    /**
     * States that the UI goes through.
     */
    public enum State {
        UNKNOWN,
        DISCOVERING,
        ADVERTISING,
        CONNECTED
    }

    @Override
    protected void onDiscoveryFailed() {
        super.onDiscoveryFailed();
        UiUtilKt.showSnackBar(this, "Can't Search Nearby Device");
        //Snackbar.make(retryImageView.getRootView(),"Can't Search Nearby Device", Snackbar.LENGTH_SHORT).show();
        retryImageView.setVisibility(View.VISIBLE);
    }
}
