package co.kwest.www.callmanager.ui.activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.ogaclejapan.smarttablayout.SmartTabLayout;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import co.kwest.www.callmanager.BuildConfig;
import co.kwest.www.callmanager.R;
import co.kwest.www.callmanager.adapter.CustomPagerAdapter;
import co.kwest.www.callmanager.ui.FABCoordinator;
import co.kwest.www.callmanager.ui.dialog.ChangelogDialog;
import co.kwest.www.callmanager.ui.fragment.DialpadFragment;
import co.kwest.www.callmanager.ui.fragment.SearchBarFragment;
import co.kwest.www.callmanager.util.CallManager;
import co.kwest.www.callmanager.util.PreferenceUtils;
import co.kwest.www.callmanager.util.ThemeUtils;
import co.kwest.www.callmanager.util.Utilities;
import co.kwest.www.callmanager.viewmodels.SharedDialViewModel;
import co.kwest.www.callmanager.viewmodels.SharedSearchViewModel;

import static co.kwest.www.callmanager.util.BiometricUtils.showBiometricPrompt;

public class MainActivity extends AbsSearchBarActivity {

  private static final String TAG = MainActivity.class.getCanonicalName();
  private static final String TAG_CHANGELOG_DIALOG = "changelog";
  boolean mIsBiometric;

  // Intent
  Intent mIntent;
  String mIntentAction;
  String mIntentType;

  // View Models
  SharedDialViewModel mSharedDialViewModel;
  SharedSearchViewModel mSharedSearchViewModel;

  //Coordinator
  FABCoordinator mFABCoordinator;

  // Fragments
  DialpadFragment mDialpadFragment;
  SearchBarFragment mSearchBarFragment;
  FragmentPagerAdapter mAdapterViewPager;

  // Other
  BottomSheetBehavior mBottomSheetBehavior;
  BiometricPrompt mBiometricPrompt;
  Menu mMenu;

  // Bluetooth
  private static final int REQUEST_ENABLE_BT = 1;
  private static final UUID CHARACTERISTIC_USER_DESCRIPTION_UUID = UUID
      .fromString("00002901-0000-1000-8000-00805f9b34fb");
  private static final UUID CLIENT_CHARACTERISTIC_CONFIGURATION_UUID = UUID
      .fromString("00002902-0000-1000-8000-00805f9b34fb");

  private static final UUID DIALER_SERVICE_UUID = UUID
      .fromString("00001234-0000-1000-8000-00805f9b34fb");

  private static final UUID DIALER_CONTROL_POINT_UUID = UUID
      .fromString("00000000-0000-1000-8000-00805f9b34fb");

  private BluetoothGattService mDialerService;
  private BluetoothGattCharacteristic mDialerControlPoint;

  public void startBluetoothService() {

    mDialerControlPoint =
        new BluetoothGattCharacteristic(DIALER_CONTROL_POINT_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE);

    mDialerService = new BluetoothGattService(DIALER_SERVICE_UUID,
        BluetoothGattService.SERVICE_TYPE_PRIMARY);
    mDialerService.addCharacteristic(mDialerControlPoint);
  }


  public ParcelUuid getServiceUUID() {
    return new ParcelUuid(DIALER_SERVICE_UUID);
  }

  private HashSet<BluetoothDevice> mBluetoothDevices;
  private BluetoothManager mBluetoothManager;
  private BluetoothAdapter mBluetoothAdapter;
  private AdvertiseSettings mAdvSettings = new AdvertiseSettings.Builder()
      .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
      .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
      .setConnectable(true)
      .build();
  private AdvertiseData mAdvData = new AdvertiseData.Builder()
      .setIncludeTxPowerLevel(true)
      .addServiceUuid(new ParcelUuid(DIALER_SERVICE_UUID))
      .build();
  private AdvertiseData mAdvScanResponse = new AdvertiseData.Builder()
      .setIncludeDeviceName(true)
      .build();

