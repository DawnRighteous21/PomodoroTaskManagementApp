package com.example.pomodoro.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.pomodoro.R;
import com.example.pomodoro.model.Quote;
import java.util.ArrayList;
import java.util.List;

public class FavoriteQuoteAdapter extends RecyclerView.Adapter<FavoriteQuoteAdapter.QuoteViewHolder> {

    private List<Quote> quotes = new ArrayList<>();
    private OnQuoteDeleteListener deleteListener;

    public interface OnQuoteDeleteListener {
        void onDeleteClick(Quote quote);
    }

    public void setOnQuoteDeleteListener(OnQuoteDeleteListener listener) {
        this.deleteListener = listener;
    }

    public void setQuotes(List<Quote> quotes) {
        this.quotes = quotes;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public QuoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorite_quote, parent, false);
        return new QuoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuoteViewHolder holder, int position) {
        Quote current = quotes.get(position);
        holder.textQuote.setText("\"" + current.getText() + "\"");
        holder.textAuthor.setText("- " + current.getAuthor());

        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteClick(current);
            }
        });
    }

    @Override
    public int getItemCount() {
        return quotes.size();
    }

    static class QuoteViewHolder extends RecyclerView.ViewHolder {
        TextView textQuote, textAuthor;
        ImageButton btnDelete;

        public QuoteViewHolder(@NonNull View itemView) {
            super(itemView);
            textQuote = itemView.findViewById(R.id.text_item_quote);
            textAuthor = itemView.findViewById(R.id.text_item_author);
            btnDelete = itemView.findViewById(R.id.btn_delete_quote);
        }
    }
}