# featureflow-java-sdk

[![][dependency-img]][dependency-url]

> Featureflow Javascript Client SDK

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

Get your 'Server Environment Api Key' frmo the environment page in featureslow and instansiate a singleton clinet:

```java
String apiKey = "<Your Server Environment Api Key goes here>";
FeatureFlowClient featureFlowClient = FeatureFlowClient.builder(apiKey).build();
```
This is a singleton so if you're using spring for example you might want to mate is a @Bean in an @Configuration class.

In your code, you can test the value of your feature where the value of `my-feature-key` is equal to `'on'` 
```java
  if (featureflow.evaluate('my-feature-key').is('on')){
    // this feature code will be run because 'my-feature-key' is set to 'on'
  }
```

Because the default variants for any feature are `'on'` and `'off'`, we have provided two helper methods `.isOn()` and `.isOff()`

```java

if(featureflow.evaluate('my-feature-key').isOn()){
  // this feature code will be run because 'my-feature-key' is set to 'on'
}

if(featureflow.evaluate('my-feature-key').isOff()){
  // this feature code won't be run because 'my-feature-key' is not set to 'off'
}
```

Further documentation can be found [here](http://docs.featureflow.io/docs)

## Roadmap
- [x] Write documentation
- [x] Release to sonatype snapshot
- [ ] Release to sonatype releases

## License

Apache-2.0

[npm-url]: https://nodei.co/npm/featureflow-client
[npm-img]: https://nodei.co/npm/featureflow-client.png

[dependency-url]: https://www.featureflow.io
[dependency-img]: https://www.featureflow.io/wp-content/uploads/2016/12/featureflow-web.png