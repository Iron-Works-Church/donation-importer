Donation Importer
=====
Imports donations from Tithely to SimpleChurch.

What it does
-----
* Fetch deposited charges from Tithely
* Find corresponding existing transactions in SimpleChurch
* Import remaining transactions into SimpleChurch 
* Created batches based on deposit date

How to build
-----
The build configuration is in `build.gradle.kts` and is executed using `./gradlew build` 

How to run
-----
Need a credentials-live.properties file in src/main/resources based on credentials.properties.sample

Then run AppKt, or by using `./gradlew run`, or execute the distribution (produced as build/distributions/appname.tar) 
using `./bin/appname`  