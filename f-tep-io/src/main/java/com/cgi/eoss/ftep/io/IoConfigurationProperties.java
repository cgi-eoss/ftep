package com.cgi.eoss.ftep.io;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("ftep.io")
@Data
public class IoConfigurationProperties {

    private Server server = new Server();
    private Cache cache = new Cache();
    private Downloader downloader = new Downloader();

    @Data
    static class Server {
        private String eurekaServiceId = "f-tep server";
    }

    @Data
    static class Cache {
        private String baseDir = "/data/cache/dl";
        private int concurrency = 4;
        private int maxWeight = 1024;
    }

    @Data
    static class Downloader {
        private boolean unzipAllDownloads = true;
        private int retryLimit = 5;

        private Ceda ceda = new Ceda();
        private IptHttp iptHttp = new IptHttp();

        @Data
        static class Ceda {
            private String cedaSearchUrl = "http://opensearch.ceda.ac.uk/opensearch/json";
            private String ftpUrlBase = "ftp://ftp.ceda.ac.uk";
            private int overallPriority = 10;
        }

        @Data
        static class IptHttp {
            private String iptSearchUrl = "https://finder.eocloud.eu/resto/";
            private String downloadUrlBase = "http://185.48.233.249";
            // TODO Add these to the credentials/datasource object?
            private String authEndpoint = "https://finder.eocloud.eu/resto/api/authidentity";
            private String authDomain = "__secret__";
            private int overallPriority = 0;
            private int downloadTimeout = 120;
            private int searchTimeout = 120;
        }
    }

}
