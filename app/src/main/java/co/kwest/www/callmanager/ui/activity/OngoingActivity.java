package co.kwest.www.callmanager.ui.activity;

import android.app.KeyguardManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.telecom.Call;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import co.kwest.www.callmanager.R;
import co.kwest.www.callmanager.database.entity.Contact;
import co.kwest.www.callmanager.ui.fragment.DialpadFragment;
import co.kwest.www.callmanager.util.CallManager;
import co.kwest.www.callmanager.util.PreferenceUtils;
import co.kwest.www.callmanager.util.ThemeUtils;
import co.kwest.www.callmanager.util.Utilities;
import timber.log.Timber;

import static co.kwest.www.callmanager.util.BiometricUtils.showBiometricPrompt;

public class OngoingActivity extends AbsThemeActivity implements DialpadFragment.OnKeyDownListener {

  private static int mState;
  private static String mStateText;

  Callback mCallback = new Callback();

  @BindView(R.id.text_status) TextView mStatusText;
  @BindView(R.id.text_caller) TextView mCallerText;

  // Image Views
  @BindView(R.id.image_placeholder)
  ImageView mPlaceholderImage;
  @BindView(R.id.image_photo) ImageView mPhotoImage;

  // PowerManager
  PowerManager powerManager;
  PowerManager.WakeLock wakeLock;
  private int field = 0x00000020;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setThemeType(ThemeUtils.TYPE_TRANSPARENT_STATUS_BAR);
    setContentView(R.layout.ongoing_call);
    PreferenceUtils.getInstance(this);
    Utilities.setUpLocale(this);

    ButterKnife.bind(this);

    // This activity needs to show even if the screen is off or locked
    Window window = getWindow();
    setShowWhenLocked(true);
    setTurnScreenOn(true);

    KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
    if (km != null) {
      km.requestDismissKeyguard(this, null);
    }
    window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

    // Display caller information
    displayInformation();

    // Initiate PowerManager and WakeLock (turn screen on/off according to distance from face)
    try {
      field = PowerManager.class.getField("PROXIMITY_SCREEN_OFF_WAKE_LOCK").getInt(null);
    } catch (Throwable ignored) {
    }
    powerManager = (PowerManager) getSystemService(POWER_SERVICE);
    wakeLock = powerManager.newWakeLock(field, getLocalClassName());

  }

  @Override
  protected void onPostCreate(@Nullable Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);

    //Listen for call state changes
    CallManager.registerCallback(mCallback);
    updateUI(CallManager.getState());

  }

  @Override
  protected void onStart() {
    super.onStart();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    CallManager.unregisterCallback(mCallback); //The activity is gone, no need to listen to changes
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == Utilities.PERMISSION_RC && Utilities.checkPermissionsGranted(grantResults)) {
    }
  }

  @Override
  public void onKeyPressed(int keyCode, KeyEvent event) {
    CallManager.keypad((char) event.getUnicodeChar());
  }

  //TODO silence the ringing
  @OnClick(R.id.hangup_btn)
  public void hangup(View view) {
    endCall();
  }


  private void endCall() {
    //mCallTimeHandler.sendEmptyMessage(TIME_STOP);
    CallManager.reject();
    releaseWakeLock();
    //if (CallManager.isAutoCalling()) {
    //  finish();
    //  CallManager.nextCall(this);
    //} else {
    //  (new Handler()).postDelayed(this::finish, END_CALL_MILLIS); // Delay the closing of the call
    //}
  }


  // -- UI -- //

  private void displayInformation() {
    // Display the information about the caller
    Contact callerContact = CallManager.getDisplayContact(this);
    if (!callerContact.getName().isEmpty()) {
      if (callerContact.getName() != null) mCallerText.setText(callerContact.getName());
      if (callerContact.getPhotoUri() != null) {
        mPlaceholderImage.setVisibility(View.INVISIBLE);
        mPhotoImage.setVisibility(View.VISIBLE);
        mPhotoImage.setImageURI(Uri.parse(callerContact.getPhotoUri()));
      }
    } else {
      mCallerText.setText(callerContact.getMainPhoneNumber());
    }
  }

  /**
   * Updates the ui given the call state
   *
   * @param state the current call state
   */
  private void updateUI(int state) {
    @StringRes int statusTextRes;
    switch (state) {
      case Call.STATE_ACTIVE: // Ongoing
        statusTextRes = R.string.status_call_active;
        break;
      case Call.STATE_DISCONNECTED: // Ended
        statusTextRes = R.string.status_call_disconnected;
        break;
      case Call.STATE_RINGING: // Incoming
        statusTextRes = R.string.status_call_incoming;
        showBiometricPrompt(this);
        break;
      case Call.STATE_DIALING: // Outgoing
        statusTextRes = R.string.status_call_dialing;
        break;
      case Call.STATE_CONNECTING: // Connecting (probably outgoing)
        statusTextRes = R.string.status_call_dialing;
        break;
      case Call.STATE_HOLDING: // On Hold
        statusTextRes = R.string.status_call_holding;
        break;
      default:
        statusTextRes = R.string.status_call_active;
        break;
    }

    mStatusText.setText(statusTextRes);
    //if (state != Call.STATE_RINGING && state != Call.STATE_DISCONNECTED) switchToCallingUI();
    if (state == Call.STATE_DISCONNECTED) endCall();
    //mState = state;
    //mStateText = getString(statusTextRes);
  }

  // -- Wake Lock -- //

  /**
   * Acquires the wake lock
   */
  private void acquireWakeLock() {
    if (!wakeLock.isHeld()) {
      wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);
    }
  }

  /**
   * Releases the wake lock
   */
  private void releaseWakeLock() {
    if (wakeLock.isHeld()) {
      wakeLock.release();
    }
  }


  /**
   * Callback class
   * Listens to the call and do stuff when something changes
   */
  public class Callback extends Call.Callback {

    @Override
    public void onStateChanged(Call call, int state) {
            /*
              Call states:

              1   = Call.STATE_DIALING
              2   = Call.STATE_RINGING
              3   = Call.STATE_HOLDING
              4   = Call.STATE_ACTIVE
              7   = Call.STATE_DISCONNECTED
              8   = Call.STATE_SELECT_PHONE_ACCOUNT
              9   = Call.STATE_CONNECTING
              10  = Call.STATE_DISCONNECTING
              11  = Call.STATE_PULLING_CALL
             */
      super.onStateChanged(call, state);
      Timber.i("State changed: %s", state);

      updateUI(state);

      if (state == Call.STATE_DISCONNECTED) {
        finish();
      }
    }

    @Override
    public void onDetailsChanged(Call call, Call.Details details) {
      super.onDetailsChanged(call, details);
      Timber.i("Details changed: %s", details.toString());
    }
  }
}
