package com.zhuoan.queue;

import com.zhuoan.service.jms.ProducerService;
import javax.annotation.Resource;
import javax.jms.Destination;
import org.springframework.stereotype.Component;

@Component
public class SqlQueue {
	@Resource
	private ProducerService producerService;
	@Resource
	private Destination sqlQueueDestination;

	public SqlQueue() {
	}

	public void addSqlTask(SqlModel sqlModel) {
		this.producerService.sendMessage(this.sqlQueueDestination, sqlModel);
	}
}
