# AWS ECS Spring Boot Sample

## init

```shell script
$ npm install -g aws-cdk@1.90.1

$ export AWS_ACCESS_KEY_ID=${YOUR_AWS_ACCESS_KEY_ID}
$ export AWS_SECRET_ACCESS_KEY=${YOUR_AWS_SECRET_ACCESS_KEY}
$ export AWS_DEFAULT_REGION=${YOUR_AWS_DEFAULT_REGION}

$ cd cdk/
```

## show stacks

```shell script
$ cdk ls

BUILD SUCCESSFUL in 4s
3 actionable tasks: 2 executed, 1 up-to-date
NetworkStack
SampleAppStack
```

## deploy all stacks

```shell script
$ cdk deploy --all
```

## clean up all stacks

```shell script
$ cdk destroy --all
```