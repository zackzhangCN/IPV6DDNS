package cn.zack.schedule;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.alidns.model.v20150109.DescribeDomainRecordsRequest;
import com.aliyuncs.alidns.model.v20150109.DescribeDomainRecordsResponse;
import com.aliyuncs.alidns.model.v20150109.UpdateDomainRecordRequest;
import com.aliyuncs.alidns.model.v20150109.UpdateDomainRecordResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

@Service
public class IPv6DDNSScheduled {

    @Value("${aliyun.ddns.regionId}")
    private String regionId;

    // 要解析的主域名
    @Value("${aliyun.ddns.domainName}")
    private String domainName;

    // 记录值
    @Value("${aliyun.ddns.keyWord}")
    private String keyWord;

    // 记录类型
    @Value("${aliyun.ddns.type}")
    private String type;

    // accesskeyId
    @Value("${aliyun.ddns.accessKeyId}")
    private String accessKeyId;

    // secret
    @Value("${aliyun.ddns.secret}")
    private String secret;

    /**
     * 定时执行ipv6DNS, 每10分钟一次
     *
     * @throws SocketException
     */
    @Scheduled(fixedRate = 6000 * 10 * 10)
    public void autoChangeIPv6DNS() throws SocketException {
        System.out.println(new Date() + "开始更新IPV6DNS信息====================");
        // 设置主机地址以及AccessKey
        DefaultProfile profile = DefaultProfile.getProfile(regionId, accessKeyId, secret);
        IAcsClient client = new DefaultAcsClient(profile);
        IPv6DDNSScheduled ddnsTestApplication = new IPv6DDNSScheduled();

        // 查询二级域名的最新解析记录
        DescribeDomainRecordsRequest describeDomainRecordsRequest = new DescribeDomainRecordsRequest();
        // 主域名
        describeDomainRecordsRequest.setDomainName(domainName);
        // 主机记录
        describeDomainRecordsRequest.setRRKeyWord(keyWord);
        // 解析记录类型
        describeDomainRecordsRequest.setType(type);
        // 获取主域名所有符合条件的解析记录
        DescribeDomainRecordsResponse describeDomainRecordsResponse = ddnsTestApplication.describeDomainRecords(describeDomainRecordsRequest, client);
        System.out.println("当前主域名的解析记录: \n" + new Gson().toJson(describeDomainRecordsResponse));
        List<DescribeDomainRecordsResponse.Record> domainRecords = describeDomainRecordsResponse.getDomainRecords();
        // 取最新的一条解析记录
        if (domainRecords.size() != 0) {
            DescribeDomainRecordsResponse.Record record = domainRecords.get(0);
            // 取记录Id
            String recordId = record.getRecordId();
            // 取记录值
            String recordValue = record.getValue();
            // 取当前主机的公网ipv6地址
            String iPv6Address = ddnsTestApplication.getIPv6Address();
            System.out.println("当前主机的ipv6地址:" + iPv6Address);
            // 判断是否取到ipv6地址, 取不到就改为和记录值相同, 不会修改解析记录
            if (iPv6Address == "") {
                System.out.println("获取当前主机的ipv6地址失败====================");
                iPv6Address = recordValue;
            }
            // 当前记录值与当前ipv6地址值不一致
            if (!iPv6Address.equals(recordValue)) {
                // 修改解析记录
                UpdateDomainRecordRequest updateDomainRecordRequest = new UpdateDomainRecordRequest();
                // 主机记录
                updateDomainRecordRequest.setRR(keyWord);
                // 记录ip
                updateDomainRecordRequest.setRecordId(recordId);
                // 记录值
                updateDomainRecordRequest.setValue(iPv6Address);
                // 解析记录类型
                updateDomainRecordRequest.setType(type);
                // 进行修改
                UpdateDomainRecordResponse updateDomainRecordResponse = ddnsTestApplication.updateDomainRecord(updateDomainRecordRequest, client);
                System.out.println("解析记录修改结果: \n" + new Gson().toJson(updateDomainRecordResponse));
                System.out.println(new Date() + "IPV6DNS信息更新完成====================");
            } else {
                System.out.println(new Date() + "无需更新IPV6DNS解析记录====================");
            }
        }
    }

    /**
     * 获取主域名的所有解析记录
     *
     * @param request
     * @param client
     * @return
     */
    private DescribeDomainRecordsResponse describeDomainRecords(DescribeDomainRecordsRequest request, IAcsClient client) {
        try {
            // 调用SDK发送请求
            return client.getAcsResponse(request);
        } catch (ClientException e) {
            e.printStackTrace();
            // 调用发生错误
            throw new RuntimeException();
        }
    }

    /**
     * 修改解析记录
     *
     * @param request
     * @param client
     * @return
     */
    private UpdateDomainRecordResponse updateDomainRecord(UpdateDomainRecordRequest request, IAcsClient client) {
        try {
            // 调用SDK发送请求
            return client.getAcsResponse(request);
        } catch (ClientException e) {
            e.printStackTrace();
            // 调用发生错误
            throw new RuntimeException();
        }
    }

    /**
     * 获取本机公网ipv6地址
     *
     * @return
     * @throws SocketException
     */
    private static String getIPv6Address() throws SocketException {
        String ipv6Address = "";
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();
            Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
            // 取所有网卡信息进行遍历
            while (addresses.hasMoreElements()) {
                InetAddress in = addresses.nextElement();
                // 取每个网卡的ipv6信息
                if (in instanceof Inet6Address) {
                    // 取ipv6地址
                    String thisIPv6Address = in.getHostAddress();
                    System.out.println(thisIPv6Address);
                    // 有线网卡分配的公网ipv6地址格式为2408:8220:9514:5870:3288:5a2e:6c5b:4d53%eth0
                    // 无线网卡分配的公网ipv6地址格式为2408:8220:9514:5870:d362:977f:a982:5c16%wlan0
                    // 此处只取有线网卡
                    if (thisIPv6Address.startsWith("2") && thisIPv6Address.endsWith("%eth0")) {
                        // 去除ipv6地址附带的网卡信息
                        ipv6Address = thisIPv6Address.split("%")[0];
                        break;
                    }
                }
            }
        }
        return ipv6Address;
    }
}
