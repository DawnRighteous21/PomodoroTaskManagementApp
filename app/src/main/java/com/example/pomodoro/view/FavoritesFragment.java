package com.example.pomodoro.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.pomodoro.R;
import com.example.pomodoro.model.Quote;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FavoritesFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView textEmpty;
    private FavoriteQuoteAdapter adapter;
    private DatabaseReference mDatabase;
    private ValueEventListener valueEventListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);

        textEmpty = view.findViewById(R.id.text_empty_favorites);
        recyclerView = view.findViewById(R.id.recycler_favorites);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new FavoriteQuoteAdapter();
        recyclerView.setAdapter(adapter);

        String dbUrl = "https://pomodoro-3cf47-default-rtdb.asia-southeast1.firebasedatabase.app/";
        mDatabase = FirebaseDatabase.getInstance(dbUrl).getReference("favorite_quotes");

        adapter.setOnQuoteDeleteListener(quote -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Xóa trích dẫn")
                    .setMessage("Bạn có muốn xóa trích dẫn này khỏi bộ sưu tập trên mây?")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        if (quote.getFirebaseKey() != null) {
                            mDatabase.child(quote.getFirebaseKey()).removeValue()
                                    .addOnSuccessListener(aVoid -> Toast.makeText(requireContext(), "Đã xóa trích dẫn", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e -> Toast.makeText(requireContext(), "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        loadFavoriteQuotes();

        return view;
    }

    private void loadFavoriteQuotes() {
        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Quote> quoteList = new ArrayList<>();
                for (DataSnapshot data : snapshot.getChildren()) {
                    if ("ping".equals(data.getKey())) continue;

                    Quote quote = data.getValue(Quote.class);
                    if (quote != null && quote.getText() != null) {
                        quote.setFirebaseKey(data.getKey());
                        quoteList.add(quote);
                    }
                }

                Collections.reverse(quoteList);
                adapter.setQuotes(quoteList);

                if (quoteList.isEmpty()) {
                    textEmpty.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    textEmpty.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Lỗi tải dữ liệu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        };
        mDatabase.addValueEventListener(valueEventListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mDatabase != null && valueEventListener != null) {
            mDatabase.removeEventListener(valueEventListener);
        }
    }
}