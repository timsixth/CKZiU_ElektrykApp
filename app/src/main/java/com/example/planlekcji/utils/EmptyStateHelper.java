package com.example.planlekcji.utils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.planlekcji.R;

public final class EmptyStateHelper {

    private EmptyStateHelper() {}

    public static View create(LayoutInflater inflater, ViewGroup parent, EmptyStateType type) {
        View view = inflater.inflate(R.layout.empty_state, parent, false);

        ImageView icon = view.findViewById(R.id.imageView_emptyIcon);
        TextView title = view.findViewById(R.id.textView_emptyTitle);
        TextView description = view.findViewById(R.id.textView_emptyDescription);

        icon.setImageResource(type.getIconResId());
        title.setText(type.getTitleResId());
        description.setText(type.getDescriptionResId());

        return view;
    }
}
