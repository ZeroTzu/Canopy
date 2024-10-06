package com.generationai.polaris.utils

import android.util.Log
import org.chromium.net.CronetException
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

class MLServiceRequest(private val callback: ResponseCallback) : UrlRequest.Callback(){
    override fun onRedirectReceived(request: UrlRequest?, info: UrlResponseInfo?, newLocationUrl: String?) {
        Log.i("MLServiceRequest", "onRedirectReceived method called.")
        // You should call the request.followRedirect() method to continue
        // processing the request.
        request?.followRedirect()
    }

    override fun onResponseStarted(request: UrlRequest?, info: UrlResponseInfo?) {
        Log.i("MLServiceRequest", "onResponseStarted method called.")
        // You should call the request.read() method before the request can be
        // further processed. The following instruction provides a ByteBuffer object
        // with a capacity of 102400 bytes for the read() method. The same buffer
        // with data is passed to the onReadCompleted() method.
        request?.read(ByteBuffer.allocateDirect(102400))
    }

    override fun onReadCompleted(request: UrlRequest?, info: UrlResponseInfo?, byteBuffer: ByteBuffer?) {
        byteBuffer?.flip()
        Log.i("MLServiceRequest", "onReadCompleted method called.")
        Log.i("MLServiceRequest", "byteBuffer: $byteBuffer")
        val responseData = StandardCharsets.UTF_8.decode(byteBuffer).toString()
        Log.i("MLServiceRequest", "Response: $responseData")
        try{
            val jsonObject = JSONObject(responseData)
            val fact = jsonObject.getString("fact")
            Log.i("MLServiceRequest", "Fact: $fact")
            callback.onResponseReceived(fact)
        }catch (e: Exception){
            e.printStackTrace()
            callback.onResponseReceived("Failed to parse JSON: ${e.message}")
        }

        // You should keep reading the request until there's no more data.
        byteBuffer?.clear()
        request?.read(byteBuffer)
    }

    override fun onSucceeded(request: UrlRequest?, info: UrlResponseInfo?) {
        Log.i("MLServiceRequest", "onSucceeded method called.")
    }
    override fun onFailed(request: UrlRequest?, info: UrlResponseInfo?, error: CronetException?) {
        Log.i("MLServiceRequest", "onFailed method called.")
    }

    interface ResponseCallback{
        fun onResponseReceived(response: String)
    }

}
