package hash.cracker.manager.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import hash.cracker.manager.types.HashTask;

public interface TaskRepository extends MongoRepository<HashTask, String> {

}
