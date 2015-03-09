/*
*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*
*/
package org.apache.qpid.server.protocol.v0_10;

import org.apache.qpid.server.message.AMQMessageHeader;
import org.apache.qpid.server.plugin.MessageMetaDataType;
import org.apache.qpid.server.store.StorableMessageMetaData;
import org.apache.qpid.transport.DeliveryProperties;
import org.apache.qpid.transport.Header;
import org.apache.qpid.transport.MessageDeliveryMode;
import org.apache.qpid.transport.MessageProperties;
import org.apache.qpid.transport.MessageTransfer;
import org.apache.qpid.transport.Struct;
import org.apache.qpid.transport.codec.BBDecoder;
import org.apache.qpid.transport.codec.BBEncoder;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class MessageMetaData_0_10 implements StorableMessageMetaData
{
    private Header _header;
    private DeliveryProperties _deliveryProps;
    private MessageProperties _messageProps;
    private MessageTransferHeader _messageHeader;
    private long _arrivalTime;
    private int _bodySize;

    private static final int ENCODER_SIZE = 1 << 10;

    public static final MessageMetaDataType.Factory<MessageMetaData_0_10> FACTORY = new MetaDataFactory();

    private static final MessageMetaDataType_0_10 TYPE = new MessageMetaDataType_0_10();

    private volatile ByteBuffer _encoded;

    public MessageMetaData_0_10(MessageTransfer xfr)
    {
        this(xfr.getHeader(), xfr.getBodySize(), System.currentTimeMillis());
    }

    public MessageMetaData_0_10(Header header, int bodySize, long arrivalTime)
    {
        _header = header;
        if(_header != null)
        {
            _deliveryProps = _header.getDeliveryProperties();
            _messageProps = _header.getMessageProperties();
        }
        else
        {
            _deliveryProps = null;
            _messageProps = null;
        }
        _messageHeader = new MessageTransferHeader(_deliveryProps, _messageProps);
        _arrivalTime = arrivalTime;
        _bodySize = bodySize;

    }



    public MessageMetaDataType getType()
    {
        return TYPE;
    }

    public int getStorableSize()
    {
        ByteBuffer buf = _encoded;

        if(buf == null)
        {
            buf = encodeAsBuffer();
            _encoded = buf;
        }

        //TODO -- need to add stuff
        return buf.limit();
    }

    private ByteBuffer encodeAsBuffer()
    {
        BBEncoder encoder = new BBEncoder(ENCODER_SIZE);

        encoder.writeInt64(_arrivalTime);
        encoder.writeInt32(_bodySize);
        int headersLength = 0;
        if(_header.getDeliveryProperties() != null)
        {
            headersLength++;
        }
        if(_header.getMessageProperties() != null)
        {
            headersLength++;
        }
        if(_header.getNonStandardProperties() != null)
        {
            headersLength += _header.getNonStandardProperties().size();
        }

        encoder.writeInt32(headersLength);

        if(_header.getDeliveryProperties() != null)
        {
            encoder.writeStruct32(_header.getDeliveryProperties());
        }
        if(_header.getMessageProperties() != null)
        {
            encoder.writeStruct32(_header.getMessageProperties());
        }
        if(_header.getNonStandardProperties() != null)
        {

            for(Struct header : _header.getNonStandardProperties())
            {
                encoder.writeStruct32(header);
            }

        }
        ByteBuffer buf = encoder.buffer();
        return buf;
    }

    public int writeToBuffer(ByteBuffer dest)
    {
        ByteBuffer buf = _encoded;

        if(buf == null)
        {
            buf = encodeAsBuffer();
            _encoded = buf;
        }

        buf = buf.duplicate();

        buf.position(0);

        if(dest.remaining() < buf.limit())
        {
            buf.limit(dest.remaining());
        }
        dest.put(buf);
        return buf.limit();
    }

    public int getContentSize()
    {
        return _bodySize;
    }

    public boolean isPersistent()
    {
        return _deliveryProps == null ? false : _deliveryProps.getDeliveryMode() == MessageDeliveryMode.PERSISTENT;
    }

    public String getRoutingKey()
    {
        return _deliveryProps == null ? null : _deliveryProps.getRoutingKey();
    }

    public AMQMessageHeader getMessageHeader()
    {
        return _messageHeader;
    }

    public long getSize()
    {

        return _bodySize;
    }

    public boolean isImmediate()
    {
        return _deliveryProps != null && _deliveryProps.getImmediate();
    }

    public long getExpiration()
    {
        return _deliveryProps == null ? 0L : _deliveryProps.getExpiration();
    }

    public long getArrivalTime()
    {
        return _arrivalTime;
    }

    public Header getHeader()
    {
        return _header;
    }

    private static class MetaDataFactory implements MessageMetaDataType.Factory<MessageMetaData_0_10>
    {
        public MessageMetaData_0_10 createMetaData(ByteBuffer buf)
        {
            BBDecoder decoder = new BBDecoder();
            decoder.init(buf);

            long arrivalTime = decoder.readInt64();
            int bodySize = decoder.readInt32();
            int headerCount = decoder.readInt32();

            DeliveryProperties deliveryProperties = null;
            MessageProperties messageProperties = null;
            List<Struct> otherProps = null;

            for(int i = 0 ; i < headerCount; i++)
            {
                Struct struct = decoder.readStruct32();
                if(struct instanceof DeliveryProperties && deliveryProperties == null)
                {
                    deliveryProperties = (DeliveryProperties) struct;
                }
                else if(struct instanceof MessageProperties && messageProperties == null)
                {
                    messageProperties = (MessageProperties) struct;
                }
                else
                {
                    if(otherProps == null)
                    {
                        otherProps = new ArrayList<Struct>();

                    }
                    otherProps.add(struct);
                }
            }
            Header header = new Header(deliveryProperties,messageProperties,otherProps);

            return new MessageMetaData_0_10(header, bodySize, arrivalTime);

        }
    }


}
