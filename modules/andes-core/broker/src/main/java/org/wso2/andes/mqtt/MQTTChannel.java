/*
 * Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.andes.mqtt;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.andes.amqp.AMQPUtils;
import org.wso2.andes.kernel.*;
import org.wso2.andes.server.ClusterResourceHolder;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * This class mainly focusses on negotiating the connections and exchanging data
 * The class will interface with the Andes kernal and will ensure that the information thats received from the bridge
 * is conforming to the data structure expected by the kernal, The basic operations done through this class will be
 * conbverting between the meta data and message content, indicate subscriptions and disconnections
 */

public class MQTTChannel {

    private static Log log = LogFactory.getLog(MQTTChannel.class);
    private static MQTTChannel instance = new MQTTChannel();
    private static final String MQTT_TOPIC_DESTINATION = "destination";
    private static final String MQTT_QUEUE_IDENTIFIER = "targetQueue";

    /**
     * The class will be declared as singleton since only one channel shold be declared across the JVM
     */
    private MQTTChannel() {
    }

    /**
     * @return Retrun the instance which is delcalred
     */
    public static MQTTChannel getInstance() {
        return instance;
    }

    /**
     * Adds the message body to the andes kernal interfaces
     *
     * @param messageBody        message content
     * @param mqttLocalMessageID channel id in which the message was published
     * @throws MQTTException
     */
    private void addMessageBody(AndesMessagePart messageBody, int mqttLocalMessageID) throws MQTTException {
        try {
            MessagingEngine.getInstance().messageContentReceived(messageBody);
            if (log.isDebugEnabled()) {
                log.debug("Content of the message with id " + mqttLocalMessageID + " added to the kernal");
            }
        } catch (AndesException ex) {
            final String message = "Error while adding message content ";
            log.error(message, ex);
            throw new MQTTException(message, ex);
        }
    }

    /**
     * Adds the message meta data to the kernal
     *
     * @param messageHeader      the meta information of the specific mqtt message
     * @param mqttLocalMessageID the channel id which is generated through mqtt protocol engine
     * @throws MQTTException at an event where the kernal could not intert the header
     */
    private void addMessageHeader(AndesMessageMetadata messageHeader, int mqttLocalMessageID) throws MQTTException {
        try {

            MessagingEngine.getInstance().messageReceived(messageHeader);
            if (log.isDebugEnabled()) {
                log.debug("Message meta data added for the message with id " + mqttLocalMessageID);
            }

        } catch (AndesException e) {
            final String error = "Error while adding the message header to the andes kernal ";
            log.error(error, e);
            throw new MQTTException(error, e);
        }
    }


    /**
     * The acked messages will be informed to the kernal
     *
     * @param messageID   the identifier of the message
     * @param topicName   the name of the topic the message was published
     * @param storageName the storage name representation of the topic
     * @throws AndesException if the ack was not processed properly
     */
    public void messageAck(long messageID, String topicName, String storageName, UUID subChannelID)
            throws AndesException {
        AndesAckData andesAckData = new AndesAckData(subChannelID, messageID,
                topicName, storageName, true);
        MessagingEngine.getInstance().ackReceived(andesAckData);
    }

    /**
     * Will add the message content which will be recived
     *
     * @param message            the content of the message which was published
     * @param messageID          the message idntifier
     * @param topic              the name of the topic which the message was published
     * @param qosLevel           the level of the qos the message was published
     * @param mqttLocalMessageID the channel id the subscriber is bound to
     * @param retain             whether the message requires to be persisted
     * @throws MQTTException occurs if there was an errro while adding the message content
     */
    public void addMessage(ByteBuffer message, long messageID, String topic, int qosLevel,
                           int mqttLocalMessageID, boolean retain) throws MQTTException {
        if (message.hasArray()) {
            //Will get the bytes of the message
            byte[] messageData = message.array();
            //Will start converting the message body
            AndesMessagePart messagePart = MQTTUtils.convertToAndesMessage(messageData, messageID);
            //Will Create the Andes Header
            AndesMessageMetadata messageHeader = MQTTUtils.convertToAndesHeader(messageID, topic, qosLevel,
                    messageData.length, retain);
            //Will write the message body
            addMessageBody(messagePart, mqttLocalMessageID);
            //Will add the message header
            addMessageHeader(messageHeader, mqttLocalMessageID);
        } else {
            throw new MQTTException("Message content is not backed by an array, or the array is read-only .");
        }
    }

