# Java App for AWS Services

### The purpose is preparing an environment for the EFS
### I'm trying to create a EFS service for OCP 4 on AWS
### After the OCP installed, there are some security groups has been created
### In this app, I open the port 22 for all IPs, 0.0.0.0/0


### Execute the following commands to create it
```
mvn clean package
mvn exec:java -Dexec.mainClass="com.example.myapp.App"
```

### The application is a sample and not well testing yet.
