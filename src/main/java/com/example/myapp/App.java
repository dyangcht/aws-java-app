package com.example.myapp;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.efs.EfsClient;
import software.amazon.awssdk.services.efs.model.Tag;
import software.amazon.awssdk.services.efs.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;

// describe instances
// Key Pair
// EFS

// Security Group
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsResponse;
import software.amazon.awssdk.services.ec2.model.SecurityGroup;

public class App {

    public static void main(String[] args) throws IOException {

        Region region = Region.US_WEST_2;
        region = Region.US_EAST_1;

        Ec2Client ec2 = Ec2Client.builder().region(region).build();
        describeEC2Instances(ec2);
        System.out.println("Creating a key pair...");
        createEC2KeyPair(ec2, "JavaKeyPair");
        deleteKeys(ec2, "JavaKeyPair");
        // sg list
        System.out.println("describing the security objects...");
        describeEC2SecurityGroups(ec2);
        ec2.close();

        // EFS
        EfsClient efs = EfsClient.builder().region(region).build();
        String fsId = createEfs(efs);
        deleteEfs(efs, fsId);
        efs.close();

        return;
        /*
        region = Region.US_WEST_2;
        S3Client s3 = S3Client.builder().region(region).build();

        String bucket = "bucket" + System.currentTimeMillis();
        String key = "key";

        tutorialSetup(s3, bucket, region);

        System.out.println("Uploading object...");

        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key)
                        .build(),
                RequestBody.fromString("Testing with the AWS SDK for Java"));

        System.out.println("Upload complete");
        System.out.printf("%n");

        cleanUp(s3, bucket, key);

        System.out.println("Closing the connection to Amazon S3");
        s3.close();
        System.out.println("Connection closed");
        System.out.println("Exiting...");
         */
    }
    public static String configureEC2SecurityGroup(Ec2Client ec2,
                                                   // String groupName,
                                                   String groupId) {
        try {
            /*
            CreateSecurityGroupRequest createRequest = CreateSecurityGroupRequest.builder()
                    .groupName(groupName)
                    .description(groupDesc)
                    .vpcId(vpcId)
                    .build();

            CreateSecurityGroupResponse response = ec2.createSecurityGroup(createRequest);
            */
            IpRange ipRange = IpRange.builder()
                    .cidrIp("0.0.0.0/0").build();
            IpPermission ipPerm = IpPermission.builder()
                    .ipProtocol("tcp")
                    .toPort(22)
                    .fromPort(22)
                    .ipRanges(ipRange)
                    .build();

            AuthorizeSecurityGroupIngressRequest authRequest =
                    AuthorizeSecurityGroupIngressRequest.builder()
                            // .groupName(groupName)
                            .groupId(groupId)
                            .ipPermissions(ipPerm)
                            // .ipPermissions(ipPerm, ipPerm2, ipPerm3)
                            .build();
            AuthorizeSecurityGroupIngressResponse authResponse =
                    ec2.authorizeSecurityGroupIngress(authRequest);
            System.out.printf(
                    "Successfully added ingress policy to Security Group %s \n",
                    groupId);
            return authResponse.toString();
        } catch (Ec2Exception e) {
            System.out.println("Security Group Configure Exception...");
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
        return "";
    }

    public static void describeEC2SecurityGroups(Ec2Client ec2) {
        String nextToken = null;
        int count = 0;
        try {
            do {
                DescribeSecurityGroupsRequest request = DescribeSecurityGroupsRequest.builder().maxResults(6).nextToken(nextToken).build();
                DescribeSecurityGroupsResponse response = ec2.describeSecurityGroups(request);

                for (SecurityGroup sg : response.securityGroups()) {
                    System.out.printf(
                            "Found Security Group %s, " +
                                    " with id %s, " +
                                    "Name %s, " +
                                    "VPC Id %s " +
                                    "and owner id is %s",
                            sg.description(),
                            sg.groupId(),
                            sg.groupName(),
                            sg.vpcId(),
                            sg.ownerId());
                    System.out.println("");
                    if (sg.groupName().contains("terraform") && (count == 0)) {
                        String code = configureEC2SecurityGroup(ec2, sg.groupId());
                        System.out.println("The return is " + code);
                        count++;
                    }
                }
                nextToken = response.nextToken();
            } while (nextToken != null);

        } catch (Ec2Exception e) {
            System.out.println("Security Group Exception...");
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }

    }
    public static String createEfs(EfsClient efs) {
        try {
            Tag tag = Tag.builder().key("Name").value("JavaApp").build();
            CreateFileSystemRequest request = CreateFileSystemRequest.builder().tags(tag).performanceMode(PerformanceMode.MAX_IO).build();
            CreateFileSystemResponse response = efs.createFileSystem(request);
            System.out.println("Create an EFS and Id is " + response.fileSystemId());
            return response.fileSystemId();
        } catch (EfsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            return "fs-error";
        }
    }

    public static void deleteEfs(EfsClient efs, String fsId) {
        try {
            DeleteFileSystemRequest request = DeleteFileSystemRequest.builder().fileSystemId(fsId).build();
            DeleteFileSystemResponse response = efs.deleteFileSystem(request);
            System.out.println("Delete an EFS: " + response.toString());
        } catch (EfsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
        }
    }

    public static void createEC2KeyPair(Ec2Client ec2, String keyName) {

        try {
            CreateKeyPairRequest request = CreateKeyPairRequest.builder()
                    .keyName(keyName).build();

            CreateKeyPairResponse response = ec2.createKeyPair(request);
            System.out.printf(
                    "Successfully created key pair named %s",
                    keyName);
            System.out.printf(
                    response.keyMaterial());
            System.out.println("");
            System.out.printf(response.keyFingerprint());
            System.out.println("");

        } catch (Ec2Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            //System.exit(1);
        }
    }

    public static void deleteKeys(Ec2Client ec2, String keyPair) {

        try {

            DeleteKeyPairRequest request = DeleteKeyPairRequest.builder()
                    .keyName(keyPair)
                    .build();

            DeleteKeyPairResponse response = ec2.deleteKeyPair(request);
            System.out.println("Delete a Key Pair: " + keyPair);

        } catch (Ec2Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
        }
    }

    public static void describeEC2Instances(Ec2Client ec2) {

        boolean done = false;
        String nextToken = null;

        try {

            do {
                DescribeInstancesRequest request = DescribeInstancesRequest.builder().maxResults(5).nextToken(nextToken).build();
                DescribeInstancesResponse response = ec2.describeInstances(request);

                for (Reservation reservation : response.reservations()) {
                    for (Instance instance : reservation.instances()) {
                        System.out.printf(
                                "Found Reservation with id %s, " +
                                        "AMI %s, " +
                                        "type %s, " +
                                        "state %s " +
                                        "and monitoring state %s",
                                instance.instanceId(),
                                instance.imageId(),
                                instance.instanceType(),
                                instance.state().name(),
                                instance.monitoring().state());
                        System.out.println("");
                    }
                }
                nextToken = response.nextToken();
            } while (nextToken != null);

        } catch (Ec2Exception e) {
            System.out.println("Exception...");
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }

    public static void tutorialSetup(S3Client s3Client, String bucketName, Region region) {
        try {
            s3Client.createBucket(CreateBucketRequest
                    .builder()
                    .bucket(bucketName)
                    .createBucketConfiguration(
                            CreateBucketConfiguration.builder()
                                    .locationConstraint(region.id())
                                    .build())
                    .build());
            System.out.println("Creating bucket: " + bucketName);
            s3Client.waiter().waitUntilBucketExists(HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build());
            System.out.println(bucketName + " is ready.");
            System.out.printf("%n");
        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }

    public static void cleanUp(S3Client s3Client, String bucketName, String keyName) {
        System.out.println("Cleaning up...");
        try {
            System.out.println("Deleting object: " + keyName);
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder().bucket(bucketName).key(keyName).build();
            s3Client.deleteObject(deleteObjectRequest);
            System.out.println(keyName + " has been deleted.");
            System.out.println("Deleting bucket: " + bucketName);
            DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder().bucket(bucketName).build();
            s3Client.deleteBucket(deleteBucketRequest);
            System.out.println(bucketName + " has been deleted.");
            System.out.printf("%n");
        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
        System.out.println("Cleanup complete");
        System.out.printf("%n");
    }
}
