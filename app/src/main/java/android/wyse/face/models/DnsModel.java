package android.wyse.face.models;

public class DnsModel {

    String dns;
    String dnstype;
    String urlType;

    public DnsModel(String dns,String dnstype){
        this.dns=dns;
        this.dnstype=dnstype;
    }

    public String getDns() {
        return dns;
    }

    public String getUrlType() {
        return urlType;
    }

    public String getDnstype() {
        return dnstype;
    }
}
