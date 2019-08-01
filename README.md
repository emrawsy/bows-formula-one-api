# bows-formula-one-api
The backend microservice for all calls to get and set user details.

## Info

This project is a Scala application built for handling JSON api calls to and from a MongoDatabase. 

  

## Running the service

#####In order to use this service you need the following:
* SBT 
* Postman (for tests)
* An instance of Mongo running (Compass or Robo3T)
* sbt run http://localhost:9000 followed by the routes

## Routes

|API|Method|Description|Endpoint|
|---|------|-----------|--------|
|present|GET|Begins/Ends a session and Welcome/Goodbye user by checking database for the user in the user collection and for an instance of the user in the session collection|/present/:_id|
|postNewUser (JSON body attached)|POST|Adds a new user to the database using a JSON payload|/user|
|delete|DELETE|Deletes a user from database by Id|/user/:_id|
|update|POST|Updates a user's details in database if they are saved as type String by sending the JSON field name and the updated data in the URL|/update-user/:_id/:key/:data|
|getBalance|GET|Retrieves the balance of a user in the database|/user/get-balance/:_id|
|transaction|POST|Calculates the new balance after a user does a transaction|/transaction/:_id/:transactionAmount|
|topUp|POST|Calculates the new balance after a user tops up their balance|/top-up/:_id/:topUpAmount|





