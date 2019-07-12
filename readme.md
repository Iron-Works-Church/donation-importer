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

Deployment
-----
With AWS credentials configured and the aws cli installed, run `deploy.sh`, which packages the CloudFormation template
and deploys the stack. This consists of a Lambda to execute the job and a schedule to run it weekly.