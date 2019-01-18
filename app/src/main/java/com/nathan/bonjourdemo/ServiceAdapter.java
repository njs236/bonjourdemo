package com.nathan.bonjourdemo;

import android.app.DownloadManager;
import android.content.Context;
import android.util.Log;
import android.widget.LinearLayout;

import com.github.druk.dnssd.BrowseListener;
import com.github.druk.dnssd.DNSSD;
import com.github.druk.dnssd.DNSSDException;
import com.github.druk.dnssd.DNSSDRegistration;
import com.github.druk.dnssd.DNSSDService;
import com.github.druk.dnssd.DomainListener;
import com.github.druk.dnssd.QueryListener;
import com.github.druk.dnssd.RegisterListener;
import com.github.druk.dnssd.ResolveListener;
import com.github.druk.rx2dnssd.BonjourService;
import com.github.druk.rx2dnssd.Rx2Dnssd;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.Flowable;

public class ServiceAdapter {

    DNSSDService registerService;
    DNSSDService domainService;
    HashMap<String, BonjourService> browserServices = new HashMap<>();
    HashMap<String, DNSSDService> dnsServices = new HashMap<>();
    private DNSSD dnssd;
    HashMap<String, Object> selectedService = new HashMap<>();
    private String TAG = this.getClass().getSimpleName();
    public OnBrowseListener listener;

    public interface OnBrowseListener {
        void browseListener(String name);
        void removeListener(String name);
        void moveToTray(String name);
        void createInMainWindow(String name);
        void refreshDisplayedServices();
        void displayServiceInformation(HashMap<String, String> values);
    }

