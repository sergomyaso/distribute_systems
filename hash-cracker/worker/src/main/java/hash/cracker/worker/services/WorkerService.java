package hash.cracker.worker.services;

import hash.cracker.worker.types.CrackHashManagerRequest;
import hash.cracker.worker.types.CrackHashWorkerResponse;
import jakarta.xml.bind.DatatypeConverter;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.paukov.combinatorics3.Generator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@Service
public class WorkerService {
    private final Duration taskTimeout = Duration.parse("PT" + System.getenv("TASK_TIMEOUT"));

    Logger logger = LoggerFactory.getLogger("worker");

    @Autowired
    AmqpTemplate rabbitTemplate;

    @Autowired
    private Queue responseQueue;

    public void ReceiveTask(CrackHashManagerRequest part) {
        List<String> alphabet = part.getAlphabet().getSymbols();
        int partNumber = part.getPartNumber();
        int partCount = part.getPartCount();
        byte[] hashBytes = DatatypeConverter.parseHexBinary(part.getHash());

        MessageDigest hasher = null;
        try {
            hasher = MessageDigest.getInstance("MD5");
        } catch(Exception e) {
            e.printStackTrace();
            return;
        }

        logger.info("Part " + partNumber + " started");
        Instant startTime = Instant.now();
        for (int i = 0; i <= part.getMaxLength(); i++) {
            double objectCount = Math.pow(alphabet.size(), i);
            long[] ranges = splitNumber((long) objectCount, partCount);
            long skipCount = ranges[partNumber];
            long limitCount = ranges[partNumber + 1] - ranges[partNumber];

            Iterator<List<String>> iter = Generator.permutation(alphabet).withRepetitions(i)
                .stream()
                .skip(skipCount)
                .limit(limitCount)
                .iterator();

            while(iter.hasNext()) {
                String str = String.join("", iter.next());
                byte[] md5 = hasher.digest(str.getBytes());
                if (Arrays.equals(md5, hashBytes)) {
                    logger.info("Found hash: " + DatatypeConverter.printHexBinary(md5).toLowerCase() + "='" + str + "', part: " + partNumber);
                    sendAnswer(part.getRequestId(), str, partNumber);
                }

                Duration dur = Duration.between(startTime, Instant.now());
                if (dur.toMillis() > taskTimeout.toMillis()) {
                    logger.warn("Part " + partNumber + " exceeded time limit of " + System.getenv("TASK_TIMEOUT") + ": exiting");
                    return;
                }
            }
        }

        // Send an empty response to show that the worker has finished
        sendAnswer(part.getRequestId(), "", partNumber);

        logger.info("Part " + partNumber + " finished");
    }

    private void sendAnswer(String id, String answer, int partNumber) {
        CrackHashWorkerResponse response = new CrackHashWorkerResponse();
        response.setRequestId(id);
        response.setAnswers(new CrackHashWorkerResponse.Answers());
        response.getAnswers().getWords().add(answer);
        response.setPartNumber(partNumber);

        rabbitTemplate.convertAndSend(responseQueue.getName(), response);
    }

    private static long[] splitNumber(long number, int numRanges) {
        long[] ranges = new long[numRanges + 1];
        long rangeSize = number / numRanges;
        long remainder = number % numRanges;
        int currentRange = 0;
        long rangeEnd = 0;

        for (int i = 0; i < numRanges + 1; i++) {
            long rangeStart = rangeEnd;
            rangeEnd = rangeStart + rangeSize;
            if (currentRange < remainder) {
                rangeEnd++;
                currentRange++;
            }
            ranges[i] = rangeStart;
        }

        return ranges;
    }
}
