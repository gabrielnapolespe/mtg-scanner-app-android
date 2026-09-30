package com.zaziapps.mtg_scanner.network

import com.zaziapps.mtg_scanner.data.model.ScryfallResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit API service interface defining network endpoint configurations for the Scryfall API.
 * Handles the declaration of outbound HTTP requests used to communicate with remote card catalog indexes.
 */
interface ScryfallApiService {

    /**
     * Executes an asynchronous HTTP GET request to search the remote database for matching card collections.
     *
     * @param query The formatted Scryfall syntax query string used to filter remote card search entries.
     * @return A network [Response] wrapping the [ScryfallResponse] data transfer container framework.
     */
    @GET("cards/search")
    suspend fun searchCard(
        @Query("q") query: String
    ): Response<ScryfallResponse>
}
