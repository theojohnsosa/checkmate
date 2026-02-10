package com.example.authtest;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.widget.AppCompatButton;

public class AppIconSelectionDialog extends Dialog {

    private final Context context;
    private final Runnable onConfirm;
    private final Runnable onCancel;
    private static final String TAG = "AppIconDialog1";

    private static final String[] AliasNames = {
            "IconWhiteAlias",
            "IconBlueAlias",
            "IconRedAlias",
            "IconYellowAlias",
            "IconGreenAlias"
    };

    private static final String[] IconDisplayNames = {
            "White",
            "Blue",
            "Red",
            "Yellow",
            "Green"
    };

    private static final int[] IconButtonIds = {
            R.id.iconButton1,
            R.id.iconButton2,
            R.id.iconButton3,
            R.id.iconButton4,
            R.id.iconButton5
    };

    private static final int[] CheckmarkIds = {
            R.id.checkmark1,
            R.id.checkmark2,
            R.id.checkmark3,
            R.id.checkmark4,
            R.id.checkmark5
    };

    private int selectedIconIndex = -1;
    private int currentlyEnabledIndex = -1;

    /*
        Creates a custom dialog with transparent background
        Sets dialog dimensions to 92% of screen width
        Calls findCurrentIcon() to detect which icons alias is currently enabled
     */
    public AppIconSelectionDialog(Context context, Runnable onConfirm, Runnable onCancel) {
        super(context);
        this.context = context;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_app_icon_selection);
        setCancelable(false);

        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(
                    (int) (context.getResources().getDisplayMetrics().widthPixels * 0.92),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        findCurrentIcon();
        setupIconButtonListeners();
        setupActionButtons();
    }

    /*
        Iterates through AliasNames array containing all available app icons variants
        Uses PackageManager.getComponentEnabledSetting() to check which icon is currently active
        Stores the currently enabled index in currentlyEnabledIndex variable
     */
    private void findCurrentIcon() {
        PackageManager pm = context.getPackageManager();
        String packageName = context.getPackageName();

        for (int i = 0; i < AliasNames.length; i++) {
            String fullAliasName = packageName + "." + AliasNames[i];
            ComponentName cn = new ComponentName(packageName, fullAliasName);

            int state = pm.getComponentEnabledSetting(cn);

            if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                currentlyEnabledIndex = i;
                selectedIconIndex = i;
                break;
            }
        }

        if (selectedIconIndex == -1) {
            selectedIconIndex = 0;
            currentlyEnabledIndex = 0;
        }

        updateCheckmarks();
    }

    /*
        Creates click listeners for each of the 5 icon option buttons
        Updates selectedIconIndex when user taps an icon
        Calls updateCheckmarks() to visually indicate selection
     */
    private void setupIconButtonListeners() {
        for (int i = 0; i < IconButtonIds.length; i++) {
            final int index = i;
            FrameLayout btn = findViewById(IconButtonIds[i]);

            if (btn == null) {
                continue;
            }

            btn.setOnClickListener(view -> {
                if (selectedIconIndex == index) {
                    Toast.makeText(context, "This icon is already selected", Toast.LENGTH_SHORT).show();
                    return;
                }

                selectedIconIndex = index;
                updateCheckmarks();
            });
        }
    }

    private void setupActionButtons() {
        AppCompatButton cancelButton = findViewById(R.id.cancelButton);
        AppCompatButton selectButton = findViewById(R.id.selectButton);

        if (cancelButton == null) {
            return;
        }

        if (selectButton == null) {
            return;
        }

        cancelButton.setOnClickListener(view -> {
            dismiss();
            if (onCancel != null) {
                onCancel.run();
            }
        });

        selectButton.setOnClickListener(view -> {
            if (selectedIconIndex == currentlyEnabledIndex) {
                Toast.makeText(context, "This icon is already active", Toast.LENGTH_SHORT).show();
                dismiss();
                if (onCancel != null) {
                    onCancel.run();
                }
                return;
            }

            changeIcon();
            dismiss();

            if (onConfirm != null) {
                onConfirm.run();
            }
        });
    }

    /*
        Shows checkmark ImageView for selected icon
        Hides checkmarks for all other icons
        Provides visual feedback to user
     */
    private void updateCheckmarks() {
        for (int i = 0; i < CheckmarkIds.length; i++) {
            ImageView checkmark = findViewById(CheckmarkIds[i]);
            if (checkmark != null) {
                if (i == selectedIconIndex) {
                    checkmark.setVisibility(View.VISIBLE);
                } else {
                    checkmark.setVisibility(View.GONE);
                }
            }
        }
    }

    /*
        Disables all icon aliases by setting them to COMPONENT_ENABLED_STATE_DISABLED
        Enables only the selected icon alias
        Uses DONT_KILL_APP flag so app remains running during icon change
        Shows toast confirming the change
     */
    private void changeIcon() {
        PackageManager pm = context.getPackageManager();
        String packageName = context.getPackageName();

        try {
            for (int i = 0; i < AliasNames.length; i++) {
                String fullAliasName = packageName + "." + AliasNames[i];
                ComponentName cn = new ComponentName(packageName, fullAliasName);

                pm.setComponentEnabledSetting(cn,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);

            }

            String selectedFullAliasName = packageName + "." + AliasNames[selectedIconIndex];
            ComponentName selectedCn = new ComponentName(packageName, selectedFullAliasName);

            pm.setComponentEnabledSetting(selectedCn,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);

            currentlyEnabledIndex = selectedIconIndex;

            Toast.makeText(context, IconDisplayNames[selectedIconIndex] + " icon activated!\n\nCheck your home screen.", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(context, "Error changing icon: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}