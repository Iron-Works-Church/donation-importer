Donation Importer
=====
Imports donations from Tithely to SimpleChurch.

What it does
-----
* Fetch deposited charges from Tithely
* Find corresponding existing transactions in SimpleChurch
* Import remaining transactions into SimpleChurch 
* Created batches based on deposit date and payment method 

How to build
-----
The build configuration is in `build.gradle.kts` and is executed using `./gradlew build` 
