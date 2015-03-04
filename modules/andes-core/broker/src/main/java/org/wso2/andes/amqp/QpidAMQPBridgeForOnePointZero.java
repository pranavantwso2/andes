/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.andes.amqp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.andes.AMQException;
import org.wso2.andes.framing.abstraction.ContentChunk;
import org.wso2.andes.kernel.*;
import org.wso2.andes.protocol.AMQConstant;
import org.wso2.andes.server.message.AMQMessage;
import org.wso2.andes.server.queue.IncomingMessage;
import org.wso2.andes.server.stats.PerformanceCounter;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class QpidAMQPBridgeForOnePointZero {

    private static Log log = LogFactory.getLog(QpidAMQPBridgeForOnePointZero.class);
    private static QpidAMQPBridgeForOnePointZero qpidAMQPBridgeForOnePointZero = null;

    private static AtomicLong receivedMessageCounter = new AtomicLong();
    private static long last10kMessageReceivedTimestamp = System.currentTimeMillis();


    public static synchronized QpidAMQPBridgeForOnePointZero getInstance(){
        if(qpidAMQPBridgeForOnePointZero == null){
            qpidAMQPBridgeForOnePointZero = new QpidAMQPBridgeForOnePointZero();
        }

        return qpidAMQPBridgeForOnePointZero;
    }

    /**
     * message metadata received from AMQP transport.
     * This should happen after all content chunks are received
     *
     * @param incomingMessage message coming in
     * @param channelID       id of the channel
     * @param andesChannel    AndesChannel
     * @throws org.wso2.andes.AMQException
     */
    public void messageReceived(IncomingMessage incomingMessage, UUID channelID, AndesChannel andesChannel) throws AMQException {

        long receivedTime = System.currentTimeMillis();
        try {
            if (log.isDebugEnabled()) {
                log.debug("Message id " + incomingMessage.getMessageNumber() + " received");
            }
            AMQMessage message = new AMQMessage(incomingMessage.getStoredMessage());

            // message arrival time set to mb node's system time without using
            // message published time by publisher.
            message.getMessageMetaData().setArrivalTime(receivedTime);

            AndesMessageMetadata metadata = AMQPUtils.convertAMQMessageToAndesMetadata(message, channelID);
            String queue = message.getRoutingKey();

            if (queue == null) {
                log.error("Queue cannot be null, for " + incomingMessage.getMessageNumber());
                return;
            }

            AndesMessage andesMessage = new AndesMessage(metadata);

            // Update Andes message with all the chunk details
            int contentChunks = incomingMessage.getBodyCount();
            int offset = 0;
            for (int i = 0; i < contentChunks; i++) {
                ContentChunk chunk = incomingMessage.getContentChunk(i);
                AndesMessagePart messagePart = messageContentChunkReceived(
                        metadata.getMessageID(), offset, chunk.getData().buf());
                offset = offset + chunk.getSize();
                andesMessage.addMessagePart(messagePart);
            }

            // Handover message to Andes
            Andes.getInstance().messageReceived(andesMessage, andesChannel);

            if(log.isDebugEnabled()) {
                PerformanceCounter.recordMessageReceived(queue, incomingMessage.getReceivedChunkCount());
            }
        } catch (AndesException e) {
            throw new AMQException(AMQConstant.INTERNAL_ERROR, "Error while storing incoming message metadata", e);
        }

        //Following code is only a performance counter
        Long localCount = receivedMessageCounter.incrementAndGet();
        if (localCount % 10000 == 0) {
            long timetook = System.currentTimeMillis() - last10kMessageReceivedTimestamp;
            log.info("Received " + localCount + ", throughput = " + (10000 * 1000 / timetook) + " msg/sec, " + timetook);
            last10kMessageReceivedTimestamp = System.currentTimeMillis();
        }

    }

    /**
     * message content chunk received to the server
     *
     * @param messageID       id of message to which content belongs
     * @param offsetInMessage chunk offset
     * @param src             Bytebuffer with content bytes
     */
    public AndesMessagePart messageContentChunkReceived(long messageID, int offsetInMessage, ByteBuffer src) {

        if (log.isDebugEnabled()) {
            log.debug("Content Part Received id " + messageID + ", offset " + offsetInMessage);
        }
        AndesMessagePart part = new AndesMessagePart();
        src = src.slice();
        final byte[] chunkData = new byte[src.limit()];
        src.duplicate().get(chunkData);

        part.setData(chunkData);
        part.setMessageID(messageID);
        part.setOffSet(offsetInMessage);
        part.setDataLength(chunkData.length);

        return part;
    }



}
