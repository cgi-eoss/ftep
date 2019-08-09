package com.cgi.eoss.ftep.io;

import com.cgi.eoss.ftep.io.download.CEDADownloader;
import com.cgi.eoss.ftep.io.download.CachingSymlinkDownloaderFacade;
import com.cgi.eoss.ftep.io.download.CreodiasHttpAuthenticator;
import com.cgi.eoss.ftep.io.download.CreodiasHttpDownloader;
import com.cgi.eoss.ftep.io.download.DownloaderFacade;
import com.cgi.eoss.ftep.io.download.FtepDownloader;
import com.cgi.eoss.ftep.io.download.FtpDownloader;
import com.cgi.eoss.ftep.io.download.HttpDownloader;
import com.cgi.eoss.ftep.io.download.IptEodataServerAuthenticator;
import com.cgi.eoss.ftep.io.download.IptEodataServerDownloader;
import com.cgi.eoss.ftep.io.download.IptHttpDownloader;
import com.cgi.eoss.ftep.io.download.KeyCloakTokenGenerator;
import com.cgi.eoss.ftep.io.download.ProtocolPriority;
import com.cgi.eoss.ftep.rpc.DiscoveryClientResolverFactory;
import com.cgi.eoss.ftep.rpc.FtepServerClient;
import com.cgi.eoss.ftep.rpc.RpcClientConfig;
import com.google.common.base.Strings;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.nio.file.Paths;

@Configuration
@EnableConfigurationProperties(IoConfigurationProperties.class)
@EnableEurekaClient
@Import({RpcClientConfig.class})
public class IoConfig {