    /**
     * Will add and indicate the subscription to the kernal the bridge will be provided as the channel
     * since per topic we will only be creating one channel with andes
     *
     * @param channel               the bridge connection as the channel
     * @param topic                 the name of the topic which has subscriber/s
     * @param clientID              the id which will distinguish the topic channel
     * @param mqttChannel           the subscription id which is local to the subscriber
     * @param isCleanSesion         should the connection be durable
     * @param qos                   the subscriber specific qos this can be either 0,1 or 2
     * @param subscriptionChannelID will hold the unique idenfier of the subscription
     * @throws MQTTException
     */
    public void addSubscriber(MQTTopicManager channel, String topic, String clientID, String mqttChannel,
                              boolean isCleanSesion, int qos, UUID subscriptionChannelID) throws MQTTException {
        //Will create a new local subscription object
        final String isBoundToTopic = "isBoundToTopic";
        final String subscribedNode = "subscribedNode";
        final String isDurable = "isDurable";
        //   final UUID subscriptionChannelID = UUID.randomUUID();
        final String myNodeID = ClusterResourceHolder.getInstance().getClusterManager().getMyNodeID();
        MQTTLocalSubscription localSubscription = new MQTTLocalSubscription(MQTT_TOPIC_DESTINATION + "=" +
                topic + "," + MQTT_QUEUE_IDENTIFIER + "=" + (isCleanSesion ? topic : topic + mqttChannel) + "," +
                isBoundToTopic + "=" + true + "," + subscribedNode + "=" + myNodeID + "," + isDurable + "="
                + !isCleanSesion);
        localSubscription.setIsTopic();
        localSubscription.setTargetBoundExchange(isCleanSesion ? AMQPUtils.TOPIC_EXCHANGE_NAME :
                AMQPUtils.DIRECT_EXCHANGE_NAME);
        localSubscription.setMqqtServerChannel(channel);
        localSubscription.setChannelID(subscriptionChannelID);
        localSubscription.setTopic(topic);
        localSubscription.setSubscriptionID(clientID);
        localSubscription.setMqttSubscriptionID(mqttChannel);
        localSubscription.setSubscriberQOS(qos);
        localSubscription.setIsActive(true);
        //Shold indicate the record in the cluster
        try {
            //First will register the subscription as a queue
            ClusterResourceHolder.getInstance().getSubscriptionManager().addSubscription(localSubscription);
            //Will indicate the subscription connection to the tracker
            MessagingEngine.getInstance().clientConnectionCreated(subscriptionChannelID);
            if (log.isDebugEnabled()) {
                log.debug("Subscription registered to the " + topic + " with channel id " + clientID);
            }
        } catch (AndesException e) {
            final String message = "Error ocured while creating the topic subscription in the kernal";
            log.error(message, e);
            throw new MQTTException(message, e);
        }
    }

    /**
     * Will trigger when subscriber disconnets from the session
     *
     * @param channel           the connection refference to the bridge
     * @param subscribedTopic   the topic the subscription disconnection should be made
     * @param clientID          the channel id of the diconnection client
     * @param subscriberChannel the cluster wide unique idenfication of the subscription
     */
    public void removeSubscriber(MQTTopicManager channel, String subscribedTopic, String clientID,
                                 UUID subscriberChannel)
            throws MQTTException {
        try {
            //Will create a new local subscription object
            MQTTLocalSubscription localSubscription = new MQTTLocalSubscription(MQTT_TOPIC_DESTINATION + "=" +
                    subscribedTopic + "," + MQTT_QUEUE_IDENTIFIER + "=" + subscribedTopic);
            localSubscription.setMqqtServerChannel(channel);
            localSubscription.setTopic(subscribedTopic);
            localSubscription.setSubscriptionID(clientID);
            localSubscription.setIsTopic();
            localSubscription.setIsActive(false);
            localSubscription.setTargetBoundExchange(AMQPUtils.TOPIC_EXCHANGE_NAME);
            localSubscription.setChannelID(subscriberChannel);
            ClusterResourceHolder.getInstance().getSubscriptionManager().closeLocalSubscription(localSubscription);
            //Will inicate the closure of the subscription connection
            MessagingEngine.getInstance().clientConnectionClosed(subscriberChannel);
            if (log.isDebugEnabled()) {
                log.debug("Disconnected subscriber from topic " + subscribedTopic);
            }

        } catch (AndesException e) {
            final String message = "Error occured while removing the subscriber ";
            log.error(message, e);
            throw new MQTTException(message, e);
        }
    }


}