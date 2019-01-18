package com.nathan.bonjourdemo;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.druk.dnssd.BrowseListener;
import com.github.druk.dnssd.DNSSD;
import com.github.druk.dnssd.DNSSDBindable;
import com.github.druk.dnssd.DNSSDEmbedded;
import com.github.druk.dnssd.DNSSDException;
import com.github.druk.dnssd.DNSSDRegistration;
import com.github.druk.dnssd.DNSSDService;
import com.github.druk.dnssd.QueryListener;
import com.github.druk.dnssd.RegisterListener;
import com.github.druk.rx2dnssd.BonjourService;
import com.github.druk.rx2dnssd.Rx2Dnssd;
import com.github.druk.rx2dnssd.Rx2DnssdBindable;
import com.github.druk.rx2dnssd.Rx2DnssdEmbedded;
import com.github.druk.rxdnssd.RxDnssd;
import com.github.druk.rxdnssd.RxDnssdBindable;
import com.github.druk.rxdnssd.RxDnssdEmbedded;

import org.w3c.dom.Text;

import java.lang.reflect.Array;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements  ServiceAdapter.OnBrowseListener {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    Context context = this;
    DNSSD dnssd;
    private DNSSDService registerService;
    private LinearLayout displayServices;
    Rx2Dnssd rx2dnssd;
    private HashMap<String, Integer> serviceImageIDs = new HashMap<>();
    private String TAG = this.getClass().getSimpleName();
    private LinearLayout otherServices;
    private Disposable registerDisposable;
    private ServiceAdapter mServiceAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context.getSystemService(Context.NSD_SERVICE);

        // Example of a call to a native method
        TextView tv = findViewById(R.id.sample_text);
        tv.setText(stringFromJNI());
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            dnssd = new DNSSDBindable(context);
            Log.i(TAG, "os VERSION: " + Build.VERSION.SDK_INT);
        } else {
            dnssd = new DNSSDEmbedded(context);
        }

        displayServices = findViewById(R.id.ll_displayServices);
        otherServices = findViewById(R.id.ll_otherServices);

        mServiceAdapter = new ServiceAdapter(dnssd, context);


    }

    public void displayServiceWithIcon(String serviceName, LinearLayout parentLayout) {
        Log.i(TAG, "displaying Service with Icon:" + serviceName);
        // LayoutParams, make ll fit the contents it has
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ImageView iv = new ImageView(context);

        //create a unique id and store it for later access. ie, findViewById
        iv.setId(ViewCompat.generateViewId());
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(lp);
        ip.height= 72;
        ip.width = 72;
        ip.gravity = Gravity.CENTER_HORIZONTAL;
        iv.setLayoutParams(ip);

        serviceImageIDs.put(serviceName, iv.getId());
        //set Image to Person icon
        iv.setImageResource(R.drawable.ic_person_black_24dp);


        // create a listener for displaying service information.
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // move all other services to bottom
                clearServiceDisplayAndFillWithServiceInformation(serviceName);
            }
        });
        TextView tv = new TextView(context);
        tv.setText(serviceName);
        //create view under displayServices linear Layout.
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setId(ViewCompat.generateViewId());
        ll.setLayoutParams(lp);
        ll.addView(iv, ip);
        ll.addView(tv, lp);
        parentLayout.addView(ll, lp);



    }

    public void clearServiceDisplayAndFillWithServiceInformation(String name) {
        displayServices.removeAllViews();
        otherServices.removeAllViews();
        mServiceAdapter.reassignServicesBasedOnPreferredSelection(name);

        displayServiceInformationToActivity(name);


    }

    public void refreshDisplayedServices() {
        displayServices.removeAllViews();
        otherServices.removeAllViews();
        mServiceAdapter.refreshDisplayedServices();
    }




    public void displayServiceInformationToActivity(String name) {
        mServiceAdapter.retrieveServiceInformation(name);



    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    @Override
    public void browseListener(String name) {
        displayServiceWithIcon(name, displayServices);
    }

    @Override
    public void removeListener(String name) {
       serviceImageIDs.remove(name);
    }

    @Override
    public void moveToTray(String name) {
        displayServiceWithIcon(name, otherServices);
    }

    @Override
    public void createInMainWindow(String name) {
        displayServiceWithIcon(name, displayServices);
    }

    public void displayServiceInformation(HashMap<String, String> data) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setId(ViewCompat.generateViewId());
        ll.setLayoutParams(lp);
        for (String key : data.keySet()) {
            TextView tv = new TextView(context);
            String string = key +": "+ data.get(key);
            tv.setText(string);
            ll.addView(tv);
        }
        displayServices.addView(ll);
    }
}