    ServiceAdapter(DNSSD newDnssd, Context context) {
        dnssd = newDnssd;
        try {
            if (context instanceof OnBrowseListener) {
                listener = (OnBrowseListener) context;
            } else {
                throw new Exception(context.getClass().getSimpleName() + " has not implemented OnBrowseListener");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        registerService();
        browseServices();



    }

    public void registerService() {
        try {
            registerService = dnssd.register("Bonjour Android Service", "_rxdnssd._tcp", 123,
                    new RegisterListener() {

                        @Override
                        public void serviceRegistered(DNSSDRegistration registration, int flags,
                                                      String serviceName, String regType, String domain) {
                            Log.i(TAG, "Register successfully ");
                        }

                        @Override
                        public void operationFailed(DNSSDService service, int errorCode) {
                            Log.e(TAG, "error " + errorCode);
                        }
                    });
        } catch (DNSSDException e) {
            Log.e(TAG, "error", e);
        }
    }

    public void browseServices() {
        BrowseListener bl = new BrowseListener() {

            @Override
            public void serviceFound(DNSSDService browser, int flags, int ifIndex,
                                     final String serviceName, String regType, String domain) {
                Log.i(TAG, "Found " + serviceName);
                // if service found, then display an icon in the activity which will get the service information
                // including the service object.
                if (!browserServices.containsKey(serviceName)) {
                    BonjourService.Builder builder = new BonjourService.Builder(flags, ifIndex, serviceName, regType, domain);
                    BonjourService service = builder.build();
                    add(serviceName, service, browser);
                    listener.browseListener(serviceName);
                }
                enumerateDomains();
            }

            @Override
            public void serviceLost(DNSSDService browser, int flags, int ifIndex,
                                    String serviceName, String regType, String domain) {
                Log.i(TAG, "Lost " + serviceName);
                // if service is in browsing services remove from services.
                if (browserServices.containsKey(serviceName)) {
                    remove(serviceName);
                    listener.removeListener(serviceName);
                    Log.i(TAG, "removed " + serviceName + "from register");
                    listener.refreshDisplayedServices();

                }
                // remove image from displaying Services.

            }

            @Override
            public void operationFailed(DNSSDService service, int errorCode) {
                Log.e(TAG, "error: " + errorCode);
            }


        };
        try {
            dnssd.browse("hidden", bl);

            dnssd.browse("hidden",bl );
        } catch (DNSSDException e) {
            Log.e(TAG, "error", e);
        }
    }

    public void enumerateDomains() {
        try {
            dnssd.enumerateDomains(DNSSD.BROWSE_DOMAINS, 0, new DomainListener() {
                @Override
                public void operationFailed(DNSSDService service, int errorCode) {
                    Log.e(TAG, "error: " + errorCode);
                    domainService.stop();
                }

                @Override
                public void domainFound(DNSSDService domainEnum, int flags, int ifIndex, String domain) {
                    Log.d(TAG, "returned Domain: " + domain);
                    domainService = domainEnum;
                }

                @Override
                public void domainLost(DNSSDService domainEnum, int flags, int ifIndex, String domain) {

                }
            });
        } catch (DNSSDException ex) {
            Log.e(TAG, "error", ex);
        }
    }

    public void add(String name, BonjourService service, DNSSDService browser) {
        Log.d(TAG, "service: " + name);
        browserServices.put(name,service);
        dnsServices.put(name, browser);
    }

    public void remove(String name) {
        Log.d(TAG, "removing service" + name);
        browserServices.remove(name);
        dnsServices.remove(name);
    }

    public void retrieveServiceInformation( String name) {
        try {
            // the hardcoded value is any services that subscribe to this particular variation of bonjour.
            // if you want to get others, will ahve to look in the discovery program on mac mini.
            // require type:
            BonjourService bs = browserServices.get(name);
            dnssd.resolve(bs.getFlags(), bs.getIfIndex(), name, bs.getRegType(), bs.getDomain(), new ResolveListener() {
                @Override
                public void operationFailed(DNSSDService service, int errorCode) {
                    Log.e(TAG, "error: " + errorCode);
                }

                @Override
                public void serviceResolved(DNSSDService resolver, int flags, int ifIndex, String fullName, String hostName, int port, Map<String, String> txtRecord) {
                    Log.d(TAG, "flags: " + flags);
                    Log.d(TAG, "ifIndex: " + ifIndex);
                    Log.d(TAG, "fullName: " + fullName);
                    Log.d(TAG, "hostName: " + hostName);
                    Log.d(TAG, "port:"  + port);
                    for (String key: txtRecord.keySet()) {
                        Log.d("TAG", "a record: " + txtRecord.get(key));
                    }

                    selectedService.put("flags", flags);
                    selectedService.put("ifIndex", ifIndex);
                    selectedService.put("fullname", fullName);
                    //selectedService.put("hostname", hostName);
                    selectedService.put("name", name);
                    selectedService.put("regType","_rxdnssd._tcp" );
                    selectedService.put("domain", "local.");
                    domainService.stop();
                    query(txtRecord, port, hostName);


                }
            });
        } catch (DNSSDException ex) {
            Log.e(TAG, "error", ex);
        }

    }
    private void query(Map<String, String> txtRecord, Integer port, String hostName){
        try {
            dnssd.queryRecord(DNSSD.MORE_COMING, 0, hostName, 1, 1, new QueryListener() {
                @Override
                public void operationFailed(DNSSDService service, int errorCode) {
                    Log.e(TAG, "error: " + errorCode);
                }

                @Override
                public void queryAnswered(DNSSDService query, int flags, int ifIndex, String fullName, int rrtype, int rrclass, byte[] rdata, int ttl) {
                    Log.d(TAG, "flags: " + flags);
                    Log.d(TAG, "ifIndex: " + ifIndex);
                    Log.d(TAG, "fullName: " + fullName);
                    Log.d(TAG, "type: " + rrtype);
                    Log.d(TAG, "class:"  + rrclass);
                    BonjourService.Builder builder = new BonjourService.Builder(flags,
                            ifIndex,
                            selectedService.get("name").toString(),
                            selectedService.get("regType").toString(),
                            selectedService.get("domain").toString())
                            .dnsRecords(txtRecord)
                            .port(port)
                            .hostname(hostName);

                    try {
                        InetAddress address = InetAddress.getByAddress(rdata);
                        if (address instanceof Inet4Address) {
                            builder.inet4Address((Inet4Address)address);
                        } else if (address instanceof Inet6Address) {
                            builder.inet6Address((Inet6Address) address);
                        }
                        BonjourService bs = builder.build();
                        Log.d(TAG, "ip: " + bs.getInet4Address().toString());


                        HashMap<String, String> values = new HashMap();
                        values.put("flags", String.valueOf(flags));
                        values.put("ifIndex", String.valueOf(ifIndex));
                        values.put("name", selectedService.get("name").toString());
                        values.put("regType", selectedService.get("regType").toString());
                        values.put("domain", selectedService.get("domain").toString());
                        values.put("hostname", hostName);
                        values.put("ip", bs.getInet4Address().toString());
                        listener.displayServiceInformation(values);


                    } catch (UnknownHostException ex) {
                        Log.e(TAG, "error", ex);
                    }

                }
            });
        } catch (DNSSDException ex) {
            Log.e(TAG, "error", ex);
        }
    }

    public void refreshDisplayedServices() {
        for (String key: browserServices.keySet()) {
            listener.createInMainWindow(key);
        }
    }

    public void reassignServicesBasedOnPreferredSelection(String name) {
        for (String key : browserServices.keySet()
        ) {
            if (key.equals(name)) {
                listener.createInMainWindow(name);
            } else {
                // child of layout moves to "otherServices"
                listener.moveToTray(key);
            }
        }
    }

}
