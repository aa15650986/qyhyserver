
package com.zhuoan.webapp.config;

import javax.annotation.Resource;
import javax.jms.ConnectionFactory;
import javax.jms.MessageListener;
import javax.jms.Queue;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

@Configuration
public class ActiveConfig {
    @Resource
    private MessageListener baseQueueMessageListener;
    @Resource
    private MessageListener sqlQueueMessageListener;
    @Resource
    private MessageListener bdxQueueMessageListener;
    @Resource
    private MessageListener nnQueueMessageListener;
    @Resource
    private MessageListener sssQueueMessageListener;
    @Resource
    private MessageListener zjhQueueMessageListener;
    @Resource
    private MessageListener qzmjQueueMessageListener;
    @Resource
    private MessageListener gppjQueueMessageListener;
    @Resource
    private MessageListener swQueueMessageListener;
    @Resource
    private MessageListener ddzQueueMessageListener;
    @Resource
    private MessageListener daoQueueMessageListener;
    @Resource
    private MessageListener matchQueueMessageListener;
    @Resource
    private MessageListener clubQueueMessageListener;
    @Resource
    private MessageListener matchDealQueueMessageListener;
    @Resource
    private MessageListener circleBaseQueueMessageListener;
    @Resource
    private MessageListener gdyQueueMessageListener;
    @Resource
    private MessageListener sgQueueMessageListener;
    @Resource
    private MessageListener gzmjQueueMessageListener;
    @Resource
    private MessageListener teaQueueMessageListener;
    @Resource
    private MessageListener pdkQueueMessageListener;

    public ActiveConfig() {
    }

    @Bean
    public Queue baseQueueDestination() {
        return new ActiveMQQueue("ZA_GAMES_BASE");
    }

    @Bean
    public Queue sqlQueueDestination() {
        return new ActiveMQQueue("ZA_GAMES_SQL");
    }

    @Bean
    public Queue bdxQueueDestination() {
        return new ActiveMQQueue("ZA_GAMES_BDX");
    }

    @Bean
    public Queue nnQueueDestination() {
        return new ActiveMQQueue("ZA_GAMES_NN");
    }

    @Bean
    public Queue sssQueueDestination() {
        return new ActiveMQQueue("ZA_GAMES_SSS");
    }

    @Bean
    public Queue zjhQueueDestination() {
        return new ActiveMQQueue("ZA_GAMES_ZJH");
    }

    @Bean
    public Queue daoQueueDestination() {
        return new ActiveMQQueue("ZA_GAMES_DAO");
    }

    @Bean
    public Queue qzmjQueueDestination() {
        return new ActiveMQQueue("ZA_GAMES_QZMJ");
    }

    @Bean
    public Queue gppjQueueDestination() {
        return new ActiveMQQueue("ZA_GAMES_GPPJ");
    }

    @Bean
    public Queue swQueueDestination() {
        return new ActiveMQQueue("ZA_GAMES_SW");
    }

    @Bean
    public Queue ddzQueueDestination() {
        return new ActiveMQQueue("ZA_GAMES_DDZ");
    }

    @Bean
    public Queue pdkQueueDestination() {
        return new ActiveMQQueue("ZA_GAMES_PDK");
    }

    @Bean
    public Queue matchQueueDestination() {
        return new ActiveMQQueue("ZA_GAMES_MATCH");
    }

    @Bean
    public Queue clubQueueDestination() {
        return new ActiveMQQueue("ZA_GAMES_CLUB");
    }

    @Bean
    public Queue matchDealQueueDestination() {
        return new ActiveMQQueue("ZA_GAMES_MATCH_DEAL");
    }

    @Bean
    public Queue gdyQueueDestination() {
        return new ActiveMQQueue("ZA_GAMES_GDY");
    }

    @Bean
    public Queue sgQueueDestination() {
        return new ActiveMQQueue("ZA_GAMES_SG");
    }

    @Bean
    public Queue gzmjQueueDestination() {
        return new ActiveMQQueue("ZA_GAMES_GZMJ");
    }

    @Bean
    public Queue circleBaseQueueDestination() {
        return new ActiveMQQueue("ZA_GAMES_CIRCLE_BASE");
    }

    @Bean
    public Queue teaQueueDestination() {
        return new ActiveMQQueue("ZA_GAMES_TEA");
    }

    @Bean
    public DefaultMessageListenerContainer baseQueueListenerContainer(ConnectionFactory connectionFactory) {
        return this.configListenerMQ(connectionFactory, this.baseQueueMessageListener, this.baseQueueDestination());
    }

    @Bean
    public DefaultMessageListenerContainer sssQueueListenerContainer(ConnectionFactory connectionFactory) {
        return this.configListenerMQ(connectionFactory, this.sssQueueMessageListener, this.sssQueueDestination());
    }

    @Bean
    public DefaultMessageListenerContainer sqlQueueListenerContainer(ConnectionFactory connectionFactory) {
        return this.configListenerMQ(connectionFactory, this.sqlQueueMessageListener, this.sqlQueueDestination());
    }

