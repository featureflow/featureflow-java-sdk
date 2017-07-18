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
    <version>0.0.15</version>
</dependency>
```

## Usage

### Quick start

Get your 'Server Environment Api Key' from the environment page in featureflow and instantiate a singleton client:

```java
String apiKey = "<Your Server Environment Api Key goes here>";
FeatureflowClient featureflow = FeatureflowClient.builder(apiKey).build();
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
FeatureflowContext context = FeatureflowContext.keyedContext("uniqueuserkey1")
    .withValue("tier", "silver")
    .withValue("age", 32)
    .withValue("signup_date", new DateTime(2017, 1, 1, 12, 0, 0, 0))    
    .withValue("name", "Joe User")
    .withValue("email", "user@featureflow.io")
    .withValues("user_role", Arrays.asList("pvt_tester", "administrator"))
    .build();

```
Context values can be of type `DateTime`, `Number`, `String` or `List<DateTime>`, `List<Number>`, `List<String>`

When a list of context values is passed in each rule may match any of the values, additionally each value is stored individually in featureflow for subsequent lookup in rule creation.

Evaluate by passing the context into the evaluate method:

```
featureflow.evaluate("example-feature", context).value());
```


Further documentation can be found [here](http://docs.featureflow.io/docs)


## About featureflow
* Featureflow is an application feature management tool that allows you to safely and effectively release, manage and evaluate your applications features across multiple applications, platforms and languages.
    * Dark / Silent Release with features turned off
    * Gradual rollout to a percent of users
    * Virtual Rollout and Rollback of features
    * Environment and Component feature itinerary
    * Target features to specific audiences
    * A/B and Multivariant test new feature variants - migrate to the winner
    All without devops, engineering or downtime.
* We have SDKs in the following languages
    * [Javascript] (https://github.com/featureflow/featureflow-javascript-sdk)
    * [Java] (https://github.com/featureflow/featureflow-java-sdk)
    * [NodeJS] (https://github.com/featureflow/featureflow-node-sdk)
    * [ReactJS] (https://github.com/featureflow/react-featureflow-client)
    * [angular] (https://github.com/featureflow/featureflow-ng)
    * [PHP] (https://github.com/featureflow/featureflow-php-sdk)
    * [.net] (https://github.com/featureflow/featureflow-dotnet-client)
* Find out more
    * [Docs] http://docs.featureflow.io/docs
    * [Web] https://www.featureflow.io/     


## Roadmap
- [x] Multiple callbacks
- [ ] Offline mode
- [ ] Read only mode

## License

Apache-2.0

[dependency-url]: https://www.featureflow.io
[dependency-img]: https://www.featureflow.io/wp-content/uploads/2016/12/featureflow-web.png