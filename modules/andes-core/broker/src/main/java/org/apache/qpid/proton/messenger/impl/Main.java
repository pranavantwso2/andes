package org.apache.qpid.proton.messenger.impl;

import org.apache.qpid.proton.amqp.messaging.Header;
import org.apache.qpid.proton.message.Message;
import org.apache.qpid.proton.message.impl.MessageImpl;
import org.apache.qpid.proton.messenger.Messenger;

import java.io.IOException;

/**
 * Created by megala on 3/19/15.
 */
public class Main {
    public static void main(String[] args){
        Messenger mes = Messenger.Factory.create();

        try {

            mes.start();
            org.apache.qpid.proton.message.Message mes1 = org.apache.qpid.proton.message.Message.Factory.create();
            mes.recv(10);
            mes1.setAddress("amqp://admin:admin@localhost:5672");
            mes1.setSubject("Hello World !");
            mes1.setHeader(new Header());
            System.out.println(mes1.getHeader().toString());
            mes.put(mes1);
            mes.send();
            Messenger msng = Messenger.Factory.create();
            msng.start();
            msng.subscribe("amqp://admin:admin@localhost:5672");
            System.out.println(mes.incoming());
            Message m = Message.Factory.create();
            while(msng.incoming() > 0)
            System.out.println(mes.get());

        }
        catch(IOException e) {

        }
    }
}
