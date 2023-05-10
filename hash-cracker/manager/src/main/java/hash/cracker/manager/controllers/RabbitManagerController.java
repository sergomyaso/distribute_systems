package hash.cracker.manager.controllers;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import hash.cracker.manager.types.CrackHashWorkerResponse;
import hash.cracker.manager.services.ManagerService;

@Component
@RabbitListener(queues = "${rabbitmq.response.queue}", id = "manager")
public class RabbitManagerController {
    @Autowired
    ManagerService managerService;

    @RabbitHandler
    public void receiver(CrackHashWorkerResponse response) {
        managerService.receiveAnswers(response);
    }
}