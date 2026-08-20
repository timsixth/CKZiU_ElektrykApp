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

import com.example.planlekcji.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.ShapeAppearanceModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Presentational component that displays a subject title
 * and dynamic toggle buttons for selecting one of its available groups.
 */
public class SubjectGroupView extends LinearLayout {

    public interface OnGroupSelectionListener {
        void onOptionSelected(String selectedOption);
    }

    private TextView titleTextView;
    private LinearLayout toggleRowsContainer;
    private final List<MaterialButtonToggleGroup> toggleGroups = new ArrayList<>();
    private String subjectTitle = "";
    private OnGroupSelectionListener selectionListener;
    private boolean isBinding = false;

    public SubjectGroupView(Context context) {
        super(context);
        init(context);
    }

    public SubjectGroupView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SubjectGroupView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        inflate(context, R.layout.item_subject_group, this);

        titleTextView = findViewById(R.id.textView_subjectTitle);
        toggleRowsContainer = findViewById(R.id.layout_toggleRowsContainer);
    }

    public void bind(
            String subjectTitle,
            List<String> options,
            String selectedOption,
            OnGroupSelectionListener listener
    ) {
        this.isBinding = true;
        this.subjectTitle = subjectTitle;
        this.selectionListener = listener;

        titleTextView.setText(subjectTitle);
        toggleRowsContainer.removeAllViews();
        toggleGroups.clear();

        if (options == null || options.isEmpty()) {
            this.isBinding = false;
            return;
        }

        float density = getResources().getDisplayMetrics().density;
        float cornerRadius = 8 * density;

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

        int itemsPerRow = options.size() <= 4 ? options.size() : 3;
        int totalRows = (int) Math.ceil((double) options.size() / itemsPerRow);

        MaterialButtonToggleGroup checkedGroup = null;
        int checkedButtonId = View.NO_ID;

        for (int r = 0; r < totalRows; r++) {
            int start = r * itemsPerRow;
            int end = Math.min(start + itemsPerRow, options.size());
            List<String> chunk = options.subList(start, end);
            boolean isFirstRow = (r == 0);
            boolean isLastRow = (r == totalRows - 1);

            MaterialButtonToggleGroup rowToggleGroup = new MaterialButtonToggleGroup(getContext());
            rowToggleGroup.setSingleSelection(true);
            rowToggleGroup.setSelectionRequired(false);

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            if (r > 0) {
                // Overlap 1dp border between rows so they touch seamlessly
                rowParams.topMargin = (int) (-1 * density);
            }
            rowToggleGroup.setLayoutParams(rowParams);

            for (int col = 0; col < chunk.size(); col++) {
                String option = chunk.get(col);
                boolean isFirstCol = (col == 0);
                boolean isLastCol = (col == chunk.size() - 1);

                float tl = (isFirstRow && isFirstCol) ? cornerRadius : 0;
                float tr = (isFirstRow && isLastCol) ? cornerRadius : 0;
                float bl = (isLastRow && isFirstCol) ? cornerRadius : 0;
                float br = (isLastRow && isLastCol) ? cornerRadius : 0;

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
                button.setStrokeWidth((int) (1 * density));
                button.setTextColor(textColors);
                button.setStrokeColor(strokeColors);
                button.setBackgroundTintList(bgColors);

                ShapeAppearanceModel shape = ShapeAppearanceModel.builder()
                        .setTopLeftCorner(CornerFamily.ROUNDED, tl)
                        .setTopRightCorner(CornerFamily.ROUNDED, tr)
                        .setBottomLeftCorner(CornerFamily.ROUNDED, bl)
                        .setBottomRightCorner(CornerFamily.ROUNDED, br)
                        .build();
                button.setShapeAppearanceModel(shape);

                LayoutParams btnParams = new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                rowToggleGroup.addView(button, btnParams);

                // Re-apply shape after addView in case toggle group modifies child corners
                button.setShapeAppearanceModel(shape);

                if (selectedOption != null && selectedOption.equals(option)) {
                    checkedGroup = rowToggleGroup;
                    checkedButtonId = button.getId();
                }
            }

            rowToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (isBinding) return;

                if (isChecked) {
                    isBinding = true;
                    for (MaterialButtonToggleGroup otherGroup : toggleGroups) {
                        if (otherGroup != group) {
                            otherGroup.clearChecked();
                        }
                    }
                    isBinding = false;

                    View checkedBtn = group.findViewById(checkedId);
                    if (checkedBtn != null && checkedBtn.getTag() instanceof String opt) {
                        if (selectionListener != null) {
                            selectionListener.onOptionSelected(opt);
                        }
                    }
                } else {
                    boolean anyChecked = false;
                    for (MaterialButtonToggleGroup g : toggleGroups) {
                        if (g.getCheckedButtonId() != View.NO_ID) {
                            anyChecked = true;
                            break;
                        }
                    }
                    if (!anyChecked && selectionListener != null) {
                        selectionListener.onOptionSelected(null);
                    }
                }
            });

            toggleGroups.add(rowToggleGroup);
            toggleRowsContainer.addView(rowToggleGroup);
        }

        if (checkedGroup != null && checkedButtonId != View.NO_ID) {
            checkedGroup.check(checkedButtonId);
        }

        this.isBinding = false;
    }

    public void clearSelection() {
        isBinding = true;
        for (MaterialButtonToggleGroup g : toggleGroups) {
            g.clearChecked();
        }
        isBinding = false;
    }

    public void setSelectedOption(String option) {
        isBinding = true;
        for (MaterialButtonToggleGroup g : toggleGroups) {
            g.clearChecked();
        }
        if (option != null) {
            for (MaterialButtonToggleGroup g : toggleGroups) {
                for (int i = 0; i < g.getChildCount(); i++) {
                    View child = g.getChildAt(i);
                    if (child.getTag() instanceof String opt && option.equals(opt)) {
                        g.check(child.getId());
                        break;
                    }
                }
            }
        }
        isBinding = false;
    }

    public String getSelectedOption() {
        for (MaterialButtonToggleGroup g : toggleGroups) {
            int checkedId = g.getCheckedButtonId();
            if (checkedId != View.NO_ID) {
                View child = g.findViewById(checkedId);
                if (child != null && child.getTag() instanceof String opt) {
                    return opt;
                }
            }
        }
        return null;
    }

    public String getSubjectTitle() {
        return subjectTitle;
    }
}
