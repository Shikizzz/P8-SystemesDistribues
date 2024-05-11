This is an optimization project for OpenClassroom formation, using concurrency.


To install dependencies, use these command lines :

mvn install:install-file -Dfile=libs/gpsUtil.jar -DgroupId=gpsUtil -DartifactId=gpsUtil -Dversion=1.0.0 -Dpackaging=jar
mvn install:install-file -Dfile=libs/RewardCentral.jar -DgroupId=rewardCentral -DartifactId=rewardCentral -Dversion=1.0.0 -Dpackaging=jar
mvn install:install-file -Dfile=libs/TripPricer.jar -DgroupId=tripPricer -DartifactId=tripPricer -Dversion=1.0.0 -Dpackaging=jar


For integration pipeline, I used Jenkins running in a Docker container.

To install jenkins, you can use this documentation : https://www.jenkins.io/doc/book/installing/docker/

Then, you need to install maven tool.
Do : Manage Jenkins > Tools > click on Add Maven, check Install from Apache (with last version), name it Maven_Home and save.

Then you can create a new pipeline, and use the script from jenkins/Jenkinsfile in this project.

