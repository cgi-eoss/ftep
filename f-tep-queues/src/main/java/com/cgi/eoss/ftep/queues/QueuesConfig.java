package com.cgi.eoss.ftep.queues;

import com.cgi.eoss.ftep.queues.service.FtepJMSQueueService;
import com.cgi.eoss.ftep.queues.service.FtepQueueService;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.BrokerRegistry;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.TransportConnector;
import org.apache.activemq.broker.region.policy.PolicyEntry;
import org.apache.activemq.broker.region.policy.PolicyMap;
import org.apache.activemq.plugin.StatisticsBrokerPlugin;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import java.net.URI;
import java.util.Arrays;

/**
 * <p>Spring configuration for the F-TEP Queues component.</p>
 * <p>Manages job and job updates queues.</p>
 */
@Configuration
@EnableJms
@ComponentScan(basePackageClasses = QueuesConfig.class)
public class QueuesConfig {

    @Bean
    public JmsTemplate jmsTemplate() {
        return new JmsTemplate(new PooledConnectionFactory(activeMQConnectionFactory())) {
            @Override
            protected void doSend(MessageProducer producer, Message message) throws JMSException {
                producer.send(message, getDeliveryMode(), message.getJMSPriority(), getTimeToLive());
            }
        };
    }

    @Bean
    public FtepQueueService ftepQueueService(JmsTemplate jmsTemplate) {
        return new FtepJMSQueueService(jmsTemplate);
    }

    public static final String Q_ftepWorker_prepareEnvironment = "Q_ftepWorker_prepareEnvironment";

    @Value("${spring.activemq.broker-url:vm://embeddedBroker}")
    private String brokerUrl;

    @Value("${spring.activemq.user:admin}")
    private String brokerUserName;

    @Value("${spring.activemq.password:admin}")
    private String brokerPassword;

    @Bean(name = "activeMQConnectionFactory")
    @DependsOn("brokerService")
    public ActiveMQConnectionFactory activeMQConnectionFactory() {
        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory();
        activeMQConnectionFactory.setUserName(brokerUserName);
        activeMQConnectionFactory.setPassword(brokerPassword);
        activeMQConnectionFactory.setTrustedPackages(Arrays.asList("com.google.protobuf", "com.cgi.eoss.ftep"));
        activeMQConnectionFactory.setBrokerURL(brokerUrl);
        return activeMQConnectionFactory;
    }

    @Bean(name = "brokerService", initMethod = "start", destroyMethod = "stop")
    public BrokerService brokerService() throws Exception {
        if ("vm://embeddedBroker".equals(brokerUrl) && BrokerRegistry.getInstance().lookup("embeddedBroker") == null) {
            BrokerService broker = new BrokerService();
            broker.setBrokerName("embeddedBroker");
            broker.setPlugins(new BrokerPlugin[]{new StatisticsBrokerPlugin()});
            broker.setPersistent(false);
            broker.setUseJmx(true);
            PolicyMap pm = new PolicyMap();
            PolicyEntry pe = new PolicyEntry();
            pe.setPrioritizedMessages(true);
            pm.setDefaultEntry(pe);
            broker.setDestinationPolicy(pm);
            TransportConnector connector = new TransportConnector();
            connector.setUri(new URI(brokerUrl));
            broker.addConnector(connector);
            broker.start();
            return broker;
        } else {
            // The broker will be autocreated by Spring
            return null;
        }
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory() {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(activeMQConnectionFactory());
        factory.setConcurrency("1-1");
        return factory;
    }
}
