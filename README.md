# spotify-party-api

This repository is a collection of modules that make up the Spotify Party API for what used to be hosted at awsum.io. It is now defunct.

The project goal was to build an application that allows users to synchronise their Spotify playout, allowing them to host a party that people can join in with remotely.

Later on we also added support for YouTube parties, where multiple people could watch the same video at the same time, together.

## Key features
- [x] Play Spotify in a synchronised way between multiple users in a party
- [x] Play YouTube videos in a synchronised way between multiple users in a party
- [x] Users can chat with each other via a chatbox function
- [x] Users can queue songs adding them to the party playlist
- [x] Users can vote on songs to move them around in the party playlist
- [x] Users can vote on playing songs to skip them (majority vote)

## Modules

### database
This module contains a shared DAL used by the other modules.

### party-api
The meat of the application handling API calls by the frontend and orchestrating playout

### party-bot
This module is used for creating bot-led parties and was used to provide some always-on content while the user count was low.

### queue-lib
Implements some utility classes for RabbitMQ