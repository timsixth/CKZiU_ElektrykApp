package com.example.planlekcji.settings;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.planlekcji.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.List;

/**
 * Presentational component that displays a subject title
 * and dynamic toggle buttons for selecting one of its available groups.
 */
public class SubjectGroupView extends LinearLayout {

    public interface OnGroupSelectionListener {
        void onOptionSelected(@Nullable String selectedOption);
    }

    private TextView titleTextView;
    private MaterialButtonToggleGroup toggleGroup;
    private String subjectTitle = "";
    private OnGroupSelectionListener selectionListener;
    private boolean isBinding = false;

    public SubjectGroupView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public SubjectGroupView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SubjectGroupView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(@NonNull Context context) {
        setOrientation(VERTICAL);
        inflate(context, R.layout.item_subject_group, this);

        titleTextView = findViewById(R.id.textView_subjectTitle);
        toggleGroup = findViewById(R.id.toggleGroup_groups);

        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isBinding) return;

            if (isChecked) {
                View checkedBtn = group.findViewById(checkedId);
                if (checkedBtn != null && checkedBtn.getTag() instanceof String opt) {
                    if (selectionListener != null) {
                        selectionListener.onOptionSelected(opt);
                    }
                }
            } else if (group.getCheckedButtonId() == View.NO_ID) {
                if (selectionListener != null) {
                    selectionListener.onOptionSelected(null);
                }
            }
        });
    }

    public void bind(
            @NonNull String subjectTitle,
            @NonNull List<String> options,
            @Nullable String selectedOption,
            @Nullable OnGroupSelectionListener listener
    ) {
        this.isBinding = true;
        this.subjectTitle = subjectTitle;
        this.selectionListener = listener;

        titleTextView.setText(subjectTitle);
        toggleGroup.removeAllViews();

        float density = getResources().getDisplayMetrics().density;
        int checkedButtonId = View.NO_ID;

        ColorStateList textColors = new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{Color.parseColor("#FFD54F"), Color.parseColor("#9E9E9E")}
        );
        ColorStateList strokeColors = new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{Color.parseColor("#99FFC107"), Color.parseColor("#2C2C2E")}
        );
        ColorStateList bgColors = new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{Color.parseColor("#2E250A"), Color.parseColor("#1E1E1E")}
        );

        for (String option : options) {
            MaterialButton button = new MaterialButton(getContext());
            button.setId(View.generateViewId());
            button.setTag(option);
            button.setText(option);
            button.setCheckable(true);
            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            button.setLetterSpacing(0f);
            button.setMinHeight((int) (38 * density));
            button.setInsetTop(0);
            button.setInsetBottom(0);
            button.setCornerRadius((int) (8 * density));
            button.setStrokeWidth((int) (1 * density));
            button.setTextColor(textColors);
            button.setStrokeColor(strokeColors);
            button.setBackgroundTintList(bgColors);

            LayoutParams btnParams = new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            toggleGroup.addView(button, btnParams);

            if (selectedOption != null && selectedOption.equals(option)) {
                checkedButtonId = button.getId();
            }
        }

        if (checkedButtonId != View.NO_ID) {
            toggleGroup.check(checkedButtonId);
        } else {
            toggleGroup.clearChecked();
        }

        this.isBinding = false;
    }

    public void clearSelection() {
        isBinding = true;
        toggleGroup.clearChecked();
        isBinding = false;
    }

    public void setSelectedOption(@Nullable String option) {
        isBinding = true;
        if (option == null) {
            toggleGroup.clearChecked();
        } else {
            for (int i = 0; i < toggleGroup.getChildCount(); i++) {
                View child = toggleGroup.getChildAt(i);
                if (child.getTag() instanceof String opt && option.equals(opt)) {
                    toggleGroup.check(child.getId());
                    break;
                }
            }
        }
        isBinding = false;
    }

    @Nullable
    public String getSelectedOption() {
        int checkedId = toggleGroup.getCheckedButtonId();
        if (checkedId == View.NO_ID) return null;
        View child = toggleGroup.findViewById(checkedId);
        if (child != null && child.getTag() instanceof String opt) {
            return opt;
        }
        return null;
    }

    @NonNull
    public String getSubjectTitle() {
        return subjectTitle;
    }
}
