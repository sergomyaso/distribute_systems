package hash.cracker.manager.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import hash.cracker.manager.repositories.TaskRepository;
import hash.cracker.manager.types.*;

@Service
public class ManagerService {
    private final int partCount = Integer.parseInt(System.getenv("PART_COUNT"));
    private final Duration taskTimeout = Duration.parse("PT" + System.getenv("TASK_TIMEOUT"));
    private final String alph = System.getenv("ALPHABET");

    private final CrackHashManagerRequest.Alphabet alphabet = new CrackHashManagerRequest.Alphabet();

    Logger logger = LoggerFactory.getLogger("manager");

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    AmqpTemplate rabbitTemplate;

    @Autowired
    private Queue requestQueue;

    @Autowired
    MongoTemplate mongoTemplate;

    public ManagerService() {
        for (String charString : alph.split("")) {
            alphabet.getSymbols().add(charString);
        }
        logger.info("Alhabet is: " + alphabet.getSymbols());
    }

    public ResponseEntity<String> submitTask(Task task) {
        logger.info("New task: " + task.toString());
        String requestId = UUID.randomUUID().toString();

        taskRepository.save(new HashTask(requestId, task.getHash(), task.getMaxLength(), partCount));
        logger.info("Saved task " + requestId + " to database");

        CrackHashManagerRequest request = new CrackHashManagerRequest();
        for (int i = 0; i < partCount; i++) {
            request.setAlphabet(alphabet);
            request.setRequestId(requestId);
            request.setHash(task.getHash());
            request.setMaxLength(task.getMaxLength());
            request.setPartCount(partCount);
            request.setPartNumber(i);

            rabbitTemplate.convertAndSend(requestQueue.getName(), request);
        }
        return ResponseEntity.ok(requestId);
    }

    public ResponseEntity<HashTask> getStatus(String requestId) {
        logger.info("Get status for task " + requestId);

        Optional<HashTask> res = taskRepository.findById(requestId);
        if (res.isEmpty()){
            return ResponseEntity.badRequest().build();
        }
        HashTask task = res.get();

        Duration dur = Duration.between(task.getStartTime(), Instant.now());
        if (dur.toMillis() > taskTimeout.toMillis() && task.getStatus() == Status.IN_PROGRESS) {
            task.setStatus(Status.ERROR);
            taskRepository.save(task);
            return ResponseEntity.ok(task);
        }

        return ResponseEntity.ok(task);
    }

    public void receiveAnswers(CrackHashWorkerResponse response) {
        Optional<HashTask> res = taskRepository.findById(response.getRequestId());
        if (res.isEmpty()){
            return;
        }
        HashTask task = res.get();

        List<String> words = response.getAnswers().getWords();
        words.remove("");

        // Assume that only finished worker can send an empty response 
        if (words.isEmpty()) {
            task.getFinishedParts().add(response.getPartNumber());
            if (task.getPartCount() == task.getFinishedParts().size()) {
                task.setStatus(Status.READY);
            }    
        } else {
            logger.info("Received answer from part "+ response.getPartNumber() + ": " + words.toString() +
                        ", took: " + Duration.between(task.getStartTime(), Instant.now()).toMillis() / 1000 + "s");
        }
        
        // Ignore dublicate answers
        for (String word : words) {
            if (!task.getData().contains(word)) {
                task.getData().add(word);
            }
        }
        
        taskRepository.save(task);
    }
}