    @Bean
    public OkHttpClient okHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        // If an http_proxy is set in the current environment, add it to the client
        String httpProxy = System.getenv("http_proxy");
        if (!Strings.isNullOrEmpty(httpProxy)) {
            URI proxyUri = URI.create(httpProxy);
            builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyUri.getHost(), proxyUri.getPort())));
        }

        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean(FtepServerClient.class)
    public FtepServerClient ftepServerClient(DiscoveryClientResolverFactory discoveryClientResolverFactory,
                                             IoConfigurationProperties properties) {
        return new FtepServerClient(discoveryClientResolverFactory, properties.getServer().getEurekaServiceId());
    }

    @Bean
    public DownloaderFacade downloaderFacade(IoConfigurationProperties properties) {
        return new CachingSymlinkDownloaderFacade(
                Paths.get(properties.getCache().getBaseDir()),
                properties.getCache().getConcurrency(),
                properties.getCache().getMaxWeight(),
                properties.getDownloader().isUnzipAllDownloads(),
                properties.getDownloader().getRetryLimit()
        );
    }

    @Bean
    public ServiceInputOutputManager serviceInputOutputManager(FtepServerClient ftepServerClient, DownloaderFacade downloaderFacade) {
        return new ServiceInputOutputManagerImpl(ftepServerClient, downloaderFacade);
    }

    @Bean
    public CEDADownloader cedaDownloader(DownloaderFacade downloaderFacade, OkHttpClient okHttpClient, IoConfigurationProperties properties) {
        IoConfigurationProperties.Downloader.Ceda cedaProperties = properties.getDownloader().getCeda();
        return new CEDADownloader(
                downloaderFacade,
                okHttpClient,
                HttpUrl.parse(cedaProperties.getCedaSearchUrl()),
                cedaProperties.getFtpUrlBase(),
                ProtocolPriority.builder().overallPriority(cedaProperties.getOverallPriority()).build());
    }

    @Bean
    public FtepDownloader ftepDownloader(DownloaderFacade downloaderFacade, FtepServerClient ftepServerClient) {
        return new FtepDownloader(downloaderFacade, ftepServerClient);
    }

    @Bean
    public FtpDownloader ftpDownloader(DownloaderFacade downloaderFacade, FtepServerClient ftepServerClient) {
        return new FtpDownloader(downloaderFacade, ftepServerClient);
    }

    @Bean
    public HttpDownloader httpDownloader(DownloaderFacade downloaderFacade, FtepServerClient ftepServerClient, OkHttpClient okHttpClient) {
        return new HttpDownloader(downloaderFacade, ftepServerClient, okHttpClient);
    }

    @Bean
    public IptHttpDownloader iptHttpDownloader(DownloaderFacade downloaderFacade, FtepServerClient ftepServerClient, OkHttpClient okHttpClient, IoConfigurationProperties properties) {
        IoConfigurationProperties.Downloader.IptHttp iptHttpProperties = properties.getDownloader().getIptHttp();
        return new IptHttpDownloader(
                okHttpClient,
                iptHttpProperties.getDownloadTimeout(),
                iptHttpProperties.getSearchTimeout(),
                ftepServerClient,
                downloaderFacade,
                IptHttpDownloader.Properties.builder()
                        .iptSearchUrl(iptHttpProperties.getIptSearchUrl())
                        .iptDownloadUrl(iptHttpProperties.getDownloadUrlBase())
                        .authEndpoint(iptHttpProperties.getAuthEndpoint())
                        .authDomain(iptHttpProperties.getAuthDomain())
                        .build(),
                ProtocolPriority.builder().overallPriority(iptHttpProperties.getOverallPriority()).build());
    }

    @Bean
    public IptEodataServerAuthenticator iptEodataServerAuthenticator(FtepServerClient ftepServerClient) {
        return new IptEodataServerAuthenticator(ftepServerClient);
    }

    @Bean
    public IptEodataServerDownloader iptEodataServerDownloader(DownloaderFacade downloaderFacade, OkHttpClient okHttpClient, IptEodataServerAuthenticator iptEodataServerAuthenticator, IoConfigurationProperties properties, KeyCloakTokenGenerator keyCloakTokenGenerator) {
        IoConfigurationProperties.Downloader.IptEodataServer iptEodataServerProperties = properties.getDownloader().getIptEodataServer();
        return new IptEodataServerDownloader(
                okHttpClient,
                iptEodataServerProperties.getDownloadTimeout(),
                iptEodataServerProperties.getSearchTimeout(),
                iptEodataServerAuthenticator,
                downloaderFacade,
                IptEodataServerDownloader.Properties.builder()
                        .iptSearchUrl(iptEodataServerProperties.getIptSearchUrl())
                        .iptDownloadUrl(iptEodataServerProperties.getDownloadUrlBase())
                        .iptOrderUrl(iptEodataServerProperties.getOrderUrl())
                        .build(),
                ProtocolPriority.builder().overallPriority(iptEodataServerProperties.getOverallPriority()).build(),
                keyCloakTokenGenerator
                );
    }

    @Bean
    public CreodiasHttpAuthenticator creodiasHttpAuthenticator(KeyCloakTokenGenerator keyCloakTokenGenerator) {
        return new CreodiasHttpAuthenticator(keyCloakTokenGenerator);
    }

    @Bean
    public KeyCloakTokenGenerator keyCloakTokenGenerator(OkHttpClient okHttpClient, FtepServerClient ftepServerClient, IoConfigurationProperties properties) {
        return new KeyCloakTokenGenerator(
                okHttpClient,
                ftepServerClient,
                properties.getDownloader().getCreodiasHttp().getAuthEndpoint(),
                properties.getDownloader().getCreodiasHttp().getAuthClientId());
    }

    @Bean
    public CreodiasHttpDownloader creodiasHttpDownloader(DownloaderFacade downloaderFacade, OkHttpClient okHttpClient, CreodiasHttpAuthenticator creodiasHttpAuthenticator, KeyCloakTokenGenerator keyCloakTokenGenerator, IoConfigurationProperties properties) {
        return new CreodiasHttpDownloader(
                okHttpClient,
                properties.getDownloader().getCreodiasHttp().getDownloadTimeout(),
                properties.getDownloader().getCreodiasHttp().getSearchTimeout(),
                creodiasHttpAuthenticator,
                keyCloakTokenGenerator,
                downloaderFacade,
                CreodiasHttpDownloader.Properties.builder()
                        .creodiasDownloadUrl(properties.getDownloader().getCreodiasHttp().getDownloadUrlBase())
                        .creodiasSearchUrl(properties.getDownloader().getCreodiasHttp().getSearchUrl())
                        .creodiasOrderUrl(properties.getDownloader().getCreodiasHttp().getOrderUrl())
                        .build(),
                ProtocolPriority.builder().overallPriority(properties.getDownloader().getCreodiasHttp().getOverallPriority()).build());
    }

}
