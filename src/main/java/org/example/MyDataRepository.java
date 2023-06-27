package org.example;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MyDataRepository extends ReactiveMongoRepository<MyData, String> {
}
