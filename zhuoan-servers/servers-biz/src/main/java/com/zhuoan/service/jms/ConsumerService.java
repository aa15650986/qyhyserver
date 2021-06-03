
package com.zhuoan.service.jms;

import javax.jms.Destination;
import javax.jms.TextMessage;

/** @deprecated */
@Deprecated
public interface ConsumerService {
    TextMessage receive(Destination var1);
}
