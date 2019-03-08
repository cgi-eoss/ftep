package com.cgi.eoss.ftep.queues;

import com.cgi.eoss.ftep.queues.service.FtepQueueService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {QueuesConfig.class})
@TestPropertySource("classpath:test-queues.properties")
public class QueuesTest {

    private static final String TEST_STRING_QUEUE = "test_string_queue";

    @Autowired
    private FtepQueueService ftepQueueService;

    @Test
    public void testSendReceiveQueueLength() {
        String sentMessage = "Simple test message";
        long initialQueueLength = ftepQueueService.getQueueLength(TEST_STRING_QUEUE);

        ftepQueueService.sendObject(TEST_STRING_QUEUE, sentMessage);
        ftepQueueService.receiveObject(TEST_STRING_QUEUE);
        long queueLength = ftepQueueService.getQueueLength(TEST_STRING_QUEUE);
        assertEquals(queueLength, initialQueueLength);

        ftepQueueService.sendObject(TEST_STRING_QUEUE, sentMessage);
        queueLength = ftepQueueService.getQueueLength(TEST_STRING_QUEUE);
        assertEquals(queueLength, initialQueueLength + 1);

        String receivedStringMessage = (String) ftepQueueService.receiveObject(TEST_STRING_QUEUE);
        assertEquals(receivedStringMessage, sentMessage);

        queueLength = ftepQueueService.getQueueLength(TEST_STRING_QUEUE);
        assertEquals(queueLength, initialQueueLength);
    }

    @Test
    public void testSendReceiveWithPriority() {
        String firstMessage = "First text message";
        String secondMessage = "Second text message";
        ftepQueueService.sendObject(TEST_STRING_QUEUE, firstMessage, 1);
        ftepQueueService.sendObject(TEST_STRING_QUEUE, secondMessage, 5);
        String receivedStringMessage = (String) ftepQueueService.receiveObject(TEST_STRING_QUEUE);
        assertEquals(receivedStringMessage, secondMessage);
    }
}
