# Live Events Service

A Spring Boot application for handling live event status updates. Java version is 21.


## Setup & run instructions

### Running the app

**Using Command Line:**
```bash
./gradlew bootRun -Dspring.profiles.active=dev
```
We are running in dev mode to be able to use accompanied docker containers for kafka cluster. `spring-boot-docker-compose` enables us to use the attached docker-compose file without the need for setting up an external cluster.

This means you need docker desktop running to be able to run the project.

**Using IntelliJ IDEA:**
1. Locate `LiveEventsApplication.java`
2. Right-click and select "Run"

---
Once running, you can trigger your first task by sending a POST request to:
```bash
POST http://localhost:8080/api/events/status

{
    "eventId": 1234,
    "status": false
}
```
#### Note on first application run:
- The project includes `spring-boot-docker-compose` dependency, which automatically handles Kafka setup
- First run may take longer as Docker images are downloaded

### Running Tests

**Using Command Line:**
```bash
./gradlew test
```

**Using IntelliJ IDEA:**
1. Navigate to `src/test` folder
2. Right-click and select "Run"


## A summary of your design decisions.


#### Rest endpoints exposed
- Spring Web for exposing scheduling endpoints


#### External API Rest Client
- Spring rest client is used to do the periodic external api calls to query the live score.
- An embedded mock endpoint is also created to be used as this external web service.
- Code for the mock service is under /mock folder
- The mock service exposed is at get http://localhost:8080/api/events/status/{event_id}
- As the score itself, I chose random UUIDs to be returned as it is the most straightforward approach to observe score changes.

#### Kafka Messaging
- Spring Boot Kafka client for message handling
- A sample Kafka listener included in `/mock` folder for local dev env testing purposes

#### Retry Mechanism
- For retries, spring boot's `@Retryable` is used, with 3 retries, and 1 sec backoff.
- `@Retryable` is pretty powerful, so we can easily modify retry strategy with advanced ones using jitter, etc.

#### Job Scheduling
- For scheduling the jobs querying the score and sending kafka messages, I chose spring boot's task scheduler.
- Spring's task scheduler allows us to dynamically schedule jobs which is what we need for this project.
We could also choose a specialised package like quartz scheduler for dynamic task management
and it would provide more features like persistent jobs, etc. 
- But, I chose a simpler approach with spring's task scheduler,
and omitted advanced features intentionally. 
- Meaning that our scheduler is in memory here.

#### Concurrency Management with in-memory jobs
- Another open area I left for improvement is the concurrent in memory storage of scheduled tasks.
- I only chose a concurrent hash map to provide a snippet of the importance of concurrency management there,
but it needs more than just a concurrent map to handle concurrency there, 
- specifically atomic operations are needed for checking for existence and then putting & removing jobs
to the hash map.

#### Logging
- While logging, I printed the record classes directly to the log output, which would be a bad practice in real life scenarios with
records containing sensitive data. But, I chose this way as this is not the case here.

#### Input Validation
- As the assignment description talks about validating input, I introduced a made up restriction for event id for demonstration purposes.
- I chose this way because I chose eventId as a long and status as a boolean and spring boot handles most of the validation
for these types. 
- So, for demonstration purposes, I introduced a rule for the id to be between 1000 and 9999, and validated that
with spring boot's rule & validation annotations like @Max, @Min and @Validated.

#### Testing Strategy

- For testing, I mostly used integration tests, especially on rest controller classes. 
- But I also included unit tests for both demonstration purposes and in some cases they were needed and integration tests would be an overkill.
- I both used wiremock and spring's embedded kafka broker to be as close as the production environment, but also mocked restclient and kafkaTemplate in another file.
- I also used awaitility to verify our asynchronous scheduled calls in this project.
- And lastly, I omitted writing tests only for /mock folder in src as that folder is out of scope for this project and I only included for dev environment manual testing purposes.

## Documentation of any AI-assisted parts (what was generated, how you verified/improved it).
- AI tools were always assisting me throughout this project. I don't have access to full compose mechanisms like Cursor's composer (agent) or IntelliJ Idea's Junie. So, I used AI only to ask questions, not to generate full projects.

- Most of the time, I asked AI about the syntax and boilerplate part of things - for example how to set wiremock server or embedded kafka broker up for tests.

- I also made AI to generate the docker-compose.yaml of this project to run the attached kafka cluster.

- In addition, when I face an unexpected error while running the code or testing, I asked AI on what the wrong thing is.

- And lastly, I used AI to format this readme file :)
