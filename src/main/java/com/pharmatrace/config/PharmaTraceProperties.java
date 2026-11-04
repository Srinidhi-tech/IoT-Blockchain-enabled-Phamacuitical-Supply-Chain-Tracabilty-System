package com.pharmatrace.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "pharmatrace")
public class PharmaTraceProperties {

    private Rfid rfid = new Rfid();
    private String defaultBatchId = "VAX-B4920-COVID19";
    private String defaultProductName = "mRNA-1273 Specialized Vaccine (Cold-Chain Grade)";
    private Temperature temperature = new Temperature();
    private Humidity humidity = new Humidity();
    private Blockchain blockchain = new Blockchain();

    public static class Rfid {
        private List<String> authorizedUids = new ArrayList<>(Arrays.asList(
                "2AF52B07", "659A1707"
        ));

        public List<String> getAuthorizedUids() {
            return authorizedUids;
        }

        public void setAuthorizedUids(List<String> authorizedUids) {
            this.authorizedUids = authorizedUids;
        }
    }

    public static class Temperature {
        private Double min = 2.0;
        private Double max = 8.0;

        public Double getMin() {
            return min;
        }

        public void setMin(Double min) {
            this.min = min;
        }

        public Double getMax() {
            return max;
        }

        public void setMax(Double max) {
            this.max = max;
        }
    }

    public static class Humidity {
        private Double min = 30.0;
        private Double max = 65.0;

        public Double getMin() {
            return min;
        }

        public void setMin(Double min) {
            this.min = min;
        }

        public Double getMax() {
            return max;
        }

        public void setMax(Double max) {
            this.max = max;
        }
    }

    public static class Blockchain {
        private String rpcUrl = "http://127.0.0.1:8545";
        private Long chainId = 1337L;
        private Integer connectTimeoutMs = 800;

        public String getRpcUrl() {
            return rpcUrl;
        }

        public void setRpcUrl(String rpcUrl) {
            this.rpcUrl = rpcUrl;
        }

        public Long getChainId() {
            return chainId;
        }

        public void setChainId(Long chainId) {
            this.chainId = chainId;
        }

        public Integer getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(Integer connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }
    }

    public Rfid getRfid() {
        return rfid;
    }

    public void setRfid(Rfid rfid) {
        this.rfid = rfid;
    }

    public String getDefaultBatchId() {
        return defaultBatchId;
    }

    public void setDefaultBatchId(String defaultBatchId) {
        this.defaultBatchId = defaultBatchId;
    }

    public String getDefaultProductName() {
        return defaultProductName;
    }

    public void setDefaultProductName(String defaultProductName) {
        this.defaultProductName = defaultProductName;
    }

    public Temperature getTemperature() {
        return temperature;
    }

    public void setTemperature(Temperature temperature) {
        this.temperature = temperature;
    }

    public Humidity getHumidity() {
        return humidity;
    }

    public void setHumidity(Humidity humidity) {
        this.humidity = humidity;
    }

    public Blockchain getBlockchain() {
        return blockchain;
    }

    public void setBlockchain(Blockchain blockchain) {
        this.blockchain = blockchain;
    }
}
