package com.example.pomodoro.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.pomodoro.R;
import com.example.pomodoro.model.DailyStat;
import java.util.ArrayList;
import java.util.List;


public class StatAdapter extends RecyclerView.Adapter<StatAdapter.StatViewHolder> {
    public interface OnItemClickListener {
        void onItemClick(String date);
    }
    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
    private List<DailyStat> stats = new ArrayList<>();

    @NonNull
    @Override
    public StatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_daily_stat, parent, false);
        return new StatViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull StatViewHolder holder, int position) {
        DailyStat stat = stats.get(position);
        holder.textDate.setText("Ngày: " + stat.getDate());
        holder.textPomodoros.setText(stat.getTotalPomodoros() + " phiên (Pomo)");
        holder.textMinutes.setText(stat.getTotalWorkMinutes() + " phút tập trung");
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(stat.getDate());
            }
        });
    }

    @Override
    public int getItemCount() { return stats.size(); }

    public void setStats(List<DailyStat> stats) {
        this.stats = stats;
        notifyDataSetChanged();
    }

    class StatViewHolder extends RecyclerView.ViewHolder {
        private TextView textDate, textPomodoros, textMinutes;
        public StatViewHolder(@NonNull View itemView) {
            super(itemView);
            textDate = itemView.findViewById(R.id.text_stat_date);
            textPomodoros = itemView.findViewById(R.id.text_stat_pomodoros);
            textMinutes = itemView.findViewById(R.id.text_stat_minutes);
        }
    }
}