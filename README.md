### Bank Account Service

##### Assignee: Nikolaos Christidis (nick.christidis@yahoo.com)
<hr>



#### Build Jar and Run
* Execute: `mvn clean package`

* Then execute: `java -jar target/revolut-home-task-1.0-SNAPSHOT.jar`


#### Execute Unit Tests
* Execute: `mvn clean test`


#### Execute Integration Tests
* Execute: `mvn clean integration-test -DskipUTs=true` or `mvn clean verify -DskipUTs=true`


#### Test Coverage (via JaCoCo)
* In order to generate reports execute: `mvn clean verify`
    * In order to see unit test coverage open with browser: `target/site/jacoco-ut/index.html`
    * In order to see integration test coverage open with browser: `target/site/jacoco-it/index.html`
    
    
#### Provided Endpoints

##### Account Management
* Create account
* Delete account
* Find all accounts
* Find account by id
* Update account


##### Account Operation
* Credit
* Debit
* Find account's transactions (display)
* Transfer amount (from ---> to)


##### Operational
* Health-check endpoint
* JMX registration of accounts in memory map



<hr>

#### Possible Alternatives
Instead of using StampedLock, we could use: `https://github.com/pveentjer/Multiverse`