  private BluetoothLeAdvertiser mAdvertiser;
  private final AdvertiseCallback mAdvCallback = new AdvertiseCallback() {
    public void onStartFailure(int errorCode) {
      super.onStartFailure(errorCode);
      Log.e(TAG, "Not broadcasting: " + errorCode);
    }
    @Override
    public void onStartSuccess(AdvertiseSettings settingsInEffect) {
      super.onStartSuccess(settingsInEffect);
      Log.v(TAG, "Broadcasting");
    }
  };

  private BluetoothGattServer mGattServer;
  private final BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
    @Override
    public void onConnectionStateChange(BluetoothDevice device, final int status, int newState) {
      super.onConnectionStateChange(device, status, newState);
      if (status == BluetoothGatt.GATT_SUCCESS) {
        if (newState == BluetoothGatt.STATE_CONNECTED) {
          mBluetoothDevices.add(device);
          updateConnectedDevicesStatus();
          Log.v(TAG, "Connected to device: " + device.getAddress());
        } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
          mBluetoothDevices.remove(device);
          updateConnectedDevicesStatus();
          Log.v(TAG, "Disconnected from device");
        }
      } else {
        mBluetoothDevices.remove(device);
        updateConnectedDevicesStatus();
        // There are too many gatt errors (some of them not even in the documentation) so we just
        // show the error to the user.
        Log.e(TAG, "Error when connecting: ");
      }
    }

    @Override
    public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                            BluetoothGattCharacteristic characteristic) {
      super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
      Log.d(TAG, "Device tried to read characteristic: " + characteristic.getUuid());
      Log.d(TAG, "Value: " + Arrays.toString(characteristic.getValue()));
      if (offset != 0) {
        mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset,
            /* value (optional) */ null);
        return;
      }
      mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
          offset, characteristic.getValue());
    }

    @Override
    public void onNotificationSent(BluetoothDevice device, int status) {
      super.onNotificationSent(device, status);
      Log.v(TAG, "Notification sent. Status: " + status);
    }

    @Override
    public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                             BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded,
                                             int offset, byte[] value) {
      //super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite,
      //    responseNeeded, offset, value);
      Log.v(TAG, "Characteristic Write request: " + Arrays.toString(value));
      //int status = mCurrentServiceFragment.writeCharacteristic(characteristic, offset, value);

      String number = new String(value, 0, value.length, StandardCharsets.UTF_8);

      if (number.equals("hangup")) {
        Intent intent = new Intent();
        intent.setAction("co.kwest.www.callmanager.hangup");
        sendBroadcast(intent);
      }
      else {
        CallManager.call(getCurrentFragment().getContext(), Utilities.getOnlyNumbers(number));
      }

      if (responseNeeded) {
        mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
            /* No need to respond with an offset */ 0,
            /* No need to respond with a value */ null);
      }
    }

    @Override
    public void onDescriptorReadRequest(BluetoothDevice device, int requestId,
                                        int offset, BluetoothGattDescriptor descriptor) {
      super.onDescriptorReadRequest(device, requestId, offset, descriptor);
      Log.d(TAG, "Device tried to read descriptor: " + descriptor.getUuid());
      Log.d(TAG, "Value: " + Arrays.toString(descriptor.getValue()));
      if (offset != 0) {
        mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset,
            /* value (optional) */ null);
        return;
      }
      mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
          descriptor.getValue());
    }

    @Override
    public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                         BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded,
                                         int offset,
                                         byte[] value) {
      super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded,
          offset, value);
      Log.v(TAG, "Descriptor Write Request " + descriptor.getUuid() + " " + Arrays.toString(value));
      int status = BluetoothGatt.GATT_SUCCESS;
      if (descriptor.getUuid() == CLIENT_CHARACTERISTIC_CONFIGURATION_UUID) {
        BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
        boolean supportsNotifications = (characteristic.getProperties() &
            BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
        boolean supportsIndications = (characteristic.getProperties() &
            BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0;

        if (!(supportsNotifications || supportsIndications)) {
          status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED;
        } else if (value.length != 2) {
          status = BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH;
        } else if (Arrays.equals(value, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
          status = BluetoothGatt.GATT_SUCCESS;
          //mCurrentServiceFragment.notificationsDisabled(characteristic);
          descriptor.setValue(value);
        } else if (supportsNotifications &&
            Arrays.equals(value, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
          status = BluetoothGatt.GATT_SUCCESS;
          //mCurrentServiceFragment.notificationsEnabled(characteristic, false /* indicate */);
          descriptor.setValue(value);
        } else if (supportsIndications &&
            Arrays.equals(value, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)) {
          status = BluetoothGatt.GATT_SUCCESS;
          //mCurrentServiceFragment.notificationsEnabled(characteristic, true /* indicate */);
          descriptor.setValue(value);
        } else {
          status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED;
        }
      } else {
        status = BluetoothGatt.GATT_SUCCESS;
        descriptor.setValue(value);
      }
      if (responseNeeded) {
        mGattServer.sendResponse(device, requestId, status,
            /* No need to respond with offset */ 0,
            /* No need to respond with a value */ null);
      }
    }
  };

  // - View Binds - //

  // Views
  @BindView(R.id.appbar) View mAppBar;
  @BindView(R.id.dialer_fragment) View mDialerView;

  // Layouts
  @BindView(R.id.root_view) CoordinatorLayout mMainLayout;

  // Buttons
  @BindView(R.id.right_button) FloatingActionButton mRightButton;
  @BindView(R.id.left_button) FloatingActionButton mLeftButton;

  // Other
  @BindView(R.id.view_pager) ViewPager mViewPager;
  @BindView(R.id.view_pager_tab) SmartTabLayout mSmartTabLayout;

  // -- Overrides -- //

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setThemeType(ThemeUtils.TYPE_NO_ACTION_BAR);
    setContentView(R.layout.activity_main);
    PreferenceUtils.getInstance(this); // Get the preferences
    Utilities.setUpLocale(this);

    // Check if first instance
    boolean isFirstInstance = PreferenceUtils.getInstance().getBoolean(R.string.pref_is_first_instance_key);
    if (!isFirstInstance) checkVersion();

    // Bind variables
    ButterKnife.bind(this);

    // Check wither this app was set as the default dialer
    boolean isDefaultDialer = Utilities.checkDefaultDialer(this);
    if (isDefaultDialer) checkPermissions(null);

    // View Pager
    mAdapterViewPager = new CustomPagerAdapter(getSupportFragmentManager());
    mViewPager.setAdapter(mAdapterViewPager);
    mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
      @Override
      public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

      }

      @Override
      public void onPageSelected(int position) {
        if (isSearchBarVisible()) toggleSearchBar();
        syncFABAndFragment();
      }

      @Override
      public void onPageScrollStateChanged(int state) {

      }
    });
    mSmartTabLayout.setViewPager(mViewPager);

    // Search Bar View Model
    mSharedSearchViewModel = ViewModelProviders.of(this).get(SharedSearchViewModel.class);
    mSharedSearchViewModel.getIsFocused().observe(this, f -> {
      if (f) {
        expandAppBar(true);
      }
    });

    // Dial View Model
    mSharedDialViewModel = ViewModelProviders.of(this).get(SharedDialViewModel.class);
    mSharedDialViewModel.getIsOutOfFocus().observe(this, b -> {
      if (b) {
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
      }
    });
    mSharedDialViewModel.getNumber().observe(this, n -> {
      if (n == null || n.length() == 0) {
//                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        toggleAddContactAction(false);
      } else {
        toggleAddContactAction(true);
      }
    });

    // Bottom Sheet Behavior
    mBottomSheetBehavior = BottomSheetBehavior.from(mDialerView); // Set the bottom sheet behaviour
    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN); // Hide the bottom sheet
    mBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
      @Override
      public void onStateChanged(@NonNull View view, int i) {
        updateButtons(i);
      }

      @Override
      public void onSlide(@NonNull View view, float v) {

      }
    });

    // Initialize FABCoordinator
    mFABCoordinator = new FABCoordinator(mRightButton, mLeftButton, this);
    syncFABAndFragment();

    // Set default page
    int pagePreference = Integer.parseInt(PreferenceUtils.getInstance().getString(R.string.pref_default_page_key));
    mViewPager.setCurrentItem(pagePreference);

    // Add the dialer fragment
    mDialpadFragment = DialpadFragment.newInstance(true);
    getSupportFragmentManager().beginTransaction()
        .add(R.id.dialer_fragment, mDialpadFragment)
        .commit();

    // Check for intents from others apps
    checkIncomingIntent();

    showBiometricPrompt(this);

    mBluetoothDevices = new HashSet<>();
    mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    mBluetoothAdapter = mBluetoothManager.getAdapter();
  }

  @Override
  public void onAttachFragment(@NonNull Fragment fragment) {
    if (fragment instanceof SearchBarFragment) {
      mSearchBarFragment = (SearchBarFragment) fragment;
    }
  }

  @Override
  protected void onStart() {
    super.onStart();
    syncFABAndFragment();

    // If the user disabled Bluetooth when the app was in the background,
    // openGattServer() will return null.
    mGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback);
    if (mGattServer == null) {
      ensureBleFeaturesAvailable();
      return;
    }
    // Add a service for a total of three services (Generic Attribute and Generic Access
    // are present by default).
    startBluetoothService();

    mGattServer.addService(mDialerService);

    if (mBluetoothAdapter.isMultipleAdvertisementSupported()) {
      mAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
      mAdvertiser.startAdvertising(mAdvSettings, mAdvData, mAdvScanResponse, mAdvCallback);
    } else {
    }

  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main_actions, menu);
    mMenu = menu;
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_add_contact: {
        String number = mSharedDialViewModel.getNumber().getValue();
        Utilities.addContactIntent(this, number);
        return true;
      }
      case R.id.action_settings: {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
        return true;
      }
      case R.id.action_about: {
        Intent intent = new Intent(this, AboutActivity.class);
        startActivity(intent);
        return true;
      }
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      View v = getCurrentFocus();
      if (v instanceof EditText) {
        Rect outRect = new Rect();
        v.getGlobalVisibleRect(outRect);
        if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
          v.clearFocus();
          InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
          imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
      }
    }
    return super.dispatchTouchEvent(event);
  }

  @Override
  public void onBackPressed() {
    super.onBackPressed();
    syncFABAndFragment();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    // Check for first instance
    boolean isFirstInstance = PreferenceUtils.getInstance().getBoolean(R.string.pref_is_first_instance_key);

    if (isFirstInstance) {
      PreferenceUtils.getInstance().putBoolean(R.string.pref_is_first_instance_key, false);
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == Utilities.DEFAULT_DIALER_RC) {
      checkPermissions(null);
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    checkPermissions(grantResults);
  }

  // -- OnClicks -- //

  @OnClick(R.id.right_button)
  public void fabRightClick() {
    mFABCoordinator.performRightClick();
  }

  @OnClick(R.id.left_button)
  public void fabLeftClick() { mFABCoordinator.performLeftClick(); }

  // -- Fragments -- //

  /**
   * Returns the currently displayed fragment. Based on <a href="this">https://stackoverflow.com/a/18611036/5407365</a> answer
   *
   * @return Fragment
   */
  private Fragment getCurrentFragment() {
    return getSupportFragmentManager()
        .findFragmentByTag("android:switcher:" + R.id.view_pager + ":" + mViewPager.getCurrentItem());
  }

  /**
   * Apply the FABCoordinator to the current fragment
   */
  public void syncFABAndFragment() {
    Fragment fragment = getCurrentFragment();
    mFABCoordinator.setListener(fragment);
    updateButtons();
  }

  // -- UI -- //

  /**
   * Change the dialer status (collapse/expand)
   *
   * @param expand
   */
  public void expandDialer(boolean expand) {

    if (expand) {
      BottomSheetBehavior.from(mDialerView).setState(BottomSheetBehavior.STATE_EXPANDED);
    } else {
      BottomSheetBehavior.from(mDialerView).setState(BottomSheetBehavior.STATE_COLLAPSED);
    }
  }

  /**
   * Extend/Collapse the appbar_main according to given parameter
   *
   * @param expand
   */
  public void expandAppBar(boolean expand) {
    mAppBarLayout.setExpanded(expand);
  }

  public void updateButtons() {
    updateButtons(mBottomSheetBehavior.getState());
  }

  public void updateButtons(int bottomSheetState) {
    if (bottomSheetState == BottomSheetBehavior.STATE_HIDDEN || bottomSheetState == BottomSheetBehavior.STATE_COLLAPSED) {
      showButtons(true);
    } else {
      showButtons(false);
    }
  }

  /**
   * Animate action buttons
   *
   * @param isShow animate to visible/invisible
   */
  public void showButtons(boolean isShow) {
    View[] buttons = {mRightButton, mLeftButton};
    for (View v : buttons) {
      if (isShow && v.isEnabled()) {
        v.animate().scaleX(1).scaleY(1).setDuration(100).start();
        v.setClickable(true);
        v.setFocusable(true);
      } else {
        v.animate().scaleX(0).scaleY(0).setDuration(100).start();
        v.setClickable(false);
        v.setFocusable(false);
      }
    }
  }

  // -- Utilities -- //

  private void checkPermissions(@Nullable int[] grantResults) {
    if (
        (grantResults != null && Utilities.checkPermissionsGranted(grantResults)) ||
            Utilities.checkPermissionsGranted(this, Utilities.MUST_HAVE_PERMISSIONS)) { //If granted
      checkVersion();
    } else {
//            Utilities.askForPermissions(this, Utilities.MUST_HAVE_PERMISSIONS);
    }
  }

  /**
   * Check for the app version
   */
  private void checkVersion() {
    int lastVersionCode = PreferenceUtils.getInstance().getInt(R.string.pref_last_version_key);
    if (lastVersionCode < BuildConfig.VERSION_CODE) {
      PreferenceUtils.getInstance().putInt(R.string.pref_last_version_key, BuildConfig.VERSION_CODE);
      new ChangelogDialog().show(getSupportFragmentManager(), TAG_CHANGELOG_DIALOG);
    }
  }

  // -- Other -- //

  /**
   * Checking for incoming intents from other applications
   */
  private void checkIncomingIntent() {
    mIntent = getIntent();
    mIntentAction = mIntent.getAction();
    mIntentType = mIntent.getType();

    if (Intent.ACTION_VIEW.equals(mIntentAction)) {
      handleViewIntent(mIntent);
    }
  }

  /**
   * Handle a VIEW intent (For example when you click a number in whatsapp)
   *
   * @param intent
   */
  private void handleViewIntent(Intent intent) {
    String sharedText = intent.getData().toString();
    String number = "";
    if (sharedText.contains("tel:")) {
      number = sharedText.substring(4, sharedText.length() - 1);

      if (number != null) {
        mSharedDialViewModel.setNumber(number);
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
      }
    }
  }

  private void toggleAddContactAction(boolean isShow) {
    if (mMenu != null) {
      MenuItem addContact = mMenu.findItem(R.id.action_add_contact);
      addContact.setVisible(isShow);
    }
  }

  ///////////////////////
  ////// Bluetooth //////
  ///////////////////////
  public static BluetoothGattDescriptor getClientCharacteristicConfigurationDescriptor() {
    BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(
        CLIENT_CHARACTERISTIC_CONFIGURATION_UUID,
        (BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE));
    descriptor.setValue(new byte[]{0, 0});
    return descriptor;
  }

  public static BluetoothGattDescriptor getCharacteristicUserDescriptionDescriptor(String defaultValue) {
    BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(
        CHARACTERISTIC_USER_DESCRIPTION_UUID,
        (BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE));
    try {
      descriptor.setValue(defaultValue.getBytes(StandardCharsets.UTF_8));
    } finally {
      return descriptor;
    }
  }

  private void ensureBleFeaturesAvailable() {
    if (mBluetoothAdapter == null) {
      Log.e(TAG, "Bluetooth not supported");
      finish();
    } else if (!mBluetoothAdapter.isEnabled()) {
      // Make sure bluetooth is enabled.
      Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }
  }
  private void disconnectFromDevices() {
    Log.d(TAG, "Disconnecting devices...");
    for (BluetoothDevice device : mBluetoothManager.getConnectedDevices(
        BluetoothGattServer.GATT)) {
      Log.d(TAG, "Devices: " + device.getAddress() + " " + device.getName());
      mGattServer.cancelConnection(device);
    }
  }

  private void updateConnectedDevicesStatus() {
  }
}
