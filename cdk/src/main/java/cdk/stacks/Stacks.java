package cdk.stacks;

import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;

public class Stacks {

    public static void main(final String[] args) {
        App app = new App();

        final var env = Environment.builder().account(Constants.ACCOUNT).region(Constants.REGION).build();
        final var networkStack = new NetworkStack(app, "NetworkStack", StackProps.builder().env(env).build());
        final var sampleAppStack = new SampleAppStack(app, "SampleAppStack", StackProps.builder().env(env).build(), networkStack);
        sampleAppStack.addDependency(networkStack);

        app.synth();
    }

}
