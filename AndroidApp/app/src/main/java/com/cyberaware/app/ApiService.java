package com.cyberaware.app;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    @POST("api/report") // সার্ভারের সেই একই ঠিকানা
    Call<String> sendReport(@Body ReportRequest request);
}