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

package org.wso2.andes.transport.network.mina;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.SimpleByteBufferAllocator;
import org.apache.mina.filter.ReadThrottleFilterBuilder;
import org.apache.mina.filter.SSLFilter;
import org.apache.mina.util.SessionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.andes.protocol.ProtocolEngine;
import org.wso2.andes.protocol.ProtocolEngineFactory;
import org.wso2.andes.ssl.SSLContextFactory;
import org.wso2.andes.transport.network.NetworkConnection;

public class MinaNetworkHandler extends IoHandlerAdapter
{
    private static final Logger log = LoggerFactory.getLogger(MinaNetworkHandler.class);

    private ProtocolEngineFactory _factory;
    private SSLContextFactory _sslFactory = null;
    private SSLFilter sslFilter = null;

    static
    {
        boolean directBuffers = Boolean.getBoolean("amqj.enableDirectBuffers");
        log.debug("Using " + (directBuffers ? "direct" : "heap") + " buffers");
        ByteBuffer.setUseDirectBuffers(directBuffers);

        //override the MINA defaults to prevent use of the PooledByteBufferAllocator
        ByteBuffer.setAllocator(new SimpleByteBufferAllocator());
    }

    public MinaNetworkHandler(SSLContextFactory sslFactory, ProtocolEngineFactory factory)
    {
        _sslFactory = sslFactory;
        _factory = factory;
    }

    public MinaNetworkHandler(SSLContextFactory sslFactory)
    {
        this(sslFactory, null);
    }

    public void messageReceived(IoSession session, Object message)
    {
        ProtocolEngine engine = (ProtocolEngine) session.getAttachment();
        ByteBuffer buf = (ByteBuffer) message;
        try
        {
            engine.received(buf.buf());
        }
        catch (RuntimeException re)
        {
            engine.exception(re);
        }
    }

    public void exceptionCaught(IoSession ioSession, Throwable throwable) throws Exception
    {
        ProtocolEngine engine = (ProtocolEngine) ioSession.getAttachment();
        if(engine != null)
        {
            log.error("Exception caught by Mina", throwable);
            engine.exception(throwable);
        }
        else
        {
            log.error("Exception caught by Mina but without protocol engine to handle it", throwable);
        }
    }

    public void sessionCreated(IoSession ioSession) throws Exception
    {
        if(log.isDebugEnabled())
        {
            log.debug("Created session: " + ioSession.getRemoteAddress());
        }

        SessionUtil.initialize(ioSession);
        IoFilterChain chain = ioSession.getFilterChain();

      if (_sslFactory != null)
        {
            // at the time of initializing sslfactory no 'protocolFilter' is created. Hence we need to add 'sslFilter' as first in filter chain.
            chain.addFirst("sslFilter", new SSLFilter(_sslFactory.buildServerContext()));

        }

        if (_factory != null)
        {
           NetworkConnection netConn = new MinaNetworkConnection(ioSession);

           ProtocolEngine engine = _factory.newProtocolEngine(netConn);
           ioSession.setAttachment(engine);
        }

        // We limit the size of receiving buffer to 1 MB
        ReadThrottleFilterBuilder readFilter = new ReadThrottleFilterBuilder();
        readFilter.setMaximumConnectionBufferSize(1024*1024);
        readFilter.attach(chain);
    }

    public void sessionClosed(IoSession ioSession) throws Exception
    {
        if(log.isDebugEnabled())
        {
            log.debug("closed: " + ioSession.getRemoteAddress());
        }

        ProtocolEngine engine = (ProtocolEngine) ioSession.getAttachment();
        if(engine != null)
        {
            engine.closed();
        }
        else
        {
            log.error("Unable to close ProtocolEngine as none was present");
        }
    }

   
    public void sessionIdle(IoSession session, IdleStatus status) throws Exception
    {
        if (IdleStatus.WRITER_IDLE.equals(status))
        {
            ((ProtocolEngine) session.getAttachment()).writerIdle();
        }
        else if (IdleStatus.READER_IDLE.equals(status))
        {
            ((ProtocolEngine) session.getAttachment()).readerIdle();
        }
    }

}
