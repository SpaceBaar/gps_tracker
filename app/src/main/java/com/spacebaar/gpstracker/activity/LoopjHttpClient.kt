package com.spacebaar.gpstracker.activity

import android.util.Log
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.AsyncHttpResponseHandler
import com.loopj.android.http.RequestParams
import cz.msebera.android.httpclient.Header

object LoopjHttpClient {
    private val client = AsyncHttpClient(true, 80, 443)
    operator fun get(url: String?, params: RequestParams?, responseHandler: AsyncHttpResponseHandler?) {
        client[url, params, responseHandler]
    }

    fun post(url: String?, requestParams: RequestParams?, responseHandler: AsyncHttpResponseHandler?) {
        client.post(url, requestParams, responseHandler)
    }

    fun debugLoopJ(TAG: String?, methodName: String?, url: String?, requestParams: RequestParams?, response: ByteArray?, headers: Array<Header>?, statusCode: Int, t: Throwable?) {
        Log.d(TAG, AsyncHttpClient.getUrlWithQueryString(false, url, requestParams))
        if (headers != null) {
            Log.e(TAG, methodName.toString())
            Log.d(TAG, "Return Headers:")
            /*
            for (Header h : headers) {
                String _h = String.format(Locale.US, "%s : %s", h.getName(), h.getValue());
                Log.d(TAG, _h);
            }
            */if (t != null) {
                Log.d(TAG, "Throwable:$t")
            }
            Log.e(TAG, "StatusCode: $statusCode")
            if (response != null) {
                Log.d(TAG, "Response: " + String(response))
            }
        }
    }
}