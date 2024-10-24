package com.example.planlekcji.settings.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.example.planlekcji.R;
import com.example.planlekcji.settings.SchoolEntriesDownloader;
import com.example.planlekcji.settings.model.TimetableInfo;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {
    private SharedPreferences sharedPref;
    private List<TimetableInfo> classInfoList = new ArrayList<>();
    private List<TimetableInfo> teachersInfoList = new ArrayList<>();
    private List<TimetableInfo> classroomsInfoList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the content view to display the settings layout.
        setContentView(R.layout.settings_layout);

        // Initialize SharedPreferences for storing application settings.
        sharedPref = this.getSharedPreferences("sharedPrefs", 0);

        // Fetch data relevant to settings.
        getData();

        // Initialize spinners and populate them with data.
        initSpinners();

        // Set a listener for the back button to handle navigation.
        setListenerToBackButton();

        // Set a listener for the switch responsible for changing visibility of replacements on timetable.
        initReplacementVisibilitySwitch();
    }

    private void initReplacementVisibilitySwitch() {
        SwitchCompat visibilitySwitch = findViewById(R.id.switch_showReplacementsOnTimetable);

        boolean visibility = sharedPref.getBoolean(getString(R.string.replacementVisibilityOnTimetable), false);

        visibilitySwitch.setChecked(visibility);

        visibilitySwitch.setOnCheckedChangeListener((compoundButton, bool) -> {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(getString(R.string.replacementVisibilityOnTimetable), bool);
            editor.apply();

            if(bool) {
                new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(getResources().getString(R.string.replacementsOnTimetable_title))
                        .setMessage(getResources().getString(R.string.replacementsOnTimetable_description))
                        .setPositiveButton(getResources().getString(android.R.string.ok), (dialogInterface, i) -> {})
                        .show();
            }
        });
    }

    private void initSpinners() {
        Spinner spinnerClassTokens = findViewById(R.id.spinnerClassTokens);
        Spinner spinnerTeacherTokens = findViewById(R.id.spinnerTeacherTokens);
        Spinner spinnerClassroomTokens = findViewById(R.id.spinnerClassroomTokens);

        setSpinner(spinnerClassTokens, classInfoList, getString(R.string.classTokenKey), getString(R.string.classTimetableUrlKey));
        setSpinner(spinnerTeacherTokens, teachersInfoList, getString(R.string.teacherTokenKey), getString(R.string.teacherTimetableUrlKey));
        setSpinner(spinnerClassroomTokens, classroomsInfoList, getString(R.string.classroomTokenKey), getString(R.string.classroomTimetableUrlKey));

        setTypeOfTimetableSpinner();
    }

    private void setSpinner(Spinner spinner, List<TimetableInfo> timetableInfoList, String sharedPreferencesToken, String sharedPreferencesUrl) {
        List<String> tokenList = new ArrayList<>();
        for (TimetableInfo timetableInfo : timetableInfoList) {
            tokenList.add(timetableInfo.getToken());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, tokenList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        String token = sharedPref.getString(sharedPreferencesToken, "");
        spinner.setSelection(adapter.getPosition(token));

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                TimetableInfo timetableInfo = timetableInfoList.get(i);

                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(sharedPreferencesToken, timetableInfo.getToken());
                editor.putString(sharedPreferencesUrl, timetableInfo.getUrl());
                editor.apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
    }

    private void setTypeOfTimetableSpinner() {
        Spinner spinnerUserType = findViewById(R.id.spinnerUserType);

        int typeIndex = sharedPref.getInt(getString(R.string.selectedTypeOfTimetableKey), 0);
        spinnerUserType.setSelection(typeIndex);

        spinnerUserType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt(getString(R.string.selectedTypeOfTimetableKey), i);
                editor.apply();

                changeVisibility();
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

        findViewById(R.id.textView_selectClass).setVisibility(classVisibility);
        findViewById(R.id.textView_selectTeacher).setVisibility(teacherVisibility);
        findViewById(R.id.textView_selectClassroom).setVisibility(classroomVisibility);

        findViewById(R.id.spinnerClassTokens).setVisibility(classVisibility);
        findViewById(R.id.spinnerTeacherTokens).setVisibility(teacherVisibility);
        findViewById(R.id.spinnerClassroomTokens).setVisibility(classroomVisibility);

        findViewById(R.id.switch_showReplacementsOnTimetable).setVisibility(classVisibility);
        findViewById(R.id.textView_showReplacementsOnTimetable).setVisibility(classVisibility);
    }

    private void getData() {
        SchoolEntriesDownloader spinnersDataDownloader = new SchoolEntriesDownloader();
        Thread thread = new Thread(spinnersDataDownloader);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        classInfoList = spinnersDataDownloader.getClassInfoList();
        teachersInfoList = spinnersDataDownloader.getTeachersInfoList();
        classroomsInfoList = spinnersDataDownloader.getClassroomsInfoList();
    }

    private void setListenerToBackButton() {
        ImageButton imageButton_goBack = findViewById(R.id.imageButton_back);
        imageButton_goBack.setOnClickListener(view -> finish());
    }
}
