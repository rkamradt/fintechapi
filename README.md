# Example FinTech API

# Implementation

to test run 

```
docker-compose up -d mongodb  # start up mongo
mvn clean install jib:build # build application image (requires logged into dockerhub account)
docker-compose up app # start up application with log in terminal
```

* for health check: http://localhost:8080/actuator/health
* for info: http://localhost:8080/actuator/info
* for swagger api: http://localhost:8080/swagger-ui.html

### Endpoints:

* POST /v1/fintech/transfer
* POST /v1/fintech/account
* GET /v1/fintech/transfers/{accountId}
* GET /v1/fintech/account/{accountId}

All endpoint require 'X-user-id' header for the main user
