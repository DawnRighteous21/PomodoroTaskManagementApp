package com.example.pomodoro.network;

import com.example.pomodoro.model.Quote;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;

public interface QuoteApi {
    @GET("api/random")
    Call<List<Quote>> getRandomQuote();
}