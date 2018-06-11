package com.eriyaz.social.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.eriyaz.social.BuildConfig;
import com.eriyaz.social.enums.PaymentStatus;
import com.eriyaz.social.managers.listeners.OnPaymentCompleteListener;
import com.paytm.pgsdk.Log;
import com.paytm.pgsdk.PaytmOrder;
import com.paytm.pgsdk.PaytmPGService;
import com.paytm.pgsdk.PaytmPaymentTransactionCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by vikas on 31/5/18.
 */

public class OfficialFeedbackRequest {
    public static final String TAG = OfficialFeedbackRequest.class.getSimpleName();
    private static String CALLBACK_URL = "https://securegw.paytm.in/theia/paytmCallback?ORDER_ID=";
    private static String CHANNEL_ID = "WAP";
    private static String INDUSTRY_TYPE_ID = "Retail109";
    private static String MID = "Seeone11117755382558";
    private static String WEBSITE = "APPPROD";

    private String customerId;
    private String orderId;
    private Integer txnAmount;
    private Context context;

    public OfficialFeedbackRequest(String customerId, String orderId, int txnAmount, Context context) {
        this.customerId = customerId;
        this.orderId = orderId;
        this.txnAmount = txnAmount;
        this.context = context;
    }

    public void create(OnPaymentCompleteListener onTaskCompleteListener) {
        // http request to generate checksum
        try {
            generateChecksum(onTaskCompleteListener);
        } catch (JSONException e) {
            onTaskCompleteListener.onTaskComplete(PaymentStatus.FAILED);
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void verifyPayment(final OnPaymentCompleteListener onTaskCompleteListener) {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(context);
        String verfiyPaymentUrl;
        if (BuildConfig.DEBUG) {
            verfiyPaymentUrl ="https://us-central1-eriyaz-social-dev.cloudfunctions.net/verfiyTransactionStatus";
        } else {
            verfiyPaymentUrl ="https://us-central1-eriyaz-social.cloudfunctions.net/verfiyTransactionStatus";
        }

        Uri.Builder uriBuilder = Uri.parse(verfiyPaymentUrl).buildUpon();
        Uri uri = uriBuilder.appendQueryParameter("customerId", customerId)
                .appendQueryParameter("orderId", orderId)
                .build();

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest (Request.Method.GET, uri.toString(), null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        LogUtil.logInfo(TAG, response.toString());
                        try {
                            String status = response.getString("STATUS");
                            if (status != null && status.equalsIgnoreCase("TXN_SUCCESS")) {
                                Toast.makeText(context, "Payment Status: Success", Toast.LENGTH_LONG).show();
                                onTaskCompleteListener.onTaskComplete(PaymentStatus.SUCCESS);
                            } else if (status != null && status.equalsIgnoreCase("PENDING")){
                                Toast.makeText(context, "Payment Status: " + status, Toast.LENGTH_LONG).show();
                                onTaskCompleteListener.onTaskComplete(PaymentStatus.PENDING);
                            } else {
                                Toast.makeText(context, "Payment Status: " + status, Toast.LENGTH_LONG).show();
                                onTaskCompleteListener.onTaskComplete(PaymentStatus.FAILED);
                            }
                        } catch (JSONException e) {
                            onTaskCompleteListener.onTaskComplete(PaymentStatus.FAILED);
                            Toast.makeText(context, "Checksum Json parse error", Toast.LENGTH_LONG).show();
                        }
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                onTaskCompleteListener.onTaskComplete(PaymentStatus.FAILED);
                Toast.makeText(context, error.getMessage(), Toast.LENGTH_LONG).show();

            }
        }
        );
        queue.add(jsonObjectRequest);
    }

