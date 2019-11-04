# Unique-Emails
Creates an endpoint that determines the number of unique emails in a list of emails.

## The Challenge
This repo is based off of the following challenge:
write a web service that takes in a list of email addresses and returns an integer 
indicating the number of unique email addresses. Where "unique" email addresses means 
they will be delivered to the same account using Gmail account matching. 
Specifically: Gmail will ignore the placement of "." in the username. 
And it will ignore any portion of the username after a "+".

Examples:
test.email@gmail.com, test.email+spam@gmail.com and testemail@gmail.com will all go to 
the same address, and thus the result should be 1.

## Running the Code
This project runs on localhost:8080, so please make sure that port 8080 is available on the machine.
The code can be run using maven. If you do not have maven, the maven wrapper is included (mvnw).
I tested this code using Java 8, so this project might not work as expected on older or newer versions
of Java.

Navigate to the directory that contains this repository on your local machine.

If you have maven installed run the project with the following command:
```
mvn spring-boot:run
```
Otherwise use the following command:
```
./mvnw spring-boot:run
```
