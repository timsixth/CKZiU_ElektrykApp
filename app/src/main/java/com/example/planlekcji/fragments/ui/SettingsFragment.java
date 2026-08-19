package com.example.planlekcji.fragments.ui;

import android.content.SharedPreferences;
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

import com.example.planlekcji.MainActivity;
import com.example.planlekcji.MainViewModel;
import com.example.planlekcji.R;
import com.example.planlekcji.ckziu_elektryk.client.timetable.SchoolEntry;
import com.example.planlekcji.settings.SchoolEntriesDownloader;

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
    private final Handler fetchHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable fetchRunnable = () -> {
        if (isAdded() && mainViewModel != null) {
            mainViewModel.fetchTimetable();
            mainViewModel.fetchReplacements();
        }
    };

    private void scheduleDataFetch() {
        fetchHandler.removeCallbacks(fetchRunnable);
        fetchHandler.postDelayed(fetchRunnable, 150);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        fetchHandler.removeCallbacks(fetchRunnable);
    }

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

        // Fetch data relevant to settings.
        getData();

        return view;
    }

    private void updateOnlineState(boolean isOnline) {
        if (view == null) return;

        View cardNotice = view.findViewById(R.id.card_settingsOfflineNotice);
        View cardTimetableType = view.findViewById(R.id.card_timetableType);
        View cardSchoolEntries = view.findViewById(R.id.card_schoolEntries);

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

                    scheduleDataFetch();
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
                    scheduleDataFetch();
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