    private void generateChecksum(final OnPaymentCompleteListener onTaskCompleteListener) throws JSONException {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(context);
        String checkSumUrl;
        if (BuildConfig.DEBUG) {
            checkSumUrl ="https://us-central1-eriyaz-social-dev.cloudfunctions.net/generateChecksum";
        } else {
            checkSumUrl ="https://us-central1-eriyaz-social.cloudfunctions.net/generateChecksum";
        }

        Uri.Builder uriBuilder = Uri.parse(checkSumUrl).buildUpon();
        Uri uri = uriBuilder.appendQueryParameter("customerId", customerId)
                .appendQueryParameter("orderId", orderId)
                .appendQueryParameter("txnAmount", Integer.toString(txnAmount))
                .build();


        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest (Request.Method.GET, uri.toString(), null,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    LogUtil.logInfo(TAG, response.toString());
                    try {
                        startTransaction(response.getString("CHECKSUMHASH"), onTaskCompleteListener);
                    } catch (JSONException e) {
                        onTaskCompleteListener.onTaskComplete(PaymentStatus.FAILED);
                        Toast.makeText(context, "Checksum Json parse error", Toast.LENGTH_LONG).show();
                    }
                }
            }, new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {
                    onTaskCompleteListener.onTaskComplete(PaymentStatus.FAILED);
                    Toast.makeText(context, error.getMessage(), Toast.LENGTH_LONG).show();

                }
            }
        );
        queue.add(jsonObjectRequest);
    }

    private void startTransaction(String checkSumHash, final OnPaymentCompleteListener onTaskCompleteListener) {
        LogUtil.logInfo(TAG, "onStartTransaction");
        PaytmPGService Service = PaytmPGService.getProductionService();
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("CALLBACK_URL",CALLBACK_URL + orderId);
        paramMap.put("CHANNEL_ID", CHANNEL_ID);
        paramMap.put("CHECKSUMHASH", checkSumHash);
        paramMap.put("CUST_ID", customerId);
        paramMap.put("INDUSTRY_TYPE_ID", INDUSTRY_TYPE_ID);
        paramMap.put("MID" ,MID);
        paramMap.put("ORDER_ID", orderId);
        paramMap.put("TXN_AMOUNT", Integer.toString(txnAmount));
        paramMap.put("WEBSITE", WEBSITE);
//        paramMap.put( "EMAIL" , "eriyazonline@gmail.com");
//        paramMap.put( "MOBILE_NO" , "9999999999");

        PaytmOrder Order = new PaytmOrder(paramMap);

        Service.initialize(Order, null);

        Service.startPaymentTransaction(context, true, true,
                new PaytmPaymentTransactionCallback() {
                    @Override
                    public void someUIErrorOccurred(String inErrorMessage) {
                        LogUtil.logInfo(TAG, "someUIErrorOccurred: " + inErrorMessage);
                        onTaskCompleteListener.onTaskComplete(PaymentStatus.FAILED);
                        Toast.makeText(context, inErrorMessage, Toast.LENGTH_LONG).show();
                        // Some UI Error Occurred in Payment Gateway Activity.
                        // // This may be due to initialization of views in
                        // Payment Gateway Activity or may be due to //
                        // initialization of webview. // Error Message details
                        // the error occurred.
                    }

                    @Override
                    public void onTransactionResponse(Bundle inResponse) {
                        LogUtil.logInfo(TAG, "successful response" + inResponse.toString());
                        verifyPayment(onTaskCompleteListener);
                    }

                    @Override
                    public void networkNotAvailable() { // If network is not
                        // available, then this
                        // method gets called.
                        onTaskCompleteListener.onTaskComplete(PaymentStatus.FAILED);
                        Toast.makeText(context, "network not available", Toast.LENGTH_LONG).show();
                        Log.e(TAG, "network not available");
                    }

                    @Override
                    public void clientAuthenticationFailed(String inErrorMessage) {
                        // This method gets called if client authentication
                        // failed. // Failure may be due to following reasons //
                        // 1. Server error or downtime. // 2. Server unable to
                        // generate checksum or checksum response is not in
                        // proper format. // 3. Server failed to authenticate
                        // that client. That is value of payt_STATUS is 2. //
                        // Error Message describes the reason for failure.
                        onTaskCompleteListener.onTaskComplete(PaymentStatus.FAILED);
                        Toast.makeText(context, "PayTM: clientAuthenticationFailed", Toast.LENGTH_LONG).show();
                        Log.e(TAG, "clientAuthenticationFailed " + inErrorMessage);
                    }

                    @Override
                    public void onErrorLoadingWebPage(int iniErrorCode,
                                                      String inErrorMessage, String inFailingUrl) {
                        onTaskCompleteListener.onTaskComplete(PaymentStatus.FAILED);
                        Toast.makeText(context, "PayTM: onErrorLoadingWebPage", Toast.LENGTH_LONG).show();
                        Log.e(TAG, "onErrorLoadingWebPage " + inErrorMessage);

                    }

                    // had to be added: NOTE
                    @Override
                    public void onBackPressedCancelTransaction() {
                        onTaskCompleteListener.onTaskComplete(PaymentStatus.FAILED);
                        Toast.makeText(context,"Back pressed. Transaction cancelled",Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onTransactionCancel(String inErrorMessage, Bundle inResponse) {
                        onTaskCompleteListener.onTaskComplete(PaymentStatus.FAILED);
                        Log.e(TAG, "onTranscationCancel " + inErrorMessage);
                        Toast.makeText(context, "Failed: "+inErrorMessage, Toast.LENGTH_LONG).show();
                    }

                });
    }
}
