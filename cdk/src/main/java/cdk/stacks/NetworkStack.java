package cdk.stacks;

import lombok.Getter;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ec2.DefaultInstanceTenancy;
import software.amazon.awscdk.services.ec2.SubnetConfiguration;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;

import java.util.List;

@Getter
public class NetworkStack extends Stack {

    private final Vpc vpc;

    public NetworkStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        vpc = Vpc.Builder.create(this, "SampleAppVpc")
                .cidr("10.0.0.0/16")
                .defaultInstanceTenancy(DefaultInstanceTenancy.DEFAULT)
                .enableDnsHostnames(true)
                .enableDnsSupport(true)
                .maxAzs(1)
                .natGateways(0)
                .subnetConfiguration(List.of(
                        SubnetConfiguration.builder()
                                .cidrMask(24)
                                .subnetType(SubnetType.PUBLIC)
                                .reserved(false)
                                .name("public-subnet-1")
                                .build(),
                        SubnetConfiguration.builder()
                                .cidrMask(24)
                                .subnetType(SubnetType.ISOLATED)
                                .name("isolated-subnet-1")
                                .reserved(false)
                                .build()
                ))
                .build();
    }
}