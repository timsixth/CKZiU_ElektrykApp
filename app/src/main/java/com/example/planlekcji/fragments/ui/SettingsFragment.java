package com.example.planlekcji.fragments.ui;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.example.planlekcji.BuildConfig;
import com.example.planlekcji.MainActivity;
import com.example.planlekcji.MainViewModel;
import com.example.planlekcji.R;
import com.example.planlekcji.ckziu_elektryk.client.timetable.SchoolEntry;
import com.example.planlekcji.settings.GroupPreferenceManager;
import com.example.planlekcji.settings.SchoolEntriesDownloader;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SettingsFragment extends Fragment {
    private SharedPreferences sharedPref;
    private List<SchoolEntry> classesSchoolEntries = new ArrayList<>();
    private List<SchoolEntry> teachersSchoolEntries = new ArrayList<>();
    private List<SchoolEntry> classroomsSchoolEntries = new ArrayList<>();
    private View view;
    private MainViewModel mainViewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_settings, container, false);

        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        // Initialize SharedPreferences for storing application settings.
        sharedPref = MainActivity.getContext().getSharedPreferences("sharedPrefs", 0);

        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();
            if (activity.getNetworkMonitor() != null) {
                activity.getNetworkMonitor().getIsOnlineLiveData().observe(getViewLifecycleOwner(), this::updateOnlineState);
            }
        }

        // Setup hide unselected switch
        SwitchMaterial switchHideUnselected = view.findViewById(R.id.switch_hideUnselectedGroups);
        View layoutHideSwitch = view.findViewById(R.id.layout_hideUnselectedSwitch);
        if (switchHideUnselected != null) {
            switchHideUnselected.setChecked(GroupPreferenceManager.isHideUnselected(sharedPref));
            switchHideUnselected.setOnCheckedChangeListener((btn, isChecked) -> {
                GroupPreferenceManager.setHideUnselected(sharedPref, isChecked);
            });
            if (layoutHideSwitch != null) {
                layoutHideSwitch.setOnClickListener(v -> switchHideUnselected.toggle());
            }
        }

        // Setup lock selection switch
        SwitchMaterial switchLockSelection = view.findViewById(R.id.switch_lockSelection);
        View layoutLockSwitch = view.findViewById(R.id.layout_lockSelectionSwitch);
        if (switchLockSelection != null) {
            switchLockSelection.setChecked(GroupPreferenceManager.isLockSelection(sharedPref));
            switchLockSelection.setOnCheckedChangeListener((btn, isChecked) -> {
                GroupPreferenceManager.setLockSelection(sharedPref, isChecked);
            });
            if (layoutLockSwitch != null) {
                layoutLockSwitch.setOnClickListener(v -> switchLockSelection.toggle());
            }
        }

        // Setup reset groups button
        View buttonReset = view.findViewById(R.id.button_resetGroups);
        if (buttonReset != null) {
            buttonReset.setOnClickListener(v -> {
                String classToken = sharedPref.getString(getString(R.string.classTokenKey), "");
                GroupPreferenceManager.resetClassChoices(sharedPref, classToken);
                Toast.makeText(requireContext(), R.string.settings_groups_reset_success, Toast.LENGTH_SHORT).show();
            });
        }

        // Setup About section
        setupAboutSection();

        // Initial visibility
        changeVisibility();

        // Fetch data relevant to settings.
        getData();

        return view;
    }

    private static final String GITHUB_REPO_URL = "https://github.com/Bokeher/PlanLekcji";
    private static final String PRIVACY_POLICY_URL = "https://github.com/Bokeher/PlanLekcji/blob/master/PRIVACY_POLICY.md";
    private static final String CONTACT_EMAIL = "rychter47@gmail.com";

    private void setupAboutSection() {
        if (view == null) return;

        TextView textViewVersion = view.findViewById(R.id.textView_aboutVersion);
        if (textViewVersion != null) {
            textViewVersion.setText(BuildConfig.VERSION_NAME);
        }

        View layoutGitHub = view.findViewById(R.id.layout_aboutGitHub);
        if (layoutGitHub != null) {
            layoutGitHub.setOnClickListener(v -> openUrl(GITHUB_REPO_URL));
        }

        View layoutPrivacyPolicy = view.findViewById(R.id.layout_aboutPrivacyPolicy);
        if (layoutPrivacyPolicy != null) {
            layoutPrivacyPolicy.setOnClickListener(v -> openUrl(PRIVACY_POLICY_URL));
        }

        View layoutContact = view.findViewById(R.id.layout_aboutContact);
        if (layoutContact != null) {
            layoutContact.setOnClickListener(v -> sendEmail());
            layoutContact.setOnLongClickListener(v -> {
                copyContactEmail();
                return true;
            });
        }
    }

    private void sendEmail() {
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:" + CONTACT_EMAIL));
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.settings_about_contact_subject));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            copyContactEmail();
            Toast.makeText(requireContext(), R.string.error_cannot_send_email, Toast.LENGTH_SHORT).show();
        }
    }

    private void copyContactEmail() {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("Email", CONTACT_EMAIL);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(requireContext(), R.string.settings_about_contact_copied, Toast.LENGTH_SHORT).show();
        }
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), R.string.error_cannot_open_link, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateOnlineState(boolean isOnline) {
        if (view == null) return;

        View cardNotice = view.findViewById(R.id.card_settingsOfflineNotice);
        View cardTimetableType = view.findViewById(R.id.card_timetableType);
        View cardSchoolEntries = view.findViewById(R.id.card_schoolEntries);
        View cardMyGroups = view.findViewById(R.id.card_myGroups);

        Spinner spinnerUserType = view.findViewById(R.id.spinnerUserType);
        Spinner spinnerClassTokens = view.findViewById(R.id.spinnerClassTokens);
        Spinner spinnerTeacherTokens = view.findViewById(R.id.spinnerTeacherTokens);
        Spinner spinnerClassroomTokens = view.findViewById(R.id.spinnerClassroomTokens);

        View layoutTimetableType = view.findViewById(R.id.layout_timetableType);
        View layoutSelectClass = view.findViewById(R.id.layout_selectClass);
        View layoutSelectTeacher = view.findViewById(R.id.layout_selectTeacher);
        View layoutSelectClassroom = view.findViewById(R.id.layout_selectClassroom);

        if (cardNotice != null) {
            cardNotice.setVisibility(isOnline ? View.GONE : View.VISIBLE);
        }

        float alpha = isOnline ? 1.0f : 0.45f;
        if (cardTimetableType != null) cardTimetableType.setAlpha(alpha);
        if (cardSchoolEntries != null) cardSchoolEntries.setAlpha(alpha);

        if (spinnerUserType != null) spinnerUserType.setEnabled(isOnline);
        if (spinnerClassTokens != null) spinnerClassTokens.setEnabled(isOnline);
        if (spinnerTeacherTokens != null) spinnerTeacherTokens.setEnabled(isOnline);
        if (spinnerClassroomTokens != null) spinnerClassroomTokens.setEnabled(isOnline);

        if (layoutTimetableType != null) layoutTimetableType.setEnabled(isOnline);
        if (layoutSelectClass != null) layoutSelectClass.setEnabled(isOnline);
        if (layoutSelectTeacher != null) layoutSelectTeacher.setEnabled(isOnline);
        if (layoutSelectClassroom != null) layoutSelectClassroom.setEnabled(isOnline);
    }

    private void initSpinners() {
        Spinner spinnerClassTokens = view.findViewById(R.id.spinnerClassTokens);
        Spinner spinnerTeacherTokens = view.findViewById(R.id.spinnerTeacherTokens);
        Spinner spinnerClassroomTokens = view.findViewById(R.id.spinnerClassroomTokens);
        Spinner spinnerUserType = view.findViewById(R.id.spinnerUserType);

        setSpinner(spinnerClassTokens, classesSchoolEntries, getString(R.string.classTokenKey));
        setSpinner(spinnerTeacherTokens, teachersSchoolEntries, getString(R.string.teacherTokenKey));
        setSpinner(spinnerClassroomTokens, classroomsSchoolEntries, getString(R.string.classroomTokenKey));

        setTypeOfTimetableSpinner();

        // Make entire card rows clickable to open their corresponding spinner
        setupRowClickListener(R.id.layout_timetableType, spinnerUserType);
        setupRowClickListener(R.id.layout_selectClass, spinnerClassTokens);
        setupRowClickListener(R.id.layout_selectTeacher, spinnerTeacherTokens);
        setupRowClickListener(R.id.layout_selectClassroom, spinnerClassroomTokens);

        if (getActivity() instanceof MainActivity) {
            updateOnlineState(((MainActivity) getActivity()).isOnline());
        }
    }

    private void setupRowClickListener(int rowId, Spinner spinner) {
        View rowView = view.findViewById(rowId);
        if (rowView != null && spinner != null) {
            rowView.setOnClickListener(v -> {
                if (rowView.isEnabled() && spinner.isEnabled()) {
                    spinner.performClick();
                }
            });
        }
    }

    private void setSpinner(Spinner spinner, List<SchoolEntry> schoolEntries, String sharedPreferencesToken) {
        List<String> tokenList = new ArrayList<>();
        for (SchoolEntry schoolEntry : schoolEntries) {
            tokenList.add(schoolEntry.shortcut());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, tokenList);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinner.setAdapter(adapter);

        String token = sharedPref.getString(sharedPreferencesToken, "");
        int selectedPosition = adapter.getPosition(token);

        if (selectedPosition < 0 && !schoolEntries.isEmpty()) {
            SchoolEntry defaultEntry = schoolEntries.get(0);

            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(sharedPreferencesToken, defaultEntry.shortcut());
            editor.apply();

            selectedPosition = 0;
        }

        if (selectedPosition >= 0) {
            spinner.setSelection(selectedPosition);
        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                SchoolEntry schoolEntry = schoolEntries.get(i);
                String oldToken = sharedPref.getString(sharedPreferencesToken, "");
                String newToken = schoolEntry.shortcut();

                if (!Objects.equals(oldToken, newToken)) {
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(sharedPreferencesToken, newToken);
                    editor.apply();

                    if (mainViewModel != null) {
                        mainViewModel.setSettingsChanged(true);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
    }

    private void setTypeOfTimetableSpinner() {
        Spinner spinnerUserType = view.findViewById(R.id.spinnerUserType);

        ArrayAdapter<CharSequence> userTypeAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.timetableTypeSpinnerItems,
                R.layout.spinner_item
        );
        userTypeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerUserType.setAdapter(userTypeAdapter);

        int typeIndex = sharedPref.getInt(getString(R.string.selectedTypeOfTimetableKey), 0);
        spinnerUserType.setSelection(typeIndex);

        spinnerUserType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                int oldTypeIndex = sharedPref.getInt(getString(R.string.selectedTypeOfTimetableKey), 0);
                if (oldTypeIndex != i) {
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putInt(getString(R.string.selectedTypeOfTimetableKey), i);
                    editor.apply();

                    changeVisibility();

                    if (mainViewModel != null) {
                        mainViewModel.setSettingsChanged(true);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
    }

    /**
     * Changes the visibility of three components: class, teacher, and classroom.
     */
    private void changeVisibility() {
        int whichIsVisible = sharedPref.getInt(getString(R.string.selectedTypeOfTimetableKey), 0) + 1;

        int classVisibility = View.GONE;
        int teacherVisibility = View.GONE;
        int classroomVisibility = View.GONE;

        if(whichIsVisible == 1) classVisibility = View.VISIBLE;
        else if(whichIsVisible == 2) teacherVisibility = View.VISIBLE;
        else if(whichIsVisible == 3) classroomVisibility = View.VISIBLE;

        View layoutClass = view.findViewById(R.id.layout_selectClass);
        View layoutTeacher = view.findViewById(R.id.layout_selectTeacher);
        View layoutClassroom = view.findViewById(R.id.layout_selectClassroom);

        if (layoutClass != null) layoutClass.setVisibility(classVisibility);
        if (layoutTeacher != null) layoutTeacher.setVisibility(teacherVisibility);
        if (layoutClassroom != null) layoutClassroom.setVisibility(classroomVisibility);

        view.findViewById(R.id.textView_selectClass).setVisibility(classVisibility);
        view.findViewById(R.id.textView_selectTeacher).setVisibility(teacherVisibility);
        view.findViewById(R.id.textView_selectClassroom).setVisibility(classroomVisibility);

        view.findViewById(R.id.spinnerClassTokens).setVisibility(classVisibility);
        view.findViewById(R.id.spinnerTeacherTokens).setVisibility(teacherVisibility);
        view.findViewById(R.id.spinnerClassroomTokens).setVisibility(classroomVisibility);

        View cardMyGroups = view.findViewById(R.id.card_myGroups);
        if (cardMyGroups != null) {
            cardMyGroups.setVisibility(whichIsVisible == 1 ? View.VISIBLE : View.GONE);
        }
    }

    private void getData() {
        SchoolEntriesDownloader spinnersDataDownloader = new SchoolEntriesDownloader(mainViewModel.getClient());
        new Thread(() -> {
            spinnersDataDownloader.run();

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!isAdded() || view == null) return;

                    classesSchoolEntries = spinnersDataDownloader.getClassesSchoolEntries();
                    teachersSchoolEntries = spinnersDataDownloader.getTeachersSchoolEntries();
                    classroomsSchoolEntries = spinnersDataDownloader.getClassroomsSchoolEntries();

                    ensurePreviewDefaults();

                    // Initialize spinners and populate them with data.
                    initSpinners();
                });
            }
        }).start();
    }

    private void ensurePreviewDefaults() {
        SharedPreferences.Editor editor = sharedPref.edit();
        boolean changed = false;

        if (!sharedPref.contains(getString(R.string.classTokenKey)) && !classesSchoolEntries.isEmpty()) {
            editor.putString(getString(R.string.classTokenKey), classesSchoolEntries.get(0).shortcut());
            changed = true;
        }

        if (!sharedPref.contains(getString(R.string.teacherTokenKey)) && !teachersSchoolEntries.isEmpty()) {
            editor.putString(getString(R.string.teacherTokenKey), teachersSchoolEntries.get(0).shortcut());
            changed = true;
        }

        if (!sharedPref.contains(getString(R.string.classroomTokenKey)) && !classroomsSchoolEntries.isEmpty()) {
            editor.putString(getString(R.string.classroomTokenKey), classroomsSchoolEntries.get(0).shortcut());
            changed = true;
        }

        if (changed) {
            editor.apply();
        }
    }
}