    @Bean
    public DefaultMessageListenerContainer zjhQueueListenerContainer(ConnectionFactory connectionFactory) {
        return this.configListenerMQ(connectionFactory, this.zjhQueueMessageListener, this.zjhQueueDestination());
    }

    @Bean
    public DefaultMessageListenerContainer nnQueueListenerContainer(ConnectionFactory connectionFactory) {
        return this.configListenerMQ(connectionFactory, this.nnQueueMessageListener, this.nnQueueDestination());
    }

    @Bean
    public DefaultMessageListenerContainer bdxQueueListenerContainer(ConnectionFactory connectionFactory) {
        return this.configListenerMQ(connectionFactory, this.bdxQueueMessageListener, this.bdxQueueDestination());
    }

    @Bean
    public DefaultMessageListenerContainer daoQueueListenerContainer(ConnectionFactory connectionFactory) {
        return this.configListenerMQ(connectionFactory, this.daoQueueMessageListener, this.daoQueueDestination());
    }

    @Bean
    public DefaultMessageListenerContainer qzmjQueueListenerContainer(ConnectionFactory connectionFactory) {
        return this.configListenerMQ(connectionFactory, this.qzmjQueueMessageListener, this.qzmjQueueDestination());
    }

    @Bean
    public DefaultMessageListenerContainer gppjQueueListenerContainer(ConnectionFactory connectionFactory) {
        return this.configListenerMQ(connectionFactory, this.gppjQueueMessageListener, this.gppjQueueDestination());
    }

    @Bean
    public DefaultMessageListenerContainer swQueueListenerContainer(ConnectionFactory connectionFactory) {
        return this.configListenerMQ(connectionFactory, this.swQueueMessageListener, this.swQueueDestination());
    }

    @Bean
    public DefaultMessageListenerContainer ddzQueueListenerContainer(ConnectionFactory connectionFactory) {
        return this.configListenerMQ(connectionFactory, this.ddzQueueMessageListener, this.ddzQueueDestination());
    }

    @Bean
    public DefaultMessageListenerContainer pdkQueueListenerContainer(ConnectionFactory connectionFactory) {
        return this.configListenerMQ(connectionFactory, this.pdkQueueMessageListener, this.pdkQueueDestination());
    }

    @Bean
    public DefaultMessageListenerContainer matchQueueListenerContainer(ConnectionFactory connectionFactory) {
        return this.configListenerMQ(connectionFactory, this.matchQueueMessageListener, this.matchQueueDestination());
    }

    @Bean
    public DefaultMessageListenerContainer clubQueueListenerContainer(ConnectionFactory connectionFactory) {
        return this.configListenerMQ(connectionFactory, this.clubQueueMessageListener, this.clubQueueDestination());
    }

    @Bean
    public DefaultMessageListenerContainer matchDealQueueListenerContainer(ConnectionFactory connectionFactory) {
        return this.configListenerMQ(connectionFactory, this.matchDealQueueMessageListener, this.matchDealQueueDestination());
    }

    @Bean
    public DefaultMessageListenerContainer circleBaseQueueListenerContainer(ConnectionFactory connectionFactory) {
        return this.configListenerMQ(connectionFactory, this.circleBaseQueueMessageListener, this.circleBaseQueueDestination());
    }

    @Bean
    public DefaultMessageListenerContainer teaQueueListenerContainer(ConnectionFactory connectionFactory) {
        return this.configListenerMQ(connectionFactory, this.teaQueueMessageListener, this.teaQueueDestination());
    }

    @Bean
    public DefaultMessageListenerContainer gdyBaseQueueListenerContainer(ConnectionFactory connectionFactory) {
        return this.configListenerMQ(connectionFactory, this.gdyQueueMessageListener, this.gdyQueueDestination());
    }

    @Bean
    public DefaultMessageListenerContainer sgBaseQueueListenerContainer(ConnectionFactory connectionFactory) {
        return this.configListenerMQ(connectionFactory, this.sgQueueMessageListener, this.sgQueueDestination());
    }

    @Bean
    public DefaultMessageListenerContainer gzmjBaseQueueListenerContainer(ConnectionFactory connectionFactory) {
        return this.configListenerMQ(connectionFactory, this.gzmjQueueMessageListener, this.gzmjQueueDestination());
    }

    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
        JmsTemplate jmsTemplate = new JmsTemplate();
        jmsTemplate.setConnectionFactory(connectionFactory);
        jmsTemplate.setDefaultDestination(this.baseQueueDestination());
        jmsTemplate.setExplicitQosEnabled(true);
        jmsTemplate.setDeliveryMode(2);
        jmsTemplate.setReceiveTimeout(10000L);
        jmsTemplate.setPubSubDomain(false);
        return jmsTemplate;
    }

    private DefaultMessageListenerContainer configListenerMQ(ConnectionFactory connectionFactory, MessageListener messageListener, Queue queue) {
        DefaultMessageListenerContainer queueListenerContainer = new DefaultMessageListenerContainer();
        queueListenerContainer.setConnectionFactory(connectionFactory);
        queueListenerContainer.setMessageListener(messageListener);
        queueListenerContainer.setDestination(queue);
        queueListenerContainer.setConcurrency("1-45");
        return queueListenerContainer;
    }
}
