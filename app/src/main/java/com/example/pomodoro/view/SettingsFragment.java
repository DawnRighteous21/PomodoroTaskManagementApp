package com.example.pomodoro.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import com.example.pomodoro.R;

public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        SwitchCompat switchStrictMode = view.findViewById(R.id.switch_strict_mode);
        SwitchCompat switchDevMode = view.findViewById(R.id.switch_dev_mode);
        Button btnHelp = view.findViewById(R.id.btn_help);

        SharedPreferences prefs = requireActivity().getSharedPreferences("TimerPrefs", Context.MODE_PRIVATE);
        switchStrictMode.setChecked(prefs.getBoolean("strict_mode", false));
        switchDevMode.setChecked(prefs.getBoolean("dev_mode", false));

        switchStrictMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && buttonView.isPressed()) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Enable Strict Mode?")
                        .setMessage("Trong chế độ Strict Mode, bạn không thể bấm tạm dừng. Rời ứng dụng quá 5 giây sẽ bị mất toàn bộ tiến độ đếm ngược. Bạn có chắc chắn không?")
                        .setPositiveButton("Bật", (dialog, which) -> prefs.edit().putBoolean("strict_mode", true).apply())
                        .setNegativeButton("Hủy", (dialog, which) -> switchStrictMode.setChecked(false))
                        .setCancelable(false)
                        .show();
            } else if (!isChecked) {
                prefs.edit().putBoolean("strict_mode", false).apply();
            }
        });

        switchDevMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("dev_mode", isChecked).apply();
        });

        btnHelp.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Hướng dẫn sử dụng")
                    .setMessage(
                            "1. Thêm công việc: Bấm biểu tượng (+) tại trang Công việc để điều chỉnh công việc theo mong muốn.\n\n" +
                                    "2. Đếm ngược: Nhấn vào một công việc để bắt đầu phiên tập trung. Tiến trình sẽ được duy trì hoạt động dưới nền khi tắt màn hình.\n\n" +
                                    "3. Strict Mode: Chế độ rèn luyện kỷ luật. Khi được kích hoạt, tiến trình của phiên hiện tại sẽ bị hủy bỏ ngay lập tức nếu ứng dụng bị đưa xuống nền quá 5 giây.\n\n" +
                                    "4. Bộ sưu tập: Lưu trữ các trích dẫn yêu thích bằng cách nhấn vào biểu tượng ngôi sao trong thời gian nghỉ ngơi.\n\n" +
                                    "5. Thống kê: Theo dõi lịch sử và tổng thời gian tập trung trong 30 ngày gần nhất."
                    )
                    .setPositiveButton("Đã hiểu", null)
                    .show();
        });
        return view;
    }
}