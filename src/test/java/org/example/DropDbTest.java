package org.example;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class DropDbTest {
	
	@Container
	private static final MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:6.0"));
	@Autowired
	MyDataRepository repository;
	
	@DynamicPropertySource
	public static void modifyContext(DynamicPropertyRegistry registry) {
		registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
	}
	
	@Test
	void testDropDb() {
		repository.save(new MyData("value")).block();
		long countAfterSave = repository.findAll().toStream().count();
		
		dropDatabase(mongo.getReplicaSetUrl());
		
		long countAfterDrop = repository.findAll().toStream().count();
		
		assertThat(countAfterSave).isEqualTo(1);
		assertThat(countAfterDrop).isZero();
	}
	
	private void dropDatabase(String uri) {
		String dbname = uri.substring(uri.lastIndexOf('/') + 1);
		try (MongoClient mongoClient = MongoClients.create(uri)) {
			
			MongoDatabase database = mongoClient.getDatabase(dbname);
			final var subscriber = new Subscriber<Void>() {
				public boolean cleared = false;
				
				@Override
				public void onSubscribe(Subscription s) {
					System.out.println("onSubscribe");
				}
				
				@Override
				public void onNext(Void unused) {
					System.out.println("onNext");
				}
				
				@Override
				public void onError(Throwable t) {
					System.out.println("ERROR during drop DB. " + t.getMessage());
				}
				
				@Override
				public void onComplete() {
					System.out.println("onComplete");
					cleared = true;
				}
			};
			database.drop().subscribe(subscriber);
			// reactor-bom 2022.0.x will fail here while 2020.0.x succeeds
			// => enable the property in pom.xml to swap to a working version
			Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> subscriber.cleared);
		}
	}
	
}
