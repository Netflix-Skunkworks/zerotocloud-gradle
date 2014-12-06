Zero to Cloud Gradle Plugin
===

A Gradle Plugin to assist in the initialization of a NetflixOSS-based cloud and continuous delivery environment.

Getting Started - Initializing the Cloud
===

Clone this repository:
---

```
$ git clone git@github.com:Netflix-Skunkworks/zerotocloud-gradle.git
```

Install Locally:
---

```
$ cd zerotocloud-gradle
$ ./gradlew clean build install
```

Get yourself a set of AWS Credentials
---

Follow [this guide](http://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSGettingStartedGuide/AWSCredentials.html).

Export your Access Key and Secret Key as environment variables
---

```
$ export AWS_ACCESS_KEY_ID=<ACCESS_KEY_ID>
$ export AWS_SECRET_KEY=<SECRET_KEY_ID>
```

Use the Example Project to Run the Plugin's Cloud Initializer Task
---

```
$ cd example/
$ ./gradlew initCloud
```

Go get a coffee... Things are happening now.

What is Happening During Initialization?
===

During initialization, the plugin is setting your account up in a NetflixOSS-friendly way.

  * It is creating a new SSH key pair, which it will download to your `~/.ssh/` directory;
  * It is creating a Base IAM Role, which will be applied to instances that are deployed through the plugin;
  * It is creating a Full Privilege IAM Role, which will be applied to those instances needing more access (like Asgard);
  * It is creating a `base` security group, which will be applied to deployments performed through the plugin;
      - exposes ports 22 and 8080
  * In this order, it will:
      - Create a new instance for the Bakery; download and install [aminator](http://github.com/netflix/aminator) on that instance; and tag the instance for identification later;
      - Create a new instance for Eureka; configure eureka for deployment; download and install [eureka](http://github.com/netflix/eureka) on that instance; and tag the instance for identification later;
      - Create a new instance for Asgard; configure asgard for deployment; download and install [asgard](http://github.com/netflix/asgard) on that instance; and tag the instance for identification later;
      - For each of these, it will also create a respective security group

Using the Plugin for Continuous Delivery
===

The Bakery, Eureka, and Asgard are all components that will be used in a continuous delivery workflow. The plugin will
provide some "glue" between those components to enable you to perform truly continuous delivery, all from a Gradle build.

Similar to the example above, where you initialized your AWS account and setup the Bakery, Eureka, and Asgard, by just applying
the plugin to a vanilla Gradle build script, you can similarly apply the plugin to your project's build script to leverage
the baking and deployment services that are contained internally.

Consider the following Gradle build, which is a [Ratpack](http://ratpack.io) web application that we want to get deployed
to the cloud:

```groovy
buildscript {
  repositories {
    jcenter()
    mavenLocal()
  }
  dependencies {
    classpath 'io.ratpack:ratpack-gradle:0.9.11'
    classpath 'com.netflix.nebula:nebula-ospackage-plugin:2.0.+'
    classpath 'com.netflix.gradle.plugins:cloud:0.1-SNAPSHOT'
  }
}

apply plugin: 'idea'
apply plugin: 'io.ratpack.ratpack-groovy'
apply plugin: 'nebula-ospackage-application'
apply plugin: 'nebula-ospackage-daemon'
apply plugin: 'com.netflix.cloud'

repositories {
  jcenter()
}

dependencies {
  compile 'com.netflix.eureka:eureka-client:1.1.145'

  runtime 'org.apache.logging.log4j:log4j-slf4j-impl:2.0.1'
  runtime 'org.apache.logging.log4j:log4j-api:2.0.1'
  runtime 'org.apache.logging.log4j:log4j-core:2.0.1'
  runtime 'com.lmax:disruptor:3.3.0'
}

ospackage {
  from("${buildDir}/install") {
    into "/apps/${project.name}"
  }

  requires("openjdk-7-jdk")

  // will override to Java 8
  preInstall file("scripts/preInstall.sh")
}

daemon {
  daemonName = "${project.name}"
  command = "/apps/${project.name}/bin/${project.name}"
}

task bake << {
  def amiId = bakery.bakeryService.bake("${bakery.pubDir}/${project.tasks.buildDeb.archivePath.name}")
  asgard.asgardService.lastAmi = amiId
}

task deploy << {
  asgard.asgardService.deployBuilder()
    .app("eutest")
    .region("us-east-1")
    .capacity(1, 1, 1)
    .zones(["us-east-1b"])
    .keyName(cloud.keyPairName)
    .securityGroups([cloud.baseSecurityGroup])
    .instanceType('m1.small')
    .instanceProfile(cloud.instanceProfileName)
    .build().deploy()
}

project.tasks.buildDeb.dependsOn = ['installApp']
project.tasks.publishDebs.dependsOn = ['buildDeb']
project.tasks.bake.dependsOn = ['publishDebs']
project.tasks.deploy.dependsOn = ['bake']
```

This build script will utilize the [Nebula OS Package Plugin](https://github.com/nebula-plugins/nebula-ospackage-plugin/) to generate
a .deb file, which will be the OS package that will be baked into our server group image.

The `bake` task utilizes the `BakeryService` to:
  * publish the os package to the Bakery server;
  * invoke aminator on the Bakery server to create an AMI for distribution;
  * once the AMI is created, its ID will be placed in state on the `AsgardService`, which will be used for deployment

The `deploy` task has been programmatically added, which will utilize the `AsgardService`'s fluent deployment API to deploy
our application. In the case where a server group already exists, a [Red/Black](http://techblog.netflix.com/2013/08/deploying-netflix-api.html) deployment
will be performed, allowing us to easily roll-back.

The lines at the bottom of the script ensure that the tasks are executed in proper order.

Configuration
===

Every aspect of the plugin and its contained services is configurable using convention mappings:

Configure Base Variables:
---

```groovy
cloud {
  iamRole = "BaseIAMRole"
  instanceProfileName = "BaseIAMRole_InstanceProfile"
  keyPairName = "nf-oss"
  assumeRolePolicyDoc = getClass().getResourceAsStream("/assumeRolePolicy.json").text
  iamPolicyDoc = getClass().getResourceAsStream("/baseIamPolicy.json").text
  region = "us-east-1"
  remoteUser = "ubuntu"
  baseSecurityGroup = "base"
  baseAmiArch = "amd64"
  baseAmiHypervisor = "xen"
  baseAmiOwner = "099720109477" /* Canonical */
  baseAmiName = "ubuntu/images/ebs/ubuntu-trusty-14.04-amd64-server" /* We'll lookup the latest one... */
  baseAmiIsPublic = true
  sshKey = new File(new File(System.properties['user.home'], '.ssh'), keyPairName)
}
```

Configure Bakery Variables
---

```groovy
bakery {
  // all of "cloud", plus:
  name = "bakery"
  instanceType = "m3.medium"
  securityGroup = "bakery"
  checkPort = 22 /* informs the initializer to wait for this port to be available before considering success */
  bakeryEnvironment = "ec2_aptitude_linux"
  pubDir = "/repo" /* this is the directory where .deb files will be "published */
}
```

Configure Eureka Variables
---

```groovy
eureka {
  // all of "cloud", plus:
  instanceType = "m3.medium"
  securityGroup = "eureka"
  checkPort = 8080
  availabilityZones = ['us-east-1a', 'us-east-1b', 'us-east-1c']
}
```

Configure Asgard Variables
---

```groovy
asgard {
  // all of "cloud", except these are overridden:
  iamPolicyDoc = getClass().getResourceAsStream("/fullPrivilegeIamPolicy.json").text
  iamRole = "AsgardIAMRole"
  instanceProfileName = "AsgardIAMRole_InstanceProfile"
  // add't config:
  instanceType = "m3.large"
  securityGroup = "asgard"
  checkPort = 8080
}
```

Authors
===

Dan Woods

License
===

See LICENSE.txt
