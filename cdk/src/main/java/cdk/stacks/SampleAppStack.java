package cdk.stacks;

import software.amazon.awscdk.core.CfnOutput;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.autoscaling.AutoScalingGroup;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ecr.assets.DockerImageAsset;
import software.amazon.awscdk.services.ecs.Protocol;
import software.amazon.awscdk.services.ecs.*;

public class SampleAppStack extends Stack {

    public SampleAppStack(final Construct scope, final String id, final StackProps props, final NetworkStack networkStack) {
        super(scope, id, props);

        final var sampleAppSecurityGroup = SecurityGroup.Builder.create(this, "SecurityGroup")
                .vpc(networkStack.getVpc())
                .build();
        sampleAppSecurityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(80), "Web App Port Connection");
        sampleAppSecurityGroup.addEgressRule(Peer.anyIpv4(), Port.allTraffic());

        final var sampleAppDockerImageAsset = DockerImageAsset.Builder.create(this, "SampleAppDockerImageAsset")
                .directory("../app/sampleapp")
                .file("Dockerfile")
                .build();
        final var sampleAppEcrImage = EcrImage.fromDockerImageAsset(sampleAppDockerImageAsset);

        CfnOutput.Builder.create(this, "SampleAppECRRepositoryArn")
                .value(sampleAppDockerImageAsset.getRepository().getRepositoryArn()).build();
        CfnOutput.Builder.create(this, "SampleAppECRImageURL")
                .value(sampleAppDockerImageAsset.getImageUri()).build();

        final var cluster = Cluster.Builder.create(this, "SampleAppCluster")
                .vpc(networkStack.getVpc())
                .build();

        final var autoScalingGroup = AutoScalingGroup.Builder.create(this, "SampleAppAutoScalingGroup")
                .associatePublicIpAddress(true)
                .securityGroup(sampleAppSecurityGroup)
                .desiredCapacity(1)
                .instanceType(new InstanceType("t2.micro"))
                .machineImage(EcsOptimizedImage.amazonLinux2())
                .vpc(networkStack.getVpc())
                .vpcSubnets(SubnetSelection.builder().subnetType(SubnetType.PUBLIC).build())
                .build();
        cluster.addAutoScalingGroup(autoScalingGroup);

        final var sampleAppTaskDefinition =
                Ec2TaskDefinition.Builder.create(this, "SampleAppTaskDefinition")
                        .networkMode(NetworkMode.BRIDGE)
                        .build();
        sampleAppTaskDefinition.addContainer("sampleAppTaskContainerDefinition", ContainerDefinitionOptions.builder()
                .image(sampleAppEcrImage)
                .memoryLimitMiB(512)
                .cpu(1024)
                .build())
                .addPortMappings(PortMapping.builder()
                        .containerPort(8080)
                        .hostPort(80)
                        .protocol(Protocol.TCP)
                        .build());
        final var sampleAppEcs = Ec2Service.Builder.create(this, "SampleAppEc2Service")
                .cluster(cluster)
                .taskDefinition(sampleAppTaskDefinition)
                .build();
    }

}
