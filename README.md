# Submission Service

Spring Boot 3 microservice for Canopy. It is running on Java 17.

Submission Service is the largest service (excluding UI) in the Canopy platform.
It handles the majority of the data and study ingest logic, including, but not limited to:
* Data Ingest
  * Submission creation
  * File upload
  * File bundling
  * File validation and scanning
  * Submission approval and rejection
* Study Registration
  * MTA form uploads and processing
  * New study creation
  * Study approval and rejection

# Install and Run

## Maven

If using secrets manager for environment variables, AWS CLI needs to be installed and configured to run cloud environments locally.

There are quite a few environment variables that need to be set:
* dbuser
  * database username
* password
  * database password for dbuser
* host
  * hostname of database
* port
  * database port
* dbname
  * database name
* InReviewS3Bucket
  * Name of S3 bucket to hold files waiting for review and approval
* ApprovedS3Bucket
  * Name of S3 bucket to hold approved files
* SFTPIngestBucket
  * Name of S3 bucket used to store sftp files before being processed into the Canopy system
* PiiQueue
  * Name of SQS queue where PII validation requests are to be sent
* EmailQueue
  * Name of SQS queue where email requests are sent and processed
* supportEmail
  * Email address that is used to send emails from the Canopy platform
* StakeholderEmailsStudyReg
  * A comma separated list of any additional study reg stakeholders, to be emailed on study upload and approval
* HostURL
  * hostname for the Canopy frontend
In a specific instance, the only environment variable that needs to be set is:
* spring_profiles_active
  * This should be set to '{environment}'
    * The current environments are dev, test, prod

Once the environment variables are set:
```
mvn clean install 
```
Once all classes are generated, you can run the application with maven or via the application context.
```
mvn spring-boot:run
```

### Endpoint

The base endpoint for this service is:
```
{{hostname}}/api/submission-service/v1/
```
