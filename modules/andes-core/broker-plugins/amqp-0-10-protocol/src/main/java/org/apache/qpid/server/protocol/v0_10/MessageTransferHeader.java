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

import java.util.*;
import org.apache.qpid.server.message.AMQMessageHeader;
import org.apache.qpid.transport.DeliveryProperties;
import org.apache.qpid.transport.MessageDeliveryPriority;
import org.apache.qpid.transport.MessageProperties;

class MessageTransferHeader implements AMQMessageHeader
{


    public static final String JMS_TYPE = "x-jms-type";

    private final DeliveryProperties _deliveryProps;
    private final MessageProperties _messageProps;

    public MessageTransferHeader(DeliveryProperties deliveryProps, MessageProperties messageProps)
    {
        _deliveryProps = deliveryProps;
        _messageProps = messageProps;
    }

    public String getCorrelationId()
    {
        if (_messageProps != null && _messageProps.getCorrelationId() != null)
        {
            return new String(_messageProps.getCorrelationId());
        }
        else
        {
            return null;
        }
    }

    public long getExpiration()
    {
        return _deliveryProps == null ? 0L : _deliveryProps.getExpiration();
    }

    public String getUserId()
    {
        byte[] userIdBytes = _messageProps == null ? null : _messageProps.getUserId();
        return userIdBytes == null ? null : new String(userIdBytes);
    }

    public String getAppId()
    {
        byte[] appIdBytes = _messageProps == null ? null : _messageProps.getAppId();
        return appIdBytes == null ? null : new String(appIdBytes);
    }

    public String getMessageId()
    {
        UUID id = _messageProps == null ? null : _messageProps.getMessageId();

        return id == null ? null : String.valueOf(id);
    }

    public String getMimeType()
    {
        return _messageProps == null ? null : _messageProps.getContentType();
    }

    public String getEncoding()
    {
        return _messageProps == null ? null : _messageProps.getContentEncoding();
    }

    public byte getPriority()
    {
        MessageDeliveryPriority priority = _deliveryProps == null || !_deliveryProps.hasPriority()
                                           ? MessageDeliveryPriority.MEDIUM
                                           : _deliveryProps.getPriority();
        return (byte) priority.getValue();
    }

    public long getTimestamp()
    {
        return _deliveryProps == null ? 0L : _deliveryProps.getTimestamp();
    }

    public String getType()
    {
        Object type = getHeader(JMS_TYPE);
        return type instanceof String ? (String) type : null;
    }

    public String getReplyTo()
    {
        if (_messageProps != null && _messageProps.getReplyTo() != null)
        {
            return _messageProps.getReplyTo().toString();
        }
        else
        {
            return null;
        }
    }

    public String getReplyToExchange()
    {
        if (_messageProps != null && _messageProps.getReplyTo() != null)
        {
            return _messageProps.getReplyTo().getExchange();
        }
        else
        {
            return null;
        }
    }

    public String getReplyToRoutingKey()
    {
        if (_messageProps != null && _messageProps.getReplyTo() != null)
        {
            return _messageProps.getReplyTo().getRoutingKey();
        }
        else
        {
            return null;
        }
    }

    public Object getHeader(String name)
    {
        Map<String, Object> appHeaders = _messageProps == null ? null : _messageProps.getApplicationHeaders();
        return appHeaders == null ? null : appHeaders.get(name);
    }

    public boolean containsHeaders(Set<String> names)
    {
        Map<String, Object> appHeaders = _messageProps == null ? null : _messageProps.getApplicationHeaders();
        return appHeaders != null && appHeaders.keySet().containsAll(names);

    }

    @Override
    public Collection<String> getHeaderNames()
    {
        Map<String, Object> appHeaders = _messageProps == null ? null : _messageProps.getApplicationHeaders();
        return appHeaders != null ? Collections.unmodifiableCollection(appHeaders.keySet()) : Collections.EMPTY_SET ;

    }

    public boolean containsHeader(String name)
    {
        Map<String, Object> appHeaders = _messageProps == null ? null : _messageProps.getApplicationHeaders();
        return appHeaders != null && appHeaders.containsKey(name);
    }
}
