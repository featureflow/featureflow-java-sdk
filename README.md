# featureflow-java-sdk

[![][dependency-img]][dependency-url]

> Featureflow Java Client SDK

Get your Featureflow account at [featureflow.io](http://www.featureflow.io)

## Get Started

The easiest way to get started is to follow the [Featureflow quick start guides](http://docs.featureflow.io/docs)

## Change Log

Please see [CHANGELOG](https://github.com/featureflow/featureflow-java-sdk/blob/master/CHANGELOG.md).

## Installation

Using Maven
```xml
<dependency>
    <groupId>io.featureflow</groupId>
    <artifactId>featureflow-java-sdk</artifactId>
    <version>0.0.4-SNAPSHOT</version>
</dependency>
```

## Usage

### Quick start

Get your 'Server Environment Api Key' from the environment page in featureflow and instantiate a singleton client:

```java
String apiKey = "<Your Server Environment Api Key goes here>";
FeatureFlowClient featureFlowClient = FeatureFlowClient.builder(apiKey).build();
```
This is a singleton, so if you're using spring you should make it a @Bean in a @Configuration class.

In your code, you can test the value of your feature where the value of `my-feature-key` is equal to `'on'` 
```java
  if (featureflow.evaluate('my-feature-key', context).is('on')){
    // this feature code will be run because 'my-feature-key' is set to 'on'
  }
```

Because the default variants for any feature are `'on'` and `'off'`, we have provided two helper methods `.isOn()` and `.isOff()`

```java

if(featureflow.evaluate('my-feature-key', context).isOn()){
  // this feature code will be run because 'my-feature-key' is set to 'on'
}

if(featureflow.evaluate('my-feature-key', context).isOff()){
  // this feature code won't be run because 'my-feature-key' is not set to 'off'
}
```

### Adding Context
You can pass context information in to allow features to be targeted.
At the point in time of evaluation (e.g. on a rest call or other call) you can create and pass in context information by builsing a `FeatureflowContext` object. We have a builder to help:

```aidl
FeatureFlowContext context = FeatureFlowContext.keyedContext("uniqueuserkey1")
    .withValue("tier", "silver")
    .withValue("age", 32)
    .withValue("signup_date", new DateTime(2017, 1, 1, 12, 0, 0, 0))
    .withValue("user_role", "standard_user")
    .withValue("name", "Joe User")
    .withValue("email", "user@featureflow.io")
    .build();

```
Context values can be of type `DateTime`, `Number`, `String` or `List<DateTime>`, `List<Number>`, `List<String>`

When a list of context values is passed in each rule may match any of the values, additionally each value is stored individually in featureflow for subsequent lookup in rule creation.

Evaluate by passing the context into the evaluate method:

```
featureFlowClient.evaluate("example-feature", context).value());
```


Further documentation can be found [here](http://docs.featureflow.io/docs)

## Roadmap
- [x] Write documentation
- [x] Release to sonatype snapshot
- [ ] Release to sonatype releases

## License

Apache-2.0

[dependency-url]: https://www.featureflow.io
[dependency-img]: https://www.featureflow.io/wp-content/uploads/2016/12/featureflow-web.png