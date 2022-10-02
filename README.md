# woob-bank-slack

Monitor your bank accounts in Slack.

## Docker instructions

### Building and pushing the image to Docker Hub

```
docker image rm bodlulu/woob-bank-slack:latest
DOCKER_USERNAME=<your docker hub login> DOCKER_PASSWORD=<your docker hub password> ./gradlew dockerPushImage
```

### Running the image

```
docker pull bodlulu/woob-bank-slack
docker run -v /path/to/where/your/backends/file/is:/root/.config/woob bodlulu/woob-bank-slack -w /usr/local/bin -s slacktoken -c channel "Account 1 name:account1id@bank1id" "Account 2 name:account2id@bank2id"
```
